FROM openjdk:21-oracle
ARG WAR_FILE=*.war
COPY ${WAR_FILE} application.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.war"]