#!/bin/bash

################################################################################
# Azure Deployment Script for Document Management System (DMS)
# 
# Prerequisites:
# - Azure CLI installed and configured
# - Azure subscription with appropriate permissions
# - Docker installed (for image building, optional)
#
# Usage:
#   ./azure-deploy.sh [environment] [action]
#   
# Examples:
#   ./azure-deploy.sh dev create       # Create dev infrastructure
#   ./azure-deploy.sh dev deploy       # Deploy to existing dev infrastructure
#   ./azure-deploy.sh prod deploy      # Deploy to production
################################################################################

set -euo pipefail

# Configuration
ENVIRONMENT="${1:-dev}"
ACTION="${2:-deploy}"
LOCATION="eastus"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Resource naming convention
PROJECT_NAME="dms"
RG_NAME="${PROJECT_NAME}-rg-${ENVIRONMENT}"
ACR_NAME="${PROJECT_NAME}acr${ENVIRONMENT}"
# Remove hyphens from ACR name (not allowed)
ACR_NAME="${ACR_NAME//-/}"

BACKEND_APP_NAME="${PROJECT_NAME}-api-${ENVIRONMENT}"
FRONTEND_APP_NAME="${PROJECT_NAME}-web-${ENVIRONMENT}"
APP_PLAN_NAME="${PROJECT_NAME}-plan-${ENVIRONMENT}"
KV_NAME="${PROJECT_NAME}-kv-${ENVIRONMENT}"
# Remove hyphens from KV name
KV_NAME="${KV_NAME//-/}"
PSQL_SERVER_NAME="${PROJECT_NAME}-db-${ENVIRONMENT}"
PSQL_DB_NAME="dms_db"
PSQL_ADMIN_USER="dmsadmin"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

################################################################################
# Utility Functions
################################################################################

log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v az &> /dev/null; then
        log_error "Azure CLI is not installed. Please install it first."
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        log_warning "Docker is not installed. Skipping image build."
    fi
    
    log_success "Prerequisites check passed."
}

azure_login() {
    log_info "Checking Azure login status..."
    if ! az account show > /dev/null 2>&1; then
        log_info "Logging in to Azure..."
        az login
    else
        log_success "Already logged in to Azure."
    fi
}

create_resource_group() {
    log_info "Creating resource group: $RG_NAME"
    
    if az group exists --name "$RG_NAME" | grep -q true; then
        log_success "Resource group $RG_NAME already exists."
    else
        az group create --name "$RG_NAME" --location "$LOCATION"
        log_success "Resource group $RG_NAME created."
    fi
}

create_container_registry() {
    log_info "Creating Azure Container Registry: $ACR_NAME"
    
    if az acr show --resource-group "$RG_NAME" --name "$ACR_NAME" &> /dev/null; then
        log_success "Container registry $ACR_NAME already exists."
    else
        az acr create \
            --resource-group "$RG_NAME" \
            --name "$ACR_NAME" \
            --sku Standard \
            --admin-enabled true
        log_success "Container registry $ACR_NAME created."
    fi
}

create_key_vault() {
    log_info "Creating Azure Key Vault: $KV_NAME"
    
    if az keyvault show --resource-group "$RG_NAME" --name "$KV_NAME" &> /dev/null; then
        log_success "Key Vault $KV_NAME already exists."
    else
        az keyvault create \
            --resource-group "$RG_NAME" \
            --name "$KV_NAME" \
            --location "$LOCATION" \
            --enable-purge-protection false
        log_success "Key Vault $KV_NAME created."
    fi
}

create_postgresql() {
    log_info "Creating Azure Database for PostgreSQL: $PSQL_SERVER_NAME"
    
    if az postgres flexible-server show --resource-group "$RG_NAME" --name "$PSQL_SERVER_NAME" &> /dev/null; then
        log_success "PostgreSQL server $PSQL_SERVER_NAME already exists."
    else
        # Generate random password
        PSQL_PASSWORD=$(openssl rand -base64 24)
        
        az postgres flexible-server create \
            --resource-group "$RG_NAME" \
            --name "$PSQL_SERVER_NAME" \
            --location "$LOCATION" \
            --admin-user "$PSQL_ADMIN_USER" \
            --admin-password "$PSQL_PASSWORD" \
            --sku-name "Standard_B1ms" \
            --tier "Burstable" \
            --storage-size 32 \
            --version 15 \
            --database-name "$PSQL_DB_NAME" \
            --public-access "0.0.0.0"
        
        # Store credentials in Key Vault
        az keyvault secret set \
            --vault-name "$KV_NAME" \
            --name "postgresql-password" \
            --value "$PSQL_PASSWORD"
        
        az keyvault secret set \
            --vault-name "$KV_NAME" \
            --name "postgresql-connection-string" \
            --value "postgresql://${PSQL_ADMIN_USER}:${PSQL_PASSWORD}@${PSQL_SERVER_NAME}.postgres.database.azure.com:5432/${PSQL_DB_NAME}?sslmode=require"
        
        log_success "PostgreSQL server $PSQL_SERVER_NAME created."
        log_info "Credentials stored in Key Vault."
    fi
}

