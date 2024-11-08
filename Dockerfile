# Start from an official Java 17 base image
FROM openjdk:17-jdk-slim

# Set environment variables for Maven
ENV MAVEN_VERSION=3.8.6

# Install Maven
RUN apt-get update && \
    apt-get install -y wget && \
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar xzvf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Update PATH
ENV PATH=/opt/maven/bin:${PATH}

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml ./
COPY src ./src
COPY .env /app/.env

RUN pwd && ls -a

# RUN echo "WHATS_NAME=krish" > /app/.env && \
#     echo "DB_URL=<db-url>" >> /app/.env && \
#     echo "DB_USERNAME=<db-un>" >> /app/.env && \
#     echo "DB_PASSWORD=<db-pass>" >> /app/.env && \
#     echo "DB_DATABASE=<db>" >> /app/.env && \
#     if [ -f /app/.env ]; then echo '.env file created successfully at /app/.env'; else echo '.env file not found'; fi




# Build the application
RUN java -version

RUN mvn -v

RUN mvn clean package

# Set the JAR file path
ARG JAR_FILE=target/tech-0.0.1-SNAPSHOT.jar
ENV JAR_FILE=$JAR_FILE

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java -jar $JAR_FILE"]

# Expose the port the application will run on (9950)
EXPOSE 10000