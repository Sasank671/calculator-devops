package com.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple HTTP server (JDK built-in, no frameworks) exposing the calculator.
 *
 * GET /api/v1/{add|subtract|multiply|divide}?a=..&b=..
 * GET /actuator/health
 */
public class CalculatorServer {

    private final Calculator calculator = new Calculator();
    private final HttpServer server;

    public CalculatorServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/v1/add", ex -> handle(ex, "add"));
        server.createContext("/api/v1/subtract", ex -> handle(ex, "subtract"));
        server.createContext("/api/v1/multiply", ex -> handle(ex, "multiply"));
        server.createContext("/api/v1/divide", ex -> handle(ex, "divide"));
        server.createContext("/actuator/health", ex -> send(ex, 200, "{\"status\":\"UP\"}"));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange ex, String operation) throws IOException {
        Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
        double a;
        double b;
        try {
            a = parseNumber(params, "a");
            b = parseNumber(params, "b");
        } catch (IllegalArgumentException e) {
            send(ex, 400, error(e.getMessage()));
            return;
        }

        double result;
        try {
            switch (operation) {
                case "add" -> result = calculator.add(a, b);
                case "subtract" -> result = calculator.subtract(a, b);
                case "multiply" -> result = calculator.multiply(a, b);
                default -> result = calculator.divide(a, b);
            }
        } catch (ArithmeticException e) {
            send(ex, 400, error(e.getMessage()));
            return;
        }

        send(ex, 200, String.format("{\"operation\":\"%s\",\"a\":%s,\"b\":%s,\"result\":%s}",
                operation, format(a), format(b), format(result)));
    }

    private static double parseNumber(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing parameter '" + name + "'");
        }
        try {
            double d = Double.parseDouble(value);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new NumberFormatException();
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number");
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    // Prints 5 instead of 5.0 for whole numbers
    private static String format(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private static String error(String message) {
        return "{\"status\":400,\"error\":\"" + message.replace("\"", "'") + "\"}";
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        CalculatorServer app = new CalculatorServer(port);
        app.start();
        System.out.println("Calculator running on port " + port);
    }
}
