#!/bin/bash

API_URL="${1:-http://localhost:8080}"

echo "Generating test transactions..."

for i in {1..100}; do
    AMOUNT=$((RANDOM % 10000 + 1))
    ACCOUNT="ACC$(printf "%05d" $((RANDOM % 1000)))"
    
    curl -X POST "$API_URL/api/v1/transactions" \
        -H "Content-Type: application/json" \
        -d "{
            \"accountId\": \"$ACCOUNT\",
            \"amount\": $AMOUNT,
            \"currency\": \"USD\",
            \"type\": \"PURCHASE\",
            \"merchant\": \"Test Merchant $i\",
            \"merchantCategory\": \"RETAIL\",
            \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",
            \"location\": {
                \"city\": \"New York\",
				\"country\": \"US\",
				\"ipAddress\": \"192.168.1.$((RANDOM % 255))\"
			},
			\"deviceInfo\": {
				\"deviceId\": \"device-$i\",
				\"deviceType\": \"mobile\",
				\"operatingSystem\": \"iOS\"
			}
			}" 
			-s -o /dev/null -w "%{http_code}\n"
			
	sleep 0.1
			
done

echo "Generated 100 test transactions"