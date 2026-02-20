# PowerShell Deployment Script for Document Management System (DMS)
# 
# Prerequisites:
# - Azure PowerShell module installed (Install-Module -Name Az -Force)
# - Azure subscription with appropriate permissions
# - Docker installed (for image building, optional)
#
# Usage:
#   .\azure-deploy.ps1 -Environment dev -Action create
#   .\azure-deploy.ps1 -Environment dev -Action deploy
#   .\azure-deploy.ps1 -Environment prod -Action deploy

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('dev', 'staging', 'prod')]
    [string]$Environment,
    
    [Parameter(Mandatory = $false)]
    [ValidateSet('create', 'deploy', 'info', 'cleanup')]
    [string]$Action = 'deploy'
)

# Configuration
$Location = "eastus"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

# Resource naming
$ProjectName = "dms"
$RgName = "$ProjectName-rg-$Environment"
$AcrName = "$ProjectName`acr$Environment" -Replace '-'
$BackendAppName = "$ProjectName-api-$Environment"
$FrontendAppName = "$ProjectName-web-$Environment"
$AppPlanName = "$ProjectName-plan-$Environment"
$KvName = "$ProjectName`kv$Environment" -Replace '-'
$PsqlServerName = "$ProjectName-db-$Environment"
$PsqlDbName = "dms_db"
$PsqlAdminUser = "dmsadmin"

################################################################################
# Utility Functions
################################################################################

function Write-Info {
    Write-Host "[INFO] $args" -ForegroundColor Blue
}

function Write-Success {
    Write-Host "[SUCCESS] $args" -ForegroundColor Green
}

function Write-Warning {
    Write-Host "[WARNING] $args" -ForegroundColor Yellow
}

function Write-Error {
    Write-Host "[ERROR] $args" -ForegroundColor Red
}

function Test-Prerequisites {
    Write-Info "Checking prerequisites..."
    
    $azModule = Get-Module -Name Az -ListAvailable
    if (-not $azModule) {
        Write-Error "Azure PowerShell module is not installed. Install it with: Install-Module -Name Az -Force"
        exit 1
    }
    
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Warning "Docker is not installed. Skipping image build."
    }
    
    Write-Success "Prerequisites check passed."
}

function Connect-AzureAccount {
    Write-Info "Checking Azure login status..."
    
    try {
        $context = Get-AzContext
        if ($null -eq $context) {
            Write-Info "Logging in to Azure..."
            Connect-AzAccount
        }
        else {
            Write-Success "Already logged in to Azure as $($context.Account.Id)"
        }
    }
    catch {
        Write-Error "Failed to connect to Azure: $_"
        exit 1
    }
}

function New-ResourceGroup {
    param(
        [string]$Name
    )
    
    Write-Info "Creating resource group: $Name"
    
    $rgExists = Get-AzResourceGroup -Name $Name -ErrorAction SilentlyContinue
    
    if ($rgExists) {
        Write-Success "Resource group $Name already exists."
    }
    else {
        New-AzResourceGroup -Name $Name -Location $Location | Out-Null
        Write-Success "Resource group $Name created."
    }
}

