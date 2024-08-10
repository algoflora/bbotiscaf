locals {
  lambda_tags = merge(var.cluster_tags, {
    lambda = var.lambda_name
  })
}


resource "aws_lambda_function" "lambda" {
  function_name = "bbotiscaf-${local.lambda_tags.cluster}-${var.lambda_name}"
  role = "${aws_iam_role.lambda.arn}"
  handler = var.lambda_handler
  memory_size = var.lambda_memory_size
  source_code_hash = filebase64sha256(var.lambda_filename)
  filename = var.lambda_filename
  runtime = var.lambda_runtime
  architectures = var.lambda_architectures
  timeout = var.lambda_timeout
  layers = [
    module.runtime.arn,
    module.deps.arn,
  ]

  file_system_config {
    arn = aws_efs_access_point.lambda.arn
    local_mount_path = "/mnt/efs"
  }

  vpc_config {
    subnet_ids         = aws_subnet.public[*].id
    security_group_ids = [aws_security_group.lambda_shared.id]
  }

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.lambda.${var.lambda_name}"
  })
}

resource "aws_cloudwatch_log_group" "lambda" {
  name = "/aws/lambda/${aws_lambda_function.lambda.function_name}"

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.cw-log-group.lambda.${var.lambda_name}"
  })
}

resource "aws_iam_role" "lambda" {
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

resource "aws_iam_policy" "lambda" {
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
        Resource = ["${aws_cloudwatch_log_group.lambda.arn}:*"]
      }
    ]
  })

  tags = merge(local.lambda_tags, {
    Name = "bbotiscaf.${local.lambda_tags.cluster}.iam-policy.${var.lambda_name}"
  })
}

resource "aws_iam_role_policy_attachment" "lambda" {
  role = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.lambda.arn
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_access" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_lambda_event_source_mapping" "sqs_trigger" {
  event_source_arn = aws_sqs_queue.normal.arn
  function_name    = aws_lambda_function.lambda.arn
  
  batch_size       = 10
  maximum_batching_window_in_seconds = 0
  
  scaling_config {
    maximum_concurrency = 2
  }

  filter_criteria {
    filter {
      pattern = jsonencode({
        attributes: {
          MessageGroupId: ["${aws_lambda_function.lambda.function_name}"]
        }
      })
    }
  }
}

resource "aws_iam_role_policy_attachment" "lambda_sqs" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

resource "aws_efs_access_point" "lambda" {
  file_system_id = aws_efs_file_system.cluster.id

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

resource "aws_iam_role_policy_attachment" "lambda_efs" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonElasticFileSystemClientFullAccess"
}
