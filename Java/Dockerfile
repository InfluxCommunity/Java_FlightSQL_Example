# Use the official Maven image as the base image
FROM maven:3.8.3-openjdk-11 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml file into the container
COPY pom.xml .

# Download and cache dependencies
RUN mvn dependency:go-offline

# Copy the rest of the source code into the container
COPY src/ ./src/

# Compile the source code and copy dependencies
RUN mvn compile dependency:copy-dependencies

# Use the official OpenJDK image as the runtime base image
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the compiled classes and dependencies from the build stage
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

# Set ARGs for --build-arg options passed in the build command
ARG DATABASE_FIELD
ARG SOURCE_HOST
ARG READ_TOKEN
ARG SOURCE_DATABASE_NAME
ARG DOWNSAMPLE_QUERY
ARG TARGET_CLUSTER_URL
ARG WRITE_TOKEN
ARG TARGET_DATABASE_NAME
ARG TARGET_TABLE_NAME

# Set run-time ENVs from ARGs
ENV DATABASE_FIELD=${DATABASE_FIELD}
# ReadConfigs
ENV SOURCE_HOST=${SOURCE_HOST}
ENV READ_TOKEN=${READ_TOKEN}
ENV SOURCE_DATABASE_NAME=${SOURCE_DATABASE_NAME}
ENV DOWNSAMPLE_QUERY=${DOWNSAMPLE_QUERY}
# WriteConfigs
ENV TARGET_CLUSTER_URL=${TARGET_CLUSTER_URL}
ENV WRITE_TOKEN=${WRITE_TOKEN}
ENV TARGET_DATABASE_NAME=${TARGET_DATABASE_NAME}
ENV TARGET_TABLE_NAME=${TARGET_TABLE_NAME}

# Set the entrypoint to run your Java application
ENTRYPOINT ["java", "-cp", "classes:dependency/*", "com.influxdb.examples.CloudReadWriteExample"]