create_app_service_plan() {
    log_info "Creating App Service Plan: $APP_PLAN_NAME"
    
    if az appservice plan show --resource-group "$RG_NAME" --name "$APP_PLAN_NAME" &> /dev/null; then
        log_success "App Service Plan $APP_PLAN_NAME already exists."
    else
        az appservice plan create \
            --resource-group "$RG_NAME" \
            --name "$APP_PLAN_NAME" \
            --is-linux \
            --sku "B2" \
            --number-of-workers 1
        
        log_success "App Service Plan $APP_PLAN_NAME created."
    fi
}

create_backend_app() {
    log_info "Creating Backend App Service: $BACKEND_APP_NAME"
    
    if az webapp show --resource-group "$RG_NAME" --name "$BACKEND_APP_NAME" &> /dev/null; then
        log_success "Backend App Service $BACKEND_APP_NAME already exists."
    else
        az webapp create \
            --resource-group "$RG_NAME" \
            --plan "$APP_PLAN_NAME" \
            --name "$BACKEND_APP_NAME" \
            --deployment-container-image-name-user "${ACR_NAME}.azurecr.io" \
            --deployment-container-image-name "$ACR_NAME.azurecr.io/dms-backend:latest"
        
        log_success "Backend App Service $BACKEND_APP_NAME created."
    fi
}

create_frontend_app() {
    log_info "Creating Frontend App Service: $FRONTEND_APP_NAME"
    
    if az webapp show --resource-group "$RG_NAME" --name "$FRONTEND_APP_NAME" &> /dev/null; then
        log_success "Frontend App Service $FRONTEND_APP_NAME already exists."
    else
        az webapp create \
            --resource-group "$RG_NAME" \
            --plan "$APP_PLAN_NAME" \
            --name "$FRONTEND_APP_NAME" \
            --deployment-container-image-name-user "${ACR_NAME}.azurecr.io" \
            --deployment-container-image-name "$ACR_NAME.azurecr.io/dms-frontend:latest"
        
        log_success "Frontend App Service $FRONTEND_APP_NAME created."
    fi
}

configure_app_settings() {
    log_info "Configuring App Service settings..."
    
    # Get PostgreSQL connection string from Key Vault
    DB_CONNECTION_STRING=$(az keyvault secret show \
        --vault-name "$KV_NAME" \
        --name "postgresql-connection-string" \
        --query value -o tsv 2>/dev/null || echo "")
    
    if [ -z "$DB_CONNECTION_STRING" ]; then
        log_warning "PostgreSQL connection string not found in Key Vault."
        log_warning "Ensure the secret 'postgresql-connection-string' exists in Key Vault or set the secret manually."
        log_warning "Example (do NOT store plaintext in repo): az keyvault secret set --vault-name $KV_NAME --name 'postgresql-connection-string' --value '<connection-string>'"
        DB_CONNECTION_STRING=""
    fi
    
    # Backend app settings
    log_info "Configuring backend app settings..."
    az webapp config appsettings set \
        --resource-group "$RG_NAME" \
        --name "$BACKEND_APP_NAME" \
        --settings \
            SPRING_DATASOURCE_URL="$DB_CONNECTION_STRING" \
            SPRING_DATASOURCE_USERNAME="$PSQL_ADMIN_USER" \
            SPRING_JPA_HIBERNATE_DDL_AUTO="validate" \
            SPRING_PROFILES_ACTIVE="prod" \
            LOGGING_LEVEL_ROOT="INFO"
    
    # Frontend app settings
    log_info "Configuring frontend app settings..."
    az webapp config appsettings set \
        --resource-group "$RG_NAME" \
        --name "$FRONTEND_APP_NAME" \
        --settings \
            API_BASE_URL="https://${BACKEND_APP_NAME}.azurewebsites.net"
    
    log_success "App settings configured."
}

