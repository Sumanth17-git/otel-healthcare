# otel-healthcare

A healthcare microservices architecture built with **Spring Boot 3.5** (Java 17), featuring full-stack observability using **OpenTelemetry (OTel)** — exporting distributed traces, metrics, and logs to **New Relic**.

## Services

| Service | Port | Description |
|---|---|---|
| `healthcare-gateway` | 8080 | Spring Cloud Gateway — routes all inbound requests |
| `healthcare-auth-service` | 8083 | Authentication — validates patient credentials, issues tokens |
| `patient-service` | 8081 | Patient registry — validates patient ID, password, and access pin |
| `compliance-service` | 8082 | Compliance check — enforces access policies per patient |

## Architecture

```
Client
  │
  ▼
healthcare-gateway (8080)
  │   routes /auth/** → healthcare-auth-service
  ▼
healthcare-auth-service (8083)
  ├── POST /patients/validate  → patient-service (8081)
  └── POST /compliance/check   → compliance-service (8082)
```

## Tech Stack

- **Spring Boot 3.5.x** — Web, JPA, Actuator, Spring Cloud Gateway
- **OpenTelemetry** — Auto-instrumentation via Java agent (injected by OTel Operator)
- **New Relic** — Observability backend (traces, metrics, logs)
- **Prometheus** — Metrics scraping via Micrometer
- **Logstash JSON logging** — Structured logs with MDC trace context
- **MySQL** — Production database (`healthcaredb`)
- **H2** — In-memory database for local development
- **Kubernetes** — Deployment, Service, ConfigMap YAMLs per service
- **k6** — Load testing script (`k6-test.js`)

---

## Local Development

### Prerequisites
- Java 17+
- Maven wrapper (`./mvnw`) included in each service

### Start all services

```bash
# patient-service (port 8081) — uses H2 in-memory DB locally
cd patient-service
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="\
  -Dspring.datasource.url=jdbc:h2:mem:patientdb;DB_CLOSE_DELAY=-1 \
  -Dspring.datasource.driver-class-name=org.h2.Driver \
  -Dspring.datasource.username=sa \
  -Dspring.datasource.password= \
  -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect"

# compliance-service (port 8082) — uses H2 in-memory DB locally
cd compliance-service
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="\
  -Dspring.datasource.url=jdbc:h2:mem:compliancedb;DB_CLOSE_DELAY=-1 \
  -Dspring.datasource.driver-class-name=org.h2.Driver \
  -Dspring.datasource.username=sa \
  -Dspring.datasource.password= \
  -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect"

# healthcare-auth-service (port 8083)
cd healthcare-auth-service
./mvnw spring-boot:run

# healthcare-gateway (port 8080)
# AUTH_SERVICE_URL defaults to http://localhost:8083 — no env var needed
cd healthcare-gateway
./mvnw spring-boot:run
```

### Test the login endpoint

```bash
# Valid patient
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"patientId":"patient001","password":"pass123","accessPin":"4321"}'

# Blocked user (returns 500)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"patientId":"staff001","password":"admin123","accessPin":"9999"}'

# Gateway health
curl http://localhost:8080/health
```

### Run k6 load tests

```bash
k6 run k6-test.js
```

---

## Kubernetes Deployment

### Prerequisites
- A running Kubernetes cluster
- Helm installed
- New Relic License Key

### Step 1: Create the Observability Namespace

```bash
kubectl create ns observability
```

### Step 2: Deploy the OpenTelemetry Collector

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update

helm upgrade --install otel-collector open-telemetry/opentelemetry-collector \
  -n observability -f otel-collector-allinone.yaml
```

### Step 3: Deploy the OpenTelemetry Operator

```bash
helm upgrade --install otel-operator open-telemetry/opentelemetry-operator \
  --namespace observability \
  --set admissionWebhooks.certManager.enabled=false \
  --set admissionWebhooks.autoGenerateCert.enabled=true
```

### Step 4: Apply RBAC and Instrumentation Resource

```bash
kubectl apply -f otel-rbac.yaml
kubectl apply -f instrumentation_otel.yaml --namespace observability

# Verify
kubectl get instrumentation -n observability
kubectl describe instrumentation spring-instrumentation -n observability
```

### Step 5: Deploy MySQL

```bash
kubectl apply -f mysql-server.yaml
```

### Step 6: Deploy All Microservices

```bash
# Apply ConfigMaps first
kubectl apply -f patient-service/patient-service-config.yaml
kubectl apply -f compliance-service/compliance-service-config.yaml
kubectl apply -f healthcare-auth-service/healthcare-auth-config.yaml
kubectl apply -f healthcare-gateway/healthcare-gateway-config.yaml

# Deploy services
kubectl apply -f patient-service/patient-service.yaml
kubectl apply -f compliance-service/compliance-service.yaml
kubectl apply -f healthcare-auth-service/healthcare-auth-service.yaml
kubectl apply -f healthcare-gateway/healthcare-gateway.yaml
```

### Step 7: Restart to Inject OTel Java Agent

```bash
kubectl rollout restart deployment/otel-operator-opentelemetry-operator -n observability
kubectl rollout restart daemonset/otel-collector-opentelemetry-collector-agent -n observability

kubectl rollout restart deployment \
  patient-service compliance-service healthcare-auth-service healthcare-gateway
```

---

## Configuration

### Gateway Routing (Environment-Driven)

The gateway route URI is controlled by the `AUTH_SERVICE_URL` environment variable:

| Environment | Value |
|---|---|
| Local | `http://localhost:8083` (default, no env var needed) |
| Kubernetes | `http://healthcare-auth-service:8083` (set in `healthcare-gateway.yaml`) |

To override locally:
```bash
AUTH_SERVICE_URL=http://my-host:8083 java -jar healthcare-gateway.jar
```

---

## Validation

### Verify OTel Java Agent Injection

```bash
kubectl exec <pod-name> -- env | grep JAVA_TOOL_OPTIONS
```

### Verify OTel Operator and Collector Logs

```bash
kubectl logs -l app.kubernetes.io/name=opentelemetry-operator -n observability -c manager | grep instrumentation
kubectl logs -l app.kubernetes.io/name=opentelemetry-collector -n observability -f
```

### Verify Network Connectivity to Collector

```bash
kubectl exec <app-pod> -- curl -v \
  http://otel-collector-opentelemetry-collector.observability.svc.cluster.local:4317
```

### Verify in New Relic

1. Open **New Relic → APM & Services** — you should see `healthcare-auth-service`, `patient-service`, `compliance-service`, `healthcare-gateway`
2. Go to **Distributed Tracing** to view end-to-end request flows
3. Check **Infrastructure → Kubernetes** for cluster metrics
