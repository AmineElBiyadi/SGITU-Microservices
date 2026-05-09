FROM python:3.11-slim

WORKDIR /app

# Create a non-root user
RUN adduser --disabled-password --gecos "" appuser

# Create and activate a virtual environment
ENV VIRTUAL_ENV=/app/venv
RUN python -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Install dependencies inside the venv (no root warning, no pip notice)
COPY requirements.txt .
RUN pip install --upgrade pip --quiet && \
    pip install --no-cache-dir -r requirements.txt

COPY . .

# Switch to non-root user before running
USER appuser

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5000"]

