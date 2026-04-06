# 1단계: 빌드용 컨테이너 (의존성 캐싱 레이어 추가)
FROM gradle:8.7-jdk17-alpine AS build
WORKDIR /app

# 의존성 정의 파일만 먼저 복사하여 캐싱 활용
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle build --no-daemon -x test

# 2단계: 실행용 컨테이너 (초경량 Alpine 이미지 사용)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8080

# 빌드 결과물만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# JVM 최적화 옵션 추가 (메모리 여유분 극대화)
ENV JAVA_OPTS="-Xms384m -Xmx384m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
