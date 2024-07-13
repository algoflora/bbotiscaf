variable "cluster_tags" {
  type = object({
    project   = string
    cluster   = string
    managedBy = string
  })
  default = {
    project   = "bbotiscaf"
    cluster   = "{{cluster}}"
    managedBy = "terraform"
  }
}

variable vpc_cidr {
  description = "CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

# VPC
resource "aws_vpc" "cluster" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.vpc"
  })
}

# Public Subnets
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.cluster.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.subnet.public-${count.index + 1}"
  })
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.cluster.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 2)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.subnet.private-${count.index + 1}"
  })
}

# Data source for available AZs
data "aws_availability_zones" "available" {
  state = "available"
}

# Internet Gateway
resource "aws_internet_gateway" "cluster" {
  vpc_id = aws_vpc.cluster.id

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.igw"
  })
}

# NAT Gateways (one per public subnet for high availability)
resource "aws_nat_gateway" "cluster" {
  count         = 2
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.nat-gw-${count.index + 1}"
  })
}

# Elastic IPs for NAT Gateways
resource "aws_eip" "nat" {
  count = 2
  domain = "vpc"

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.nat-eip-${count.index + 1}"
  })
}

# Route Table for Private Subnets
resource "aws_route_table" "private" {
  count  = 2
  vpc_id = aws_vpc.cluster.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.cluster[count.index].id
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.private-rt-${count.index + 1}"
  })
}

# Route Table Association for Private Subnets
resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# Shared security group for Lambda functions
resource "aws_security_group" "lambda_shared" {
  name        = "bbotiscaf.${var.cluster_tags.cluster}.sg.lambda-shared"
  description = "Shared security group for Lambda functions"
  vpc_id      = aws_vpc.cluster.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sg.lambda-shared"
  })
}

# SQS Queue for Webhook Requests
resource "aws_sqs_queue" "normal" {
  name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.normal"

  fifo_queue                  = true
  content_based_deduplication = true
  deduplication_scope         = "messageGroup"
  fifo_throughput_limit       = "perMessageGroupId"
  visibility_timeout_seconds  = 60
  sqs_managed_sse_enabled     = true
  message_retention_seconds   = 1209600  # 14 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = 5
  })

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.normal"
  })
}

# Dead-Letter Queue for messages that fail processing
resource "aws_sqs_queue" "dlq" {
  name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.dlq"

  fifo_queue                  = true
  content_based_deduplication = true
  sqs_managed_sse_enabled     = true
  message_retention_seconds   = 1209600  # 14 days

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.dlq"
  })
}

# API Gateway
resource "aws_apigatewayv2_api" "cluster" {
  name          = "bbotiscaf.${var.cluster_tags.cluster}.apigw"
  protocol_type = "HTTP"

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw"
  })
}

# API Gateway Stage
resource "aws_apigatewayv2_stage" "cluster" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw.stage"
  })
}

# Universal SQS Integration for API Gateway
resource "aws_apigatewayv2_integration" "sqs" {
  api_id           = aws_apigatewayv2_api.main.id
  integration_type = "AWS_PROXY"
  integration_uri  = "arn:aws:apigateway:${data.aws_region.current.name}:sqs:path/${data.aws_caller_identity.current.account_id}/${aws_sqs_queue.normal.name}"
  
  credentials_arn  = aws_iam_role.api_gateway_sqs.arn
  integration_method = "POST"
  payload_format_version = "1.0"

  request_parameters = {
    "MessageBody" : "$request.body",
    "MessageGroupId" : "$request.path.lambda_name"
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw.integration"
  })
}

data "aws_caller_identity" "current" {}

# Route for SQS Integration for API Gateway
resource "aws_apigatewayv2_route" "sqs_route" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /{lambda_name}"
  target    = "integrations/${aws_apigatewayv2_integration.sqs.id}"

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw.route"
  })
}

# IAM Role for API Gateway to send Messages to SQS
resource "aws_iam_role" "api_gateway_sqs" {
  name = "bbotiscaf.${var.cluster_tags.cluster}.iam-role.api-gateway-sqs"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "apigateway.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.iam-role.api-gateway-sqs"
  })
}

# Policy for API Gateway Role
resource "aws_iam_role_policy" "api_gateway_sqs" {
  name = "bbotiscaf.${var.cluster_tags.cluster}.iam-policy.api-gateway-sqs"
  role = aws_iam_role.api_gateway_sqs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage"
        ]
        Resource = aws_sqs_queue.normal.arn
      }
    ]
  })

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.iam-policy.api-gateway-sqs"
  })
}

# Output API Endpoint (Webhook) 
output "api_gateway_url" {
  value = aws_apigatewayv2_api.main.api_endpoint
}

# Cluster-wide EFS
resource "aws_efs_file_system" "cluster" {
  creation_token = "bbotiscaf-${var.cluster_tags.cluster}-efs"
  encrypted      = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.efs"
  })
}

# EFS Mount Targets for each private Subnet
resource "aws_efs_mount_target" "cluster" {
  count           = length(aws_subnet.private)
  file_system_id  = aws_efs_file_system.cluster.id
  subnet_id       = aws_subnet.private[count.index].id
  security_groups = [aws_security_group.efs.id]

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.efs.mnt-target-${count.index + 1}"
  })
}

# Security Group for Mount Targets
resource "aws_security_group" "efs" {
  name        = "bbotiscaf-${var.cluster_tags.cluster}-efs-sg"
  description = "Allow NFS traffic from Lambda functions"
  vpc_id      = aws_vpc.cluster.id

  ingress {
    description     = "NFS from Lambda"
    from_port       = 2049
    to_port         = 2049
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda_shared.id]
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sg.efs"
  })
}

# Policy for EFS Access
resource "aws_efs_file_system_policy" "cluster" {
  file_system_id = aws_efs_file_system.cluster.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "LambdaAccess"
        Effect = "Allow"
        Principal = {
          AWS = "*"
        }
        Action = [
          "elasticfilesystem:ClientMount",
          "elasticfilesystem:ClientWrite"
        ]
        Resource = aws_efs_file_system.cluster.arn
        Condition = {
          StringLike = {
            "aws:PrincipalArn": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/bbotiscaf.${var.cluster_tags.cluster}.iam-role.*"
          }
        }
      }
    ]
  })

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.efs-policy.cluster"
  })
}

# Security Groups Rule for Lambdas to EFS
resource "aws_security_group_rule" "lambda_to_efs" {
  type                     = "egress"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  security_group_id        = aws_security_group.lambda_shared.id
  source_security_group_id = aws_security_group.efs.id
}
