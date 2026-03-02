package dev.httpserver.net;

import java.net.Socket;

public final class Connection {

    private final Socket socket;
    private boolean keepAlive = true;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
