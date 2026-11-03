package com.graniteflow.app;

import com.graniteflow.app.data.dataset.EvaluationResult;
import com.graniteflow.app.data.dataset.GroundTruth;
import com.graniteflow.app.data.dataset.OcrPrediction;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.engine.DatasetEvaluationEngine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatasetEvaluationEngineTest {

    @Test
    public void testExactBlockMatchGivesPerfectScores() {
        OcrPrediction prediction = new OcrPrediction();
        prediction.setDatasetRecordId("REC-001");
        prediction.setParserVersion("v1.0.0");
        prediction.setOcrEngine("Google ML Kit");

        List<OcrPrediction.PredictedBlock> pBlocks = new ArrayList<>();
        OcrPrediction.PredictedBlock pb = new OcrPrediction.PredictedBlock();
        pb.setBlockNumber("101");
        pb.setGrossLengthCm(280.0);
        pb.setGrossHeightCm(140.0);
        pb.setGrossWidthCm(120.0);
        pb.setAllowanceCm(10.0);
        pb.setNetCbm(3.861);
        pBlocks.add(pb);
        prediction.setBlocks(pBlocks);

        GroundTruth gt = new GroundTruth("GT-001", "REC-001", 1, "REV-1");
        List<Block> gBlocks = new ArrayList<>();
        Block gb = new Block();
        gb.setBlockNumber("101");
        gb.setGrossLengthCm(280.0);
        gb.setGrossHeightCm(140.0);
        gb.setGrossWidthCm(120.0);
        gb.setAllowanceCm(10.0);
        gb.setNetCbm(3.861);
        gBlocks.add(gb);
        gt.setBlocks(gBlocks);

        EvaluationResult result = DatasetEvaluationEngine.evaluate(prediction, gt);

        assertNotNull(result);
        assertEquals(1, result.getRowsExpected());
        assertEquals(1, result.getRowsPredicted());
        assertEquals(1, result.getAlignedRows());
        assertEquals(1, result.getDimensionExactMatches());
        assertEquals(1.0, result.getPrecision(), 0.001);
        assertEquals(1.0, result.getRecall(), 0.001);
        assertEquals(1.0, result.getF1(), 0.001);
        assertEquals(1.0, result.getDimensionAccuracy(), 0.001);
        assertEquals(1.0, result.getCbmAccuracy(), 0.001);
    }

    @Test
    public void testPreventPredictionRowReuse() {
        // Two expected blocks with same dimensions
        GroundTruth gt = new GroundTruth("GT-002", "REC-002", 1, "REV-1");
        List<Block> gBlocks = new ArrayList<>();

        Block gb1 = new Block();
        gb1.setBlockNumber("201");
        gb1.setGrossLengthCm(250.0);
        gb1.setGrossHeightCm(150.0);
        gb1.setGrossWidthCm(100.0);
        gb1.setNetCbm(3.024);
        gBlocks.add(gb1);

        Block gb2 = new Block();
        gb2.setBlockNumber("202");
        gb2.setGrossLengthCm(250.0);
        gb2.setGrossHeightCm(150.0);
        gb2.setGrossWidthCm(100.0);
        gb2.setNetCbm(3.024);
        gBlocks.add(gb2);

        gt.setBlocks(gBlocks);

        // Only ONE prediction row available
        OcrPrediction prediction = new OcrPrediction();
        List<OcrPrediction.PredictedBlock> pBlocks = new ArrayList<>();
        OcrPrediction.PredictedBlock pb = new OcrPrediction.PredictedBlock();
        pb.setBlockNumber("201");
        pb.setGrossLengthCm(250.0);
        pb.setGrossHeightCm(150.0);
        pb.setGrossWidthCm(100.0);
        pb.setNetCbm(3.024);
        pBlocks.add(pb);
        prediction.setBlocks(pBlocks);

        EvaluationResult result = DatasetEvaluationEngine.evaluate(prediction, gt);

        // Should align exactly 1 row, leaving 1 expected row missing
        assertEquals(2, result.getRowsExpected());
        assertEquals(1, result.getRowsPredicted());
        assertEquals(1, result.getAlignedRows());
        assertEquals(0.5, result.getRecall(), 0.001);
        assertEquals(1.0, result.getPrecision(), 0.001);
    }

    @Test
    public void testDimensionToleranceIsStrictExactMatch() {
        OcrPrediction prediction = new OcrPrediction();
        List<OcrPrediction.PredictedBlock> pBlocks = new ArrayList<>();
        OcrPrediction.PredictedBlock pb = new OcrPrediction.PredictedBlock();
        pb.setBlockNumber("301");
        pb.setGrossLengthCm(280.5); // 0.5 cm off
        pb.setGrossHeightCm(140.0);
        pb.setGrossWidthCm(120.0);
        pb.setNetCbm(3.861);
        pBlocks.add(pb);
        prediction.setBlocks(pBlocks);

        GroundTruth gt = new GroundTruth("GT-003", "REC-003", 1, "REV-1");
        List<Block> gBlocks = new ArrayList<>();
        Block gb = new Block();
        gb.setBlockNumber("301");
        gb.setGrossLengthCm(280.0); // Exact 280.0 cm
        gb.setGrossHeightCm(140.0);
        gb.setGrossWidthCm(120.0);
        gb.setNetCbm(3.861);
        gBlocks.add(gb);
        gt.setBlocks(gBlocks);

        EvaluationResult result = DatasetEvaluationEngine.evaluate(prediction, gt);

        // Dimensions should NOT match because tolerance is 0.0 cm
        assertEquals(0, result.getDimensionExactMatches());
        assertEquals(0.0, result.getDimensionAccuracy(), 0.001);
        // But block number matched
        assertEquals(1, result.getBlockNumberExactMatches());
    }

    @Test
    public void testZeroRowsSafeDenominators() {
        OcrPrediction emptyPred = new OcrPrediction();
        emptyPred.setBlocks(Collections.emptyList());

        GroundTruth emptyGt = new GroundTruth("GT-EMPTY", "REC-EMPTY", 1, "REV-1");
        emptyGt.setBlocks(Collections.emptyList());

        EvaluationResult result = DatasetEvaluationEngine.evaluate(emptyPred, emptyGt);

        assertNotNull(result);
        assertEquals(0, result.getRowsExpected());
        assertEquals(0, result.getRowsPredicted());
        assertEquals(-1.0, result.getPrecision(), 0.001);
        assertEquals(-1.0, result.getRecall(), 0.001);
        assertEquals(-1.0, result.getF1(), 0.001);
        assertFalse(Double.isNaN(result.getPrecision()));
        assertFalse(Double.isNaN(result.getRecall()));
    }
}
