#!/bin/bash

# Istio Installation Script for Gogidix Property Marketplace
# Enterprise Service Mesh Installation

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ISTIO_VERSION="1.20.3"
ISTIO_NAMESPACE="istio-system"
ISTIO_PROFILE="default"
VALUES_FILE="istio-values.yaml"

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN} Installing Istio Service Mesh${NC}"
echo -e "${GREEN} Version: ${ISTIO_VERSION}${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        exit 1
    fi

    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        echo -e "${RED}Error: helm is not installed${NC}"
        exit 1
    fi

    # Check if cluster is accessible
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi

    # Check Kubernetes version
    K8S_VERSION=$(kubectl version --short | grep 'Server Version' | awk '{print $3}')
    echo -e "${GREEN}Kubernetes version: ${K8S_VERSION}${NC}"

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Download Istio
download_istio() {
    echo -e "${YELLOW}Downloading Istio ${ISTIO_VERSION}...${NC}"

    if [ ! -d "istio-${ISTIO_VERSION}" ]; then
        curl -L "https://istio.io/downloadIstio" | ISTIO_VERSION=${ISTIO_VERSION} sh -
    fi

    export PATH=$PWD/istio-${ISTIO_VERSION}/bin:$PATH

    # Verify installation
    istioctl version --remote=false
    echo -e "${GREEN}✓ Istio downloaded successfully${NC}"
}

