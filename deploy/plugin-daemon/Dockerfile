FROM python:3.12-slim
WORKDIR /app
COPY plugin_daemon_v3.py /app/
RUN pip install --no-cache-dir requests
EXPOSE 5002
CMD ["python", "plugin_daemon_v3.py"]
