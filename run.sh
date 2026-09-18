#!/bin/bash

# Export environment variables from .env file
set -a
source .env
set +a

# Run the application
mvn spring-boot:run -pl management-jsf-app/ #-Dspring-boot.run.arguments=--server.port=$SERVER_PORT \
    