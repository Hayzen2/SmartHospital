# SmartHospital Backend

A comprehensive Spring Boot application for managing hospital operations with real-time chat, appointment scheduling, and AI-powered features.

## Prerequisites

Before you begin, ensure you have installed:

- **Java 21** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)
- **PostgreSQL 14+** - [Download](https://www.postgresql.org/download/) (or use Docker)
- **Git** - [Download](https://git-scm.com/download)

## Quick Start

### 1. Clone and Navigate to Backend

```bash
cd SmartHospital/backend
```

### 2. Set Up Environment Variables

Create a `.env` file in the `backend` root directory with the following configuration:

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/SmartHospital_DB
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# Email Configuration (Gmail)
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_app_specific_password

# Frontend URL (for CORS)
FRONTEND_URL=http://localhost:3000

# RabbitMQ Configuration
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# MinIO Configuration (File Storage)
MINIO_USER=minioadmin
MINIO_PASSWORD=minioadmin

# JWT Secret
JWT_SECRET=your_super_secret_jwt_key_change_this_in_production
```

> **Note:** For Gmail's `MAIL_PASSWORD`, use an [App-Specific Password](https://myaccount.google.com/apppasswords) instead of your regular Gmail password.

### 3. Start Services with Docker Compose

Start RabbitMQ, Redis, and MinIO:

```bash
docker-compose up -d
```

This will start:
- **RabbitMQ**: Message broker (ports 5672, 15672)
- **Redis**: Caching layer (port 6379)
- **MinIO**: File storage (ports 9000, 9001)

Verify services are running:

```bash
docker-compose ps
```

### 4. Initialize the Database

Create the PostgreSQL database:

```bash
# Using PostgreSQL CLI
psql -U postgres

# In PostgreSQL prompt
CREATE DATABASE "SmartHospital_DB";
\q
```

Or if using Docker for PostgreSQL:

```bash
docker run --name postgres -e POSTGRES_PASSWORD=your_postgres_password \
  -p 5432:5432 -d postgres:14
```

### 5. Build the Application

```bash
# Using Maven wrapper (recommended)
./mvnw clean package

# Or if using Maven directly
mvn clean package
```

### 6. Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or run the packaged JAR
java -jar target/SmartHospital-0.0.1-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

## API Documentation

Once the application is running, access the Swagger UI documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Service Endpoints

| Service | URL | Purpose |
|---------|-----|---------|
| Backend API | http://localhost:8080 | Main application |
| RabbitMQ Management | http://localhost:15672 | Message broker UI |
| Redis | localhost:6379 | Caching service |
| MinIO Console | http://localhost:9001 | File storage UI |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/SmartHospital/
│   │   │   ├── config/              # Configuration classes (JWT, Security, Redis, MinIO, WebSocket)
│   │   │   ├── controller/          # REST API endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Database access layer
│   │   │   ├── model/               # JPA entities
│   │   │   ├── dtos/                # Data Transfer Objects
│   │   │   ├── enums/               # Enumerations
│   │   │   └── helper/              # Utility classes
│   │   └── resources/
│   │       ├── application.yml      # Application configuration
│   │       └── mailTemplate/        # Email templates
│   └── test/                        # Unit tests
├── pom.xml                          # Maven dependencies
├── compose.yaml                     # Docker Compose configuration
├── init-db.sql                      # Database initialization script
└── README.md                        # This file
```

## Key Features

- ✅ **Spring Boot 4.0.0** with Java 21
- ✅ **JWT Authentication** - Secure token-based authentication
- ✅ **WebSocket** - Real-time chat and notifications
- ✅ **RabbitMQ** - Message queue for async operations
- ✅ **Redis** - Caching and session management
- ✅ **MinIO** - File/avatar storage
- ✅ **PostgreSQL** - Relational database
- ✅ **Swagger/OpenAPI** - Interactive API documentation
- ✅ **Email Support** - Gmail SMTP integration
- ✅ **Security** - Spring Security with CORS configuration

## Troubleshooting

### Port Already in Use
If port 8080 is in use, change it in `application.yml`:
```yaml
server:
  port: 8081
```

### Database Connection Error
Ensure PostgreSQL is running and credentials in `.env` are correct:
```bash
psql -U postgres -h localhost -d SmartHospital_DB
```

### Docker Services Won't Start
Remove conflicting containers:
```bash
docker-compose down
docker-compose up -d
```

### Redis Connection Issues
```bash
# Test Redis connection
docker exec redis redis-cli ping
```

### MinIO Connection Issues
Access MinIO console: http://localhost:9001
- Username: `minioadmin`
- Password: (from `.env` file)

## Development Commands

```bash
# Clean build
./mvnw clean

# Build without tests
./mvnw package -DskipTests

# Run tests
./mvnw test

# Format code
./mvnw spring-javaformat:apply

# Check code style
./mvnw spring-javaformat:validate

# View Maven dependency tree
./mvnw dependency:tree
```

## Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/SmartHospital_DB` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `secure_password` |
| `MAIL_USERNAME` | Gmail address | `your_email@gmail.com` |
| `MAIL_PASSWORD` | Gmail app password | `xxxx xxxx xxxx xxxx` |
| `FRONTEND_URL` | Frontend application URL | `http://localhost:3000` |
| `RABBITMQ_USER` | RabbitMQ username | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |
| `MINIO_USER` | MinIO root user | `minioadmin` |
| `MINIO_PASSWORD` | MinIO root password | `minioadmin` |
| `JWT_SECRET` | Secret key for JWT tokens | `your_secret_key` |

## Production Deployment

For production deployment:

1. Build the WAR/JAR:
   ```bash
   ./mvnw clean package
   ```

2. Update environment variables for production

3. Use environment-specific profiles:
   ```bash
   java -jar target/SmartHospital-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

4. Configure HTTPS/SSL certificates

5. Set up proper database backups

6. Use managed services for PostgreSQL, Redis, and RabbitMQ (cloud providers)

## Support & Documentation

- **Spring Boot**: https://spring.io/projects/spring-boot
- **Spring Security**: https://spring.io/projects/spring-security
- **PostgreSQL**: https://www.postgresql.org/docs/
- **RabbitMQ**: https://www.rabbitmq.com/documentation.html
- **Redis**: https://redis.io/documentation
- **MinIO**: https://docs.min.io/

## License

This project is part of the SmartHospital graduation project.
