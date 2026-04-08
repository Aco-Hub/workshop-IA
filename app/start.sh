#!/usr/bin/env bash
set -euo pipefail

# Timesheet Application Startup Script
# Usage: ./start.sh
# Press Ctrl+C to stop all services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Use bundled JDK from VS Code extension if JAVA_HOME is invalid
if [ ! -x "${JAVA_HOME:-}/bin/javac" ]; then
    VSCODE_JDK="$HOME/.vscode/extensions/redhat.java-1.53.0-linux-x64/jre/21.0.10-linux-x86_64"
    if [ -x "$VSCODE_JDK/bin/javac" ]; then
        export JAVA_HOME="$VSCODE_JDK"
    fi
fi
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"

# Detect local IP for display purposes
LOCAL_IP=$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7; exit}')
HOST_DISPLAY="${LOCAL_IP:-localhost}"

BACKEND_PID=""
FRONTEND_PID=""
DOCKER_PG=false

# Default database config
DB_PORT="${DB_PORT:-5432}"
DB_HOST="${DB_HOST:-localhost}"
DB_NAME="${DB_NAME:-timesheet}"
DB_USER="${DB_USERNAME:-postgres}"
DB_PASS="${DB_PASSWORD:-postgres}"

cleanup() {
    echo ""
    echo "Shutting down services..."
    if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo "Stopping frontend (PID $FRONTEND_PID)..."
        kill "$FRONTEND_PID" 2>/dev/null || true
        wait "$FRONTEND_PID" 2>/dev/null || true
    fi
    if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "Stopping backend (PID $BACKEND_PID)..."
        kill "$BACKEND_PID" 2>/dev/null || true
        wait "$BACKEND_PID" 2>/dev/null || true
    fi
    echo "All services stopped."
}

trap cleanup EXIT INT TERM

echo "=== Timesheet Application ==="
echo ""

# Step 1: Check PostgreSQL
echo "[1/6] Checking PostgreSQL on port $DB_PORT..."
if ! PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "SELECT 1;" >/dev/null 2>&1; then
    # Try Docker if available
    if command -v docker >/dev/null 2>&1; then
        echo "Starting PostgreSQL via Docker..."
        docker run -d --name timesheet-db \
            -e POSTGRES_USER="$DB_USER" \
            -e POSTGRES_PASSWORD="$DB_PASS" \
            -e POSTGRES_DB="$DB_NAME" \
            -p "$DB_PORT":5432 \
            postgres:18 2>/dev/null || {
                docker start timesheet-db 2>/dev/null || true
            }
        DOCKER_PG=true
        echo "Waiting for PostgreSQL to be ready..."
        for i in $(seq 1 30); do
            if PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "SELECT 1;" >/dev/null 2>&1; then
                break
            fi
            sleep 1
        done
    elif pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
        # Native PostgreSQL is running but credentials don't match — set them up
        echo "Setting up PostgreSQL user and password (requires sudo)..."
        sudo -u postgres psql -c "ALTER USER $DB_USER WITH PASSWORD '$DB_PASS';" 2>/dev/null || \
            sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS' CREATEDB SUPERUSER;" 2>/dev/null || true
    fi
fi

if PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" >/dev/null 2>&1; then
    echo "PostgreSQL is ready."
else
    echo "ERROR: Cannot connect to PostgreSQL on port $DB_PORT"
    exit 1
fi

# Step 2: Create database if needed
echo "[2/6] Ensuring database '$DB_NAME' exists..."
PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1 || \
    PGPASSWORD="$DB_PASS" createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" 2>/dev/null || true
echo "Database ready."

# Step 3: Kill any existing process on port 8082
echo "[3/7] Checking for existing processes on ports 8082/5173..."
for port in 8082 5173; do
    if lsof -ti:$port >/dev/null 2>&1; then
        echo "Killing existing process on port $port..."
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
done
sleep 1

# Step 4: Build backend
echo "[4/7] Building backend..."
cd "$BACKEND_DIR"
./gradlew build -x test --no-daemon --quiet 2>&1

# Step 5: Start backend
echo "[5/7] Starting backend on http://$HOST_DISPLAY:8082..."
DB_URL="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
DB_USERNAME="$DB_USER" \
DB_PASSWORD="$DB_PASS" \
./gradlew bootRun --no-daemon --quiet 2>&1 &
BACKEND_PID=$!

# Step 6: Wait for backend health check
echo "[6/7] Waiting for backend health check..."
for i in $(seq 1 60); do
    if curl -sf http://localhost:8082/actuator/health >/dev/null 2>&1; then
        echo "Backend is healthy."
        break
    fi
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo "ERROR: Backend process died."
        exit 1
    fi
    sleep 1
done

if ! curl -sf http://localhost:8082/actuator/health >/dev/null 2>&1; then
    echo "ERROR: Backend failed to start within 60 seconds."
    exit 1
fi

# Step 7: Start frontend
echo "[7/7] Starting frontend on http://$HOST_DISPLAY:5173..."
cd "$FRONTEND_DIR"
npm install --silent 2>/dev/null
NG_CLI_ANALYTICS=false npx ng serve --port 5173 --host 0.0.0.0 --proxy-config proxy.conf.json </dev/null 2>&1 &
FRONTEND_PID=$!

echo ""
echo "==================================="
echo " Timesheet is running!"
echo " Frontend: http://$HOST_DISPLAY:5173"
echo " Backend:  http://$HOST_DISPLAY:8082"
echo " Login:    albin@test.com / Vernhes"
echo "==================================="
echo ""
echo "Press Ctrl+C to stop all services."

# Wait for any child to exit
wait
