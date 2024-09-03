terraform {
  backend "s3" {
    bucket = "{{tfstate-bucket}}"
    key    = "{{cluster}}/terraform.tfstate"
    encrypt = true
  }
}

variable "runtime_layer_name" {}
variable "runtime_layer_compatible_architectures" {}
variable "runtime_layer_compatible_runtimes" {}
variable "runtime_layer_filename" {}
variable "deps_layer_name" {}
variable "deps_layer_compatible_architectures" {}
variable "deps_layer_compatible_runtimes" {}
variable "deps_layer_filename" {}

variable "lambda_name" {}
variable "lambda_handler" {}
variable "lambda_filename" {}
variable "lambda_memory_size" {}
variable "lambda_runtime" {}
variable "lambda_architectures" {}
variable "lambda_timeout" {}

variable "cluster_workspace" {
  type = string
  default = "cluster-{{cluster}}"
}

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
  count = terraform.workspace == var.cluster_workspace ? 1 : 0
  
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.vpc"
  })
}

# Public Subnets
resource "aws_subnet" "public" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  vpc_id                  = aws_vpc.cluster[0].id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.subnet.public-${count.index + 1}"
  })
}

# Private Subnets
resource "aws_subnet" "private" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  vpc_id            = aws_vpc.cluster[0].id
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
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  vpc_id = aws_vpc.cluster[0].id

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.igw"
  })
}

# Route Table for Public Subnets
resource "aws_route_table" "public" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  vpc_id = aws_vpc.cluster[0].id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.cluster[0].id
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.public-rt"
  })
}

# Route Table Association for Public Subnets
resource "aws_route_table_association" "public" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.public) : 0

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

# Elastic IP for the NAT Gateway
resource "aws_eip" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.public) : 0

  domain = "vpc"
}

# NAT Gateway!
resource "aws_nat_gateway" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.public) : 0

  # Allocating the Elastic IP to the NAT Gateway!
  allocation_id = aws_eip.cluster[count.index].id

  # Associating it in the Public Subnet!
  subnet_id = aws_subnet.public[count.index].id

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.ngw"
  })
}

# Creating a Route Table for the Nat Gateway!
resource "aws_route_table" "ngw" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.private) : 0

  vpc_id = aws_vpc.cluster[0].id

  route {
    cidr_block = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.cluster[count.index].id
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.ngw-rt"
  })
}

# Creating an Route Table Association of the NAT Gateway route 
# table with the Private Subnet!
resource "aws_route_table_association" "ngw" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.private) : 0

  #  Private Subnet ID for adding this route table to the DHCP server of Private subnet!
  subnet_id      = aws_subnet.private[count.index].id

  # Route Table ID
  route_table_id = aws_route_table.ngw[count.index].id
}


# Shared security group for Lambda functions
resource "aws_security_group" "lambda_shared" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name        = "bbotiscaf.${var.cluster_tags.cluster}.sg.lambda-shared"
  description = "Shared security group for Lambda functions"
  vpc_id      = aws_vpc.cluster[0].id

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

# Dead-Letter Queue for messages that fail processing
resource "aws_sqs_queue" "dlq" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "bbotiscaf-${var.cluster_tags.cluster}-sqs-dlq.fifo"

  fifo_queue                  = true
  content_based_deduplication = true
  sqs_managed_sse_enabled     = true
  message_retention_seconds   = 1209600  # 14 days

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.dlq"
  })
}

# API Gateway
resource "aws_api_gateway_rest_api" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "bbotiscaf.${var.cluster_tags.cluster}.apigw"

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw"
  })
}

# Ping API Gateway Resource
resource "aws_api_gateway_resource" "ping" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  parent_id   = aws_api_gateway_rest_api.cluster[0].root_resource_id
  path_part   = "ping"
  rest_api_id = aws_api_gateway_rest_api.cluster[0].id
}

# Ping API Gateway Method
resource "aws_api_gateway_method" "ping" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  authorization = "NONE"
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.ping[0].id
  rest_api_id   = aws_api_gateway_rest_api.cluster[0].id
}

# Ping API Gateway Integration
resource "aws_api_gateway_integration" "ping" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.cluster[0].id
  resource_id = aws_api_gateway_resource.ping[0].id
  http_method = aws_api_gateway_method.ping[0].http_method

  type = "MOCK"
}

# Ping API Gateway Integration Response
resource "aws_api_gateway_method_response" "ping" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.cluster[0].id
  resource_id = aws_api_gateway_resource.ping[0].id
  http_method = aws_api_gateway_method.ping[0].http_method
  status_code = "200"
}

# API Gateway Deployment
resource "aws_api_gateway_deployment" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  depends_on = [aws_api_gateway_method.ping[0]]
  
  rest_api_id = aws_api_gateway_rest_api.cluster[0].id

  triggers = {
    redeployment = timestamp()
  }

  lifecycle {
    create_before_destroy = true
  }
}