build_and_push_images() {
    log_info "Building and pushing Docker images..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is required for building images."
        return 1
    fi
    
    # Get ACR login credentials
    ACR_USERNAME=$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'username' -o tsv)
    ACR_PASSWORD=$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'passwords[0].value' -o tsv)
    
    # Login to ACR
    echo "$ACR_PASSWORD" | docker login "$ACR_NAME.azurecr.io" -u "$ACR_USERNAME" --password-stdin
    
    # Build and push backend image
    log_info "Building backend image..."
    docker build -t "$ACR_NAME.azurecr.io/dms-backend:latest" -f "$PROJECT_ROOT/backend/Dockerfile" "$PROJECT_ROOT/backend"
    
    log_info "Pushing backend image..."
    docker push "$ACR_NAME.azurecr.io/dms-backend:latest"
    
    # Build and push frontend image
    log_info "Building frontend image..."
    docker build -t "$ACR_NAME.azurecr.io/dms-frontend:latest" -f "$PROJECT_ROOT/frontend/Dockerfile" "$PROJECT_ROOT/frontend"
    
    log_info "Pushing frontend image..."
    docker push "$ACR_NAME.azurecr.io/dms-frontend:latest"
    
    log_success "Docker images built and pushed successfully."
}

deploy_images() {
    log_info "Deploying images to App Services..."
    
    # Configure container settings
    az webapp config container set \
        --resource-group "$RG_NAME" \
        --name "$BACKEND_APP_NAME" \
        --docker-custom-image-name "$ACR_NAME.azurecr.io/dms-backend:latest" \
        --docker-registry-server-url "https://$ACR_NAME.azurecr.io" \
        --docker-registry-server-user "$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'username' -o tsv)" \
        --docker-registry-server-password "$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'passwords[0].value' -o tsv)"
    
    az webapp config container set \
        --resource-group "$RG_NAME" \
        --name "$FRONTEND_APP_NAME" \
        --docker-custom-image-name "$ACR_NAME.azurecr.io/dms-frontend:latest" \
        --docker-registry-server-url "https://$ACR_NAME.azurecr.io" \
        --docker-registry-server-user "$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'username' -o tsv)" \
        --docker-registry-server-password "$(az acr credential show --resource-group "$RG_NAME" --name "$ACR_NAME" --query 'passwords[0].value' -o tsv)"
    
    log_success "Images deployed successfully."
}

enable_managed_identity() {
    log_info "Enabling managed identity for App Services..."
    
    az webapp identity assign \
        --resource-group "$RG_NAME" \
        --name "$BACKEND_APP_NAME"
    
    az webapp identity assign \
        --resource-group "$RG_NAME" \
        --name "$FRONTEND_APP_NAME"
    
    log_success "Managed identities enabled."
}

setup_infrastructure() {
    log_info "Setting up Azure infrastructure for $ENVIRONMENT environment..."
    
    check_prerequisites
    azure_login
    create_resource_group
    create_key_vault
    create_postgresql
    create_container_registry
    create_app_service_plan
    create_backend_app
    create_frontend_app
    enable_managed_identity
    configure_app_settings
    
    log_success "Infrastructure setup completed!"
}

deploy_application() {
    log_info "Deploying application to $ENVIRONMENT environment..."
    
    azure_login
    build_and_push_images
    deploy_images
    configure_app_settings
    
    log_success "Deployment completed!"
}

show_deployment_info() {
    log_info "Deployment information for $ENVIRONMENT environment:"
    echo ""
    echo "Resource Group: $RG_NAME"
    echo "Container Registry: $ACR_NAME.azurecr.io"
    echo "Backend URL: https://${BACKEND_APP_NAME}.azurewebsites.net"
    echo "Frontend URL: https://${FRONTEND_APP_NAME}.azurewebsites.net"
    echo "PostgreSQL Server: ${PSQL_SERVER_NAME}.postgres.database.azure.com"
    echo "Database Name: $PSQL_DB_NAME"
    echo "Key Vault: $KV_NAME"
    echo ""
}

cleanup() {
    log_warning "Deleting all resources in $RG_NAME..."
    read -p "Are you sure? Type 'yes' to confirm: " -r
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        az group delete --resource-group "$RG_NAME" --yes --no-wait
        log_success "Resource group deletion initiated."
    else
        log_info "Cleanup cancelled."
    fi
}

################################################################################
# Main Script
################################################################################

main() {
    case "$ACTION" in
        create)
            setup_infrastructure
            show_deployment_info
            ;;
        deploy)
            deploy_application
            show_deployment_info
            ;;
        info)
            show_deployment_info
            ;;
        cleanup)
            cleanup
            ;;
        *)
            log_error "Unknown action: $ACTION"
            echo ""
            echo "Usage: $0 [environment] [action]"
            echo ""
            echo "Environments: dev, staging, prod"
            echo "Actions:"
            echo "  create   - Create all infrastructure resources"
            echo "  deploy   - Build and deploy application"
            echo "  info     - Show deployment information"
            echo "  cleanup  - Delete all resources"
            echo ""
            exit 1
            ;;
    esac
}

main
