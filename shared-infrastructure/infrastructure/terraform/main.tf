# Gogidix Property Marketplace - Infrastructure as Code
# Terraform configuration for AWS EKS cluster and supporting infrastructure

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    bucket = "gogidix-terraform-state"
    key    = "infrastructure/terraform.tfstate"
    region = var.aws_region
    encrypt = true
    dynamodb_table = "gogidix-terraform-locks"
  }
}

# Provider Configuration
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Gogidix-Property-Marketplace"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Team        = "Infrastructure"
    }
  }
}

# Generate random suffix for unique resource naming
resource "random_pet" "suffix" {
  length = 2
}

# VPC Configuration
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.2"

  name = "gogidix-vpc-${random_pet.suffix.id}"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  private_subnets = var.private_subnet_cidrs
  public_subnets  = var.public_subnet_cidrs

  enable_nat_gateway = true
  single_nat_gateway   = false
  one_nat_gateway_per_az = true

  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "gogidix-vpc"
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "19.15.3"

  cluster_name    = "gogidix-eks-${random_pet.suffix.id}"
  cluster_version = var.kubernetes_version

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_private_access = true
  cluster_endpoint_public_access  = true

  # Managed Node Group for general workloads
  managed_node_groups = {
    general = {
      name            = "general-nodes"
      instance_type   = "m6i.xlarge"
      min_size        = 3
      max_size        = 10
      desired_size    = 5
      subnet_ids      = module.vpc.private_subnets

      k8s_labels = {
        Environment = var.environment
        NodePool    = "general"
      }

      additional_tags = {
        NodePool = "general"
      }
    }

    # Compute-optimized for data processing
    compute = {
      name            = "compute-nodes"
      instance_type   = "c6i.2xlarge"
      min_size        = 2
      max_size        = 8
      desired_size    = 3
      subnet_ids      = module.vpc.private_subnets

      k8s_labels = {
        Environment = var.environment
        NodePool    = "compute"
      }

      additional_tags = {
        NodePool = "compute"
      }
    }

    # Memory-optimized for caching and databases
    memory = {
      name            = "memory-nodes"
      instance_type   = "r6i.2xlarge"
      min_size        = 2
      max_size        = 6
      desired_size    = 3
      subnet_ids      = module.vpc.private_subnets

      k8s_labels = {
        Environment = var.environment
        NodePool    = "memory"
      }

      additional_tags = {
        NodePool = "memory"
      }
    }

    # GPU nodes for AI/ML workloads
    gpu = {
      name            = "gpu-nodes"
      instance_type   = "g5.xlarge"
      min_size        = 0
      max_size        = 4
      desired_size    = 0
      subnet_ids      = module.vpc.private_subnets

      k8s_labels = {
        Environment = var.environment
        NodePool    = "gpu"
        Accelerator = "nvidia-tesla-t4"
      }

      additional_tags = {
        NodePool = "gpu"
      }

      taints = {
        gpu = {
          key    = "nvidia.com/gpu"
          value  = "true"
          effect = "NO_SCHEDULE"
        }
      }
    }
  }

  # IAM Role for EKS Cluster
  iam_role_arn = aws_iam_role.eks_cluster.arn

  # OIDC Provider
  oidc_providers = {
    ex = {
      client_id = "sts.amazonaws.com"
    }
  }

  tags = {
    Name = "gogidix-eks"
  }
}

# EKS Cluster IAM Role
resource "aws_iam_role" "eks_cluster" {
  name = "gogidix-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster.name
}

# EKS Node IAM Role
resource "aws_iam_role" "eks_node" {
  name = "gogidix-eks-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_node_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node.name
}

resource "aws_iam_role_policy_attachment" "eks_node_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node.name
}

resource "aws_iam_role_policy_attachment" "eks_node_container_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node.name
}

