data "archive_file" "quarkus-sqs-lambda" {
  type = "zip"
  source_dir = "${path.module}/.."
  output_path = "./target/function.zip"
}

data "aws_iam_role" "iam_for_sqs_lambda" {
  name = "quarkus-yoyo-playground-role"
}


resource "aws_lambda_function" "test_sqs_lambda" {
  # If the file is not in the current working directory you will need to include a
  # path.module in the filename.
  filename      = "${path.module}/../target/function.zip"
  function_name = "sqs-playground"
  role          = data.aws_iam_role.iam_for_sqs_lambda.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  reserved_concurrent_executions = 1
  publish = true
  source_code_hash = data.archive_file.quarkus-sqs-lambda.output_base64sha256
  architectures = ["x86_64"]
  runtime = "java21"
  timeout = "60"
  memory_size = "1769"
  environment {
    variables = {
      foo = "yoyo"
      QUARKUS_LAMBDA_HANDLER="sqs-p2"
      QUARKUS_LOG_CONSOLE_JSON= "true"
    }
  }
}


resource "aws_lambda_alias" "quarkus-sqs-alias" {
  function_name    = aws_lambda_function.test_sqs_lambda.function_name
  function_version = aws_lambda_function.test_sqs_lambda.version
  name             = "FINAL_VERSION"
}

resource "aws_lambda_event_source_mapping" "example" {
  event_source_arn = aws_sqs_queue.terraform_queue.arn
  function_name    = aws_lambda_alias.quarkus-sqs-alias.arn
  maximum_batching_window_in_seconds = 20
  function_response_types = ["ReportBatchItemFailures"]
  batch_size       = 50
  enabled          = true
}

resource "aws_sqs_queue" "terraform_queue" {
  name = "terraform-example-queue"
  visibility_timeout_seconds = 60
  receive_wait_time_seconds = 4
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.terraform_queue_deadletter.arn
    maxReceiveCount     = 4
  })
}

resource "aws_sqs_queue" "terraform_queue_deadletter" {
  name = "terraform-example-deadletter-queue"
}

resource "aws_sqs_queue_redrive_allow_policy" "terraform_queue_redrive_allow_policy" {
  queue_url = aws_sqs_queue.terraform_queue_deadletter.id


  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue",
    sourceQueueArns   = [aws_sqs_queue.terraform_queue.arn]
  })

}

resource "aws_cloudwatch_log_group" "lambda_logging" {
  name = "/aws/lambda/${aws_lambda_function.test_sqs_lambda.function_name}"
  retention_in_days = 1

}