function New-ContainerRegistry {
    param(
        [string]$ResourceGroupName,
        [string]$RegistryName
    )
    
    Write-Info "Creating Azure Container Registry: $RegistryName"
    
    $registry = Get-AzContainerRegistry -ResourceGroupName $ResourceGroupName -Name $RegistryName -ErrorAction SilentlyContinue
    
    if ($registry) {
        Write-Success "Container registry $RegistryName already exists."
    }
    else {
        New-AzContainerRegistry `
            -ResourceGroupName $ResourceGroupName `
            -Name $RegistryName `
            -Location $Location `
            -Sku Standard `
            -AdminUserEnabled $true | Out-Null
        
        Write-Success "Container registry $RegistryName created."
    }
}

function New-KeyVault {
    param(
        [string]$ResourceGroupName,
        [string]$VaultName
    )
    
    Write-Info "Creating Azure Key Vault: $VaultName"
    
    $vault = Get-AzKeyVault -ResourceGroupName $ResourceGroupName -VaultName $VaultName -ErrorAction SilentlyContinue
    
    if ($vault) {
        Write-Success "Key Vault $VaultName already exists."
    }
    else {
        New-AzKeyVault `
            -ResourceGroupName $ResourceGroupName `
            -VaultName $VaultName `
            -Location $Location `
            -EnablePurgeProtection $false | Out-Null
        
        Write-Success "Key Vault $VaultName created."
    }
}

function New-PostgreSQLServer {
    param(
        [string]$ResourceGroupName,
        [string]$ServerName,
        [string]$DatabaseName,
        [string]$AdminUsername,
        [string]$VaultName
    )
    
    Write-Info "Creating Azure Database for PostgreSQL: $ServerName"
    
    $server = Get-AzPostgreSqlFlexibleServer -ResourceGroupName $ResourceGroupName -Name $ServerName -ErrorAction SilentlyContinue
    
    if ($server) {
        Write-Success "PostgreSQL server $ServerName already exists."
    }
    else {
        # Generate random password with sufficient complexity
        $password = -join ((33..126) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
        $passwordSecure = ConvertTo-SecureString $password -AsPlainText -Force
        
        New-AzPostgreSqlFlexibleServer `
            -ResourceGroupName $ResourceGroupName `
            -Name $ServerName `
            -Location $Location `
            -AdministratorUserName $AdminUsername `
            -AdministratorLoginPassword $passwordSecure `
            -Sku "Standard_B1ms" `
            -DatabaseName $DatabaseName `
            -Version 15 `
            -StorageInGb 32 | Out-Null
        
        # Store credentials in Key Vault
        $passwordSecure | ConvertFrom-SecureString -AsPlainText | `
            Set-AzKeyVaultSecret -VaultName $VaultName -Name "postgresql-password" -Force | Out-Null
        
        $connectionString = "postgresql://${AdminUsername}:${password}@${ServerName}.postgres.database.azure.com:5432/${DatabaseName}?sslmode=require"
        $connectionStringSecure = ConvertTo-SecureString $connectionString -AsPlainText -Force
        $connectionStringSecure | ConvertFrom-SecureString -AsPlainText | `
            Set-AzKeyVaultSecret -VaultName $VaultName -Name "postgresql-connection-string" -Force | Out-Null
        
        Write-Success "PostgreSQL server $ServerName created."
        Write-Info "Credentials stored in Key Vault."
    }
}

