package dev.httpserver.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpSerializer {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] COLON_SPACE = ": ".getBytes(StandardCharsets.ISO_8859_1);

    public void serialize(HttpResponse response, OutputStream out) throws IOException {
        HttpStatus status = response.getStatus();

        String statusLine = "HTTP/1.1 " + status.getCode() + " " + status.getReason();
        out.write(statusLine.getBytes(StandardCharsets.ISO_8859_1));
        out.write(CRLF);

        for (Map.Entry<String, String> e : response.getHeaders().entrySet()) {
            out.write(e.getKey().getBytes(StandardCharsets.ISO_8859_1));
            out.write(COLON_SPACE);
            out.write(e.getValue().getBytes(StandardCharsets.ISO_8859_1));
            out.write(CRLF);
        }

        out.write(CRLF);

        byte[] body = response.getBody();
        if (body.length > 0) {
            out.write(body);
        }

        out.flush();
    }
}
