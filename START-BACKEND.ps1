r#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Karmika HRMS Backend Startup Script

.DESCRIPTION
    Builds and starts the Karmika HRMS backend server

.NOTES
    Requires: Maven, Java 17+, MySQL
#>

Write-Host @"
============================================
  Karmika HRMS - Backend Startup Script
============================================
"@ -ForegroundColor Cyan

# Change to backend directory
Set-Location "e:\Workspace\Karmika\backend"

# Check if Java is installed
try {
    $javaVersion = java -version 2>&1
    Write-Host "[✓] Java found" -ForegroundColor Green
} catch {
    Write-Host "[✗] Java not found or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 17 or higher"
    Read-Host "Press Enter to exit"
    exit 1
}

# Display Java version
Write-Host ""
Write-Host "Java Version:"
java -version 2>&1 | ForEach-Object { Write-Host $_ }

# Clean previous builds
Write-Host ""
Write-Host "[1/4] Cleaning previous builds..." -ForegroundColor Yellow
mvn clean -q

# Build the application
Write-Host "[2/4] Building the application..." -ForegroundColor Yellow
mvn package -DskipTests -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "[✗] Build failed. Running verbose build..." -ForegroundColor Red
    mvn compile
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "[✓] Build successful" -ForegroundColor Green

# Start the application
Write-Host ""
Write-Host "[3/4] Starting the application..." -ForegroundColor Yellow
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Karmika HRMS Backend Server" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Server URL: http://localhost:8080" -ForegroundColor Green
Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host ""
Write-Host "Default Credentials:" -ForegroundColor Cyan
Write-Host "  Admin   - Username: admin, Password: admin123"
Write-Host "  HR      - Username: hr, Password: hr123"
Write-Host "  Employee - Username: employee, Password: employee123"
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan

java -jar target/hrms-1.0.0.jar

