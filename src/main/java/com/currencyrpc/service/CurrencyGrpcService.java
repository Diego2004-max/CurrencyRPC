package com.currencyrpc.service;

import com.currencyrpc.grpc.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CurrencyGrpcService extends CurrencyServiceGrpc.CurrencyServiceImplBase {
    private static final long UPDATE_INTERVAL_MILLIS = 1000;
    private static final List<BigDecimal> VARIATIONS = List.of(
            new BigDecimal("1.000"), new BigDecimal("1.001"), new BigDecimal("0.999"),
            new BigDecimal("1.002"), new BigDecimal("1.0005"));
    private final CurrencyRateService rates;

    public CurrencyGrpcService(CurrencyRateService rates) { this.rates = rates; }

    @Override
    public void convertCurrency(ConvertRequest request, StreamObserver<ConvertResponse> observer) {
        try {
            var result = rates.convert(request.getFromCurrency(), request.getToCurrency(), request.getAmount());
            observer.onNext(ConvertResponse.newBuilder()
                    .setFromCurrency(result.from()).setToCurrency(result.to())
                    .setOriginalAmount(result.original().toPlainString())
                    .setConvertedAmount(result.converted().toPlainString())
                    .setExchangeRate(result.rate().toPlainString())
                    .setMessage("Conversion completed successfully").build());
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getSupportedCurrencies(SupportedCurrenciesRequest request,
                                       StreamObserver<SupportedCurrenciesResponse> observer) {
        observer.onNext(SupportedCurrenciesResponse.newBuilder()
                .addAllCurrencies(rates.supportedCurrencies()).build());
        observer.onCompleted();
    }

    @Override
    public void watchExchangeRate(RateRequest request, StreamObserver<RateUpdate> observer) {
        Context context = Context.current();
        try {
            String from = rates.normalize(request.getFromCurrency());
            String to = rates.normalize(request.getToCurrency());
            BigDecimal base = rates.exchangeRate(from, to);
            for (int i = 0; i < VARIATIONS.size(); i++) {
                if (i > 0) Thread.sleep(UPDATE_INTERVAL_MILLIS);
                if (context.isCancelled()) return;
                BigDecimal rate = from.equals(to) ? BigDecimal.ONE : base.multiply(VARIATIONS.get(i));
                observer.onNext(RateUpdate.newBuilder().setFromCurrency(from).setToCurrency(to)
                        .setExchangeRate(rate.setScale(CurrencyRateService.RATE_SCALE, RoundingMode.HALF_UP).toPlainString())
                        .setSequence(i + 1).setTimestampEpochMillis(System.currentTimeMillis()).build());
            }
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!context.isCancelled()) {
                observer.onError(Status.CANCELLED.withDescription("Stream interrupted").asRuntimeException());
            }
        }
    }
}
