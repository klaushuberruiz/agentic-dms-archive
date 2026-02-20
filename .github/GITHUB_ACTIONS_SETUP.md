# GitHub Actions Setup Guide

## Required Secrets

Configure these secrets in your GitHub repository settings under `Settings → Secrets and variables → Actions`:

### 1. Azure Service Principal

Create a service principal for GitHub Actions authentication:

```bash
# Set variables
SUBSCRIPTION_ID="your-azure-subscription-id"
ENVIRONMENT="dev"

# Create service principal
az ad sp create-for-rbac \
  --name "github-dms-${ENVIRONMENT}" \
  --role Contributor \
  --scopes /subscriptions/${SUBSCRIPTION_ID} \
  --json-auth
```

Store the entire JSON output as a secret named: **AZURE_CREDENTIALS**

### 2. Container Registry Credentials

Get credentials from Azure Container Registry:

```bash
# Get CR username
az acr credential show \
  --resource-group dms-rg-${ENVIRONMENT} \
  --name dmsacr${ENVIRONMENT} \
  --query username -o tsv
```

Store as secret: **AZURE_CR_USERNAME**

```bash
# Get CR password (first one)
az acr credential show \
  --resource-group dms-rg-${ENVIRONMENT} \
  --name dmsacr${ENVIRONMENT} \
  --query 'passwords[0].value' -o tsv
```

Store as secret: **AZURE_CR_PASSWORD**

## Pipeline Workflow

### Triggered On
- Push to `main` branch → Deploy to production
- Push to `develop` branch → Deploy to development
- Pull requests → Run tests only

### Job Sequence

1. **test-backend**
   - Spins up PostgreSQL container
   - Runs Maven tests
   - Uploads coverage reports

2. **test-frontend**
   - Installs Node.js
   - Runs linter and tests
   - Uploads coverage reports

3. **build** (runs only on successful tests and push event)
   - Builds Docker images for backend and frontend
   - Pushes to both GHCR and Azure Container Registry

4. **deploy-dev** (runs on push to `develop`)
   - Deploys backend to `dms-api-dev` App Service
   - Deploys frontend to `dms-web-dev` App Service

5. **deploy-prod** (runs on push to `main`)
   - Deploys backend to `dms-api` App Service
   - Deploys frontend to `dms-web` App Service
   - Runs smoke tests

## Environment Variables

The workflow uses these environment variables (hardcoded in the YAML):

```yaml
REGISTRY: ghcr.io
IMAGE_NAME_BACKEND: ${{ github.repository }}-backend
IMAGE_NAME_FRONTEND: ${{ github.repository }}-frontend
AZURE_RESOURCE_GROUP: dms-rg
AZURE_APP_SERVICE_BACKEND: dms-api
AZURE_APP_SERVICE_FRONTEND: dms-web
AZURE_CONTAINER_REGISTRY: dmsacr
```

### Customization

Edit [.github/workflows/deploy.yml](.github/workflows/deploy.yml) to change:
- Image names
- App Service names
- Resource group names

## Monitoring

### View Workflow Runs
1. Go to `Actions` tab in GitHub repository
2. Click on the workflow run to view details
3. Click on individual jobs to see logs

### Common Issues

**Test Failures**
- Check test logs in the workflow output
- Ensure all dependencies are listed in pom.xml and package.json
- Verify database migrations are correct

**Build Failures**
- Check Docker build logs
- Ensure Dockerfile paths are correct
- Verify base images are available

**Deployment Failures**
- Verify Azure credentials are correct
- Check App Service configuration
- View Azure Portal error messages

## Rollback

### Quick Rollback

```bash
# Deploy a specific image tag
az webapp config container set \
  --resource-group dms-rg-prod \
  --name dms-api \
  --docker-custom-image-name dmsacr.azurecr.io/dms-backend:previous-tag
```

### Via GitHub

Redeploy previous commit using workflow_dispatch:

```bash
# Trigger workflow for a specific commit
gh workflow run deploy.yml \
  --ref main \
  --field environment=prod
```

## Best Practices

1. **Always test locally before pushing**
   ```bash
   # Test locally
   mvn -f backend/pom.xml clean test
   cd frontend && npm test
   ```

2. **Use meaningful commit messages**
   - Helps identify which commit caused issues

3. **Monitor deployments**
   - Watch the Actions tab during deployment
   - Check application logs after deployment

4. **Verify smoke tests pass**
   - Production deployment includes smoke tests
   - Ensure endpoints respond correctly

5. **Keep credentials secure**
   - Never commit secrets to repository
   - Use GitHub Actions secrets only
   - Rotate credentials regularly

## Customization

### Adding More Tests

Add test step in the appropriate job:

```yaml
- name: Run integration tests
  run: mvn verify -DskipUnitTests=false
```

### Adding Security Scans

Add to the build job:

```yaml
- name: Run Trivy vulnerability scan
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ env.IMAGE_NAME_BACKEND }}
    format: 'sarif'
    output: 'trivy-results.sarif'
```

### Adding Notifications

Add Slack notification:

```yaml
- name: Notify Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
```

## References

- [GitHub Actions Documentation](https://docs.github.com/actions)
- [Azure Login Action](https://github.com/Azure/login)
- [Azure WebApps Deploy](https://github.com/Azure/webapps-deploy)
- [Docker Build and Push Action](https://github.com/docker/build-push-action)
