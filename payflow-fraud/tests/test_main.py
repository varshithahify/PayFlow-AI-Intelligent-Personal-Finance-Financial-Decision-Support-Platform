import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import main

from fastapi.testclient import TestClient


client = TestClient(main.app)


class MockFraudModel:
    def predict_proba(self, features):
        assert len(features) == 1
        return [[0.9, 0.1]]


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
        "incr",
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
