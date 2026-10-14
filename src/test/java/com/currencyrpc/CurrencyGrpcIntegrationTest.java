package com.currencyrpc;

import com.currencyrpc.grpc.*;
import com.currencyrpc.service.CurrencyGrpcService;
import com.currencyrpc.service.CurrencyRateService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CurrencyGrpcIntegrationTest {
    private Server server;
    private ManagedChannel channel;
    private ExecutorService executor;
    private CurrencyServiceGrpc.CurrencyServiceBlockingStub stub;

    @BeforeAll void start() throws Exception {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0))
                .executor(executor).addService(new CurrencyGrpcService(new CurrencyRateService())).build().start();
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).usePlaintext().build();
        stub = CurrencyServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll void stop() throws InterruptedException {
        try {
            if (channel != null) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } finally {
            if (server != null) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            if (executor != null) executor.shutdownNow();
        }
    }

    @Test void listsSixCurrencies() {
        assertEquals(List.of("USD", "COP", "EUR", "GBP", "JPY", "MXN"),
                stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                        .getSupportedCurrencies(SupportedCurrenciesRequest.getDefaultInstance()).getCurrenciesList());
    }

    @Test void convertsOverHttp2() {
        var result = stub.withDeadlineAfter(5, TimeUnit.SECONDS).convertCurrency(ConvertRequest.newBuilder()
                .setFromCurrency("USD").setToCurrency("COP").setAmount("100").build());
        assertEquals("400000.00", result.getConvertedAmount());
        assertEquals("Conversion completed successfully", result.getMessage());
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "NaN", ""})
    void invalidAmountUsesGrpcStatus(String amount) {
        var error = assertThrows(StatusRuntimeException.class, () -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .convertCurrency(ConvertRequest.newBuilder().setFromCurrency("USD")
                        .setToCurrency("COP").setAmount(amount).build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    }

    @Test void unknownCurrencyUsesGrpcStatus() {
        var error = assertThrows(StatusRuntimeException.class, () -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .convertCurrency(ConvertRequest.newBuilder().setFromCurrency("XYZ")
                        .setToCurrency("COP").setAmount("100").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    }

    @Test void streamsFiveOrderedUpdatesAndKeepsUnaryRateFixed() {
        var updates = new ArrayList<RateUpdate>();
        stub.withDeadlineAfter(10, TimeUnit.SECONDS).watchExchangeRate(pair("USD", "COP"))
                .forEachRemaining(updates::add);
        assertEquals(5, updates.size());
        assertEquals(List.of("4000.000000000000", "4004.000000000000", "3996.000000000000",
                "4008.000000000000", "4002.000000000000"), updates.stream().map(RateUpdate::getExchangeRate).toList());
        for (int i = 0; i < updates.size(); i++) {
            assertEquals(i + 1, updates.get(i).getSequence());
            assertEquals("USD", updates.get(i).getFromCurrency());
            assertEquals("COP", updates.get(i).getToCurrency());
            if (i > 0) assertTrue(updates.get(i).getTimestampEpochMillis() > updates.get(i - 1).getTimestampEpochMillis());
        }
        convertsOverHttp2();
    }

    @Test void invalidStreamUsesGrpcStatus() {
        var error = assertThrows(StatusRuntimeException.class, () -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .watchExchangeRate(pair("USD", "XYZ")).forEachRemaining(update -> fail("Unexpected update")));
        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    }

    @Test void clientDeadlineStopsStreamAndServerRemainsAvailable() {
        var error = assertThrows(StatusRuntimeException.class, () -> stub.withDeadlineAfter(200, TimeUnit.MILLISECONDS)
                .watchExchangeRate(pair("USD", "COP")).forEachRemaining(update -> {}));
        assertEquals(Status.Code.DEADLINE_EXCEEDED, error.getStatus().getCode());
        listsSixCurrencies();
    }

    private static RateRequest pair(String from, String to) {
        return RateRequest.newBuilder().setFromCurrency(from).setToCurrency(to).build();
    }
}
