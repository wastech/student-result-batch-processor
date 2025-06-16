# Student Batch Processing System

A Spring Boot application that processes student results through batch jobs, with support for CSV file uploads, grade calculation, and result querying.

## Features

- **Batch Processing**: Upload CSV files containing student results for batch processing
- **Grade Calculation**: Automatically calculates grades based on scores (A-F scale)
- **Job Management**: Start, stop, and monitor batch job executions
- **Result Querying**: Retrieve processed student results with overall averages
- **Redis Caching**: Integrated Redis for performance optimization
- **Error Handling**: Comprehensive validation and error handling with skip logic
- **Database Integration**: PostgreSQL for persistent storage
- **Containerized Deployment**: Docker Compose setup for easy deployment

## Technology Stack

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Batch**
- **Spring Data JPA**
- **PostgreSQL**
- **Redis**
- **Docker & Docker Compose**

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven (for building from source)

## Quick Start

### Using Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone <repository-url>
cd student-batch-processing
```

2. Start all services:
```bash
docker-compose up -d
```

3. Access the application:
   - Application: http://localhost:8080
   - PgAdmin: http://localhost:5050 (admin@admin.com / admin)
   - Redis: localhost:6379

### Manual Setup

1. Start PostgreSQL and Redis services
2. Update `application.properties` with your database credentials
3. Build and run the application:
```bash
mvn clean package
java -jar target/student-batch-*.jar
```

## API Endpoints

### Batch Job Operations

#### Upload File and Start Batch Job
```http
POST /api/batch/upload
Content-Type: multipart/form-data

Parameters:
- file: CSV file containing student results
```

#### Get Job Status
```http
GET /api/batch/status/{jobExecutionId}
```

#### Get Job History
```http
GET /api/batch/history/{jobName}
```

#### Stop Running Job
```http
POST /api/batch/stop/{jobExecutionId}
```

### Student Results

#### Get Student Results
```http
GET /api/batch/student/{studentId}/results
```

### Cache Management

#### Clear Redis Cache
```http
POST /api/batch/cache/clear
```

## CSV File Format

The application expects CSV files with the following format:

```csv
studentId,courseName,score
S001,Mathematics,85
S001,Physics,92
S002,Chemistry,78
S002,Biology,88
```

**Required columns:**
- `studentId`: Unique identifier for the student
- `courseName`: Name of the course
- `score`: Numeric score (0-100)

## Grade Calculation

Grades are automatically calculated based on scores:
- **A**: 90-100
- **B**: 80-89
- **C**: 70-79
- **D**: 60-69
- **F**: Below 60

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/studentdb
spring.datasource.username=user
spring.datasource.password=password

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379

# Batch Configuration
batch.chunk.size=100
batch.upload.directory=/tmp/batch-uploads

# Batch Job Tables
spring.batch.jdbc.initialize-schema=always
```

### Environment Variables

When using Docker Compose, the following environment variables are configured:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `BATCH_UPLOAD_DIRECTORY`

## Architecture

### Key Components

1. **BatchController**: REST API endpoints for batch operations
2. **BatchJobService**: Core service handling batch job lifecycle
3. **StudentResultItemProcessor**: Processes and validates individual records
4. **JobCompletionNotificationListener**: Monitors job completion and provides statistics
5. **CustomSkipListener**: Handles error scenarios and skipped records

### Data Flow

1. User uploads CSV file via REST API
2. File is saved to configured upload directory
3. Batch job is started with file path as parameter
4. Each record is read, processed, and validated
5. Valid records are saved to database with calculated grades
6. Job completion statistics are logged
7. Results can be queried via API

## Error Handling

The application includes comprehensive error handling:

- **File Validation**: Checks for empty files and valid formats
- **Data Validation**: Validates student IDs, course names, and scores
- **Skip Logic**: Invalid records are skipped (up to 1000 per job)
- **Job Monitoring**: Real-time job status and execution details

## Monitoring and Logging

- Detailed logging for all batch operations
- Job execution summaries with read/write/skip counts
- Database record verification after job completion
- Redis cache management and monitoring

## Development

### Building from Source

```bash
mvn clean compile
mvn test
mvn package
```

### Running Tests

```bash
mvn test
```

### Database Schema

The application automatically creates the required tables:
- Spring Batch metadata tables
- `student_result` table for storing processed results

## Troubleshooting

### Common Issues

1. **File Upload Fails**
   - Check upload directory permissions
   - Verify file format matches expected CSV structure

2. **Job Execution Fails**
   - Check database connectivity
   - Verify CSV file format and data validity
   - Review application logs for specific error messages

3. **Database Connection Issues**
   - Ensure PostgreSQL is running
   - Verify connection credentials
   - Check network connectivity in Docker environment

### Logs and Debugging

- Application logs provide detailed information about batch processing
- Job execution summaries include processing statistics
- Database verification logs help identify data issues

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

