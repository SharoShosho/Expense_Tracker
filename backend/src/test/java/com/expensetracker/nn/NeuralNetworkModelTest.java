package com.expensetracker.nn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeuralNetworkModelTest {

    @Test
    void predictReturnsExpectedShape() {
        NeuralNetworkModel model = new NeuralNetworkModel();

        double[] singleInput = new double[NeuralNetworkModel.INPUT_SIZE];
        double[] singleOutput = model.predict(singleInput);

        assertEquals(NeuralNetworkModel.OUTPUT_SIZE, singleOutput.length);

        double[][] batchInput = new double[5][NeuralNetworkModel.INPUT_SIZE];
        double[][] batchOutput = model.predict(batchInput);

        assertEquals(5, batchOutput.length);
        for (double[] row : batchOutput) {
            assertEquals(NeuralNetworkModel.OUTPUT_SIZE, row.length);
        }
    }

    @Test
    void fitImprovesLossOnSimplePattern() {
        NeuralNetworkModel model = new NeuralNetworkModel(0.01);

        int samples = 128;
        double[][] features = new double[samples][NeuralNetworkModel.INPUT_SIZE];
        double[][] labels = new double[samples][NeuralNetworkModel.OUTPUT_SIZE];

        // Synthetic learnable rule: if feature[0] >= 0.5 then label[0]=1 else 0.
        for (int i = 0; i < samples; i++) {
            double value = i < samples / 2 ? 0.0 : 1.0;
            features[i][0] = value;
            labels[i][0] = value;
        }

        double beforeLoss = model.binaryCrossEntropy(features, labels);
        model.fit(features, labels, 120, 0.01);
        double afterLoss = model.binaryCrossEntropy(features, labels);

        assertTrue(afterLoss < beforeLoss, "Expected training to reduce loss");
    }

    @Test
    void saveAndLoadPreservesPrediction(@TempDir Path tempDir) throws Exception {
        NeuralNetworkModel model = new NeuralNetworkModel();
        double[][] features = new double[32][NeuralNetworkModel.INPUT_SIZE];
        double[][] labels = new double[32][NeuralNetworkModel.OUTPUT_SIZE];

        for (int i = 0; i < features.length; i++) {
            double value = i % 2 == 0 ? 0.0 : 1.0;
            features[i][0] = value;
            labels[i][0] = value;
        }

        model.fit(features, labels, 50, 0.01);

        double[] probe = new double[NeuralNetworkModel.INPUT_SIZE];
        probe[0] = 1.0;
        double[] expected = model.predict(probe);

        File modelFile = tempDir.resolve("nn-model.zip").toFile();
        model.save(modelFile);

        NeuralNetworkModel loaded = NeuralNetworkModel.load(modelFile);
        double[] actual = loaded.predict(probe);

        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], 1e-9);
        }
    }
}


