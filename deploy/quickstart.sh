#!/bin/bash

# Quick start guide for Azure DMS deployment
# This script provides interactive prompts to set up deployment

set -euo pipefail

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Document Management System - Azure Deployment Setup       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Step 1: Check Azure CLI
echo -e "${YELLOW}Step 1: Checking Azure CLI...${NC}"
if ! command -v az &> /dev/null; then
    echo -e "${RED}✗ Azure CLI not found${NC}"
    echo "  Install from: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli"
    exit 1
fi
echo -e "${GREEN}✓ Azure CLI found${NC}"

# Step 2: Check authentication
echo ""
echo -e "${YELLOW}Step 2: Checking Azure authentication...${NC}"
if ! az account show > /dev/null 2>&1; then
    echo -e "${YELLOW}Not logged in to Azure. Starting login...${NC}"
    az login
fi
ACCOUNT=$(az account show --query 'name' -o tsv)
echo -e "${GREEN}✓ Logged in as: $ACCOUNT${NC}"

# Step 3: Set subscription
echo ""
echo -e "${YELLOW}Step 3: Selecting Azure subscription...${NC}"
SUBSCRIPTIONS=$(az account list --query '[].{name:name, id:id}' -o json)
echo "$SUBSCRIPTIONS" | jq -r '.[] | "\(.name) (\(.id))"' | nl

read -p "Enter subscription number: " SUB_NUM
SUBSCRIPTION_ID=$(echo "$SUBSCRIPTIONS" | jq -r ".[$((SUB_NUM-1))].id")
az account set --subscription "$SUBSCRIPTION_ID"
echo -e "${GREEN}✓ Selected subscription: $SUBSCRIPTION_ID${NC}"

# Step 4: Choose environment
echo ""
echo -e "${YELLOW}Step 4: Choose deployment environment...${NC}"
echo "1) Development (dev)"
echo "2) Staging (staging)"
echo "3) Production (prod)"
read -p "Enter environment number (default: 1): " ENV_NUM
ENV_NUM=${ENV_NUM:-1}

case $ENV_NUM in
    1) ENVIRONMENT="dev" ;;
    2) ENVIRONMENT="staging" ;;
    3) ENVIRONMENT="prod" ;;
    *) ENVIRONMENT="dev" ;;
esac
echo -e "${GREEN}✓ Selected environment: $ENVIRONMENT${NC}"

# Step 5: Choose action
echo ""
echo -e "${YELLOW}Step 5: Choose action...${NC}"
echo "1) Create infrastructure (first time only)"
echo "2) Deploy application"
echo "3) View deployment info"
read -p "Enter action number (default: 2): " ACTION_NUM
ACTION_NUM=${ACTION_NUM:-2}

case $ACTION_NUM in
    1) ACTION="create" ;;
    2) ACTION="deploy" ;;
    3) ACTION="info" ;;
    *) ACTION="deploy" ;;
esac

# Step 6: Confirm settings
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║ Deployment Summary                                         ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo "Subscription: $SUBSCRIPTION_ID"
echo "Environment:  $ENVIRONMENT"
echo "Action:       $ACTION"
echo ""
read -p "Continue with deployment? (yes/no): " CONFIRM
if [[ $CONFIRM != "yes" ]]; then
    echo "Deployment cancelled."
    exit 0
fi

# Step 7: Run deployment script
echo ""
echo -e "${YELLOW}Starting deployment...${NC}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$SCRIPT_DIR/azure-deploy.sh" ]; then
    bash "$SCRIPT_DIR/azure-deploy.sh" "$ENVIRONMENT" "$ACTION"
else
    echo -e "${RED}Error: azure-deploy.sh not found${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Deployment complete!${NC}"
echo ""
echo "Next steps:"
if [ "$ACTION" = "create" ]; then
    echo "1. Review resources in Azure Portal"
    echo "2. Configure GitHub secrets (see .github/GITHUB_ACTIONS_SETUP.md)"
    echo "3. Push code to trigger CI/CD pipeline"
else
    echo "1. Monitor deployment in Azure Portal"
    echo "2. View application logs: az webapp log tail -g dms-rg-$ENVIRONMENT -n dms-api-$ENVIRONMENT"
fi
