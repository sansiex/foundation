# External Libraries Directory

This directory contains third-party JavaScript libraries downloaded for offline use.

## Libraries

### Core Dependencies
- **marked.min.js** (35.1KB)
  - **Purpose**: Markdown parsing and rendering
  - **Version**: Latest compatible version
  - **Source**: https://marked.js.org/
  - **Usage**: Renders user messages and AI responses with Markdown formatting

- **simple-purify.js** (2.1KB)
  - **Purpose**: HTML sanitization for security
  - **Type**: Custom implementation (DOMPurify alternative)
  - **Usage**: Sanitizes HTML content from Markdown rendering to prevent XSS

### WebSocket Dependencies (Optional)
- **sockjs.min.js** (2.0KB)
  - **Purpose**: WebSocket fallback and compatibility
  - **Type**: Minimal local implementation
  - **Source**: Based on SockJS Client
  - **Usage**: Enables real-time communication when WebSocket is available

- **stomp.umd.min.js** (3.9KB)
  - **Purpose**: STOMP protocol over WebSocket
  - **Type**: Minimal local implementation  
  - **Source**: Based on @stomp/stompjs
  - **Usage**: Message protocol for WebSocket communication

## Offline Configuration

These libraries have been downloaded locally to ensure the application can run completely offline without external CDN dependencies. The application will automatically fall back to HTTP streaming if WebSocket libraries are not available.

## Maintenance

When updating these libraries:
1. Download the new version to this directory
2. Update the version information in this README
3. Test the application to ensure compatibility
4. Update any breaking changes in the application code

## Directory Structure
```
/lib/
├── README.md          # This documentation file
├── marked.min.js      # Markdown parsing library
├── simple-purify.js  # HTML sanitization (custom)
├── sockjs.min.js     # WebSocket fallback (minimal)
└── stomp.umd.min.js  # STOMP protocol (minimal)
```