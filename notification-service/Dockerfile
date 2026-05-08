FROM eclipse-temurin:17-jre
WORKDIR /app
COPY notification-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
