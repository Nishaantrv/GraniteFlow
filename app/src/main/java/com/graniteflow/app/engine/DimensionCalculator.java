package com.graniteflow.app.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DimensionCalculator {

    /**
     * Calculates net dimension by subtracting the allowance once from gross dimension.
     * Rule: net = gross - allowance
     */
    public static double calculateNetDimension(double grossCm, double allowanceCm) {
        double net = grossCm - allowanceCm;
        return Math.max(0, net);
    }

    /**
     * Calculates CBM in cubic meters from dimensions in centimeters.
     * Formula: (length_cm * height_cm * width_cm) / 1,000,000.0
     * Rounded to 3 decimal places using standard half-up rounding.
     */
    public static double calculateCbm(double lengthCm, double heightCm, double widthCm) {
        if (lengthCm <= 0 || heightCm <= 0 || widthCm <= 0) {
            return 0.0;
        }
        double volumeCm3 = lengthCm * heightCm * widthCm;
        double rawCbm = volumeCm3 / 1_000_000.0;
        BigDecimal bd = BigDecimal.valueOf(rawCbm).setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Validates dimensions for physical plausibility.
     * @return null if valid, or an error message if invalid.
     */
    public static String validateDimensions(double grossL, double grossH, double grossW, double allowance) {
        if (grossL <= 0 || grossH <= 0 || grossW <= 0) {
            return "Gross dimensions must all be greater than zero.";
        }
        if (allowance < 0) {
            return "Allowance cannot be negative.";
        }
        if (grossL <= allowance || grossH <= allowance || grossW <= allowance) {
            return "Allowance (" + allowance + " cm) is larger than or equal to one of the gross dimensions.";
        }
        return null; // Valid
    }

    public static final double CBM_TOLERANCE = 0.005;

    /**
     * Attempts to automatically correct a single misread digit or dimension by cross-referencing
     * reported CBM against (netL * netH * netW) / 1,000,000.
     * Returns a double array of [correctedGrossL, correctedGrossH, correctedGrossW] or null if no single correction matches.
     */
    public static double[] autoHealMisreadDimensions(double grossL, double grossH, double grossW, double allowance, double reportedCbm) {
        if (reportedCbm <= 0) return null;
        double netL = Math.max(0, grossL - allowance);
        double netH = Math.max(0, grossH - allowance);
        double netW = Math.max(0, grossW - allowance);
        if (Math.abs(calculateCbm(netL, netH, netW) - reportedCbm) <= CBM_TOLERANCE) {
            return new double[]{grossL, grossH, grossW}; // Already matches!
        }

        double targetVolumeCm3 = reportedCbm * 1_000_000.0;

        // Candidate 1: Width was misread (L & H are correct)
        if (netL > 0 && netH > 0) {
            double expectedNetW = Math.round(targetVolumeCm3 / (netL * netH));
            double candidateGrossW = expectedNetW + allowance;
            if (candidateGrossW >= 30 && candidateGrossW <= 600) {
                if (Math.abs(calculateCbm(netL, netH, expectedNetW) - reportedCbm) <= CBM_TOLERANCE) {
                    return new double[]{grossL, grossH, candidateGrossW};
                }
            }
        }

        // Candidate 2: Height was misread (L & W are correct)
        if (netL > 0 && netW > 0) {
            double expectedNetH = Math.round(targetVolumeCm3 / (netL * netW));
            double candidateGrossH = expectedNetH + allowance;
            if (candidateGrossH >= 30 && candidateGrossH <= 600) {
                if (Math.abs(calculateCbm(netL, expectedNetH, netW) - reportedCbm) <= CBM_TOLERANCE) {
                    return new double[]{grossL, candidateGrossH, grossW};
                }
            }
        }

        // Candidate 3: Length was misread (H & W are correct)
        if (netH > 0 && netW > 0) {
            double expectedNetL = Math.round(targetVolumeCm3 / (netH * netW));
            double candidateGrossL = expectedNetL + allowance;
            if (candidateGrossL >= 30 && candidateGrossL <= 800) {
                if (Math.abs(calculateCbm(expectedNetL, netH, netW) - reportedCbm) <= CBM_TOLERANCE) {
                    return new double[]{candidateGrossL, grossH, grossW};
                }
            }
        }

        return null;
    }

    /**
     * Validates whether a reported CBM matches the calculated CBM derived from net dimensions.
     * @return null if consistent within tolerance (<= 0.005 M³), or an error description.
     */
    public static String validateReportedCbmConsistency(double netL, double netH, double netW, double reportedCbm) {
        double calcCbm = calculateCbm(netL, netH, netW);
        double diff = Math.abs(reportedCbm - calcCbm);
        if (diff > CBM_TOLERANCE) {
            return String.format("Reported CBM (%.3f M³) does not match calculated CBM (%.3f M³ derived from %dx%dx%d cm net). Discrepancy: %.3f M³.",
                    reportedCbm, calcCbm, (int)netL, (int)netH, (int)netW, diff);
        }
        return null; // Consistent
    }
}
