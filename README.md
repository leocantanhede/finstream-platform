# FinStream Analytics Platform

Sistema de processamento de transações financeiras em tempo real com detecção de fraudes e análises preditivas.

## 🏗️ Arquitetura

- **Microsserviços**: Spring Boot 3.x com Java 21
- **Mensageria**: Apache Kafka para event-driven architecture
- **Banco de Dados**: PostgreSQL + Redis + TimescaleDB
- **Containerização**: Docker + Kubernetes
- **Cloud**: AWS (EKS, RDS, MSK, ElastiCache)
- **CI/CD**: GitHub Actions
- **Observabilidade**: Prometheus + Grafana + ELK

## 🚀 Quick Start

### Pré-requisitos
- Java 21
- Docker & Docker Compose
- Maven 3.9+
- kubectl (para deploy em K8s)

### Setup Local
```bash
# Clone o repositório
git clone https://github.com/your-username/finstream-platform.git
cd finstream-platform

# Execute o script de setup
chmod +x scripts/setup-local.sh
./scripts/setup-local.sh

# Inicie os serviços
cd services/transaction-ingestion
../../mvnw spring-boot:run

# Em outro terminal
cd services/fraud-detection
../../mvnw spring-boot:run
```

### Acessos
- API: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:8090
- Grafana: http://localhost:3000 (admin/admin123)
- Prometheus: http://localhost:9090

## 📦 Serviços

### Transaction Ingestion Service
Port: 8080
Responsável por receber e validar transações.

### Fraud Detection Service
Port: 8081
Detecta padrões suspeitos em tempo real.

### Analytics Service
Port: 8082
Processa agregações e métricas.

## 🧪 Testes
```bash
# Testes unitários
./mvnw test

# Testes de integração
./mvnw verify -P integration-tests

# Gerar dados de teste
./scripts/generate-test-data.sh
```

## 📊 Métricas e Monitoramento

- Prometheus expõe métricas em `/actuator/prometheus`
- Grafana dashboards pré-configurados
- Distributed tracing com OpenTelemetry

## 🔧 Desenvolvimento

### Estrutura do Projeto
````
finstream-platform/
├── services/          # Microsserviços
├── libraries/         # Bibliotecas compartilhadas
├── infrastructure/    # IaC e configs
├── docs/             # Documentação
└── scripts/          # Scripts utilitários
