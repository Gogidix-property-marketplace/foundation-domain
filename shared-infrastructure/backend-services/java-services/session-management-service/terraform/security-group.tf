# Security Groups for EnterpriseTestService-EnterpriseTestService infrastructure

# EKS Cluster Security Group
resource "aws_security_group"eks_cluster" {
  name        = "${var.project_name}-eks-cluster-sg"
  description = "Security group for EKS cluster"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-eks-cluster-sg"
  }
}

# EKS Node Security Group
resource "aws_security_group"eks_nodes" {
  name        = "${var.project_name}-eks-nodes-sg"
  description = "Security group for EKS worker nodes"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-eks-nodes-sg"
  }
}

# Node to Cluster communication
resource "aws_security_group_rule"node_cluster_ingress" {
  description              = "Allow pods to communicate with the cluster API"
  from_port               = 443
  to_port                 = 443
  protocol                = "tcp"
  security_group_id       = aws_security_group.eks_nodes.id
  source_security_group_id = aws_security_group.eks_cluster.id
  type                    = "ingress"
}

# Cluster to Node communication
resource "aws_security_group_rule"cluster_node_ingress" {
  description              = "Allow cluster API to communicate with pods"
  from_port               = 1024
  to_port                 = 65535
  protocol                = "tcp"
  security_group_id       = aws_security_group.eks_cluster.id
  source_security_group_id = aws_security_group.eks_nodes.id
  type                    = "ingress"
}

# Node to Node communication
resource "aws_security_group_rule"node_node_ingress" {
  description              = "Allow worker nodes to communicate with each other"
  from_port               = 0
  to_port                 = 65535
  protocol                = "-1"
  security_group_id       = aws_security_group.eks_nodes.id
  source_security_group_id = aws_security_group.eks_nodes.id
  type                    = "ingress"
}

# ALB Ingress Controller Security Group
resource "aws_security_group"alb_ingress_controller" {
  name        = "${var.project_name}-alb-ingress-sg"
  description = "Security group for ALB Ingress Controller"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-alb-ingress-sg"
  }
}

# ALB to Node communication
resource "aws_security_group_rule"alb_node_ingress" {
  description              = "Allow ALB to communicate with worker nodes"
  from_port               = 30080
  to_port                 = 32767
  protocol                = "tcp"
  security_group_id       = aws_security_group.alb_ingress_controller.id
  source_security_group_id = aws_security_group.eks_nodes.id
  type                    = "ingress"
}

# Application Load Balancer security group
resource "aws_security_group"alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from any"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from any"
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
    Name = "${var.project_name}-alb-sg"
  }
}

# Application security group
resource "aws_security_group"application" {
  name        = "${var.project_name}-app-sg"
  description = "Security group for application pods"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-app-sg"
  }
}

# ALB to Application communication
resource "aws_security_group_rule"alb_app_ingress" {
  description              = "Allow ALB to communicate with application"
  from_port               = 8080
  to_port                 = 8080
  protocol                = "tcp"
  security_group_id       = aws_security_group.alb.id
  source_security_group_id = aws_security_group.application.id
  type                    = "ingress"
}

# Database security group
resource "aws_security_group"database" {
  name        = "${var.project_name}-db-sg"
  description = "Security group for RDS database"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-db-sg"
  }
}

# Application to Database communication
resource "aws_security_group_rule"app_db_ingress" {
  description              = "Allow application to access database"
  from_port               = var.db_port
  to_port                 = var.db_port
  protocol                = "tcp"
  security_group_id       = aws_security_group.database.id
  source_security_group_id = aws_security_group.application.id
  type                    = "ingress"
}

# Redis security group
resource "aws_security_group"redis" {
  name        = "${var.project_name}-redis-sg"
  description = "Security group for Redis cluster"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-redis-sg"
  }
}

# Application to Redis communication
resource "aws_security_group_rule"app_redis_ingress" {
  description              = "Allow application to access Redis"
  from_port               = var.redis_port
  to_port                 = var.redis_port
  protocol                = "tcp"
  security_group_id       = aws_security_group.redis.id
  source_security_group_id = aws_security_group.application.id
  type                    = "ingress"
}