#!/bin/bash

set -e

echo "🚀 Setting up FinStream Platform locally..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📦 Starting infrastructure services...${NC}"
cd infrastructure/docker
docker-compose up -d mysql redis zookeeper kafka

echo -e "${BLUE}⏳ Waiting for services to be ready...${NC}"
sleep 30

# Wait for Kafka to be ready
echo -e "${BLUE}Waiting for Kafka...${NC}"
until docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "Kafka not ready yet..."
    sleep 5
done

echo -e "${GREEN}✅ Kafka is ready${NC}"

# Create Kafka topics
echo -e "${BLUE}📝 Creating Kafka topics...${NC}"
docker-compose exec -T kafka kafka-topics --create --if-not-exists \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 3 \
    --topic transactions.incoming

docker-compose exec -T kafka kafka-topics --create --if-not-exists \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 3 \
    --topic transactions.validated

docker-compose exec -T kafka kafka-topics --create --if-not-exists \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 3 \
    --topic fraud.alerts

echo -e "${GREEN}✅ Topics created${NC}"

# Build projects
cd ../..
echo -e "${BLUE}🔨 Building projects...${NC}"
./mvnw clean install -DskipTests

echo -e "${GREEN}✅ Build complete${NC}"

echo -e "${BLUE}📊 Starting monitoring stack...${NC}"
cd infrastructure/docker
docker-compose up -d prometheus grafana

echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "Services running:"
echo "  MySQL: localhost:3306"
echo "  Redis: localhost:6379"
echo "  Kafka: localhost:9092"
echo "  Kafka UI: http://localhost:8090"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin123)"
echo ""
echo "To start the services:"
echo "  cd services/transaction-ingestion && ../../mvnw spring-boot:run"
echo "  cd services/fraud-detection && ../../mvnw spring-boot:run"