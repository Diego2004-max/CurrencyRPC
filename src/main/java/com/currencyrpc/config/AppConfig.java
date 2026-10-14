package com.currencyrpc.config;

public final class AppConfig {
    private AppConfig() {}

    public static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static int port(String name, int fallback) {
        try {
            int port = Integer.parseInt(env(name, Integer.toString(fallback)));
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer between 1 and 65535");
        }
    }

    public static boolean tls() {
        String value = env("GRPC_TLS", "false");
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("GRPC_TLS must be true or false");
        }
        return Boolean.parseBoolean(value);
    }
}