# Create Istio namespace
create_namespace() {
    echo -e "${YELLOW}Creating Istio namespace...${NC}"

    kubectl create namespace ${ISTIO_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace ${ISTIO_NAMESPACE} istio-injection=enabled --overwrite

    echo -e "${GREEN}✓ Namespace created${NC}"
}

# Create Istio values file
create_values_file() {
    echo -e "${YELLOW}Creating Istio values file...${NC}"

    cat > ${VALUES_FILE} <<EOF
# Istio Configuration for Gogidix Property Marketplace

global:
  controlPlaneSecurityEnabled: true
  mtls:
    enabled: true
  meshID: mesh1
  clusterName: gogidix-cluster
  network: gogidix-network
  hub: docker.io/istio
  tag: ${ISTIO_VERSION}

# Pilot configuration
pilot:
  enabled: true
  autoscaleEnabled: true
  autoscaleMin: 2
  autoscaleMax: 5
  replicaCount: 2
  resources:
    requests:
      cpu: 500m
      memory: 2048Mi
    limits:
      cpu: 1000m
      memory: 4096Mi
  tracing:
    enabled: true
    sampling: 100

# Gateway configuration
gateways:
  istio-ingressgateway:
    enabled: true
    autoscaleEnabled: true
    autoscaleMin: 2
    autoscaleMax: 5
    replicaCount: 2
    resources:
      requests:
        cpu: 100m
        memory: 128Mi
      limits:
        cpu: 2000m
        memory: 1024Mi
    type: LoadBalancer
    # Custom annotations for cloud provider
    annotations: |
      service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
      service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
    ports:
    - name: http2
      port: 80
      targetPort: 8080
    - name: https
      port: 443
      targetPort: 8443

  istio-egressgateway:
    enabled: true
    autoscaleEnabled: true
    autoscaleMin: 1
    autoscaleMax: 3
    replicaCount: 1

# Policy and telemetry
policy:
  enabled: true

telemetry:
  enabled: true
  v2:
    enabled: true

# Kiali configuration
kiali:
  enabled: true
  createDemoSecret: false
  dashboard:
    viewOnly: false
  auth:
    strategy: "login"
  prometheus:
    namespace: "istio-system"
  grafana:
    enabled: true
    inClusterURL: "http://grafana.istio-system:3000"
  jaeger:
    enabled: true
    inClusterURL: "http://jaeger.istio-system:16686"

# Grafana configuration
grafana:
  enabled: true
  persistence:
    enabled: true
    size: 10Gi
  env:
    GF_SECURITY_ADMIN_PASSWORD: "gogidix123!"
    GF_INSTALL_PLUGINS: "grafana-piechart-panel"

# Jaeger configuration
tracing:
  enabled: true
  jaeger:
    template: all-in-one
    persistence:
      enabled: true
      size: 10Gi

# Cert-Manager configuration
certmanager:
  enabled: false

# Sidecar injector configuration
sidecarInjectorWebhook:
  enabled: true
  replicaCount: 2
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 500m
      memory: 512Mi

# Node agent configuration
nodeagent:
  enabled: true

# CNI configuration
cni:
  enabled: true

# Configuration for performance
meshConfig:
  defaultConfig:
    concurrency: 2
    proxyStatsMatcher:
      inclusionRegexps:
      - ".*_cx_.*"
      - ".*_tcp_.*"
    holdApplicationUntilProxyStarts: true
    proxyMetadata:
      PILOT_ENABLE_WORKLOAD_ENTRY: "true"
      PILOT_TRACE_SAMPLING: "100"

# Enable telemetry v2
defaultSettings:
  pilot: "policy.enabled=true,telemetry.enabled=true"
EOF

    echo -e "${GREEN}✓ Values file created${NC}"
}

# Install Istio
install_istio() {
    echo -e "${YELLOW}Installing Istio...${NC}"

    # Install base Istio
    istioctl install --set valuesFile=${VALUES_FILE} -y

    echo -e "${GREEN}✓ Istio base installed${NC}"

    # Install Istio with custom profile
    istioctl install -f ${VALUES_FILE} -y

    echo -e "${GREEN}✓ Istio installed successfully${NC}"
}

# Verify installation
verify_installation() {
    echo -e "${YELLOW}Verifying Istio installation...${NC}"

    # Wait for pods to be ready
    kubectl wait --for=condition=ready pod -l app=istiod -n ${ISTIO_NAMESPACE} --timeout=300s
    kubectl wait --for=condition=ready pod -l app=istio-ingressgateway -n ${ISTIO_NAMESPACE} --timeout=300s

    # Check pod status
    echo -e "${YELLOW}Pods in ${ISTIO_NAMESPACE}:${NC}"
    kubectl get pods -n ${ISTIO_NAMESPACE}

    # Check services
    echo -e "${YELLOW}Services in ${ISTIO_NAMESPACE}:${NC}"
    kubectl get svc -n ${ISTIO_NAMESPACE}

    # Verify control plane
    istioctl verify-install

    echo -e "${GREEN}✓ Istio verification completed${NC}"
}

# Create example application for testing
create_example_app() {
    echo -e "${YELLOW}Creating example application for testing...${NC}"

    # Create namespace for test app
    kubectl create namespace test-app --dry-run=client -o yaml | kubectl apply -f -
    kubectl label namespace test-app istio-injection=enabled --overwrite

    # Create httpbin application
    cat > httpbin.yaml <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: httpbin
  namespace: test-app
---
apiVersion: v1
kind: Service
metadata:
  name: httpbin
  namespace: test-app
  labels:
    app: httpbin
spec:
  ports:
  - name: http
    port: 8000
    targetPort: 8080
  selector:
    app: httpbin
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: httpbin
  namespace: test-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: httpbin
      version: v1
  template:
    metadata:
      labels:
        app: httpbin
        version: v1
    spec:
      serviceAccountName: httpbin
      containers:
      - name: httpbin
        image: kennethreitz/httpbin
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
EOF

    kubectl apply -f httpbin.yaml

    # Create sleep application
    cat > sleep.yaml <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: sleep
  namespace: test-app
---
apiVersion: v1
kind: Service
metadata:
  name: sleep
  namespace: test-app
  labels:
    app: sleep
spec:
  ports:
  - port: 80
    name: http
  selector:
    app: sleep
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sleep
  namespace: test-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: sleep
      version: v1
  template:
    metadata:
      labels:
        app: sleep
        version: v1
    spec:
      serviceAccountName: sleep
      containers:
      - name: sleep
        image: curlimages/curl
        command: ["/bin/sleep", "infinity"]
        imagePullPolicy: IfNotPresent
EOF

    kubectl apply -f sleep.yaml

    echo -e "${GREEN}✓ Example application created${NC}"
}

# Create Istio gateways
create_gateways() {
    echo -e "${YELLOW}Creating Istio gateways...${NC}"

    cat > gogidix-gateway.yaml <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: gogidix-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "*"
    tls:
      httpsRedirect: true
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: gogidix-tls-secret
    hosts:
    - api.gogidix.com
    - admin.gogidix.com
    - "*.gogidix.com"
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: gogidix-root
  namespace: istio-system
spec:
  hosts:
  - "*"
  gateways:
  - gogidix-gateway
  http:
  - match:
    - uri:
        prefix: /
    directResponse:
      status: 200
      body:
        string: "Gogidix Property Marketplace - Service Mesh Active"
EOF

    kubectl apply -f gogidix-gateway.yaml

    echo -e "${GREEN}✓ Gateways created${NC}"
}

# Setup monitoring
setup_monitoring() {
    echo -e "${YELLOW}Setting up monitoring...${NC}"

    # Create Prometheus configuration
    cat > prometheus-config.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: istio-prometheus
  namespace: istio-system
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    rule_files:
    - "/etc/prometheus/rules/*.rules"
    scrape_configs:
    - job_name: 'istiod'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - istio-system
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istiod;http-monitoring
    - job_name: 'envoy-stats'
      kubernetes_sd_configs:
      - role: endpoints
        namespaces:
          names:
          - istio-system
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: istio-proxy;http-monitoring
EOF

    kubectl apply -f prometheus-config.yaml

    echo -e "${GREEN}✓ Monitoring setup completed${NC}"
}

# Install add-ons
install_addons() {
    echo -e "${YELLOW}Installing Istio add-ons...${NC}"

    # Install Kiali
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION}/samples/addons/kiali.yaml

    # Install Grafana
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION}/samples/addons/grafana.yaml

    # Install Jaeger
    kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-${ISTIO_VERSION}/samples/addons/jaeger.yaml

    echo -e "${GREEN}✓ Add-ons installed${NC}"
}

