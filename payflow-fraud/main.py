import os

import joblib
import pandas as pd
import redis
from fastapi import FastAPI
from pydantic import BaseModel, Field


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

MODEL_PATH = os.path.join(
    os.path.dirname(__file__),
    "model",
    "fraud_model.pkl"
)

REDIS_URL = os.getenv(
    "REDIS_URL",
    "redis://localhost:6379"
)

VELOCITY_WINDOW_SECONDS = 60 * 60

VELOCITY_LIMIT = 5

HIGH_AMOUNT_LIMIT = 100000


# ---------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------

app = FastAPI(
    title="PayFlow AI Fraud Service",
    version="0.1.0"
)


# ---------------------------------------------------------
# Load trained model
# ---------------------------------------------------------

model_package = joblib.load(MODEL_PATH)

fraud_model = model_package["model"]
MODEL_FEATURES = model_package["features"]
FEATURE_MEDIANS = model_package["feature_medians"]


# ---------------------------------------------------------
# Redis connection
# ---------------------------------------------------------

redis_client = redis.Redis.from_url(
    REDIS_URL,
    decode_responses=True
)


# ---------------------------------------------------------
# Request / Response models
# ---------------------------------------------------------

class FraudScoreRequest(BaseModel):
    transaction_amt: float = Field(..., alias="TransactionAmt")
    card1: float | None = None
    card2: float | None = None
    addr1: float | None = None
    dist1: float | None = None
    c1: float | None = None
    c2: float | None = None
    v1: float | None = None
    v2: float | None = None
    v3: float | None = None

    card_hash: str | None = None
    email: str | None = None

    class Config:
        populate_by_name = True


class FraudScoreResponse(BaseModel):
    score: float
    action: str
    ml_score: float
    velocity_score: float
    triggered_rules: list[str]


# ---------------------------------------------------------
# Health check
# ---------------------------------------------------------

@app.get("/health")
def health_check():
    redis_status = "UP"

    try:
        redis_client.ping()
    except Exception:
        redis_status = "DOWN"

    return {
        "status": "UP",
        "service": "fraud-service",
        "redis": redis_status,
        "model": "loaded"
    }


# ---------------------------------------------------------
# Fraud scoring
# ---------------------------------------------------------

@app.post("/score", response_model=FraudScoreResponse)
def score_transaction(request: FraudScoreRequest):

    # -----------------------------------------------------
    # Build model input
    # -----------------------------------------------------

    feature_values = {
        "TransactionAmt": request.transaction_amt,
        "card1": request.card1,
        "card2": request.card2,
        "addr1": request.addr1,
        "dist1": request.dist1,
        "C1": request.c1,
        "C2": request.c2,
        "V1": request.v1,
        "V2": request.v2,
        "V3": request.v3,
    }

    features = pd.DataFrame(
        [[feature_values.get(feature) for feature in MODEL_FEATURES]],
        columns=MODEL_FEATURES
    )

    # Convert everything to numeric.
    for column in MODEL_FEATURES:
        features[column] = pd.to_numeric(
            features[column],
            errors="coerce"
        )

    # Apply the same median values used during training.
    features = features.fillna(FEATURE_MEDIANS)

    # -----------------------------------------------------
    # ML fraud score
    # -----------------------------------------------------

    ml_probability = fraud_model.predict_proba(
        features
    )[0][1]

    ml_score = float(ml_probability * 100)

    # -----------------------------------------------------
    # Velocity rule
    # -----------------------------------------------------

    velocity_score = 0.0
    triggered_rules = []

    velocity_identifier = (
        request.card_hash
        or request.email
    )

    if velocity_identifier:

        velocity_key = (
            f"velocity:{velocity_identifier}:1hr"
        )

        try:
            transaction_count = redis_client.incr(
                velocity_key
            )

            if transaction_count == 1:
                redis_client.expire(
                    velocity_key,
                    VELOCITY_WINDOW_SECONDS
                )

            if transaction_count > VELOCITY_LIMIT:
                velocity_score = 40.0
                triggered_rules.append(
                    "HIGH_VELOCITY"
                )

        except Exception:
            # Fraud scoring should remain available even
            # when Redis is temporarily unavailable.
            transaction_count = 0

    # -----------------------------------------------------
    # High amount rule
    # -----------------------------------------------------

    if request.transaction_amt > HIGH_AMOUNT_LIMIT:
        triggered_rules.append(
            "HIGH_AMOUNT"
        )

        # The guide specifies +20 for high amount.
        amount_score = 20.0
    else:
        amount_score = 0.0

    # -----------------------------------------------------
    # Geographic score
    # -----------------------------------------------------

    # Phase 6 currently has no geographic input.
    geo_score = 0.0

    # -----------------------------------------------------
    # Final score
    # -----------------------------------------------------

    final_score = (
        ml_score * 0.6
        + velocity_score * 0.3
        + geo_score * 0.1
        + amount_score
    )

    final_score = min(
        final_score,
        100.0
    )

    # -----------------------------------------------------
    # Fraud action
    # -----------------------------------------------------

    if final_score >= 75:
        action = "BLOCK"
    elif final_score >= 50:
        action = "FLAG"
    else:
        action = "ALLOW"

    return FraudScoreResponse(
        score=round(final_score, 2),
        action=action,
        ml_score=round(ml_score, 2),
        velocity_score=round(velocity_score, 2),
        triggered_rules=triggered_rules
    )