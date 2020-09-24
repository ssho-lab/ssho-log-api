FROM openjdk:8-jre
ARG JAR_FILE=target/log-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} log-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/log-0.0.1-SNAPSHOT.jar"]
