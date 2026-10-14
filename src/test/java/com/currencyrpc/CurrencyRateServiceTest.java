package com.currencyrpc;

import com.currencyrpc.service.CurrencyRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyRateServiceTest {
    private final CurrencyRateService rates = new CurrencyRateService();

    @Test void convertsUsdToCop() {
        assertEquals(new BigDecimal("400000.00"), rates.convert("USD", "COP", "100").converted());
    }

    @Test void convertsCopToUsd() {
        assertEquals(new BigDecimal("125.00"), rates.convert("COP", "USD", "500000").converted());
    }

    @Test void convertsCrossCurrencyWithoutIntermediateRounding() {
        assertEquals(new BigDecimal("400000.00"), rates.convert("EUR", "COP", "92").converted());
        assertEquals(new BigDecimal("92.00"), rates.convert("COP", "EUR", "400000").converted());
    }

    @Test void normalizesCodesAndRoundsHalfUp() {
        var result = rates.convert(" usd ", "usd", "1.005");
        assertEquals("USD", result.from());
        assertEquals(new BigDecimal("1.01"), result.converted());
        assertEquals(0, BigDecimal.ONE.compareTo(result.rate()));
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings = {"0", "-1", "0.00", "NaN", "Infinity", "1e10", "1,5", "1.123456789", "1234567890123456789"})
    void rejectsInvalidAmounts(String amount) {
        assertThrows(IllegalArgumentException.class, () -> rates.convert("USD", "COP", amount));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "XYZ"})
    void rejectsInvalidCurrencyInEitherPosition(String currency) {
        assertThrows(IllegalArgumentException.class, () -> rates.convert(currency, "USD", "1"));
        assertThrows(IllegalArgumentException.class, () -> rates.convert("USD", currency, "1"));
    }
}
