stalln@echo off
REM Backend Startup Script for Karmika HRMS

echo.
echo ============================================
echo   Karmika HRMS - Backend Startup Script
echo ============================================
echo.

REM Change to backend directory
cd /d "e:\Workspace\Karmika\backend"

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

echo [1/4] Checking Java version...
java -version

echo.
echo [2/4] Cleaning previous builds...
call mvn clean -q

echo.
echo [3/4] Building the application...
call mvn package -DskipTests -q

if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    echo Running with verbose output...
    call mvn compile
    pause
    exit /b 1
)

echo.
echo [4/4] Starting the application...
echo.
echo Backend will start on: http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.
echo Default Credentials:
echo   Admin - Username: admin, Password: admin123
echo   HR - Username: hr, Password: hr123
echo.
pause

REM Start the application
java -jar target/hrms-1.0.0.jar

pause

