package com.graniteflow.app;

import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.engine.BlockNumberRuleEngine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BlockNumberRuleEngineTest {

    @Test
    public void testCleanToDigits() {
        assertEquals("101", BlockNumberRuleEngine.cleanToDigits("101"));
        assertEquals("102", BlockNumberRuleEngine.cleanToDigits("I02"));
        assertEquals("100", BlockNumberRuleEngine.cleanToDigits("LOO"));
    }

    @Test
    public void testSequentialRuleHealsUserScreenshotCase() {
        // Exact input from user screenshot: 101, IS230, IES, 104, ISOR60, LOO, 270
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block("101", 170, 115, 150, 10));
        blocks.add(new Block("IS230", 35, 120, 254, 10));
        blocks.add(new Block("IES", 190, 80, 105, 10));
        blocks.add(new Block("104", 250, 80, 195, 10));
        blocks.add(new Block("ISOR60", 185, 125, 34, 10));
        blocks.add(new Block("LOO", 190, 155, 155, 10));
        blocks.add(new Block("270", 205, 170, 245, 10));

        BlockNumberRuleEngine.enforceSequentialBlockNumbers(blocks, 101);

        assertEquals("101", blocks.get(0).getBlockNumber());
        assertEquals("102", blocks.get(1).getBlockNumber());
        assertEquals("103", blocks.get(2).getBlockNumber());
        assertEquals("104", blocks.get(3).getBlockNumber());
        assertEquals("105", blocks.get(4).getBlockNumber());
        assertEquals("106", blocks.get(5).getBlockNumber());
        assertEquals("107", blocks.get(6).getBlockNumber());
    }
}
