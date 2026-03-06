#!/bin/bash
set -e

BASE_DIR="/home/terraform17/healthcare-multiservices"

echo "Stopping healthcare microservices stack..."

echo "Deleting healthcare-gateway..."
kubectl delete -f $BASE_DIR/healthcare-gateway/ --ignore-not-found

echo "Deleting healthcare-auth-service..."
kubectl delete -f $BASE_DIR/healthcare-auth-service/ --ignore-not-found

echo "Deleting compliance-service..."
kubectl delete -f $BASE_DIR/compliance-service/ --ignore-not-found

echo "Deleting patient-service..."
kubectl delete -f $BASE_DIR/patient-service/ --ignore-not-found

echo "All services stopped."
kubectl get pods

