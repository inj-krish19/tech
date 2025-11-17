# ----------------------------------------
# 1. Build Stage (Maven + JDK)
# ----------------------------------------
FROM eclipse-temurin:21-jdk AS builder

# Install Maven
ARG MAVEN_VERSION=3.8.6
RUN apt-get update && apt-get install -y wget tar && \
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven

ENV PATH="/opt/maven/bin:${PATH}"

WORKDIR /app

# Copy dependencies first for caching
COPY pom.xml .

# Download dependencies (Better build caching)
RUN mvn dependency:go-offline -B

# Copy source code and other folders
COPY src ./src
COPY uploads ./uploads
COPY .env .env

# Build the application (creates target/*.jar)
RUN mvn clean package -DskipTests


# ----------------------------------------
# 2. Run Stage (Lightweight JDK Image)
# ----------------------------------------
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/uploads ./uploads
COPY --from=builder /app/.env .env

# Expose Spring Boot port
EXPOSE 10000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