# API Gateway Stage
resource "aws_api_gateway_stage" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.cluster[0].id
  stage_name    = "default"
  deployment_id = aws_api_gateway_deployment.cluster[0].id

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway[0].arn
    format = jsonencode({
      requestId                  = "$context.requestId"
      sourceIp                   = "$context.identity.sourceIp"
      requestTime                = "$context.requestTime"
      protocol                   = "$context.protocol"
      httpMethod                 = "$context.httpMethod"
      resourcePath               = "$context.resourcePath"
      routeKey                   = "$context.routeKey"
      status                     = "$context.status"
      path                       = "$context.path"
      responseLength             = "$context.responseLength"
      errorMessage               = "$context.error.message"
      errorMessageString         = "$context.error.messageString"
      errorResponseType          = "$context.error.responseType"
      integrationStatus          = "$context.integration.status"
      integrationError           = "$context.integration.error"
      integrationErrorMessage    = "$context.integrationErrorMessage"
    })
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.apigw.stage"
  })
}

# CloudWatch Log Group for API Gateway
resource "aws_cloudwatch_log_group" "api_gateway" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "/aws/bbotiscaf/${var.cluster_tags.cluster}/api_gateway"

  retention_in_days = 30

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.cw-log-group.api-gateway"
  })
}

# IAM Role for API Gateway CloudWatch Logging
resource "aws_iam_role" "api_gateway_cloudwatch" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "bbotiscaf.${var.cluster_tags.cluster}.iam-role.api-gateway-cloudwatch"

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
    Name = "bbotiscaf.${var.cluster_tags.cluster}.iam-role.api-gateway-cloudwatch"
  })
}

# IAM Policy for API Gateway CloudWatch Logging
resource "aws_iam_role_policy" "api_gateway_cloudwatch" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "bbotiscaf.${var.cluster_tags.cluster}.iam-policy.api-gateway-cloudwatch"
  role = aws_iam_role.api_gateway_cloudwatch[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams",
          "logs:PutLogEvents",
          "logs:GetLogEvents",
          "logs:FilterLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.api_gateway[0].arn}:*"
      }
    ]
  })
}

# API Gateway Account
resource "aws_api_gateway_account" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  cloudwatch_role_arn = aws_iam_role.api_gateway_cloudwatch[0].arn
}


# IAM Role for API Gateway to send Messages to SQS
resource "aws_iam_role" "api_gateway_sqs" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

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
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name = "bbotiscaf.${var.cluster_tags.cluster}.iam-policy.api-gateway-sqs"
  role = aws_iam_role.api_gateway_sqs[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage"
        ]
        Resource = ["arn:aws:sqs:*:*:bbotiscaf-${var.cluster_tags.cluster}-sqs-*"]
      }
    ]
  })
}

# Cluster-wide EFS
resource "aws_efs_file_system" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  creation_token = "bbotiscaf-${var.cluster_tags.cluster}-efs"
  encrypted      = true

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.efs"
  })
}

# EFS Mount Targets for each private Subnet
resource "aws_efs_mount_target" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? length(aws_subnet.private) : 0

  file_system_id  = aws_efs_file_system.cluster[0].id
  subnet_id       = aws_subnet.private[count.index].id
  security_groups = [aws_security_group.efs[0].id]
}

# Security Group for Mount Targets
resource "aws_security_group" "efs" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  name        = "bbotiscaf-${var.cluster_tags.cluster}-efs-sg"
  description = "Allow NFS traffic from Lambda functions"
  vpc_id      = aws_vpc.cluster[0].id

  ingress {
    description     = "NFS from Lambda"
    from_port       = 2049
    to_port         = 2049
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda_shared[0].id]
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sg.efs"
  })
}

data "aws_caller_identity" "current" {}

# Policy for EFS Access
resource "aws_efs_file_system_policy" "cluster" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  file_system_id = aws_efs_file_system.cluster[0].id

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
        Resource = [aws_efs_file_system.cluster[0].arn]
        Condition = {
          StringLike = {
            "aws:PrincipalArn": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/bbotiscaf.${var.cluster_tags.cluster}.iam-role.*"
          }
        }
      }
    ]
  })
}

# Security Groups Rule for Lambdas to EFS
resource "aws_security_group_rule" "lambda_to_efs" {
  count = terraform.workspace == var.cluster_workspace ? 1 : 0

  type                     = "egress"
  from_port                = 2049
  to_port                  = 2049
  protocol                 = "tcp"
  security_group_id        = aws_security_group.lambda_shared[0].id
  source_security_group_id = aws_security_group.efs[0].id
}


output "api_gateway" {
  value = try(aws_api_gateway_rest_api.cluster[0], null)
}

output "api_gateway_sqs_role_arn" {
  value = try(aws_iam_role.api_gateway_sqs[0].arn, null)
}

output "dlq_arn" {
  value = try(aws_sqs_queue.dlq[0].arn, null)
}

output "aws_subnet_public" {
  value = try(aws_subnet.public[*], null)
}

output "aws_subnet_private" {
  value = try(aws_subnet.private[*], null)
}

output "aws_security_group_lambda_shared" {
  value = try(aws_security_group.lambda_shared[0], null)
}

output "aws_efs_file_system_cluster" {
  value = try(aws_efs_file_system.cluster[0], null)
}
