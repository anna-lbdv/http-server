package dev.httpserver.routing;

import dev.httpserver.http.HttpMethod;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    private Router router;

    private static final Handler STUB = req -> HttpResponseBuilder.okText("ok");

    @BeforeEach
    void setUp() {
        router = Router.builder()
                .register(HttpMethod.GET, "/health", STUB)
                .register(HttpMethod.GET, "/echo/{id}", STUB)
                .register(HttpMethod.POST, "/json", STUB)
                .register(HttpMethod.GET, "/a/{x}/b/{y}", STUB)
                .build();
    }

    // Static routes

    @Test
    void matchesExactStaticRoute() {
        MatchResult result = router.match(HttpMethod.GET, "/health");
        assertTrue(result.isMatched());
        assertTrue(result.getPathParams().isEmpty());
    }

    @Test
    void matchesPostRoute() {
        MatchResult result = router.match(HttpMethod.POST, "/json");
        assertTrue(result.isMatched());
    }

    // Parametric routes

    @Test
    void extractsSinglePathParam() {
        MatchResult result = router.match(HttpMethod.GET, "/echo/42");
        assertTrue(result.isMatched());
        assertEquals("42", result.getPathParams().get("id"));
    }

    @Test
    void extractsMultiplePathParams() {
        MatchResult result = router.match(HttpMethod.GET, "/a/foo/b/bar");
        assertTrue(result.isMatched());
        assertEquals("foo", result.getPathParams().get("x"));
        assertEquals("bar", result.getPathParams().get("y"));
    }

    // Priority: static before parametric

    @Test
    void prefersStaticOverParametric() {
        // Register /echo/special as static AFTER the parametric /echo/{id}
        Router r = Router.builder()
                .register(HttpMethod.GET, "/echo/{id}", STUB)
                .register(HttpMethod.GET, "/echo/special", req -> HttpResponseBuilder.okText("special"))
                .build();

        MatchResult result = r.match(HttpMethod.GET, "/echo/special");
        assertTrue(result.isMatched());
        // Static handler should be invoked
        HttpResponse resp;
        try {
            resp = result.getHandler().handle(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals("special", new String(resp.getBody()));
    }

    // Not found

    @Test
    void returnsNotFoundForUnknownPath() {
        MatchResult result = router.match(HttpMethod.GET, "/unknown");
        assertTrue(result.isNotFound());
    }

    // Method not allowed

    @Test
    void returnsMethodNotAllowedWhenPathExistsButWrongMethod() {
        MatchResult result = router.match(HttpMethod.DELETE, "/health");
        assertTrue(result.isMethodNotAllowed());
    }

    @Test
    void returnsMethodNotAllowedForParametricWithWrongMethod() {
        MatchResult result = router.match(HttpMethod.POST, "/echo/99");
        assertTrue(result.isMethodNotAllowed());
    }
}
