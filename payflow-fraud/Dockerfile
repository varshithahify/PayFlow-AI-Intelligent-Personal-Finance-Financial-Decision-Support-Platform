# ==========================================
# Python Fraud Detection Service
# ==========================================

FROM python:3.14-slim

# ==========================================
# Python Runtime Settings
# ==========================================

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# ==========================================
# Operating System Security Updates
# ==========================================

RUN apt-get update \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/*

# ==========================================
# Install Python Dependencies
# ==========================================

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt \
    && find /usr/local/lib -path '*/pip/_vendor/bom.cdx.json' -delete

# ==========================================
# Copy Application Source
# ==========================================

COPY . .

# ==========================================
# Create Non-Root Application User
# ==========================================

RUN groupadd --system --gid 10001 app \
    && useradd --system \
       --uid 10001 \
       --gid app \
       --home-dir /app \
       --shell /usr/sbin/nologin app \
    && chown -R app:app /app

# ==========================================
# Run as Non-Root User
# ==========================================

USER app

# ==========================================
# FastAPI Service Port
# ==========================================

EXPOSE 8083

# ==========================================
# Container Health Check
# ==========================================

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=20s \
    --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8083/health', timeout=3)" || exit 1

# ==========================================
# Start FastAPI Application
# ==========================================

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8083"]