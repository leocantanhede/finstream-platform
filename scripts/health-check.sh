#!/bin/bash

NAMESPACE="${1:-finstream-dev}"

echo "Checking health of services in namespace: $NAMESPACE"

SERVICES=("transaction-ingestion" "fraud-detection" "analytics-service")

for SERVICE in "${SERVICES[@]}"; do
    echo "Checking $SERVICE..."
    
    POD=$(kubectl get pods -n $NAMESPACE -l app=$SERVICE -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    
    if [ -z "$POD" ]; then
        echo "❌ No pods found for $SERVICE"
        continue
    fi
    
    HEALTH=$(kubectl exec -n $NAMESPACE $POD -- curl -s http://localhost:8080/actuator/health | jq -r '.status')
    
    if [ "$HEALTH" == "UP" ]; then
        echo "✅ $SERVICE is healthy"
    else
        echo "❌ $SERVICE is unhealthy: $HEALTH"
    fi
done