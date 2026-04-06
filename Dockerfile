# Build stage
FROM gradle:8.7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build without running tests to save time and resources
RUN gradle build --no-daemon -x test

# Run stage
FROM openjdk:17-slim
WORKDIR /app
EXPOSE 8080

# Copy only the executable jar from the build stage
COPY --from=build /home/gradle/src/build/libs/*-SNAPSHOT.jar app.jar

# Set standard environment variables
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# Run the application with dynamic port (Spring Boot picks up $PORT automatically if configured)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
