package dev.httpserver.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpParser {

    private static final String HTTP_VERSION = "HTTP/1.1";

    private final int maxHeaderSizeBytes;
    private final int maxBodySizeBytes;

    public HttpParser(int maxHeaderSizeBytes, int maxBodySizeBytes) {
        this.maxHeaderSizeBytes = maxHeaderSizeBytes;
        this.maxBodySizeBytes = maxBodySizeBytes;
    }

    public HttpRequest parse(InputStream in) throws ParseException, IOException {
        int remaining = maxHeaderSizeBytes;

        String requestLine = readLine(in, remaining);
        if (requestLine == null) {
            // Connection closed before any data arrived (not a protocol error)
            throw new ParseException("Connection closed", null);
        }
        remaining -= requestLine.length() + 2; // account for CRLF

        String[] parts = requestLine.split(" ", 3);
        if (parts.length != 3) {
            throw new ParseException("Malformed request line: " + requestLine, HttpStatus.BAD_REQUEST);
        }

        HttpMethod method = HttpMethod.fromString(parts[0]);
        if (method == null) {
            throw new ParseException("Unknown HTTP method: " + parts[0], HttpStatus.METHOD_NOT_ALLOWED);
        }

        String fullPath = parts[1];
        String version = parts[2].trim();
        if (!HTTP_VERSION.equals(version)) {
            throw new ParseException("Unsupported HTTP version: " + version, HttpStatus.BAD_REQUEST);
        }

        String path;
        Map<String, String> queryParams = new HashMap<>();
        int qIdx = fullPath.indexOf('?');
        if (qIdx >= 0) {
            path = fullPath.substring(0, qIdx);
            parseQueryString(fullPath.substring(qIdx + 1), queryParams);
        } else {
            path = fullPath;
        }

        Map<String, String> headers = new HashMap<>();
        while (true) {
            if (remaining <= 0) {
                throw new ParseException("Headers exceed maxHeaderSizeBytes", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            String line = readLine(in, remaining);
            if (line == null) {
                throw new ParseException("Connection closed while reading headers", null);
            }
            remaining -= line.length() + 2;

            if (line.isEmpty()) {
                break;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx < 1) {
                throw new ParseException("Malformed header line: " + line, HttpStatus.BAD_REQUEST);
            }
            String name = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();
            headers.put(name, value);
        }

        if (!headers.containsKey("host")) {
            throw new ParseException("Missing Host header", HttpStatus.BAD_REQUEST);
        }

        byte[] body = new byte[0];
        String contentLengthStr = headers.get("content-length");
        if (contentLengthStr != null) {
            int contentLength;
            try {
                contentLength = Integer.parseInt(contentLengthStr.trim());
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid Content-Length value", HttpStatus.BAD_REQUEST);
            }
            if (contentLength < 0) {
                throw new ParseException("Negative Content-Length", HttpStatus.BAD_REQUEST);
            }
            if (contentLength > maxBodySizeBytes) {
                throw new ParseException(
                        "Body size " + contentLength + " exceeds maxBodySizeBytes " + maxBodySizeBytes,
                        HttpStatus.PAYLOAD_TOO_LARGE);
            }
            body = readExact(in, contentLength);
        }

        return HttpRequest.builder()
                .method(method)
                .path(path)
                .queryParams(queryParams)
                .headers(headers)
                .body(body)
                .build();
    }

    private String readLine(InputStream in, int maxBytes) throws IOException, ParseException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int b = in.read();
            if (b < 0) {
                return sb.length() == 0 ? null : sb.toString();
            }
            if (b == '\r') {
                int next = in.read();
                // next may be LF (CRLF)
                if (next != '\n' && next >= 0) {
                    sb.append((char) next);
                }
                return sb.toString();
            }
            if (b == '\n') {
                return sb.toString();
            }
            sb.append((char) b);
            if (sb.length() > maxBytes) {
                throw new ParseException("Header line exceeds limit", HttpStatus.PAYLOAD_TOO_LARGE);
            }
        }
    }

    private byte[] readExact(InputStream in, int length) throws IOException, ParseException {
        byte[] buf = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(buf, read, length - read);
            if (n < 0) {
                throw new ParseException("Connection closed while reading body", null);
            }
            read += n;
        }
        return buf;
    }

    private void parseQueryString(String query, Map<String, String> out) {
        if (query == null || query.isEmpty())
            return;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            try {
                if (eq >= 0) {
                    out.put(
                            URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                } else {
                    out.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static final class ParseException extends Exception {
        private final HttpStatus suggestedStatus;

        public ParseException(String message, HttpStatus suggestedStatus) {
            super(message);
            this.suggestedStatus = suggestedStatus;
        }

        public HttpStatus getSuggestedStatus() {
            return suggestedStatus;
        }
    }
}
