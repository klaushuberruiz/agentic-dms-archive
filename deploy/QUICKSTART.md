# Quick Start Guide

## Prerequisites

Before deploying to Azure, ensure you have:

1. **Azure Subscription** - Sign up at https://azure.microsoft.com
2. **Azure CLI** or **PowerShell** installed
3. **Docker** (optional, for building images locally)
4. **GitHub account** (for GitHub Actions CI/CD)

## Installation

### Azure CLI (macOS/Linux)
```bash
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
az --version
```

### Azure CLI (Windows)
```powershell
winget install Microsoft.AzureCLI
az --version
```

### Azure PowerShell (Windows)
```powershell
Install-Module -Name Az -Force -AllowClobber
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## Quick Start - Using Bash Script (Recommended)

```bash
cd deploy

# Interactive setup
chmod +x quickstart.sh azure-deploy.sh
./quickstart.sh

# Or manual commands for development
./azure-deploy.sh dev create    # Create infrastructure (one-time)
./azure-deploy.sh dev deploy    # Deploy application
./azure-deploy.sh dev info      # View deployment information
```

## Quick Start - Using PowerShell Script (Windows)

```powershell
cd deploy

# Interactive setup (coming soon)
# .\quickstart.ps1

# Or manual commands for development
.\azure-deploy.ps1 -Environment dev -Action create    # Create infrastructure
.\azure-deploy.ps1 -Environment dev -Action deploy    # Deploy application
.\azure-deploy.ps1 -Environment dev -Action info      # View information
```

## Quick Start - Complete Workflow

### 1. Prepare GitHub (One-time)

1. Create service principal:
```bash
az ad sp create-for-rbac \
  --name "github-dms-dev" \
  --role Contributor \
  --scopes /subscriptions/YOUR-SUBSCRIPTION-ID \
  --json-auth
```

2. Add GitHub Secrets in your repository:
   - Settings → Secrets and variables → Actions
   - Add `AZURE_CREDENTIALS` (from step 1)
   - Add `AZURE_CR_USERNAME` and `AZURE_CR_PASSWORD` (from ACR)

### 2. Create Infrastructure (One-time)

```bash
# Option A: Interactive
cd deploy && ./quickstart.sh

# Option B: Direct command
./azure-deploy.sh dev create
```

This creates:
- Resource group
- PostgreSQL database
- Container registry
- App Service infrastructure

### 3. Deploy Application

```bash
# Option A: Push to GitHub (automatic via Actions)
git push origin develop       # Triggers dev deployment
git push origin main          # Triggers prod deployment

# Option B: Manual deployment
./azure-deploy.sh dev deploy
```

### 4. Access Application

After deployment:

**Development**:
- Frontend: https://dms-web-dev.azurewebsites.net
- Backend: https://dms-api-dev.azurewebsites.net

**Production**:
- Frontend: https://dms-web.azurewebsites.net
- Backend: https://dms-api.azurewebsites.net

## Environment Setup Summary

### Resource Costs

| Resource | Dev Tier | Monthly Cost |
|----------|----------|--------------|
| App Service | B2 | ~$160 |
| PostgreSQL | B1ms | ~$40 |
| Container Registry | Standard | ~$100 |
| **Total** | | **~$310/mo** |

### Configuration Timeline

| Step | Manual | Scripted | Notes |
|------|--------|----------|-------|
| Create RG | 1 min | 30 sec | Quick via portal or CLI |
| Database | 10 min | 2 min | Longest part |
| App Services | 5 min | 1 min | Fast creation |
| Container Registry | 2 min | 30 sec | Quick |
| Configure networking | 5 min | Auto | Scripts handle this |
| Deploy images | 5 min | 2 min | Push and pull |
| **Total** | ~30 min | ~5 min | Scripted is 6x faster |

## Development Workflow

### Local Development

```bash
# Terminal 1: Backend
cd backend
mvn spring-boot:run

# Terminal 2: Frontend  
cd frontend
npm install
npm start
```

Access at: http://localhost:4200 (backend API proxied to :8080)

### Deploy to Dev

```bash
# Just push to develop branch
git add .
git commit -m "Your changes"
git push origin feature/your-feature
# Create PR and merge to develop

# GitHub Actions automatically deploys to dev environment
```

### Deploy to Production

```bash
# Create PR from develop to main
git checkout main
git pull origin develop
git push origin main

# GitHub Actions automatically deploys to production
# with smoke tests
```

## Important Files

| File | Purpose |
|------|---------|
| `deploy/azure-deploy.sh` | Main deployment script (Bash) |
| `deploy/azure-deploy.ps1` | Deployment script (PowerShell) |
| `deploy/README.md` | Detailed deployment documentation |
| `.github/workflows/deploy.yml` | CI/CD pipeline definition |
| `.github/GITHUB_ACTIONS_SETUP.md` | GitHub Actions setup guide |
| `backend/Dockerfile` | Backend container image |
| `frontend/Dockerfile` | Frontend container image |
| `deploy/ENVIRONMENTS.md` | Environment-specific configuration |

## Common Tasks

### View Application Logs

```bash
# Backend logs
az webapp log tail -g dms-rg-dev -n dms-api-dev

# Follow in real-time
az webapp log tail -g dms-rg-dev -n dms-api-dev --follow
```

### Update Application Setting

```bash
az webapp config appsettings set \
  -g dms-rg-dev \
  -n dms-api-dev \
  --settings SPRING_PROFILES_ACTIVE=prod
```

### Scale Up App Service

```bash
az appservice plan update \
  -g dms-rg-dev \
  -n dms-plan-dev \
  --sku P1V2
```

### Restart App Service

```bash
az webapp restart -g dms-rg-dev -n dms-api-dev
```

### Cleanup Resources

```bash
# WARNING: This deletes everything!
./azure-deploy.sh dev cleanup
```

## Troubleshooting

### Issue: Deployment fails with "Authentication failed"

**Solution**: 
```bash
# Login again
az login

# Check current subscription
az account show
```

### Issue: Container image not found in registry

**Solution**:
```bash
# Build and push manually
docker build -t dmsacr.azurecr.io/dms-backend:latest ./backend
docker push dmsacr.azurecr.io/dms-backend:latest
```

### Issue: Database connection timeout

**Solution**:
```bash
# Check PostgreSQL is running
az postgres flexible-server show -g dms-rg-dev -n dms-db-dev

# Get connection string
az keyvault secret show -g dms-rg-dev \
  --vault-name dmskv \
  --name postgresql-connection-string
```

### Issue: App Service stuck in "Creating" state

**Solution**:
```bash
# Delete and recreate
az webapp delete -g dms-rg-dev -n dms-api-dev
az webapp create -g dms-rg-dev \
  -p dms-plan-dev \
  -n dms-api-dev
```

## Next Steps

1. ✅ Install prerequisites
2. ✅ Create infrastructure with `./azure-deploy.sh dev create`
3. ✅ Set up GitHub secrets (see `.github/GITHUB_ACTIONS_SETUP.md`)
4. ✅ Push code to create CI/CD pipeline
5. ✅ Monitor deployments in GitHub Actions

## Support

- **Azure CLI Help**: `az --help` or https://docs.microsoft.com/cli/azure
- **GitHub Actions**: https://docs.github.com/actions
- **Troubleshooting**: See `deploy/README.md`
