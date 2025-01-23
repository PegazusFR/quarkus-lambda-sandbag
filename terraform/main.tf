terraform {
  backend "s3" {
    key = "yoann-quarkus-playgroud.tfstate"
    region = "eu-central-1"
    bucket = "terraform-yoann-bucket"
  }

  required_version = "1.10.3"
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = "5.82.0"
    }
  }
}

