# start-locally.ps1
# Cloud DMS (Document Management System) - Local Development Startup Guide

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Cloud DMS - Local Development" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "INSTRUCTIONS FOR KIRO IDE" -ForegroundColor Yellow
Write-Host ""
Write-Host "To start the services as Kiro background processes, ask Kiro:" -ForegroundColor White
Write-Host ""
Write-Host '  "Start the backend and frontend as background processes"' -ForegroundColor Green
Write-Host ""
Write-Host "Kiro will use controlPwshProcess to start both services." -ForegroundColor Gray
Write-Host "You can monitor them with listProcesses and getProcessOutput." -ForegroundColor Gray
Write-Host ""
Write-Host "Backend command:" -ForegroundColor Cyan
Write-Host '  cmd /c "set SPRING_PROFILES_ACTIVE=local && mvn spring-boot:run"' -ForegroundColor Gray
Write-Host '  Working directory: C:\Dev\agentic-dms-archive\backend' -ForegroundColor Gray
Write-Host ""
Write-Host "Frontend command:" -ForegroundColor Cyan
Write-Host '  npm start' -ForegroundColor Gray
Write-Host '  Working directory: C:\Dev\agentic-dms-archive\frontend' -ForegroundColor Gray
Write-Host ""
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "MANUAL STARTUP (Alternative)" -ForegroundColor Yellow
Write-Host ""
Write-Host "If you prefer to start manually, use these commands:" -ForegroundColor White
Write-Host ""
Write-Host "Backend (in Kiro terminal):" -ForegroundColor Cyan
Write-Host '  cd backend' -ForegroundColor Gray
Write-Host '  $env:SPRING_PROFILES_ACTIVE="local"' -ForegroundColor Gray
Write-Host '  mvn spring-boot:run' -ForegroundColor Gray
Write-Host ""
Write-Host "Frontend (in separate Kiro terminal):" -ForegroundColor Cyan
Write-Host '  cd frontend' -ForegroundColor Gray
Write-Host '  npm start' -ForegroundColor Gray
Write-Host ""
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "SERVICE ENDPOINTS" -ForegroundColor Yellow
Write-Host ""
Write-Host "Frontend:  http://localhost:4200" -ForegroundColor Green
Write-Host "Backend:   http://localhost:8080" -ForegroundColor Green
Write-Host "Health:    http://localhost:8080/actuator/health" -ForegroundColor Green
Write-Host "Info:      http://localhost:8080/actuator/info" -ForegroundColor Gray
Write-Host ""
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "DATABASE (H2 In-Memory - Local Profile)" -ForegroundColor Yellow
Write-Host ""
Write-Host "H2 Console: http://localhost:8080/h2-console" -ForegroundColor Green
Write-Host "JDBC URL:   jdbc:h2:mem:dmsdb" -ForegroundColor Gray
Write-Host "Username:   sa" -ForegroundColor Gray
Write-Host "Password:   (empty)" -ForegroundColor Gray
Write-Host ""
Write-Host "Flyway migrations run automatically on startup." -ForegroundColor Cyan
Write-Host ""
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
Write-Host "API ENDPOINTS (REST)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Documents:      POST/GET /api/v1/documents" -ForegroundColor Gray
Write-Host "Search:         POST     /api/v1/search" -ForegroundColor Gray
Write-Host "Hybrid Search:  POST     /api/v1/search-hybrid" -ForegroundColor Gray
Write-Host "Document Types: GET/POST /api/v1/document-types" -ForegroundColor Gray
Write-Host "Groups:         GET/POST /api/v1/groups" -ForegroundColor Gray
Write-Host "Audit:          GET      /api/v1/audit" -ForegroundColor Gray
Write-Host "Legal Holds:    POST/GET /api/v1/legal-holds" -ForegroundColor Gray
Write-Host "Admin:          GET      /api/v1/admin" -ForegroundColor Gray
Write-Host "MCP Tools:      POST     /mcp/tools/*" -ForegroundColor Gray
Write-Host ""
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host ""
