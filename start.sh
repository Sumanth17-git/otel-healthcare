#!/bin/bash
set -e

BASE_DIR="/home/terraform17/healthcare-multiservices"

echo "Starting healthcare microservices stack..."

echo "Applying patient-service..."
kubectl apply -f $BASE_DIR/patient-service/

echo "Applying compliance-service..."
kubectl apply -f $BASE_DIR/compliance-service/

echo "Applying healthcare-auth-service..."
kubectl apply -f $BASE_DIR/healthcare-auth-service/

echo "Applying healthcare-gateway..."
kubectl apply -f $BASE_DIR/healthcare-gateway/

echo "Waiting for pods to be ready..."
kubectl rollout status deployment/patient-service
kubectl rollout status deployment/compliance-service
kubectl rollout status deployment/healthcare-auth-service
kubectl rollout status deployment/healthcare-gateway

echo "All services started."
kubectl get pods
kubectl get svc

