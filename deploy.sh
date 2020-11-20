aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 932486566412.dkr.ecr.ap-northeast-2.amazonaws.com
docker build -t ssho-log-api .
docker tag ssho-log-api:latest 932486566412.dkr.ecr.ap-northeast-2.amazonaws.com/ssho-log-api:latest
docker push 932486566412.dkr.ecr.ap-northeast-2.amazonaws.com/ssho-log-api:latest

