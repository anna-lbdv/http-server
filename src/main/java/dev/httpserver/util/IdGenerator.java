package dev.httpserver.util;

import java.util.UUID;

/** Generates unique request/correlation identifiers. */
public final class IdGenerator {

    private IdGenerator() {}

    /** Returns a random UUID string */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
