package com.currencyrpc.server;

import com.currencyrpc.config.AppConfig;
import com.currencyrpc.service.CurrencyGrpcService;
import com.currencyrpc.service.CurrencyRateService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class CurrencyServer {
    private CurrencyServer() {}

    public static void main(String[] args) throws Exception {
        int port = AppConfig.port("PORT", 9090);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        Server server = NettyServerBuilder.forAddress(new InetSocketAddress("0.0.0.0", port))
                .executor(executor).addService(new CurrencyGrpcService(new CurrencyRateService())).build();
        try {
            server.start();
        } catch (Exception e) {
            executor.shutdownNow();
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping CurrencyRPC...");
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                    server.awaitTermination(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdownNow();
            }
        }, "currency-shutdown"));
        System.out.printf("CurrencyRPC listening on 0.0.0.0:%d (gRPC / HTTP/2, simulated rates)%n", port);
        server.awaitTermination();
    }
}
