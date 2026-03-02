package dev.httpserver.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpParserTest {

    private HttpParser parser;

    @BeforeEach
    void setUp() {
        parser = new HttpParser(4096, 1024);
    }

    // Valid requests

    @Test
    void parsesSimpleGetRequest() throws Exception {
        String raw = "GET /health HTTP/1.1\r\nHost: localhost\r\n\r\n";
        HttpRequest req = parse(raw);

        assertEquals(HttpMethod.GET, req.getMethod());
        assertEquals("/health", req.getPath());
        assertTrue(req.getQueryParams().isEmpty());
        assertTrue(req.getBody().length == 0);
    }

    @Test
    void parsesQueryParameters() throws Exception {
        String raw = "GET /search?q=hello&page=2 HTTP/1.1\r\nHost: localhost\r\n\r\n";
        HttpRequest req = parse(raw);

        assertEquals("/search", req.getPath());
        assertEquals("hello", req.getQueryParams().get("q"));
        assertEquals("2", req.getQueryParams().get("page"));
    }

    @Test
    void parsesPostWithBody() throws Exception {
        String body = "{\"name\":\"anna\"}";
        String raw = "POST /json HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: " + body.length() + "\r\n"
                + "\r\n"
                + body;

        HttpRequest req = parse(raw);

        assertEquals(HttpMethod.POST, req.getMethod());
        assertEquals(body, new String(req.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    void headersAreLowercased() throws Exception {
        String raw = "GET / HTTP/1.1\r\nHost: localhost\r\nContent-Type: text/plain\r\n\r\n";
        HttpRequest req = parse(raw);

        assertNotNull(req.getHeader("host"));
        assertNotNull(req.getHeader("content-type"));
    }

    @Test
    void acceptsBareLineFeedAsLineSeparator() throws Exception {
        String raw = "GET /health HTTP/1.1\nHost: localhost\n\n";
        HttpRequest req = parse(raw);
        assertEquals("/health", req.getPath());
    }

    // Error cases

    @Test
    void rejectsMissingHostHeader() {
        String raw = "GET /health HTTP/1.1\r\n\r\n"; // no Host
        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class, () -> parse(raw));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getSuggestedStatus());
    }

    @Test
    void rejectsMalformedRequestLine() {
        String raw = "NOTVALID\r\nHost: localhost\r\n\r\n";
        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class, () -> parse(raw));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getSuggestedStatus());
    }

    @Test
    void rejectsUnknownMethod() {
        String raw = "PATCH /health HTTP/1.1\r\nHost: localhost\r\n\r\n";
        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class, () -> parse(raw));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getSuggestedStatus());
    }

    @Test
    void rejectsOversizedBody() {
        String bigBody = "x".repeat(2048); // parser allows only 1024
        String raw = "POST /json HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Content-Length: " + bigBody.length() + "\r\n"
                + "\r\n"
                + bigBody;

        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class, () -> parse(raw));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getSuggestedStatus());
    }

    @Test
    void rejectsOversizedHeaders() {
        // Parser with tiny header limit
        HttpParser tinyParser = new HttpParser(50, 1024);
        // Request line + host alone exceed 50 bytes
        String raw = "GET /very/long/path/that/exceeds/the/limit HTTP/1.1\r\nHost: localhost\r\n\r\n";
        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class,
                () -> tinyParser.parse(stream(raw)));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getSuggestedStatus());
    }

    @Test
    void returnsNullStatusForSilentEof() {
        String raw = ""; // empty stream
        HttpParser.ParseException ex = assertThrows(HttpParser.ParseException.class, () -> parse(raw));
        assertNull(ex.getSuggestedStatus());
    }

    // Helpers

    private HttpRequest parse(String raw) throws HttpParser.ParseException, IOException {
        return parser.parse(stream(raw));
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