# Print access information
print_access_info() {
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN} Installation Complete!${NC}"
    echo -e "${GREEN}=========================================${NC}"

    echo -e "\n${YELLOW}To access the Istio dashboards:${NC}"
    echo -e "Kiali: ${GREEN}kubectl port-forward -n istio-system svc/kiali 20001:20001${NC}"
    echo -e "Grafana: ${GREEN}kubectl port-forward -n istio-system svc/grafana 3000:3000${NC}"
    echo -e "Jaeger: ${GREEN}kubectl port-forward -n istio-system svc/jaeger 16686:16686${NC}"
    echo -e "Prometheus: ${GREEN}kubectl port-forward -n istio-system svc/prometheus 9090:9090${NC}"

    echo -e "\n${YELLOW}Default credentials:${NC}"
    echo -e "Kiali: ${GREEN}admin / gogidix123!${NC}"
    echo -e "Grafana: ${GREEN}admin / gogidix123!${NC}"

    echo -e "\n${YELLOW}Next steps:${NC}"
    echo -e "1. Label your namespaces with: ${GREEN}kubectl label namespace <namespace> istio-injection=enabled${NC}"
    echo -e "2. Deploy your applications - they will automatically be injected with sidecars"
    echo -e "3. Configure mTLS policies for your services"
    echo -e "4. Set up traffic management rules"
}

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    rm -f httpbin.yaml sleep.yaml gogidix-gateway.yaml prometheus-config.yaml
    echo -e "${GREEN}✓ Cleanup completed${NC}"
}

# Trap cleanup on script exit
trap cleanup EXIT

# Main execution
main() {
    check_prerequisites
    download_istio
    create_namespace
    create_values_file
    install_istio
    verify_installation
    install_addons
    create_gateways
    create_example_app
    setup_monitoring
    print_access_info
}

# Run main function
main "$@"