# Additional IAM policies for enhanced functionality
resource "aws_iam_policy" "eks_node_additional" {
  name        = "gogidix-eks-node-additional"
  description = "Additional permissions for EKS nodes"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = [
          "arn:aws:s3:::gogidix-logs-*",
          "arn:aws:s3:::gogidix-backups-*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:Encrypt",
          "kms:GenerateDataKey"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParametersByPath"
        ]
        Resource = "arn:aws:ssm:*:*:parameter/gogidix/*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_node_additional_policy" {
  policy_arn = aws_iam_policy.eks_node_additional.arn
  role       = aws_iam_role.eks_node.name
}

# RDS Aurora PostgreSQL Cluster
module "rds" {
  source  = "terraform-aws-modules/rds-aurora/aws"
  version = "8.2.0"

  name          = "gogidix-aurora"
  engine        = "aurora-postgresql"
  engine_version = "15.4"

  instance_class = "db.r6g.xlarge"
  instances = {
    1 = {
      identifier = "gogidix-db-1"
    }
    2 = {
      identifier = "gogidix-db-2"
    }
  }

  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.database_subnet_group_name

  create_random_password = true
  master_username        = "gogidix_admin"
  port                  = 5432

  storage_encrypted   = true
  kms_key_id          = aws_kms_key.rds.arn

  backup_retention_period = 30
  preferred_backup_window = "03:00-04:00"
  skip_final_snapshot       = false
  final_snapshot_identifier = "gogidix-final-snapshot"

  tags = {
    Name = "gogidix-aurora"
  }
}

# KMS Key for RDS encryption
resource "aws_kms_key" "rds" {
  description             = "KMS key for RDS encryption"
  deletion_window_in_days = 7
}

resource "aws_kms_alias" "rds" {
  name          = "alias/gogidix-rds"
  target_key_id = aws_kms_key.rds.key_id
}

# Redis Cluster
module "redis" {
  source  = "terraform-aws-modules/elasticache/aws"
  version = "1.0.0"

  create_replication_group = true
  replication_group_id      = "gogidix-redis"
  description              = "Redis cluster for Gogidix"

  node_type               = "cache.r6g.xlarge"
  num_cache_clusters      = 2
  automatic_failover_enabled = true
  multi_az_enabled        = true

  port                    = 6379
  auth_token              = random_password.redis_auth.result
  transit_encryption_enabled = true
  at_rest_encryption_enabled   = true

  subnet_ids = module.vpc.private_subnets
  security_group_ids = [aws_security_group.redis.id]
  vpc_id             = module.vpc.vpc_id

  tags = {
    Name = "gogidix-redis"
  }
}

# Random password for Redis
resource "random_password" "redis_auth" {
  length  = 64
  special = true
}

# S3 Buckets
resource "aws_s3_bucket" "logs" {
  bucket = "gogidix-logs-${random_pet.suffix.id}"
}

resource "aws_s3_bucket_versioning" "logs" {
  bucket = aws_s3_bucket.logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  bucket = aws_s3_bucket.logs.id

  rule {
    id     = "log_lifecycle"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 60
      storage_class = "GLACIER"
    }

    transition {
      days          = 365
      storage_class = "DEEP_ARCHIVE"
    }

    expiration {
      days = 2555
    }
  }
}

resource "aws_s3_bucket" "backups" {
  bucket = "gogidix-backups-${random_pet.suffix.id}"
}

resource "aws_s3_bucket_versioning" "backups" {
  bucket = aws_s3_bucket.backups.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Application Load Balancers
resource "aws_lb" "api_gateway" {
  name               = "gogidix-api-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets

  enable_deletion_protection = false

  tags = {
    Name = "gogidix-api-alb"
  }
}

# Security Groups
resource "aws_security_group" "alb" {
  name        = "gogidix-alb-sg"
  description = "Security group for ALB"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gogidix-alb-sg"
  }
}

resource "aws_security_group" "redis" {
  name        = "gogidix-redis-sg"
  description = "Security group for Redis"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "Redis from VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  tags = {
    Name = "gogidix-redis-sg"
  }
}

# ACM Certificate
resource "aws_acm_certificate" "wildcard" {
  domain_name       = "*.gogidix.com"
  validation_method = "DNS"

  tags = {
    Name = "gogidix-wildcard-cert"
  }
}

# Route53 for DNS management (if enabled)
resource "aws_route53_zone" "main" {
  count = var.enable_route53 ? 1 : 0
  name  = "gogidix.com"

  tags = {
    Name = "gogidix-dns-zone"
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "eks" {
  name              = "/aws/eks/gogidix-eks/cluster"
  retention_in_days = 30

  tags = {
    Name = "gogidix-eks-logs"
  }
}

# Kubernetes Provider
data "aws_eks_cluster" "cluster" {
  name = module.eks.cluster_name
}

data "aws_eks_cluster_auth" "cluster_auth" {
  name = module.eks.cluster_name
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cluster_auth.token
}

# Kubernetes resources will be created in separate files