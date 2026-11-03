package com.graniteflow.app.ocr;

import com.graniteflow.app.data.model.Block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Layout & Measurement Slip Parser for GraniteFlow.
 * Handles both row-based and columnar table formats from physical quarry measurement slips.
 */
public class GraniteSlipParser {

    public static class ParsedInspectionResult implements Serializable {
        public String engineUsed = "Gemini Vision AI (gemini-3.8-flash)";
        public String materialName = "";
        public String locationCode = "";
        public String date = "";
        public String inspectorName = "";
        public double defaultAllowanceCm = 10.0;
        public List<Block> blocks = new ArrayList<>();
        public List<String> rawRowTexts = new ArrayList<>();
        public String rawOcrText = "";
        public List<String> warnings = new ArrayList<>();

        public ParsedInspectionResult() {}

        public double calculateTotalNetCbm() {
            double total = 0.0;
            for (Block b : blocks) {
                total += b.getNetCbm();
            }
            return Math.round(total * 1000.0) / 1000.0;
        }
    }

    private static final List<String> KNOWN_MATERIALS = Arrays.asList(
            "BLACK GALAXY", "KATTIMA BLACK", "STEEL GREY", "TAN BROWN",
            "VISCOUNT WHITE", "ABSOLUTE BLACK", "COLOMBO JUAPARANA",
            "KASHMIR WHITE", "IMPERIAL RED", "INDIAN AURORA",
            "RIVER WHITE", "COLONIAL WHITE", "PARADISO", "BLACK PEARL"
    );

    /**
     * Parses raw OCR string into structured inspection entity.
     */
    public static ParsedInspectionResult parse(String rawText) {
        ParsedInspectionResult result = new ParsedInspectionResult();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.warnings.add("Empty OCR text received.");
            return result;
        }

        result.rawOcrText = rawText;
        String[] lines = rawText.split("\\r?\\n");

        // 1. Parse Header Information
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String upper = trimmed.toUpperCase();

            // Check Material Name
            for (String mat : KNOWN_MATERIALS) {
                if (upper.contains(mat)) {
                    result.materialName = mat;
                    break;
                }
            }
            if (upper.startsWith("MATERIAL:") || upper.startsWith("STONE:")) {
                String val = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                if (!val.isEmpty()) result.materialName = val.toUpperCase();
            }

            // Check Location / Quarry
            if (upper.startsWith("LOCATION:") || upper.startsWith("QUARRY:") || upper.startsWith("SITE:")) {
                String val = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                if (!val.isEmpty()) result.locationCode = val.toUpperCase();
            } else if (upper.contains("ONG-") || upper.contains("XOW/") || upper.contains("CHIKKA")) {
                Matcher m = Pattern.compile("([A-Z0-9/\\-_]{4,15})").matcher(upper);
                if (m.find()) {
                    result.locationCode = m.group(1);
                }
            }

            // Check Date
            Matcher dateMatcher = Pattern.compile("(\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b)").matcher(trimmed);
            if (dateMatcher.find()) {
                result.date = dateMatcher.group(1);
            }

            // Check Allowance
            if (upper.contains("ALLOWANCE") || upper.contains("ALLOW")) {
                Matcher allowMatcher = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(CM)?").matcher(upper);
                if (allowMatcher.find()) {
                    try {
                        result.defaultAllowanceCm = Double.parseDouble(allowMatcher.group(1));
                    } catch (Exception ignored) {}
                }
            }

