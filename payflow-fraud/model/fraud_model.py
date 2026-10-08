import os
import joblib
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score
from xgboost import XGBClassifier


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

DATASET_PATH = os.path.join(
    os.path.dirname(os.path.dirname(__file__)),
    "dataset",
    "train_transaction.csv"
)

MODEL_PATH = os.path.join(
    os.path.dirname(__file__),
    "fraud_model.pkl"
)

FEATURES = [
    "TransactionAmt",
    "card1",
    "card2",
    "addr1",
    "dist1",
    "C1",
    "C2",
    "V1",
    "V2",
    "V3"
]

TARGET = "isFraud"


# ---------------------------------------------------------
# Train Fraud Detection Model
# ---------------------------------------------------------

def train_model():
    print("=" * 60)
    print("PayFlow AI - Fraud Detection Model Training")
    print("=" * 60)

    print("\n[1/6] Loading dataset...")
    print(f"Dataset: {DATASET_PATH}")

    # Load only the required columns to reduce memory usage.
    required_columns = FEATURES + [TARGET]

    df = pd.read_csv(
        DATASET_PATH,
        usecols=required_columns
    )

    print(f"Loaded rows: {len(df):,}")
    print(f"Loaded columns: {len(df.columns)}")

    # -----------------------------------------------------
    # Prepare features
    # -----------------------------------------------------

    print("\n[2/6] Preparing features...")

    X = df[FEATURES].copy()
    y = df[TARGET].copy()

    # Make sure all model features are numeric.
    for column in FEATURES:
        X[column] = pd.to_numeric(
            X[column],
            errors="coerce"
        )

    # Fill missing values using median values.
    # Median values are calculated from the complete dataset
    # before the train/test split.
    feature_medians = X.median()

    X = X.fillna(feature_medians)

    print("Missing values handled.")
    print(f"Fraud transactions: {y.sum():,}")
    print(f"Non-fraud transactions: {(y == 0).sum():,}")

    # -----------------------------------------------------
    # Train/Test Split
    # -----------------------------------------------------

    print("\n[3/6] Splitting dataset...")

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.20,
        random_state=42,
        stratify=y
    )

    print(f"Training rows: {len(X_train):,}")
    print(f"Testing rows:  {len(X_test):,}")

    # -----------------------------------------------------
    # XGBoost Model
    # -----------------------------------------------------

    print("\n[4/6] Training XGBoost model...")

    model = XGBClassifier(
        n_estimators=300,
        max_depth=6,
        learning_rate=0.05,
        scale_pos_weight=30,
        objective="binary:logistic",
        eval_metric="auc",
        random_state=42,
        n_jobs=2
    )

    model.fit(
        X_train,
        y_train
    )

    print("Model training completed.")

    # -----------------------------------------------------
    # Evaluation
    # -----------------------------------------------------

    print("\n[5/6] Evaluating model...")

    probabilities = model.predict_proba(X_test)[:, 1]

    roc_auc = roc_auc_score(
        y_test,
        probabilities
    )

    print(f"ROC-AUC: {roc_auc:.4f}")

    # -----------------------------------------------------
    # Save model + preprocessing information
    # -----------------------------------------------------

    print("\n[6/6] Saving model...")

    model_package = {
        "model": model,
        "features": FEATURES,
        "feature_medians": feature_medians.to_dict()
    }

    joblib.dump(
        model_package,
        MODEL_PATH
    )

    print(f"Model saved to: {MODEL_PATH}")

    print("\n" + "=" * 60)
    print("Fraud model training completed successfully.")
    print("=" * 60)


if __name__ == "__main__":
    train_model()