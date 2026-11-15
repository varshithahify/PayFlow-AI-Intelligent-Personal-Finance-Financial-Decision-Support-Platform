import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import main


client = TestClient(main.app)


# ---------------------------------------------------------
# Mock fraud model
# ---------------------------------------------------------

class MockFraudModel:

    def predict_proba(self, features):
        assert len(features) == 1
        return [[0.9, 0.1]]


# ---------------------------------------------------------
# Health endpoint tests
# ---------------------------------------------------------

def test_health_endpoint(monkeypatch):

    monkeypatch.setattr(
        main.redis_client,
        "ping",
        lambda: True
    )

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "UP"
    assert response.json()["service"] == "fraud-service"
    assert response.json()["redis"] == "UP"
    assert response.json()["model"] == "loaded"


def test_health_when_redis_unavailable(monkeypatch):

    def raise_connection_error():
        raise ConnectionError("Redis unavailable")

    monkeypatch.setattr(
        main.redis_client,
        "ping",
        raise_connection_error
    )

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["redis"] == "DOWN"


# ---------------------------------------------------------
# Fraud scoring tests
# ---------------------------------------------------------

def test_successful_fraud_scoring(monkeypatch):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    response = client.post(
        "/score",
        json={"TransactionAmt": 100}
    )

    assert response.status_code == 200

    data = response.json()

    assert data["action"] == "ALLOW"
    assert data["score"] == 6.0
    assert data["ml_score"] == 10.0
    assert data["velocity_score"] == 0.0
    assert data["triggered_rules"] == []


def test_high_amount_rule(monkeypatch):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    response = client.post(
        "/score",
        json={"TransactionAmt": 150000}
    )

    assert response.status_code == 200

    data = response.json()

    assert data["action"] == "ALLOW"
    assert "HIGH_AMOUNT" in data["triggered_rules"]


def test_missing_transaction_amount():

    response = client.post(
        "/score",
        json={}
    )

    assert response.status_code == 422


# ---------------------------------------------------------
# Redis failure test
# ---------------------------------------------------------

def test_redis_failure_does_not_stop_scoring(monkeypatch):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    def raise_redis_error(*args, **kwargs):
        raise ConnectionError("Redis unavailable")

    monkeypatch.setattr(
        main.redis_client,
        "eval",
        raise_redis_error
    )

    response = client.post(
        "/score",
        json={
            "TransactionAmt": 100,
            "card_hash": "synthetic-test-card"
        }
    )

    assert response.status_code == 200
    assert response.json()["action"] == "ALLOW"
    assert response.json()["velocity_score"] == 0.0


# ---------------------------------------------------------
# Transaction amount validation tests
# ---------------------------------------------------------

@pytest.mark.parametrize(
    "amount",
    [0, -1, -500]
)
def test_invalid_transaction_amounts(amount):

    response = client.post(
        "/score",
        json={"TransactionAmt": amount}
    )

    assert response.status_code == 422


@pytest.mark.parametrize(
    "amount",
    ["invalid", "NaN", "Infinity", "-Infinity"]
)
def test_non_numeric_or_non_finite_amounts(amount):

    response = client.post(
        "/score",
        json={"TransactionAmt": amount}
    )

    assert response.status_code == 422


def test_valid_positive_transaction_amount(monkeypatch):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    response = client.post(
        "/score",
        json={"TransactionAmt": 100}
    )

    assert response.status_code == 200


# ---------------------------------------------------------
# Redis atomic velocity counter tests
# ---------------------------------------------------------

def test_velocity_counter_script_increments_atomically(monkeypatch):

    calls = []

    def mock_eval(script, numkeys, key, expiry):

        calls.append({
            "script": script,
            "numkeys": numkeys,
            "key": key,
            "expiry": expiry
        })

        return 6

    monkeypatch.setattr(
        main.redis_client,
        "eval",
        mock_eval
    )

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    response = client.post(
        "/score",
        json={
            "TransactionAmt": 100,
            "card_hash": "velocity-test-card"
        }
    )

    assert response.status_code == 200
    assert len(calls) == 1

    assert calls[0]["numkeys"] == 1
    assert calls[0]["key"] == "velocity:velocity-test-card:1hr"
    assert calls[0]["expiry"] == main.VELOCITY_WINDOW_SECONDS

    assert response.json()["velocity_score"] == 40.0
    assert "HIGH_VELOCITY" in response.json()["triggered_rules"]


# ---------------------------------------------------------
# Velocity threshold tests
# ---------------------------------------------------------

@pytest.mark.parametrize(
    "transaction_count, expected_velocity_score",
    [
        (1, 0.0),
        (5, 0.0),
        (6, 40.0),
        (10, 40.0)
    ]
)
def test_velocity_threshold(
    monkeypatch,
    transaction_count,
    expected_velocity_score
):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    monkeypatch.setattr(
        main.redis_client,
        "eval",
        lambda *args, **kwargs: transaction_count
    )

    response = client.post(
        "/score",
        json={
            "TransactionAmt": 100,
            "card_hash": f"threshold-test-{transaction_count}"
        }
    )

    assert response.status_code == 200

    assert (
        response.json()["velocity_score"]
        == expected_velocity_score
    )


# ---------------------------------------------------------
# High velocity decision policy test
# ---------------------------------------------------------

def test_high_velocity_forces_flag(monkeypatch):

    monkeypatch.setattr(
        main,
        "fraud_model",
        MockFraudModel()
    )

    monkeypatch.setattr(
        main.redis_client,
        "eval",
        lambda *args, **kwargs: 6
    )

    response = client.post(
        "/score",
        json={
            "TransactionAmt": 100,
            "card_hash": "high-velocity-policy-test"
        }
    )

    assert response.status_code == 200

    data = response.json()

    assert "HIGH_VELOCITY" in data["triggered_rules"]
    assert data["velocity_score"] == 40.0
    assert data["action"] == "FLAG"

def test_high_risk_transaction_forces_block(monkeypatch):
    class HighRiskFraudModel:
        def predict_proba(self, features):
            return [[0.01, 0.99]]

    monkeypatch.setattr(
        main,
        "fraud_model",
        HighRiskFraudModel()
    )

    monkeypatch.setattr(
        main.redis_client,
        "eval",
        lambda *args, **kwargs: 6
    )

    response = client.post(
        "/score",
        json={
            "TransactionAmt": 150000,
            "card_hash": "block-test-card",
            "features": {}
        }
    )

    assert response.status_code == 200

    result = response.json()

    assert result["score"] >= 75
    assert result["action"] == "BLOCK"
