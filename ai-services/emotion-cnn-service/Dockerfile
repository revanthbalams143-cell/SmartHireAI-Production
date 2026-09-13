FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py .
COPY training ./training
COPY model ./model
EXPOSE 8095
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8095}"]
