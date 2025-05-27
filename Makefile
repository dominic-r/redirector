.PHONY: build test clean run docker-build docker-run test-unit test-integration test-backend test-metrics verify lint-checkstyle lint-check lint-apply

# Variables
APP_NAME := dot-org-redirector
MVN := mvn
JAVA := java
DOCKER := docker
DOCKER_COMPOSE := docker compose

# Build the application
build:
	$(MVN) clean package -DskipTests

# Run the application
run: build
	$(JAVA) -jar target/$(APP_NAME)-*.jar

# Run all tests
test:
	$(MVN) test

# Run unit tests only
test-unit:
	$(MVN) test -Dtest=Redirect*Test

# Run integration tests only
test-integration:
	$(MVN) test -Dgroups="integration"

# Run specific tests
test-backend:
	$(MVN) test -Dtest=BackendEndpointsTest

test-metrics:
	$(MVN) test -Dtest=RedirectMetricsTest

test-security:
	$(MVN) test -Dtest=SecurityConfigTest

# Verify the application without running tests
verify-no-tests:
	$(MVN) verify -DskipTests

# Verify the application with all tests
verify:
	$(MVN) verify

# Run checkstyle
lint-checkstyle:
	$(MVN) checkstyle:check

# Run check
lint-check:
	$(MVN) spotless:check
	$(MVN) checkstyle:check

# Apply checkstyle
lint-apply:
	$(MVN) spotless:apply

# Clean build artifacts
clean:
	$(MVN) clean

# Build Docker image
docker-build:
	$(DOCKER) build -t $(APP_NAME):$(VERSION) .

# Run with Docker Compose
docker-run:
	$(DOCKER_COMPOSE) up --build

# Default target
all: build 