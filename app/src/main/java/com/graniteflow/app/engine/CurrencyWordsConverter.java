package com.graniteflow.app.engine;

import java.math.BigDecimal;

public class CurrencyWordsConverter {

    private static final String[] ONES = {
        "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
        "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"
    };

    private static final String[] TENS = {
        "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
    };

    public static String convertToWords(double amount, String currency) {
        if (amount < 0) {
            return "NEGATIVE " + convertToWords(-amount, currency);
        }

        BigDecimal bd = BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        long dollars = bd.longValue();
        int cents = bd.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        StringBuilder result = new StringBuilder();
        if (currency != null && !currency.isEmpty()) {
            result.append(currency.toUpperCase()).append(" ");
        }

        if (dollars == 0) {
            result.append("ZERO");
        } else {
            result.append(convertNumberToWords(dollars));
        }

        if (cents > 0) {
            result.append(" AND CENTS ").append(convertNumberToWords(cents));
        }

        result.append(" ONLY");
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private static String convertNumberToWords(long n) {
        if (n == 0) return "";
        if (n < 20) return ONES[(int) n];
        if (n < 100) {
            return TENS[(int) (n / 10)] + ((n % 10 != 0) ? " " + ONES[(int) (n % 10)] : "");
        }
        if (n < 1000) {
            return ONES[(int) (n / 100)] + " HUNDRED" + ((n % 100 != 0) ? " " + convertNumberToWords(n % 100) : "");
        }
        if (n < 1_000_000) {
            return convertNumberToWords(n / 1000) + " THOUSAND" + ((n % 1000 != 0) ? " " + convertNumberToWords(n % 1000) : "");
        }
        if (n < 1_000_000_000) {
            return convertNumberToWords(n / 1_000_000) + " MILLION" + ((n % 1_000_000 != 0) ? " " + convertNumberToWords(n % 1_000_000) : "");
        }
        return convertNumberToWords(n / 1_000_000_000L) + " BILLION" + ((n % 1_000_000_000L != 0) ? " " + convertNumberToWords(n % 1_000_000_000L) : "");
    }
}
