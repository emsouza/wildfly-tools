package org.jboss.tools.jmx.jolokia.test.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

public class MockJolokiaServer {

    private final HttpServer server;
    private String listResponse;
    private String searchResponse;
    private String readResponse;
    private String writeResponse;
    private String execResponse;

    public MockJolokiaServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "mock-jolokia");
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/", this::handleRequest);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getUrl() {
        return "http://127.0.0.1:" + getPort() + "/j4p";
    }

    public void setListResponse(String json) { this.listResponse = json; }
    public void setSearchResponse(String json) { this.searchResponse = json; }
    public void setReadResponse(String json) { this.readResponse = json; }
    public void setWriteResponse(String json) { this.writeResponse = json; }
    public void setExecResponse(String json) { this.execResponse = json; }

    private void handleRequest(HttpExchange exchange) throws java.io.IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getRawPath();
        String query = exchange.getRequestURI().getRawQuery();
        String body = null;

        if ("POST".equals(method)) {
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        String jolokiaType = extractType(method, path, query, body);
        String jsonResponse = selectResponse(jolokiaType);

        byte[] resp = jsonResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private static String extractType(String method, String path, String query, String body) {
        // REST-style path: /j4p/read/... or /j4p/search/...
        if (path != null) {
            if (path.contains("/search/")) return "search";
            if (path.contains("/list/") || path.contains("/list\"")) return "list";
            if (path.contains("/read/")) return "read";
            if (path.contains("/write/")) return "write";
            if (path.contains("/exec/")) return "exec";
            if (path.endsWith("/version")) return "version";
            if (path.contains("/search")) return "search";
            if (path.contains("/list") && !path.equals("/j4p")) return "list";
        }
        // Query string fallback (legacy Jolokia 1.x style)
        if (query != null) {
            if (query.contains("type=read")) return "read";
            if (query.contains("type=write")) return "write";
            if (query.contains("type=exec")) return "exec";
            if (query.contains("type=list")) return "list";
            if (query.contains("type=search")) return "search";
            if (query.contains("type=version")) return "version";
        }
        // JSON body (POST)
        if (body != null) {
            if (body.contains("\"type\":\"read\"")) return "read";
            if (body.contains("\"type\":\"write\"")) return "write";
            if (body.contains("\"type\":\"exec\"")) return "exec";
            if (body.contains("\"type\":\"list\"")) return "list";
            if (body.contains("\"type\":\"search\"")) return "search";
            if (body.contains("\"type\":\"version\"")) return "version";
        }
        return "unknown";
    }

    private String selectResponse(String type) {
        return switch (type) {
            case "read" -> readResponse != null ? readResponse : "{\"status\":200,\"value\":{}}";
            case "write" -> writeResponse != null ? writeResponse : "{\"status\":200,\"value\":{}}";
            case "exec" -> execResponse != null ? execResponse : "{\"status\":200,\"value\":null}";
            case "list" -> listResponse != null ? listResponse : "{\"status\":200,\"value\":{}}";
            case "search" -> searchResponse != null ? searchResponse : "{\"status\":200,\"value\":[]}";
            case "version" -> "{\"status\":200,\"value\":{\"agent\":\"2.6.0\",\"protocol\":\"7.2\"}}";
            default -> "{\"status\":200,\"value\":{}}";
        };
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }
}
