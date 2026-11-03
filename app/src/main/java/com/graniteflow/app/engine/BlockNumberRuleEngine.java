package com.graniteflow.app.engine;

import com.graniteflow.app.data.model.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based engine to enforce quarry block number standards:
 * 1. Numeric Only: Block numbers must contain only numbers (or standard quarry prefix like "BG-501").
 *    Garbled OCR letter words like ISOR60, IES, LOO are strictly converted to numeric digits.
 * 2. Ordered Fashion: In quarry slips, block numbers follow sequential/ordered progression.
 *    Any OCR/vision misread anomalies or letter confusions are automatically resolved to the sequential series.
 */
public class BlockNumberRuleEngine {

    /**
     * Extracts standard prefix like "BG-" or "B-" if present with delimiter.
     */
    public static String extractPrefix(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        int idx = s.indexOf('-');
        if (idx < 0) idx = s.indexOf('_');
        if (idx > 0 && idx <= 5) {
            String p = s.substring(0, idx + 1);
            String rest = s.substring(idx + 1).trim();
            if (p.matches("^[A-Za-z0-9]{1,5}[-_]$") && rest.matches("^\\d+$")) {
                return p.toUpperCase();
            }
        }
        return "";
    }

    /**
     * Clean a raw candidate string into numeric digits using OCR letter-to-digit substitution.
     */
    public static String cleanToDigits(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Strip known structured prefix if present (e.g. "BG-501" -> "501")
        String prefix = extractPrefix(s);
        if (!prefix.isEmpty()) {
            s = s.substring(prefix.length()).trim();
        }

        // If it's already a clean positive number, return it
        if (s.matches("^\\d+$")) {
            try {
                int val = Integer.parseInt(s);
                if (val > 0) return String.valueOf(val);
            } catch (Exception ignored) {}
        }

        // Apply OCR letter-to-digit substitutions
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                char upper = Character.toUpperCase(c);
                switch (upper) {
                    case 'I':
                    case 'L':
                    case '|':
                    case '!':
                    case '/':
                        sb.append('1');
                        break;
                    case 'O':
                    case 'D':
                    case 'Q':
                        sb.append('0');
                        break;
                    case 'S':
                    case '$':
                        sb.append('5');
                        break;
                    case 'Z':
                        sb.append('2');
                        break;
                    case 'E':
                        sb.append('3');
                        break;
                    case 'A':
                        sb.append('4');
                        break;
                    case 'G':
                        sb.append('6');
                        break;
                    case 'T':
                        sb.append('7');
                        break;
                    case 'B':
                        sb.append('8');
                        break;
                    default:
                        // Ignore separators / unmapped characters
                        break;
                }
            }
        }

        String digits = sb.toString();
        if (!digits.isEmpty()) {
            try {
                long parsed = Long.parseLong(digits);
                if (parsed > 0 && parsed <= 999999) {
                    return String.valueOf(parsed);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Enforce strict numeric and ordered rules across a list of blocks.
     * Reconstructs the true sequential progression (e.g. 101, 102, 103, 104, 105, 106, 107...)
     * by voting on the implied arithmetic progression start number.
     */
    public static void enforceSequentialBlockNumbers(List<Block> blocks, int defaultStartingNo) {
        if (blocks == null || blocks.isEmpty()) return;

        int n = blocks.size();
        Integer[] cleanedValues = new Integer[n];
        boolean[] wasOriginallyNumeric = new boolean[n];
        Map<String, Integer> prefixVotes = new HashMap<>();

        // Step 1: Clean every block number and identify high-confidence numeric anchors
        Map<Integer, Integer> startVotes = new HashMap<>();

        for (int i = 0; i < n; i++) {
            Block b = blocks.get(i);
            String raw = b.getBlockNumber();
            String prefix = extractPrefix(raw);
            if (!prefix.isEmpty()) {
                prefixVotes.put(prefix, prefixVotes.getOrDefault(prefix, 0) + 1);
            }

            String digits = cleanToDigits(raw);
            if (digits != null) {
                try {
                    int val = Integer.parseInt(digits);
                    if (val > 0) {
                        cleanedValues[i] = val;
                        boolean pureNumeric = raw != null && (raw.trim().matches("^\\d+$") || !prefix.isEmpty());
                        wasOriginallyNumeric[i] = pureNumeric;
                        int impliedStart = val - i;
                        if (impliedStart > 0) {
                            int weight = pureNumeric ? 3 : 1;
                            startVotes.put(impliedStart, startVotes.getOrDefault(impliedStart, 0) + weight);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Check if there is an established common prefix across blocks (e.g. "BG-")
        String bestPrefix = "";
        int maxPrefixVotes = 0;
        for (Map.Entry<String, Integer> entry : prefixVotes.entrySet()) {
            if (entry.getValue() > maxPrefixVotes) {
                maxPrefixVotes = entry.getValue();
                bestPrefix = entry.getKey();
            }
        }

        // Step 2: Find best starting base number by highest consensus
        int bestStart = defaultStartingNo > 0 ? defaultStartingNo : 101;
        int maxVotes = -1;

        for (Map.Entry<Integer, Integer> entry : startVotes.entrySet()) {
            int start = entry.getKey();
            int votes = entry.getValue();
            if (votes > maxVotes) {
                maxVotes = votes;
                bestStart = start;
            }
        }

        // Fallback: if no consensus votes, check if first block has a number
        if (maxVotes <= 0) {
            if (cleanedValues[0] != null && cleanedValues[0] > 0) {
                bestStart = cleanedValues[0];
            } else {
                for (int i = 0; i < n; i++) {
                    if (cleanedValues[i] != null && cleanedValues[i] > i) {
                        bestStart = cleanedValues[i] - i;
                        break;
                    }
                }
            }
        }

        if (bestStart <= 0) {
            bestStart = defaultStartingNo > 0 ? defaultStartingNo : 101;
        }

        // Step 3: Reconstruct sequence in strictly ordered fashion
        int currentSeq = bestStart;
        for (int i = 0; i < n; i++) {
            Block b = blocks.get(i);
            int expected = bestStart + i;

            Integer cVal = cleanedValues[i];
            int assignedNo;

            if (cVal != null && wasOriginallyNumeric[i] && Math.abs(cVal - expected) <= 1 && cVal >= currentSeq) {
                assignedNo = cVal;
            } else {
                assignedNo = expected;
            }

            b.setBlockNumber(bestPrefix + assignedNo);
            currentSeq = assignedNo + 1;
        }
    }
}
