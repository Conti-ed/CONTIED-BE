# Build stage
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build without running tests to save time and resources
RUN gradle build --no-daemon -x test

# 2단계: 실행용 컨테이너
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
EXPOSE 8080

# Build 단계에서 생성된 jar 파일을 실행 디렉토리로 복사
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Set standard environment variables
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# Run the application with dynamic port (Spring Boot picks up $PORT automatically if configured)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
