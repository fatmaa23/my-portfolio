# End-to-End DevOps Project: CI/CD and Monitoring for Portfolio Website

![Project Status](https://img.shields.io/badge/Status-Complete-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s-326CE5)
![Jenkins](https://img.shields.io/badge/CI-Jenkins-D33833)
![ArgoCD](https://img.shields.io/badge/CD-ArgoCD-EF7B4D)

## 📋 Project Overview

This project demonstrates a complete DevOps workflow implementing CI/CD pipelines and monitoring for a static portfolio website. The architecture follows modern DevOps principles including **Automation**, **GitOps**, and **Observability** to create a robust, scalable deployment solution.

### 🎯 Key Objectives
- Implement automated CI/CD pipeline from code commit to production deployment
- Apply GitOps methodology for deployment management
- Establish comprehensive monitoring and observability
- Demonstrate industry-standard DevOps practices

## 🏗️ Architecture

```
GitHub Repository → Jenkins (CI) → Docker Hub → ArgoCD (CD) → Kubernetes Cluster
                                                                      ↓
                                                            Prometheus & Grafana
                                                               (Monitoring)
```

## 🛠️ Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Application** | Static HTML/CSS | Portfolio website |
| **Containerization** | Docker & Docker Hub | Application packaging and registry |
| **Orchestration** | K3s (Lightweight Kubernetes) | Container orchestration |
| **CI Pipeline** | Jenkins | Continuous Integration |
| **CD Pipeline** | ArgoCD | GitOps-based Continuous Deployment |
| **Monitoring** | Prometheus & Grafana | Metrics collection and visualization |
| **Automation** | GitHub Webhooks + ngrok | Automated pipeline triggers |

## 🚀 Features

**Automated CI/CD Pipeline**: Code changes trigger automatic build, test, and deployment with full automation from commit to production.

**GitOps Workflow**: Git repository serves as the single source of truth for all deployment configurations and application state.

**Multi-stage Docker Build**: Optimized containerization with Nginx for efficient static content serving and minimal image size.

**Kubernetes Deployment**: Scalable container orchestration using K3s for lightweight yet robust cluster management.

**Real-time Monitoring**: Comprehensive metrics collection and alerting through Prometheus and Grafana integration.

**Webhook Integration**: Automated pipeline triggers using GitHub webhooks with ngrok for seamless local development.

**Infrastructure as Code**: All configurations are version-controlled and managed through declarative manifests.

## 📁 Project Structure

```
project-root/
├── Dockerfile                 # Multi-stage Docker build
├── Jenkinsfile               # Pipeline configuration
├── k8s/                      # Kubernetes manifests
│   ├── deployment.yaml       # Application deployment
│   └── service.yaml          # Service configuration
├── vars/                     # Jenkins shared library
│   └── buildAndDeploy.groovy # CI pipeline logic
├── src/                      # Application source code
│   ├── index.html
│   └── styles.css
└── README.md
```

## 🔧 Setup Instructions

### Prerequisites

- 2 CentOS VMs (VMware or similar virtualization platform)
  - **Master Node**: 2 vCPU, 2GB RAM
  - **Agent Node**: 1 vCPU, 1GB RAM
- Docker Hub account
- GitHub repository
- Basic knowledge of Kubernetes, Docker, and CI/CD concepts

### Phase 1: Environment Setup

#### 1.1 Kubernetes Cluster (K3s)

**Master Node Setup:**
```bash
# Install K3s server
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644

# Verify installation
sudo k3s kubectl get nodes
```

**Agent Node Setup:**
```bash
# Get token from master node
sudo cat /var/lib/rancher/k3s/server/node-token

# Join agent to cluster
curl -sfL https://get.k3s.io | K3S_URL=https://<MASTER_IP>:6443 K3S_TOKEN=<YOUR_TOKEN> sh -
```

#### 1.2 Application Containerization

Create a `Dockerfile` for the static website:
```dockerfile
# Multi-stage build example
FROM nginx:alpine
COPY src/ /usr/share/nginx/html/
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Phase 2: Jenkins CI Pipeline

#### 2.1 Jenkins Installation (Agent Node)

```bash
# Install dependencies
sudo yum update -y
sudo yum install -y java-11-openjdk git docker

# Install Jenkins
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
sudo yum install -y jenkins

# Start services
sudo systemctl start jenkins docker
sudo systemctl enable jenkins docker

# Add jenkins user to docker group
sudo usermod -aG docker jenkins
```

#### 2.2 Jenkins Configuration

1. Access Jenkins at `http://<agent-node-ip>:8080`
2. Install required plugins: Docker Pipeline, GitHub Integration
3. Configure credentials:
   - Docker Hub credentials
   - GitHub Personal Access Token

#### 2.3 Pipeline Setup

Create a Jenkins Pipeline job pointing to your GitHub repository with the provided `Jenkinsfile`.

### Phase 3: ArgoCD GitOps Deployment

#### 3.1 ArgoCD Installation

```bash
# Create namespace and install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Expose ArgoCD UI
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'

# Get initial admin password
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | base64 -d
```

#### 3.2 Create ArgoCD Application

Apply the ArgoCD Application manifest to deploy your portfolio website.

### Phase 4: Monitoring Setup

#### 4.1 Prometheus & Grafana Installation

```bash
# Install Helm
curl https://get.helm.sh/helm-v3.12.0-linux-amd64.tar.gz | tar xz
sudo mv linux-amd64/helm /usr/local/bin/

# Add Prometheus Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install monitoring stack
kubectl create namespace monitoring
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring
```

#### 4.2 Access Grafana

```bash
# Expose Grafana
kubectl patch svc prometheus-grafana -n monitoring -p '{"spec": {"type": "NodePort"}}'

# Get Grafana admin password
kubectl get secret prometheus-grafana -n monitoring -o jsonpath="{.data.admin-password}" | base64 -d
```

### Phase 5: Webhook Automation

#### 5.1 Setup ngrok (for local development)

```bash
# Download and install ngrok
wget https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip
unzip ngrok-stable-linux-amd64.zip
sudo mv ngrok /usr/local/bin/

# Expose Jenkins
./ngrok http 8080
```

#### 5.2 Configure GitHub Webhook

1. Go to your GitHub repository settings
2. Add webhook with ngrok URL: `<ngrok_url>/github-webhook/`
3. Select "push" events only
4. Set content type to `application/json`

## 🔄 Workflow

1. **Developer pushes code** to GitHub repository
2. **GitHub webhook triggers** Jenkins pipeline automatically
3. **Jenkins pipeline**:
   - Clones repository
   - Builds Docker image
   - Pushes to Docker Hub
   - Updates Kubernetes manifests with new image tag
   - Commits changes back to repository
4. **ArgoCD detects** manifest changes and syncs
5. **New version deploys** to Kubernetes cluster
6. **Prometheus & Grafana** provide monitoring and alerting

## 📊 Monitoring & Observability

- **Prometheus**: Metrics collection from Kubernetes cluster
- **Grafana**: Visualization dashboards and alerting
- **Pre-configured dashboards** for Kubernetes cluster monitoring
- **Real-time metrics** for application and infrastructure health

## 🔍 Accessing Services

| Service | URL | Default Credentials |
|---------|-----|-------------------|
| Jenkins | `http://<agent-node-ip>:8080` | admin / (check initial setup) |
| ArgoCD | `http://<master-node-ip>:<nodeport>` | admin / (kubectl get secret) |
| Grafana | `http://<master-node-ip>:<nodeport>` | admin / (kubectl get secret) |
| Application | `http://<cluster-ip>:<nodeport>` | - |

## 🛡️ Security Considerations

The project implements several security best practices including secrets management for sensitive data storage, Role-Based Access Control (RBAC) for Kubernetes and ArgoCD components, regular security updates for all system components, network policies for proper cluster isolation, and secure webhook endpoints with proper authentication mechanisms.

## 🔧 Troubleshooting

### Common Issues

**Jenkins Docker Connection Problems**: If Jenkins cannot connect to Docker, ensure the jenkins user is added to the docker group using `sudo usermod -aG docker jenkins` and restart the Jenkins service afterward.

**ArgoCD Synchronization Issues**: When experiencing sync problems, verify repository credentials are correctly configured and check that all Kubernetes manifest files have proper YAML syntax.

**Webhook Trigger Failures**: If webhooks are not triggering the pipeline, verify that the ngrok tunnel is active and accessible, and confirm the Jenkins webhook configuration matches the ngrok URL format.

## 📝 Future Enhancements

The project can be extended with automated testing integration in the CI pipeline, security scanning for vulnerability assessment, blue-green or canary deployment strategies for safer releases, automatic rollback mechanisms for failed deployments, integration with external secret management systems like HashiCorp Vault, and multi-environment deployment support for development, staging, and production environments.

## 🤝 Contributing

Contributions are welcome! To contribute to this project, fork the repository and create a feature branch for your changes. Make sure to test your modifications thoroughly before submitting a pull request with a clear description of the improvements or fixes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Fatma Ahmed Abd El-Fadeel**  
ITI - Information Technology Institute  

## 📧 Contact

For questions or support, please open an issue in the GitHub repository.

---

**⭐ If this project helped you, please give it a star!**
