# Authentication Multiservices

This repository contains a robust microservices architecture for authentication and policy management, built with Spring Boot (Java 17). It includes full-stack observability using **OpenTelemetry (OTel)**, exporting metrics, logs, and distributed traces to **New Relic**.

## Services
- `user-service`
- `auth-service`
- `gateway-service`
- `policy-service`

## Full-Stack OpenTelemetry Implementation Guide

The following steps outline how full-stack OpenTelemetry is implemented in this Kubernetes cluster, and how to validate the setup.

### Prerequisites
- A running Kubernetes cluster.
- Helm installed.
- New Relic License Key configured (used in `custom-1.yaml`).

### Step 1: Create the Observability Namespace
```bash
kubectl create ns observability
```

### Step 2: Deploy the OpenTelemetry Collector
We use Helm to deploy the OpenTelemetry Collector. The collector is configured via `otel-collector-k8s.yaml` to receive telemetry via OTLP, collect Kubernetes host/kubelet metrics, and export data directly to New Relic (`otlphttp/newrelic`).

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update

# Install the OTel Collector using the custom configuration
helm upgrade --install otel-collector open-telemetry/opentelemetry-collector -n observability -f otel-collector-k8s.yaml
```

### Step 3: Deploy the OpenTelemetry Operator
The OpenTelemetry Operator manages the automatic injection of instrumentation libraries (e.g., OpenTelemetry Java agent) into our microservices pods.

```bash
helm upgrade --install otel-operator open-telemetry/opentelemetry-operator \
  --namespace observability \
  --set admissionWebhooks.certManager.enabled=false \
  --set admissionWebhooks.autoGenerateCert.enabled=true
```

### Step 4: Apply the Instrumentation Resource
Ensure the instrumentation CR (Custom Resource) is applied to the cluster, which tells the Operator how to instrument the Spring Boot apps (typically `spring-instrumentation`). Apply it using your configuration file:

```bash
kubectl apply -f instrumentation_otel.yaml --namespace observability
```

*You can verify the instrumentation resource:*
```bash
kubectl get instrumentation -n observability
kubectl describe instrumentation spring-instrumentation -n observability
```

### Step 5: Add Annotation to Deployments
To enable automatic instrumentation, update the `spec.template.metadata.annotations` in your microservice deployment files (such as `user-service.yaml`) to inject the OpenTelemetry Java Agent from the `observability` namespace:

```yaml
      annotations:
        instrumentation.opentelemetry.io/inject-java: "observability/spring-instrumentation"
```

### Step 6: Restart the Services
To apply the automatic instrumentation, restart the Deployments/Daemonsets. The Operator will inject the `JAVA_TOOL_OPTIONS` with the `-javaagent` automatically.

```bash
# Restart Operator and Collector if necessary
kubectl rollout restart deployment/otel-operator-opentelemetry-operator -n observability
kubectl rollout restart daemonset/otel-collector-opentelemetry-collector-agent -n observability

# Restart microservices to inject the OTel Java Agent
# (Replace your-app-namespace with the namespace where your apps are deployed, e.g., default)
kubectl rollout restart deployment user-service auth-service gateway-service policy-service
```

---

## Testing & Validation

Once the services are up and running, follow these steps to verify telemetry is flowing successfully.

### 1. Verify Java Agent Injection
Check if the OpenTelemetry Java agent was successfully injected into the containers.
```bash
# Verify JAVA_TOOL_OPTIONS includes the OTEL javaagent
# (Replace <pod-name> with the actual name of your microservice pod)
kubectl exec <pod-name> -- env | grep JAVA_TOOL_OPTIONS
```

### 2. Verify Operator & Collector Logs
Check the logs to ensure there are no connection drops or RBAC permission issues.
```bash
# Check OTel Operator logs for successful instrumentation injection
kubectl logs -l app.kubernetes.io/name=opentelemetry-operator -n observability -c manager | grep instrumentation

# Check OTel Collector logs to see if data is being exported
kubectl logs -l app.kubernetes.io/name=opentelemetry-collector -n observability -f
```

### 3. Test Network Connectivity
Verify that the microservices can reach the OpenTelemetry Collector on the OTLP port (4317).
```bash
# Verify connectivity from app pod to collector
kubectl exec <app-pod> -- curl -v http://otel-collector-opentelemetry-collector.observability.svc.cluster.local:4317
```

### 4. Verify in New Relic
1. Open your New Relic Dashboard.
2. Go to **APM & Services**. You should see `user-service`, `auth-service`, `gateway-service`, and `policy-service` listed.
3. Click on **Distributed Tracing** to view end-to-end request flows across the microservices.
4. Check **Infrastructure** or **Kubernetes** to see cluster metrics.
