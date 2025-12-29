terraform {
  required_version = ">= 1.5"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  backend "s3" {
    bucket         = "finstream-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "finstream-terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "FinStream"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# VPC
module "vpc" {
  source = "./modules/vpc"
  
  environment         = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
}

# EKS Cluster
module "eks" {
  source = "./modules/eks"
  
  environment        = var.environment
  cluster_name       = "${var.project_name}-${var.environment}-cluster"
  cluster_version    = var.eks_cluster_version
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  
  node_groups = {
    general = {
      desired_size = 3
      min_size     = 2
      max_size     = 10
      instance_types = ["t3.large"]
    }
    compute = {
      desired_size = 2
      min_size     = 1
      max_size     = 5
      instance_types = ["c5.xlarge"]
    }
  }
}

# RDS PostgreSQL
module "rds" {
  source = "./modules/rds"
  
  environment          = var.environment
  identifier           = "${var.project_name}-${var.environment}-db"
  engine_version       = "16.1"
  instance_class       = var.db_instance_class
  allocated_storage    = 100
  vpc_id               = module.vpc.vpc_id
  subnet_ids           = module.vpc.private_subnet_ids
  allowed_cidr_blocks  = [module.vpc.vpc_cidr]
}

# ElastiCache Redis
module "redis" {
  source = "./modules/redis"
  
  environment         = var.environment
  cluster_id          = "${var.project_name}-${var.environment}-redis"
  node_type           = var.redis_node_type
  num_cache_nodes     = 3
  vpc_id              = module.vpc.vpc_id
  subnet_ids          = module.vpc.private_subnet_ids
  allowed_cidr_blocks = [module.vpc.vpc_cidr]
}

# MSK (Managed Kafka)
module "msk" {
  source = "./modules/msk"
  
  environment         = var.environment
  cluster_name        = "${var.project_name}-${var.environment}-kafka"
  kafka_version       = "3.6.0"
  number_of_nodes     = 3
  instance_type       = var.msk_instance_type
  vpc_id              = module.vpc.vpc_id
  subnet_ids          = module.vpc.private_subnet_ids
}

# Outputs
output "eks_cluster_endpoint" {
  value = module.eks.cluster_endpoint
}

output "rds_endpoint" {
  value = module.rds.endpoint
}

output "redis_endpoint" {
  value = module.redis.endpoint
}

output "msk_bootstrap_brokers" {
  value = module.msk.bootstrap_brokers
}