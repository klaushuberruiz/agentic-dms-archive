# Azure Deployment Guide

## Overview

This directory contains scripts and configurations for deploying the Document Management System to Azure. Two deployment scripts are provided for different platforms:

- **azure-deploy.sh** - Bash script for Linux/macOS
- **azure-deploy.ps1** - PowerShell script for Windows

## Prerequisites

### Global Requirements
- Azure subscription with appropriate permissions
- Azure CLI or Azure PowerShell installed
- Docker installed (optional, only needed for local image building)

### For Bash Script (azure-deploy.sh)
```bash
# Install Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Install Docker (if needed)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

### For PowerShell Script (azure-deploy.ps1)
```powershell
# Install Azure PowerShell
Install-Module -Name Az -Force -AllowClobber

# Update execution policy if needed
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## Configuration

### Environment Variables

Create a `.env` file in the deploy directory (not committed to git):

```bash
# Azure
AZURE_SUBSCRIPTION_ID="your-subscription-id"
AZURE_TENANT_ID="your-tenant-id"

# Resource naming
LOCATION="eastus"
ENVIRONMENT="dev"  # or "staging", "prod"
```

### GitHub Secrets

For GitHub Actions deployment, configure these secrets in your repository:

1. **AZURE_CREDENTIALS**
   ```bash
   # Create a service principal
   az ad sp create-for-rbac --name "github-dms-${ENVIRONMENT}" \
     --role contributor \
     --scopes /subscriptions/${SUBSCRIPTION_ID} \
     --json-auth
   ```
   Output the JSON and paste as `AZURE_CREDENTIALS` secret.

2. **AZURE_CR_USERNAME** & **AZURE_CR_PASSWORD**
   ```bash
   # Get Container Registry credentials
   az acr credential show --resource-group dms-rg-${ENVIRONMENT} \
     --name dmsacr${ENVIRONMENT}
   ```

## Deployment

### Using GitHub Actions (Recommended)

1. Push code to `develop` branch for dev deployment or `main` branch for production
2. GitHub Actions automatically:
   - Runs tests
   - Builds Docker images
   - Pushes to Azure Container Registry
   - Deploys to App Services

### Manual Deployment - Bash

```bash
# Create infrastructure (one-time)
./azure-deploy.sh dev create

# Deploy application
./azure-deploy.sh dev deploy

# View deployment info
./azure-deploy.sh dev info

# Cleanup resources
./azure-deploy.sh dev cleanup
```

### Manual Deployment - PowerShell

```powershell
# Create infrastructure
.\azure-deploy.ps1 -Environment dev -Action create

# Deploy application
.\azure-deploy.ps1 -Environment dev -Action deploy

# View deployment info
.\azure-deploy.ps1 -Environment dev -Action info

# Cleanup resources
.\azure-deploy.ps1 -Environment dev -Action cleanup
```

## Infrastructure Created

The deployment scripts create:

- **Resource Group** - Container for all resources
- **Azure Container Registry** - Stores Docker images
- **Key Vault** - Stores credentials and secrets
- **PostgreSQL Flexible Server** - Database (with auto-generated password)
- **App Service Plan** - Compute resources
- **App Service (Backend)** - Spring Boot API
- **App Service (Frontend)** - Angular web application

### Resource Naming Convention

- Resource Group: `dms-rg-{environment}`
- Container Registry: `dmsacr{environment}`
- Backend App: `dms-api-{environment}`
- Frontend App: `dms-web-{environment}`
- PostgreSQL Server: `dms-db-{environment}`
- Key Vault: `dmskv{environment}`

## Configuration Details

### Backend (Spring Boot)

Environment variables set in App Service:

```
SPRING_DATASOURCE_URL = postgresql://host:5432/dms_db
SPRING_DATASOURCE_USERNAME = dmsadmin
SPRING_DATASOURCE_PASSWORD = (from Key Vault)
SPRING_JPA_HIBERNATE_DDL_AUTO = validate
SPRING_PROFILES_ACTIVE = prod
```

### Frontend (Angular)

Environment variables set in App Service:

```
API_BASE_URL = https://dms-api-{environment}.azurewebsites.net
```

## Docker Images

### Backend Image
- **Base**: `maven:3.9-eclipse-temurin-17` (build) → `eclipse-temurin:17-jre-alpine` (runtime)
- **Size**: ~300MB
- **Health Check**: `/actuator/health` endpoint

### Frontend Image
- **Base**: `node:18-alpine` (build) → `nginx:alpine` (runtime)
- **Size**: ~50MB
- **Health Check**: GET `/`

## Troubleshooting

### 1. Deployment Fails
```bash
# Check App Service logs
az webapp log tail --resource-group dms-rg-dev --name dms-api-dev

# View real-time logs
az webapp log tail --resource-group dms-rg-dev --name dms-api-dev --follow
```

### 2. Database Connection Issues
```bash
# Get connection string from Key Vault
az keyvault secret show --vault-name dmskv{environment} \
  --name postgresql-connection-string --query value
```

### 3. Container Registry Issues
```bash
# List images in ACR
az acr repository list --name dmsacr{environment}

# Check image tags
az acr repository show-tags --name dmsacr{environment} --repository dms-backend
```

### 4. App Service Configuration
```bash
# View current settings
az webapp config appsettings list --resource-group dms-rg-dev --name dms-api-dev

# Update a setting
az webapp config appsettings set --resource-group dms-rg-dev \
  --name dms-api-dev --settings KEY=VALUE
```

## Monitoring

### Azure Portal
- Application Insights metrics
- App Service diagnostics
- PostgreSQL server monitoring

### Useful Commands
```bash
# Stream logs from App Service
az monitor app-insights query --app dms-api-dev \
  --resource-group dms-rg-dev \
  --analytics-query "traces | order by timestamp desc"

# Check App Service status
az webapp show --resource-group dms-rg-dev --name dms-api-dev
```

## Cost Optimization

### Current Configuration
- **App Service Plan**: B2 (2-core, 3.5GB RAM, ~$80/month each)
- **PostgreSQL**: Burstable tier (B1ms, ~$40/month)
- **Container Registry**: Standard (~$100/month)

### Cost Reduction Options
- Use B1 or B2 App Service Plan for dev/staging
- Auto-shutdown for non-prod environments
- Reserved instances for production
- Enable Azure Autoscale

## Advanced Configuration

### Custom Domain
```bash
az appservice web config hostname add --resource-group dms-rg-prod \
  --webapp-name dms-api-prod --hostname api.yourdomain.com
```

### SSL Certificate
Do NOT store certificate passwords in source. Store certificate files and passwords in Key Vault and reference them when configuring App Services. Example (retrieve password from Key Vault):

```bash
# Retrieve certificate password from Key Vault
az keyvault secret show --vault-name dmskv{environment} --name cert-password --query value -o tsv

# Upload certificate using the retrieved password (replace <password> with the value retrieved above)
az appservice web config ssl upload --resource-group dms-rg-prod \
  --name dms-api-prod --certificate-file cert.pfx --certificate-password <password>
```

### Backup Configuration
```bash
az webapp backup create --resource-group dms-rg-prod --name dms-api-prod
```

## Support

For issues or questions:
1. Check Azure Portal for error messages
2. Review application logs
3. Consult Azure documentation: https://docs.microsoft.com/azure/
