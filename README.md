

does not work
LAMBDA_ROLE_ARN="arn:aws:iam::372345383787:role/quarkus-yoyo-playground-role" sh target/manage.sh create




terraform init
terraform plan
terraform apply

cd ./terraform && terraform init && terraform plan && cd ..

mvn clean install -DskipTests && cd ./terraform && terraform init && terraform apply -auto-approve && cd ..

https://quarkus.io/blog/mutiny-concurrent-uni/# quarkus-lambda-sandbag
