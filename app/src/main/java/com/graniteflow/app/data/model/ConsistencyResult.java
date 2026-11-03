package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConsistencyResult implements Serializable {
    private boolean valid;
    private List<Discrepancy> discrepancies;

    public static class Discrepancy implements Serializable {
        private String fieldName;
        private String expectedValue;
        private String actualValue;
        private String sourceDocument;
        private String targetDocument;

        public Discrepancy(String fieldName, String expectedValue, String actualValue, String sourceDocument, String targetDocument) {
            this.fieldName = fieldName;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.sourceDocument = sourceDocument;
            this.targetDocument = targetDocument;
        }

        public String getFieldName() { return fieldName; }
        public String getExpectedValue() { return expectedValue; }
        public String getActualValue() { return actualValue; }
        public String getSourceDocument() { return sourceDocument; }
        public String getTargetDocument() { return targetDocument; }

        public String getFormattedMessage() {
            return "[" + fieldName + "] Mismatch between " + sourceDocument + " (" + expectedValue + ") and " + targetDocument + " (" + actualValue + ")";
        }
    }

    public ConsistencyResult() {
        this.valid = true;
        this.discrepancies = new ArrayList<>();
    }

    public void addDiscrepancy(String fieldName, String expectedValue, String actualValue, String source, String target) {
        this.valid = false;
        this.discrepancies.add(new Discrepancy(fieldName, expectedValue, actualValue, source, target));
    }

    public boolean isValid() { return valid; }
    public List<Discrepancy> getDiscrepancies() { return discrepancies; }
}
