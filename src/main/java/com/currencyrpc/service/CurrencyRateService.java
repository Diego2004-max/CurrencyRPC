package com.currencyrpc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CurrencyRateService {
    public static final int RATE_SCALE = 12;
    private static final int AMOUNT_SCALE = 2;
    private static final List<String> CURRENCIES = List.of("USD", "COP", "EUR", "GBP", "JPY", "MXN");
    // Units of each currency per USD. Fixed academic data, not market quotes.
    private static final Map<String, BigDecimal> RATES = Map.of(
            "USD", new BigDecimal("1"), "COP", new BigDecimal("4000"),
            "EUR", new BigDecimal("0.92"), "GBP", new BigDecimal("0.79"),
            "JPY", new BigDecimal("150"), "MXN", new BigDecimal("17"));

    public List<String> supportedCurrencies() { return CURRENCIES; }

    public String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        String code = currency.trim().toUpperCase(Locale.ROOT);
        if (!RATES.containsKey(code)) throw new IllegalArgumentException("Unsupported currency: " + code);
        return code;
    }

    public BigDecimal exchangeRate(String from, String to) {
        return RATES.get(normalize(to)).divide(RATES.get(normalize(from)), RATE_SCALE, RoundingMode.HALF_UP);
    }

    public Conversion convert(String from, String to, String amountText) {
        String source = normalize(from);
        String target = normalize(to);
        // Bound input size and disallow exponents to keep decimal parsing predictable.
        if (amountText == null || !amountText.matches("[0-9]{1,18}(\\.[0-9]{1,8})?")) {
            throw new IllegalArgumentException("Amount must be a positive decimal (up to 18 integer and 8 fractional digits)");
        }
        BigDecimal amount = new BigDecimal(amountText);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
        // Round only after cross conversion; the displayed rate is rounded separately.
        BigDecimal converted = amount.multiply(RATES.get(target))
                .divide(RATES.get(source), AMOUNT_SCALE, RoundingMode.HALF_UP);
        return new Conversion(source, target, amount, converted, exchangeRate(source, target));
    }

    public record Conversion(String from, String to, BigDecimal original,
                             BigDecimal converted, BigDecimal rate) {}
}
