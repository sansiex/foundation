# AI Chat Foundation

A comprehensive Spring Boot-based web application that provides an intelligent conversational interface powered by the locally deployed OLLAMA qwen2.5-vl:32b model. The service offers a modern, user-friendly chat experience with multimodal capabilities, supporting both text and image inputs with real-time streaming responses.

## Features

- **Multimodal Chat Interface**: Support for both text and image inputs
- **Real-time Streaming**: Live response streaming with typewriter effect
- **File Upload Management**: Secure image handling and processing
- **Conversation History**: Session-based chat persistence
- **Responsive Design**: Mobile and desktop compatible interface
- **WebSocket Support**: Real-time communication for optimal user experience
- **RESTful API**: Complete REST API for integration
- **Comprehensive Testing**: Unit and integration tests included

## Technology Stack

### Backend
- **Spring Boot 3.2.0** with Java 17
- **Spring WebFlux** for reactive streaming
- **Spring Data JPA** with H2/PostgreSQL support
- **Spring Security** for authentication and authorization
- **Spring WebSocket** for real-time communication
- **Jackson** for JSON processing
- **Apache HTTP Client** for OLLAMA API integration

### Frontend
- **HTML5/CSS3/JavaScript** with modern ES6+ features
- **WebSocket/SSE** for real-time communication
- **Responsive Design** with CSS Grid and Flexbox
- **File Upload** with drag-and-drop support

### Database
- **H2 Database** (development)
- **PostgreSQL** (production)

## Project Structure

```
src/
├── main/
│   ├── java/com/aichat/foundation/
│   │   ├── client/           # OLLAMA API client
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST and WebSocket controllers
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── entity/           # JPA entities
│   │   ├── exception/        # Custom exceptions and handlers
│   │   ├── repository/       # Data repositories
│   │   └── service/          # Business logic services
│   └── resources/
│       ├── static/           # Frontend assets (HTML, CSS, JS)
│       └── application.yml   # Configuration file
└── test/                     # Unit and integration tests
```

## Prerequisites

1. **Java 17** or higher
2. **OLLAMA** installed and running locally
3. **qwen2.5-vl:32b** model downloaded in OLLAMA

### Installing OLLAMA and the Model

1. Install OLLAMA from [https://ollama.ai](https://ollama.ai)
2. Pull the required model:
   ```bash
   ollama pull qwen2.5-vl:32b
   ```
3. Verify OLLAMA is running:
   ```bash
   curl http://localhost:11434/api/tags
   ```

## Quick Start

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd foundation
   ./mvnw clean compile
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the application:**
   - Open your browser and navigate to: http://localhost:8080
   - The H2 database console is available at: http://localhost:8080/h2-console

## Configuration

### Application Configuration (application.yml)

```yaml
# OLLAMA Configuration
ollama:
  base-url: http://localhost:11434
  model: qwen2.5-vl:32b
  timeout: 30000
  max-retries: 3

# File Upload Configuration
file:
  upload-dir: ./uploads
  max-size: 10485760  # 10MB
  allowed-types: image/jpeg,image/png,image/gif,image/webp

# Server Configuration
server:
  port: 8080
```

### Environment-Specific Profiles

- **Development**: Default profile with H2 database
- **Production**: PostgreSQL with environment variable configuration
- **Test**: In-memory database for testing

## API Endpoints

### Chat Management
- `GET /api/chat/sessions` - Get all chat sessions
- `POST /api/chat/sessions` - Create new session
- `GET /api/chat/sessions/{id}` - Get specific session
- `DELETE /api/chat/sessions/{id}` - Delete session
- `GET /api/chat/sessions/{id}/messages` - Get session messages

### Messaging
- `POST /api/chat/message` - Send text message
- `POST /api/chat/message/multimodal` - Send message with image
- `GET /ws/chat` - WebSocket endpoint for real-time streaming

### File Management
- `POST /api/files/upload` - Upload file
- `GET /api/files/{id}` - Download file
- `DELETE /api/files/{id}` - Delete file

### Health Check
- `GET /api/chat/health` - Service health status

## Usage Examples

### Creating a New Chat Session

```javascript
const response = await fetch('/api/chat/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: 'My Chat' })
});
const session = await response.json();
```

### Sending a Text Message

```javascript
const response = await fetch('/api/chat/message', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        sessionId: sessionId,
        content: 'Hello, AI!'
    })
});
```

### Uploading an Image

```javascript
const formData = new FormData();
formData.append('sessionId', sessionId);
formData.append('content', 'What do you see in this image?');
formData.append('file', imageFile);

const response = await fetch('/api/chat/message/multimodal', {
    method: 'POST',
    body: formData
});
```

## Development

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ChatServiceTest

# Run with coverage
./mvnw test jacoco:report
```

### Building for Production

```bash
# Build JAR file
./mvnw clean package

# Run with production profile
java -jar target/foundation-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Development Mode

```bash
# Run with auto-reload
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Architecture Highlights

### Real-time Communication
- **WebSocket** primary communication method
- **Server-Sent Events** fallback for streaming
- **HTTP polling** ultimate fallback

### File Upload Security
- File type validation (images only)
- Size limits (10MB default)
- Secure file storage with UUID naming
- Malicious file detection

### Error Handling
- Global exception handling
- User-friendly error messages
- Comprehensive logging
- Graceful degradation

### Scalability Considerations
- Stateless session management
- Database connection pooling
- Async processing for OLLAMA requests
- Configurable timeouts and retries

## Troubleshooting

### Common Issues

1. **OLLAMA Connection Failed**
   - Ensure OLLAMA is running: `ollama serve`
   - Check model availability: `ollama list`
   - Verify network connectivity

2. **File Upload Issues**
   - Check file size limits in configuration
   - Verify allowed file types
   - Ensure upload directory permissions

3. **WebSocket Connection Problems**
   - Check browser WebSocket support
   - Verify CORS configuration
   - Test with HTTP fallback

### Logging

- Application logs: `logs/ai-chat.log`
- Debug mode: Set `logging.level.com.aichat.foundation: DEBUG`
- WebSocket debugging: Enable in browser developer tools

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Check the [Issues](link-to-issues) page
- Review the [Wiki](link-to-wiki) for detailed documentation
- Contact the development team

---

**Note**: This application requires OLLAMA to be running locally with the qwen2.5-vl:32b model for full functionality. The chat interface will gracefully handle cases where the AI service is unavailable.