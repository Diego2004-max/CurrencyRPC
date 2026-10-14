package com.currencyrpc.client;

import com.currencyrpc.config.AppConfig;
import com.currencyrpc.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.TimeUnit;

public final class CurrencyClient {
    private CurrencyClient() {}

    public static void main(String[] args) throws InterruptedException {
        int exitCode = 0;
        ManagedChannel channel = null;
        try {
            String host = AppConfig.env("GRPC_HOST", "localhost");
            int port = AppConfig.port("GRPC_PORT", 9090);
            boolean tls = AppConfig.tls();
            var builder = NettyChannelBuilder.forAddress(host, port);
            channel = (tls ? builder.useTransportSecurity() : builder.usePlaintext()).build();
            var stub = CurrencyServiceGrpc.newBlockingStub(channel);
            System.out.printf("=== CurrencyRPC - gRPC Client ===%nServer: %s:%d | TLS: %s%n", host, port, tls);
            System.out.println("1. Supported currencies: " + stub.withDeadlineAfter(30, TimeUnit.SECONDS)
                    .getSupportedCurrencies(SupportedCurrenciesRequest.getDefaultInstance()).getCurrenciesList());
            System.out.println("2. Currency conversion (simulated rates)");
            convert(stub, "USD", "COP", "100");
            convert(stub, "COP", "USD", "500000");
            System.out.println("3. Exchange rate streaming: USD -> COP");
            var updates = stub.withDeadlineAfter(30, TimeUnit.SECONDS).watchExchangeRate(
                    RateRequest.newBuilder().setFromCurrency("USD").setToCurrency("COP").build());
            updates.forEachRemaining(update -> System.out.printf("Update %d | %s -> %s | Rate: %s | Timestamp: %d%n",
                    update.getSequence(), update.getFromCurrency(), update.getToCurrency(),
                    update.getExchangeRate(), update.getTimestampEpochMillis()));
            System.out.println("4. Input validation (expected errors)");
            verifyInvalidInput(stub, "USD", "COP", "0");
            verifyInvalidInput(stub, "XYZ", "COP", "100");
            System.out.println("Demo completed successfully.");
        } catch (StatusRuntimeException e) {
            System.err.printf("gRPC error: %s - %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
            exitCode = 1;
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            exitCode = 1;
        } finally {
            if (channel != null && !channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        }
        if (exitCode != 0) System.exit(exitCode);
    }

    private static void verifyInvalidInput(CurrencyServiceGrpc.CurrencyServiceBlockingStub stub,
                                           String from, String to, String amount) {
        try {
            stub.withDeadlineAfter(30, TimeUnit.SECONDS).convertCurrency(ConvertRequest.newBuilder()
                    .setFromCurrency(from).setToCurrency(to).setAmount(amount).build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.INVALID_ARGUMENT) throw e;
            System.out.printf("Validation %s %s -> %s | %s: %s%n", amount, from, to,
                    e.getStatus().getCode(), e.getStatus().getDescription());
            return;
        }
        throw new IllegalStateException("Server accepted an invalid demo request");
    }

    private static void convert(CurrencyServiceGrpc.CurrencyServiceBlockingStub stub,
                                String from, String to, String amount) {
        var response = stub.withDeadlineAfter(30, TimeUnit.SECONDS).convertCurrency(ConvertRequest.newBuilder()
                .setFromCurrency(from).setToCurrency(to).setAmount(amount).build());
        System.out.printf("%s %s -> %s %s | Rate: %s | %s%n", response.getOriginalAmount(),
                response.getFromCurrency(), response.getConvertedAmount(), response.getToCurrency(),
                response.getExchangeRate(), response.getMessage());
    }
}
