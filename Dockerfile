FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY management-jsf-app management-jsf-app
RUN mvn -B -DskipTests package
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/management-jsf-app/target/management-jsf-app-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","app.jar"]