function New-AppServicePlan {
    param(
        [string]$ResourceGroupName,
        [string]$PlanName
    )
    
    Write-Info "Creating App Service Plan: $PlanName"
    
    $plan = Get-AzAppServicePlan -ResourceGroupName $ResourceGroupName -Name $PlanName -ErrorAction SilentlyContinue
    
    if ($plan) {
        Write-Success "App Service Plan $PlanName already exists."
    }
    else {
        New-AzAppServicePlan `
            -ResourceGroupName $ResourceGroupName `
            -Name $PlanName `
            -Location $Location `
            -Linux `
            -Tier B2 | Out-Null
        
        Write-Success "App Service Plan $PlanName created."
    }
}

function New-WebApp {
    param(
        [string]$ResourceGroupName,
        [string]$AppName,
        [string]$PlanName
    )
    
    Write-Info "Creating Web App: $AppName"
    
    $app = Get-AzWebApp -ResourceGroupName $ResourceGroupName -Name $AppName -ErrorAction SilentlyContinue
    
    if ($app) {
        Write-Success "Web App $AppName already exists."
    }
    else {
        New-AzWebApp `
            -ResourceGroupName $ResourceGroupName `
            -Name $AppName `
            -Location $Location `
            -AppServicePlan $PlanName | Out-Null
        
        Write-Success "Web App $AppName created."
    }
}

function Set-AppSettings {
    param(
        [string]$ResourceGroupName,
        [string]$AppName,
        [hashtable]$Settings
    )
    
    Write-Info "Configuring app settings for $AppName..."
    
    $appSettings = @()
    foreach ($key in $Settings.Keys) {
        $appSettings += @{
            Name  = $key
            Value = $Settings[$key]
        }
    }
    
    Set-AzWebApp -ResourceGroupName $ResourceGroupName -Name $AppName -AppSettings $appSettings | Out-Null
    
    Write-Success "App settings configured for $AppName."
}

function Build-and-Push-Images {
    Write-Info "Building and pushing Docker images..."
    
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker is required for building images."
        return $false
    }
    
    # Get ACR credentials
    $acr = Get-AzContainerRegistry -ResourceGroupName $RgName -Name $AcrName
    $acrCreds = Get-AzContainerRegistryCredential -Registry $acr
    
    # Login to ACR
    $acrCreds.Password | docker login "$($AcrName).azurecr.io" -u $acrCreds.Username --password-stdin
    
    # Build and push backend image
    Write-Info "Building backend image..."
    docker build -t "$($AcrName).azurecr.io/dms-backend:latest" -f "$ProjectRoot\backend\Dockerfile" "$ProjectRoot\backend"
    
    Write-Info "Pushing backend image..."
    docker push "$($AcrName).azurecr.io/dms-backend:latest"
    
    # Build and push frontend image
    Write-Info "Building frontend image..."
    docker build -t "$($AcrName).azurecr.io/dms-frontend:latest" -f "$ProjectRoot\frontend\Dockerfile" "$ProjectRoot\frontend"
    
    Write-Info "Pushing frontend image..."
    docker push "$($AcrName).azurecr.io/dms-frontend:latest"
    
    Write-Success "Docker images built and pushed successfully."
    return $true
}

function Enable-ManagedIdentity {
    Write-Info "Enabling managed identity for App Services..."
    
    Update-AzWebApp -ResourceGroupName $RgName -Name $BackendAppName -IdentityType SystemAssigned | Out-Null
    Update-AzWebApp -ResourceGroupName $RgName -Name $FrontendAppName -IdentityType SystemAssigned | Out-Null
    
    Write-Success "Managed identities enabled."
}

function Setup-Infrastructure {
    Write-Info "Setting up Azure infrastructure for $Environment environment..."
    
    Test-Prerequisites
    Connect-AzureAccount
    New-ResourceGroup -Name $RgName
    New-KeyVault -ResourceGroupName $RgName -VaultName $KvName
    New-PostgreSQLServer -ResourceGroupName $RgName -ServerName $PsqlServerName -DatabaseName $PsqlDbName -AdminUsername $PsqlAdminUser -VaultName $KvName
    New-ContainerRegistry -ResourceGroupName $RgName -RegistryName $AcrName
    New-AppServicePlan -ResourceGroupName $RgName -PlanName $AppPlanName
    New-WebApp -ResourceGroupName $RgName -AppName $BackendAppName -PlanName $AppPlanName
    New-WebApp -ResourceGroupName $RgName -AppName $FrontendAppName -PlanName $AppPlanName
    Enable-ManagedIdentity
    
    # Configure app settings
    $dbConnectionString = (Get-AzKeyVaultSecret -VaultName $KvName -Name "postgresql-connection-string").SecretValueText
    
    $backendSettings = @{
        "SPRING_DATASOURCE_URL"            = $dbConnectionString
        "SPRING_DATASOURCE_USERNAME"       = $PsqlAdminUser
        "SPRING_JPA_HIBERNATE_DDL_AUTO"    = "validate"
        "SPRING_PROFILES_ACTIVE"           = "prod"
        "LOGGING_LEVEL_ROOT"               = "INFO"
    }
    
    $frontendSettings = @{
        "API_BASE_URL" = "https://$BackendAppName.azurewebsites.net"
    }
    
    Set-AppSettings -ResourceGroupName $RgName -AppName $BackendAppName -Settings $backendSettings
    Set-AppSettings -ResourceGroupName $RgName -AppName $FrontendAppName -Settings $frontendSettings
    
    Write-Success "Infrastructure setup completed!"
}

function Deploy-Application {
    Write-Info "Deploying application to $Environment environment..."
    
    Connect-AzureAccount
    
    if (Build-and-Push-Images) {
        Write-Info "Deploying containers to App Services..."
        
        $acr = Get-AzContainerRegistry -ResourceGroupName $RgName -Name $AcrName
        $acrCreds = Get-AzContainerRegistryCredential -Registry $acr
        
        # Update backend app with container image
        $appSettings = Get-AzWebApp -ResourceGroupName $RgName -Name $BackendAppName | Select-Object -ExpandProperty SiteConfig -ErrorAction SilentlyContinue
        
        Write-Success "Deployment initiated!"
        Write-Info "Monitor deployment in Azure Portal: https://portal.azure.com"
    }
}

function Show-DeploymentInfo {
    Write-Info "Deployment information for $Environment environment:"
    Write-Host ""
    Write-Host "Resource Group: $RgName"
    Write-Host "Container Registry: $AcrName.azurecr.io"
    Write-Host "Backend URL: https://${BackendAppName}.azurewebsites.net"
    Write-Host "Frontend URL: https://${FrontendAppName}.azurewebsites.net"
    Write-Host "PostgreSQL Server: ${PsqlServerName}.postgres.database.azure.com"
    Write-Host "Database Name: $PsqlDbName"
    Write-Host "Key Vault: $KvName"
    Write-Host ""
}

function Cleanup-Resources {
    Write-Warning "Deleting all resources in $RgName..."
    $response = Read-Host "Are you sure? Type 'yes' to confirm"
    
    if ($response -eq 'yes') {
        Remove-AzResourceGroup -Name $RgName -Force -AsJob
        Write-Success "Resource group deletion initiated."
    }
    else {
        Write-Info "Cleanup cancelled."
    }
}

################################################################################
# Main Script
################################################################################

switch ($Action) {
    'create' {
        Setup-Infrastructure
        Show-DeploymentInfo
    }
    'deploy' {
        Deploy-Application
        Show-DeploymentInfo
    }
    'info' {
        Show-DeploymentInfo
    }
    'cleanup' {
        Cleanup-Resources
    }
    default {
        Write-Error "Unknown action: $Action"
        exit 1
    }
}
