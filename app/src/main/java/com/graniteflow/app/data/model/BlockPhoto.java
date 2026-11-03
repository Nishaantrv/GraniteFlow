package com.graniteflow.app.data.model;

import java.io.Serializable;

public class BlockPhoto implements Serializable {
    private String id;
    private String blockNumber;
    private String sideLabel; // e.g. "A SIDE", "B SIDE", "FRONT", "BACK", "TOP"
    private String caption;
    private String localUri;
    private long timestamp;

    public BlockPhoto() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.sideLabel = "A SIDE";
        this.timestamp = System.currentTimeMillis();
    }

    public BlockPhoto(String blockNumber, String sideLabel, String localUri, String caption) {
        this.id = String.valueOf(System.currentTimeMillis()) + "_" + Math.round(Math.random() * 1000);
        this.blockNumber = blockNumber;
        this.sideLabel = sideLabel != null && !sideLabel.isEmpty() ? sideLabel : "A SIDE";
        this.localUri = localUri;
        this.caption = caption;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDisplayTitle() {
        return "BLOCK NO : " + (blockNumber != null ? blockNumber : "") + "/" + (sideLabel != null ? sideLabel : "A SIDE");
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBlockNumber() { return blockNumber; }
    public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }

    public String getSideLabel() { return sideLabel; }
    public void setSideLabel(String sideLabel) { this.sideLabel = sideLabel; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getLocalUri() { return localUri; }
    public void setLocalUri(String localUri) { this.localUri = localUri; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
