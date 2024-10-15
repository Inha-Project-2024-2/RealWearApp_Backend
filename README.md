# RealWearApp-Backend

## 서버를 실행 하기 전에 사전에 필요한 작업

1. 아래의 커맨드를 실행하여 dockerhub에 올라와 있는 KMS 서버 이미지를 먼저 다운 받는다.

```
docker pull kurento/kurento-media-server:7.1.0
```

2. 운영체제에 따라 다운 받은 이미지를 아래와 같이 실행한다.
- 리눅스 운영체제의 경우
```
docker run -d --name kurento --network host \
    kurento/kurento-media-server:7.1.0
```

- Mac OS 혹은 Window OS의 경우

```
docker run --rm \
    -p 8888:8888/tcp \
    -p 5000-5050:5000-5050/udp \
    -e KMS_MIN_PORT=5000 \
    -e KMS_MAX_PORT=5050 \
    kurento/kurento-media-server:7.1.0
```

### 서버 실행 방법
- git clone을 받은 뒤 RealWearApp.jar파일이 존재하는 디렉토리로 이동하여 아래와 같은 커맨드를 입력한다.

```
java -jar RealWear.jar
```

- 이후 크롬 브라우저에서 `localhost:8443` 으로 접속하면 된다.