            // Check Inspector
            if (upper.startsWith("INSPECTOR:") || upper.startsWith("SURVEYOR:") || upper.startsWith("CHECKED BY:")) {
                String val = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                if (!val.isEmpty()) result.inspectorName = val;
            }
        }

        // 2. Strategy A: Line-by-Line Row-based parsing
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String upper = trimmed.toUpperCase();
            if (upper.contains("MEASUREMENT SLIP") || (upper.contains("LENGTH") && upper.contains("HEIGHT"))
                    || upper.startsWith("GROSS") || upper.startsWith("NET") || upper.startsWith("VIEW")) {
                continue;
            }

            String cleanedLine = line.replaceAll("[xX×*]", " X ")
                    .replaceAll("(?i)\\b(\\d+)o\\b", "$10")
                    .replaceAll("(?i)\\b[oO](\\d+)\\b", "0$1")
                    .replaceAll("(?i)\\b[lI](\\d{2,})\\b", "1$1");

            // Direct measurement pattern match (e.g. 180 X 125 X 160 or 235 X 90 X 165)
            Matcher dimMatcher = Pattern.compile("(?i)\\b([0-9IlOo]{2,4})\\s*[xX×*]\\s*([0-9IlOo]{2,4})\\s*[xX×*]\\s*([0-9IlOo]{2,4})\\b").matcher(cleanedLine);
            if (dimMatcher.find()) {
                double l = parseCleanDouble(dimMatcher.group(1));
                double h = parseCleanDouble(dimMatcher.group(2));
                double w = parseCleanDouble(dimMatcher.group(3));

                if (l >= 30 && h >= 30 && w >= 30 && l <= 800 && h <= 600 && w <= 600) {
                    int nextNo = result.blocks.size() + 101;
                    Block b = new Block(String.valueOf(nextNo), l, h, w, result.defaultAllowanceCm);
                    b.recalculate();
                    result.blocks.add(b);
                    result.rawRowTexts.add(trimmed);
                    continue;
                }
            }

            String normalized = cleanedLine.replaceAll("[|,:xX×*]+", " ");
            String[] tokens = normalized.trim().split("\\s+");

            List<Double> numbers = new ArrayList<>();
            String candidateBlockNo = null;

            for (String tok : tokens) {
                double num = parseCleanDouble(tok);
                if (num > 0) {
                    numbers.add(num);
                } else if (tok.matches("^[A-Za-z0-9\\-_]{2,10}$") && !tok.equalsIgnoreCase("CM") && !tok.equalsIgnoreCase("CBM")
                        && !tok.equalsIgnoreCase("GROSS") && !tok.equalsIgnoreCase("NET") && !tok.equalsIgnoreCase("TOTAL")
                        && !tok.equalsIgnoreCase("VIEW") && !tok.equalsIgnoreCase("VIEWA") && !tok.equalsIgnoreCase("VIEWB")
                        && !tok.equalsIgnoreCase("REMARKS") && !tok.equalsIgnoreCase("MARKING") && !tok.equalsIgnoreCase("HISTORY")) {
                    if (candidateBlockNo == null) {
                        candidateBlockNo = tok.toUpperCase();
                    }
                }
            }

            if (numbers.size() >= 3) {
                int dimStartIndex = 0;
                if (candidateBlockNo == null && numbers.size() >= 4) {
                    if (numbers.get(0) < 50 && numbers.get(1) >= 50) {
                        candidateBlockNo = String.valueOf(numbers.get(1).intValue());
                        dimStartIndex = 2;
                    } else if (numbers.get(0) >= 100 && numbers.get(0) < 10000) {
                        candidateBlockNo = String.valueOf(numbers.get(0).intValue());
                        dimStartIndex = 1;
                    }
                }

                if (candidateBlockNo != null && numbers.size() >= 4 && numbers.get(0) < 40.0) {
                    dimStartIndex = 1;
                }

                if (dimStartIndex + 2 < numbers.size()) {
                    double l = numbers.get(dimStartIndex);
                    double h = numbers.get(dimStartIndex + 1);
                    double w = numbers.get(dimStartIndex + 2);

                    if (l >= 40.0 && h >= 40.0 && w >= 40.0 && l <= 700.0 && h <= 500.0 && w <= 500.0) {
                        double allow = result.defaultAllowanceCm;
                        double reportedCbm = -1.0;
                        if (numbers.size() > dimStartIndex + 3) {
                            double fourth = numbers.get(dimStartIndex + 3);
                            if (fourth <= 30.0 && fourth >= 0.0) {
                                allow = fourth;
                            }
                        }
                        if (numbers.size() > dimStartIndex + 4) {
                            reportedCbm = numbers.get(dimStartIndex + 4);
                        }

                        if (candidateBlockNo == null) {
                            candidateBlockNo = String.valueOf(result.blocks.size() + 101);
                        }

                        boolean verified = false;
                        if (reportedCbm > 0) {
                            double[] healed = com.graniteflow.app.engine.DimensionCalculator.autoHealMisreadDimensions(l, h, w, allow, reportedCbm);
                            if (healed != null) {
                                l = healed[0];
                                h = healed[1];
                                w = healed[2];
                                verified = true;
                            }
                        }

                        Block b = new Block(candidateBlockNo, l, h, w, allow);
                        b.setMathVerified(verified);
                        b.recalculate();
                        result.blocks.add(b);
                        result.rawRowTexts.add(trimmed);
                    }
                }
            }
        }

        // 3. Strategy B: Columnar Table Layout Reconstruction (if row-based parsing didn't find blocks)
        if (result.blocks.isEmpty()) {
            List<String> blockIds = new ArrayList<>();
            List<List<Double>> numGroups = new ArrayList<>();
            List<Double> currentGroup = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Match block id pattern (e.g. BG-501 or B-102)
                if (trimmed.matches("^[A-Za-z]{1,4}-\\d{2,5}$")) {
                    blockIds.add(trimmed.toUpperCase());
                }

                // Check numbers
                if (trimmed.matches("^\\d{1,4}(\\.\\d+)?$")) {
                    try {
                        currentGroup.add(Double.parseDouble(trimmed));
                    } catch (Exception ignored) {}
                } else {
                    if (currentGroup.size() >= 3) {
                        numGroups.add(new ArrayList<>(currentGroup));
                    }
                    currentGroup.clear();
                }
            }
            if (currentGroup.size() >= 3) {
                numGroups.add(new ArrayList<>(currentGroup));
            }

            // If we found dimensional groups matching the count of block IDs
            if (!blockIds.isEmpty() && numGroups.size() >= 3) {
                int count = blockIds.size();
                List<Double> lengths = null;
                List<Double> heights = null;
                List<Double> widths = null;

                for (List<Double> grp : numGroups) {
                    if (grp.size() == count) {
                        if (lengths == null) lengths = grp;
                        else if (heights == null) heights = grp;
                        else if (widths == null) widths = grp;
                    }
                }

                if (lengths != null && heights != null && widths != null) {
                    for (int i = 0; i < count; i++) {
                        String bNum = blockIds.get(i);
                        double l = lengths.get(i);
                        double h = heights.get(i);
                        double w = widths.get(i);
                        Block b = new Block(bNum, l, h, w, result.defaultAllowanceCm);
                        b.recalculate();
                        result.blocks.add(b);
                        result.rawRowTexts.add(bNum + " " + (int)l + " " + (int)h + " " + (int)w);
                    }
                }
            }
        }

        // 4. Strategy C: Single-Block Measurement Slip Layout (e.g. real_photographed_slip.png or SRE report pages)
        if (result.blocks.isEmpty()) {
            String singleBlockNo = null;
            double grossL = 0, grossH = 0, grossW = 0;
            double reportedNetCbm = -1;
            double diagL = 0, diagH = 0, diagW = 0;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String upper = trimmed.toUpperCase();

                // Block No pattern (e.g. BLOCK NO : 301 or BLOCK NO : 187 or BLOCK 301)
                Matcher blockMatcher = Pattern.compile("(?:BLOCK(?:\\s*NO\\.?)?|BLOCK\\s*#?)\\s*[:\\-]?\\s*([A-Za-z0-9\\-_/]+)").matcher(upper);
                if (blockMatcher.find()) {
                    String candidate = blockMatcher.group(1).trim();
                    if (!candidate.equals("MEASUREMENT") && !candidate.equals("GROSS") && !candidate.equals("NET") && !candidate.equals("CM")) {
                        singleBlockNo = candidate;
                    }
                }

                // Check horizontal GROSS row: e.g. "GROSS 280 185 185 9.583" or "GROSS 280 160 180"
                if (upper.startsWith("GROSS") || upper.contains("GROSS ")) {
                    Matcher mGross = Pattern.compile("(\\d{2,4}(?:\\.\\d+)?)\\s+(\\d{2,4}(?:\\.\\d+)?)\\s+(\\d{2,4}(?:\\.\\d+)?)").matcher(upper);
                    if (mGross.find()) {
                        try {
                            grossL = Double.parseDouble(mGross.group(1));
                            grossH = Double.parseDouble(mGross.group(2));
                            grossW = Double.parseDouble(mGross.group(3));
                        } catch (Exception ignored) {}
                    }
                }

                // Check diagram labels: e.g. "L 280", "L280", "H 185", "W 185"
                Matcher mL = Pattern.compile("\\bL\\s*[:=]?\\s*(\\d{2,4})\\b").matcher(upper);
                if (mL.find()) {
                    try { diagL = Double.parseDouble(mL.group(1)); } catch (Exception ignored) {}
                }
                Matcher mH = Pattern.compile("\\bH\\s*[:=]?\\s*(\\d{2,4})\\b").matcher(upper);
                if (mH.find()) {
                    try { diagH = Double.parseDouble(mH.group(1)); } catch (Exception ignored) {}
                }
                Matcher mW = Pattern.compile("\\bW\\s*[:=]?\\s*(\\d{2,4})\\b").matcher(upper);
                if (mW.find()) {
                    try { diagW = Double.parseDouble(mW.group(1)); } catch (Exception ignored) {}
                }

                // Length line (e.g. LENGTH 280 270 or LENGTH : 280) - only if line is not just a column header
                if ((upper.startsWith("LENGTH") || upper.startsWith("LEN")) && !upper.contains("HEIGHT") && !upper.contains("WIDTH")) {
                    Matcher m = Pattern.compile("(\\d{2,4}(?:\\.\\d+)?)").matcher(upper);
                    if (m.find()) {
                        try { grossL = Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                }

                // Height line (e.g. HEIGHT 185 175 or HEIGHT : 185)
                if ((upper.startsWith("HEIGHT") || upper.startsWith("HT")) && !upper.contains("LENGTH") && !upper.contains("WIDTH")) {
                    Matcher m = Pattern.compile("(\\d{2,4}(?:\\.\\d+)?)").matcher(upper);
                    if (m.find()) {
                        try { grossH = Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                }

                // Width line (e.g. WIDTH 185 175 or WIDTH : 185)
                if ((upper.startsWith("WIDTH") || upper.startsWith("WD") || upper.startsWith("BREADTH")) && !upper.contains("LENGTH") && !upper.contains("HEIGHT")) {
                    Matcher m = Pattern.compile("(\\d{2,4}(?:\\.\\d+)?)").matcher(upper);
                    if (m.find()) {
                        try { grossW = Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
                    }
                }

                // Allowance line e.g. "ALLOWANCE 10 CM" or "10 CM"
                if (upper.contains("ALLOW") || upper.matches("^\\d{1,2}\\s*CM$")) {
                    Matcher mAllow = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*CM").matcher(upper);
                    if (mAllow.find()) {
                        try { result.defaultAllowanceCm = Double.parseDouble(mAllow.group(1)); } catch (Exception ignored) {}
                    }
                }

                // Reported CBM (e.g. NET CBM 8.269 or CBM 8.269)
                if (upper.contains("CBM")) {
                    Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(upper);
                    if (m.find()) {
                        try {
                            reportedNetCbm = Double.parseDouble(m.group(1));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // If GROSS row wasn't found, try diagram labels
            if ((grossL <= 0 || grossH <= 0 || grossW <= 0) && (diagL > 0 && diagH > 0 && diagW > 0)) {
                grossL = diagL;
                grossH = diagH;
                grossW = diagW;
            }

            if (grossL > 0 && grossH > 0 && grossW > 0) {
                if (singleBlockNo == null) singleBlockNo = "301";
                boolean verified = false;
                if (reportedNetCbm > 0) {
                    double[] healed = com.graniteflow.app.engine.DimensionCalculator.autoHealMisreadDimensions(
                            grossL, grossH, grossW, result.defaultAllowanceCm, reportedNetCbm);
                    if (healed != null) {
                        grossL = healed[0];
                        grossH = healed[1];
                        grossW = healed[2];
                        verified = true;
                    }
                }

                Block b = new Block(singleBlockNo, grossL, grossH, grossW, result.defaultAllowanceCm);
                b.setMathVerified(verified);
                b.recalculate();
                result.blocks.add(b);
                result.rawRowTexts.add(singleBlockNo + " L=" + (int)grossL + " H=" + (int)grossH + " W=" + (int)grossW + " Allow=" + (int)result.defaultAllowanceCm);

                if (reportedNetCbm > 0 && !verified) {
                    String cbmError = com.graniteflow.app.engine.DimensionCalculator.validateReportedCbmConsistency(
                            b.getNetLengthCm(), b.getNetHeightCm(), b.getNetWidthCm(), reportedNetCbm);
                    if (cbmError != null) {
                        result.warnings.add(cbmError);
                    }
                }
            }
        }

        if (result.blocks.isEmpty()) {
            result.warnings.add("No valid granite block measurement rows could be parsed from OCR text.");
        }

        // Rule-based post processing: enforce numeric-only and ordered block numbers
        com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(result.blocks, 101);

        System.out.println("[GraniteFlowOCR] [GraniteSlipParser] Structured parsing completed: " +
                result.blocks.size() + " blocks parsed, Total Net CBM: " + result.calculateTotalNetCbm());

        return result;
    }

    private static double parseCleanDouble(String raw) {
        if (raw == null) return -1;
        String trimmed = raw.trim();
        if (trimmed.matches("^[A-Za-z]{1,4}[-_].*") || trimmed.matches("^[A-Za-z]{2,}.*")) {
            return -1;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            String s = trimmed
                    .replaceAll("(?i)[oO]", "0")
                    .replaceAll("(?i)[lI]", "1")
                    .replaceAll("(?i)[sS]", "5")
                    .replaceAll("[^0-9.]", "");
            try {
                return !s.isEmpty() ? Double.parseDouble(s) : -1;
            } catch (Exception ignored) {
                return -1;
            }
        }
    }
}
