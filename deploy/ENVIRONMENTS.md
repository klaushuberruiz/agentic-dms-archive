# Deployment Environments Configuration

This file documents the configuration for different deployment environments.

## Development (dev)

**Purpose**: Development and testing environment

**Resources**:
- App Service Plan: B2
- PostgreSQL: Standard_B1ms (Burstable), 32GB storage
- Container Registry: Standard

**Access**:
- Frontend: https://dms-web-dev.azurewebsites.net
- Backend: https://dms-api-dev.azurewebsites.net

**Characteristics**:
- Auto-scaling disabled
- Logging level: DEBUG
- Database: Fresh daily (optional)
- Retention: Keep for 7 days

## Staging (staging)

**Purpose**: Pre-production testing environment

**Resources**:
- App Service Plan: B3
- PostgreSQL: Standard_B2 (General purpose), 64GB storage
- Container Registry: Standard with geo-replication

**Access**:
- Frontend: https://dms-web-staging.azurewebsites.net
- Backend: https://dms-api-staging.azurewebsites.net

**Characteristics**:
- Auto-scaling enabled (2-5 instances)
- Logging level: INFO
- Database: Copy of production (automated weekly)
- Retention: Keep for 30 days
- Monitoring: Full Azure Monitor integration

## Production (prod)

**Purpose**: Production environment for end-users

**Resources**:
- App Service Plan: P1V2 or higher
- PostgreSQL: General purpose tier or higher, 256GB+ storage
- Container Registry: Premium with geo-replication

**Access**:
- Frontend: https://dms.yourdomain.com
- Backend: https://api.dms.yourdomain.com (or same domain with /api path)

**Characteristics**:
- Auto-scaling enabled (5-20 instances)
- Logging level: WARNING
- Database: Daily automated backups (7-day retention)
- Monitoring: Full Azure Monitor, Application Insights
- Alerts: Critical errors, performance degradation
- Disaster Recovery: Geo-replicated backups
- Security: Managed identities, private endpoints (optional)
- CDN: For static assets (optional)

## Environment-Specific Configuration

### Spring Boot Application Properties

**development**:
```yaml
spring.datasource.url=jdbc:postgresql://localhost:5432/dms_db
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL15Dialect
spring.jpa.show-sql=true
logging.level.root=DEBUG
```

**production**:
```yaml
spring.datasource.url=${DB_CONNECTION_STRING}
spring.datasource.hikari.maximum-pool-size=20
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL15Dialect
spring.jpa.show-sql=false
logging.level.root=WARN
```

### Angular Environment Configuration

**development** (`frontend/src/environments/environment.ts`):
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  logLevel: 'debug'
};
```

**production** (`frontend/src/environments/environment.prod.ts`):
```typescript
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  logLevel: 'error'
};
```

## Deployment Process

### Development

```bash
# Automatic via GitHub Actions on push to 'develop' branch
# Manual:
./deploy/azure-deploy.sh dev deploy
```

### Staging

```bash
# Create feature branch and PR for review
# Merge to 'staging' branch to trigger deployment
# Manual testing before production release
```

### Production

```bash
# Create PR from 'develop' to 'main'
# Require code review and approval
# Merge to 'main' to trigger production deployment
# Automatic smoke tests validate deployment
```

## Monitoring and Alerts

### All Environments

- Application Insights: Track performance and errors
- Azure Monitor: Infrastructure health
- Log Analytics: Centralized logging

### Production Only

- Critical errors: Page incident on-call
- Response time > 2s: Alert engineering team
- Error rate > 1%: Alert engineering team
- Database CPU > 80%: Scale up or optimize queries
- Storage full: Alert DevOps team

## Backup and Recovery

### Development

- No backup required
- Can rebuild from source

### Staging

- Database: Weekly snapshot
- Retention: 30 days
- Restore time: < 1 hour

### Production

- Database: Automated daily backup
- Retention: 35 days
- Retention policy: Point-in-time recovery (35 days)
- Multiple geo-replicated backups
- Restore time: < 4 hours
- RTO: 4 hours
- RPO: 1 hour

## Scaling Configuration

### Development

- Minimum instances: 1
- Maximum instances: 1
- Always off: Not applicable

### Staging

- Minimum instances: 2
- Maximum instances: 5
- CPU threshold: 70%
- Memory threshold: 75%

### Production

- Minimum instances: 5
- Maximum instances: 20
- CPU threshold: 60%
- Memory threshold: 70%
- Scale up: Within 2 minutes
- Scale down: Within 15 minutes

## Cost Estimates (Monthly)

### Development Environment

- App Service (B2): ~$160
- PostgreSQL (B1ms): ~$40
- Container Registry: ~$100
- Storage: ~$10
- **Total**: ~$310

### Production Environment (Conservative)

- App Service (P1V2, avg 8 instances): ~$1,600
- PostgreSQL (GP, 256GB): ~$400
- Container Registry (Premium): ~$250
- CDN (if enabled): ~$100-500
- Storage: ~$50
- Backup storage: ~$20
- **Total**: ~$2,400-3,000+

## Disaster Recovery Plan

### RTO/RPO Targets

| Scenario | RTO | RPO |
|----------|-----|-----|
| Single server failure | 5 minutes | 0 minutes |
| App Service failure | 1 minute | 0 minutes |
| Region failure | 1 hour | 1 hour |
| Data corruption | 4 hours | 1 hour |

### Recovery Procedures

1. **App Service failure**: Auto-restart via health checks
2. **Database failure**: Restore from backup (< 4 hours)
3. **Data corruption**: Point-in-time restore to 1 hour before
4. **Region failure**: Deploy to alternate region (manual process)

## Compliance and Security

### All Environments

- RBAC: Role-based access control
- Encryption: TLS in transit, encryption at rest
- Key Vault: Secrets management
- Audit logging: All database changes logged

### Production Only

- Private endpoints (optional)
- Network security groups (NSG)
- Web Application Firewall (WAF)
- DDoS protection
- Advanced threat protection
- Compliance: SOC 2, HIPAA (if applicable)
