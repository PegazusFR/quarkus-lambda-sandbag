data "archive_file" "quarkus-lambda" {
  type = "zip"
  source_dir = "${path.module}/.."
  output_path = "./target/function.zip"
}

data "aws_iam_role" "iam_for_lambda" {
  name = "quarkus-yoyo-playground-role"
}


resource "aws_lambda_function" "test_lambda" {
  # If the file is not in the current working directory you will need to include a
  # path.module in the filename.
  filename      = "${path.module}/../target/function.zip"
  function_name = "quarkus-yoyo-playground"
  role          = data.aws_iam_role.iam_for_lambda.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"

  publish = true
  source_code_hash = data.archive_file.quarkus-lambda.output_base64sha256
  reserved_concurrent_executions = 1
  architectures = ["x86_64"]
  runtime = "java21"
  timeout = "120"
  memory_size = "1769"
  environment {
    variables = {
      foo = "yoyo"
    }
  }
}


resource "aws_lambda_alias" "quarkus-alias" {
  function_name    = aws_lambda_function.test_lambda.function_name
  function_version = aws_lambda_function.test_lambda.version
  name             = "FINAL_VERSION"
}