locals {
  lambda_tags = merge(var.cluster_tags, {
    lambda = var.lambda_name
  })
}

variable "lambda_workspace" {
  type = string
  default = "lambda-{{cluster}}-{{lambda-name}}"
}

resource "null_resource" "workspace_management" {
  count = terraform.workspace == "default" ? 1 : 0
  
  provisioner "local-exec" {
    command = <<EOT
      if ! terraform workspace list | grep -q "${var.cluster_workspace}"; then
        terraform workspace new "${var.cluster_workspace}"
      else
        echo "Workspace ${var.cluster_workspace} already exists"
      fi
      terraform workspace select "${var.cluster_workspace}";
      terraform apply -auto-approve;

      if ! terraform workspace list | grep -q "${var.lambda_workspace}"; then
        terraform workspace new "${var.lambda_workspace}"
      else
        echo "Workspace ${var.lambda_workspace} already exists"
      fi
      terraform workspace select "${var.lambda_workspace}";
      terraform apply -auto-approve;

      terraform workspace select default;
    EOT
  }

  triggers = {
    always_run = "${timestamp()}"
  }
}

data "terraform_remote_state" "cluster" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0
  
  backend = "s3"
  config = {
    bucket = "{{tfstate-bucket}}"
    key    = "env:/${var.cluster_workspace}/{{cluster}}/terraform.tfstate"
  }
}

module "runtime-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  source = "./{{tf-module-dir}}"

  layer_name = var.runtime_layer_name
  compatible_architectures = var.runtime_layer_compatible_architectures
  compatible_runtimes = var.runtime_layer_compatible_runtimes
  filename = var.runtime_layer_filename
}

module "deps-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  source = "./{{tf-module-dir}}"

  layer_name = var.deps_layer_name
  compatible_architectures = var.deps_layer_compatible_architectures
  compatible_runtimes = var.deps_layer_compatible_runtimes
  filename = var.deps_layer_filename
}

resource "aws_lambda_function" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  function_name = "bbotiscaf-${local.lambda_tags.cluster}-${var.lambda_name}"
  role = "${aws_iam_role.lambda-{{lambda-name}}[0].arn}"
  handler = var.lambda_handler
  memory_size = var.lambda_memory_size
  source_code_hash = filebase64sha256(var.lambda_filename)
  filename = var.lambda_filename
  runtime = var.lambda_runtime
  architectures = var.lambda_architectures
  timeout = var.lambda_timeout
  layers = [
    module.runtime-{{lambda-name}}[0].arn,
    module.deps-{{lambda-name}}[0].arn,
  ]

  file_system_config {
    arn = aws_efs_access_point.lambda-{{lambda-name}}[0].arn
    local_mount_path = "/mnt/efs"
  }

  vpc_config {
    subnet_ids         = data.terraform_remote_state.cluster[0].outputs.aws_subnet_public[*].id
    security_group_ids = [data.terraform_remote_state.cluster[0].outputs.aws_security_group_lambda_shared.id]
  }

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.lambda.${var.lambda_name}"
  })
}

# SQS Queue for the Lambda function
resource "aws_sqs_queue" "lambda_queue-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  name = "bbotiscaf-${var.cluster_tags.cluster}-sqs-${var.lambda_name}.fifo"

  fifo_queue                  = true
  content_based_deduplication = true
  deduplication_scope         = "messageGroup"
  fifo_throughput_limit       = "perMessageGroupId"
  visibility_timeout_seconds  = 60
  sqs_managed_sse_enabled     = true
  message_retention_seconds   = 1209600  # 14 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = data.terraform_remote_state.cluster[0].outputs.dlq_arn
    maxReceiveCount     = 5
  })

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.sqs.${var.lambda_name}"
  })
}

# API Gateway Integration for the SQS Queue
resource "aws_apigatewayv2_integration" "sqs_integration-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  api_id           = data.terraform_remote_state.cluster[0].outputs.api_gateway_id
  integration_type = "AWS_PROXY"
  integration_subtype = "SQS-SendMessage"
  credentials_arn     = data.terraform_remote_state.cluster[0].outputs.api_gateway_sqs_role_arn
  request_parameters  = {
    QueueUrl    = aws_sqs_queue.lambda_queue-{{lambda-name}}[0].url
    MessageBody = "$request.body"
    MessageGroupId = var.lambda_name
  }
  
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000
}

# API Gateway Route for the Lambda function
resource "aws_apigatewayv2_route" "lambda_route-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  api_id    = data.terraform_remote_state.cluster[0].outputs.api_gateway_id
  route_key = "POST /${var.lambda_name}"
  target    = "integrations/${aws_apigatewayv2_integration.sqs_integration-{{lambda-name}}[0].id}"
}

resource "aws_lambda_event_source_mapping" "sqs_trigger-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  event_source_arn = aws_sqs_queue.lambda_queue-{{lambda-name}}[0].arn
  function_name    = aws_lambda_function.lambda-{{lambda-name}}[0].arn
  
  batch_size       = 10
  maximum_batching_window_in_seconds = 0
  
  scaling_config {
    maximum_concurrency = 1000
  }
}

resource "aws_cloudwatch_log_group" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  name = "/aws/lambda/${aws_lambda_function.lambda-{{lambda-name}}[0].function_name}"

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.cw-log-group.lambda.${var.lambda_name}"
  })
}

resource "aws_iam_role" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  name = "bbotiscaf.${local.lambda_tags.cluster}.iam-role.${var.lambda_name}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.iam-role.${var.lambda_name}"
  })
}

resource "aws_iam_policy" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  name = "bbotiscaf.${local.lambda_tags.cluster}.iam-policy.${var.lambda_name}"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = ["${aws_cloudwatch_log_group.lambda-{{lambda-name}}[0].arn}:*"]
      }
    ]
  })

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.iam-policy.${var.lambda_name}"
  })
}

resource "aws_iam_role_policy_attachment" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  role = aws_iam_role.lambda-{{lambda-name}}[0].name
  policy_arn = aws_iam_policy.lambda-{{lambda-name}}[0].arn
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  role       = aws_iam_role.lambda-{{lambda-name}}[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_sqs-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  role       = aws_iam_role.lambda-{{lambda-name}}[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

resource "aws_efs_access_point" "lambda-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0

  file_system_id = try(data.terraform_remote_state.cluster[0].outputs.aws_efs_file_system_cluster.id, null)

  root_directory {
    path = "/${var.lambda_name}"
    creation_info {
      owner_gid   = 1000
      owner_uid   = 1000
      permissions = "755"
    }
  }

  posix_user {
    gid = 1000
    uid = 1000
  }

  tags = merge(var.cluster_tags, {
    Name = "bbotiscaf.${var.cluster_tags.cluster}.efs-ap.${var.lambda_name}"
  })
}

resource "aws_iam_role_policy_attachment" "lambda_efs-{{lambda-name}}" {
  count = terraform.workspace == var.lambda_workspace ? 1 : 0
 
  role       = aws_iam_role.lambda-{{lambda-name}}[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonElasticFileSystemClientFullAccess"
}

# Output API Endpoint (Webhook) 
output "api_gateway_url" {
  value = try("${data.terraform_remote_state.cluster[0].outputs.api_gateway_endpoint}/${var.lambda_name}", null)
}
