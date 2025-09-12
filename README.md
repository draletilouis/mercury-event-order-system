# Mercury Order System - CI/CD & Infrastructure Guide

This guide provides comprehensive instructions for setting up a complete CI/CD pipeline with infrastructure as code for the Mercury Order System.

## 🏗️ Architecture Overview

The Mercury Order System is a microservices architecture with the following components:

- **Services**: API Gateway, Inventory, Orders, Payments
- **Infrastructure**: AWS EKS, RDS PostgreSQL, MSK Kafka, VPC
- **Observability**: OpenTelemetry, Prometheus, Grafana, Loki
- **CI/CD**: GitHub Actions with Docker builds and Helm deployments

## 📋 Prerequisites

### Required Tools
- [Terraform](https://www.terraform.io/downloads.html) >= 1.0
- [AWS CLI](https://aws.amazon.com/cli/) >= 2.0
- [kubectl](https://kubernetes.io/docs/tasks/tools/) >= 1.28
- [Helm](https://helm.sh/docs/intro/install/) >= 3.0
- [Docker](https://docs.docker.com/get-docker/) >= 20.0

### AWS Account Setup
1. Create an AWS account with appropriate permissions
2. Configure AWS CLI with your credentials:
   ```bash
   aws configure
   ```
3. Create an IAM user with the following policies:
   - `AmazonEKSClusterPolicy`
   - `AmazonEKSWorkerNodePolicy`
   - `AmazonEKS_CNI_Policy`
   - `AmazonRDSFullAccess`
   - `AmazonMSKFullAccess`
   - `AmazonVPCFullAccess`

## 🚀 Quick Start

### 1. Infrastructure Setup

#### Clone and Configure
```bash
git clone <your-repo-url>
cd mercury-order-system
```

#### Configure Terraform
```bash
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your specific values
```

#### Deploy Infrastructure
```bash
terraform init
terraform plan
terraform apply
```

This will create:
- VPC with public/private subnets
- EKS cluster with managed node groups
- RDS PostgreSQL instance
- MSK Kafka cluster
- Security groups and IAM roles

### 2. Configure kubectl
```bash
aws eks update-kubeconfig --region us-west-2 --name mercury
```

### 3. Deploy Services with Helm

#### Install Dependencies
```bash
helm dependency update helm/mercury
```

#### Deploy to Staging
```bash
helm upgrade --install mercury-staging ./helm/mercury \
  --namespace mercury-staging \
  --create-namespace \
  --set image.tag=develop \
  --set environment=staging
```

#### Deploy to Production
```bash
helm upgrade --install mercury-production ./helm/mercury \
  --namespace mercury-production \
  --create-namespace \
  --set image.tag=main \
  --set environment=production
```

## 🔧 CI/CD Pipeline

### GitHub Actions Setup

The CI/CD pipeline is configured in `.github/workflows/ci-cd.yml` and includes:

1. **Build & Test**: Compiles code and runs tests
2. **Docker Build**: Creates container images for all services
3. **Push to Registry**: Pushes images to GitHub Container Registry
4. **Deploy**: Deploys to staging (develop branch) or production (main branch)

### Required GitHub Secrets

Configure these secrets in your GitHub repository:

```bash
# AWS Credentials
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# Container Registry (automatically provided)
GITHUB_TOKEN=auto-generated
```

### Pipeline Triggers

- **Pull Requests**: Build and test only
- **Push to develop**: Build, test, Docker build, push, deploy to staging
- **Push to main**: Build, test, Docker build, push, deploy to production

## 📊 Observability Stack

### OpenTelemetry Tracing

The system includes comprehensive distributed tracing:

- **Automatic instrumentation** for Spring Boot applications
- **Custom spans** for business logic using `@Traced` annotation
- **Jaeger** for trace visualization
- **OTLP** export for compatibility

### Prometheus & Grafana

- **Prometheus** scrapes metrics from all services
- **Grafana** provides dashboards for:
  - Request rates and response times
  - Error rates and availability
  - JVM metrics and resource usage
  - Custom business metrics

### Loki Logging

- **Loki** aggregates logs from all services
- **Promtail** collects logs from Kubernetes pods
- **Structured logging** with JSON format
- **Log correlation** with traces via trace IDs

## 🐳 Docker Configuration

Each service has its own Dockerfile with:

- **Multi-stage builds** for optimized image size
- **Non-root user** for security
- **Health checks** for container monitoring
- **OpenTelemetry** instrumentation

### Building Images Locally

```bash
# Build all services
docker build -f services/api-gateway/Dockerfile -t mercury/api-gateway:latest .
docker build -f services/inventory/Dockerfile -t mercury/inventory:latest .
docker build -f services/orders/Dockerfile -t mercury/orders:latest .
docker build -f services/payments/Dockerfile -t mercury/payments:latest .
```

## 🔐 Security Considerations

### Infrastructure Security
- **Private subnets** for application workloads
- **Security groups** with minimal required access
- **IAM roles** with least privilege principle
- **Encryption at rest** for RDS and MSK

### Application Security
- **Non-root containers** in all Docker images
- **Read-only root filesystems** where possible
- **Resource limits** to prevent resource exhaustion
- **Network policies** for service-to-service communication

### Secrets Management
- **AWS Secrets Manager** for database credentials
- **Kubernetes secrets** for application configuration
- **GitHub secrets** for CI/CD pipeline

## 📈 Monitoring & Alerting

### Key Metrics to Monitor

1. **Application Metrics**
   - Request rate and latency
   - Error rates by service
   - Database connection pool usage
   - Kafka consumer lag

2. **Infrastructure Metrics**
   - CPU and memory utilization
   - Disk I/O and network throughput
   - Pod restart counts
   - Node availability

3. **Business Metrics**
   - Order processing rate
   - Payment success rate
   - Inventory levels
   - Customer satisfaction scores

### Recommended Alerts

```yaml
# Example Prometheus alert rules
groups:
  - name: mercury.rules
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
```

## 🛠️ Troubleshooting

### Common Issues

1. **Pod Startup Failures**
   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   kubectl logs <pod-name> -n <namespace>
   ```

2. **Database Connection Issues**
   ```bash
   kubectl get secrets -n <namespace>
   kubectl describe secret <db-secret-name> -n <namespace>
   ```

3. **Kafka Connectivity**
   ```bash
   kubectl exec -it <pod-name> -n <namespace> -- kafka-topics --list --bootstrap-server <kafka-endpoint>
   ```

### Debug Commands

```bash
# Check cluster status
kubectl get nodes
kubectl get pods -A

# Check service endpoints
kubectl get svc -A
kubectl get ingress -A

# Check logs
kubectl logs -f deployment/<service-name> -n <namespace>

# Port forward for local testing
kubectl port-forward svc/<service-name> 8080:80 -n <namespace>
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

For questions or support, please contact the Mercury development team.

