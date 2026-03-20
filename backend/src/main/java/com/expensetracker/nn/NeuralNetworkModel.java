package com.expensetracker.nn;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * DL4J-backed multi-label model for saving-tip predictions.
 */
public class NeuralNetworkModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int INPUT_SIZE = 20;
    public static final int HIDDEN_1 = 64;
    public static final int HIDDEN_2 = 32;
    public static final int OUTPUT_SIZE = 8;
    public static final double LEARNING_RATE = 0.001;

    private final MultiLayerNetwork network;

    public NeuralNetworkModel() {
        this(LEARNING_RATE);
    }

    public NeuralNetworkModel(double learningRate) {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(42)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(learningRate))
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(INPUT_SIZE)
                        .nOut(HIDDEN_1)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(HIDDEN_1)
                        .nOut(HIDDEN_2)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                        .nIn(HIDDEN_2)
                        .nOut(OUTPUT_SIZE)
                        .activation(Activation.SIGMOID)
                        .build())
                .build();

        this.network = new MultiLayerNetwork(config);
        this.network.init();
    }

    private NeuralNetworkModel(MultiLayerNetwork network) {
        this.network = network;
    }

    public double[] predict(double[] features) {
        double[][] out = predict(new double[][]{features});
        return out.length > 0 ? out[0] : new double[OUTPUT_SIZE];
    }

    public double[][] predict(double[][] features) {
        if (features.length == 0) {
            return new double[0][OUTPUT_SIZE];
        }
        DataSet ds = new DataSet(Nd4j.create(features), Nd4j.zeros(features.length, OUTPUT_SIZE));
        var output = network.output(ds.getFeatures(), false);
        return output.toDoubleMatrix();
    }

    public void fit(double[][] features, double[][] labels, int epochs, double learningRate) {
        if (features.length == 0 || labels.length == 0) {
            return;
        }

        int rows = Math.min(features.length, labels.length);
        double[][] feat = new double[rows][INPUT_SIZE];
        double[][] lab = new double[rows][OUTPUT_SIZE];
        for (int i = 0; i < rows; i++) {
            feat[i] = features[i];
            lab[i] = labels[i];
        }

        DataSet trainingSet = new DataSet(Nd4j.create(feat), Nd4j.create(lab));
        for (int epoch = 0; epoch < epochs; epoch++) {
            DataSet shuffled = trainingSet.copy();
            shuffled.shuffle();
            network.fit(shuffled);
        }
    }

    public double binaryCrossEntropy(double[][] features, double[][] labels) {
        if (features.length == 0 || labels.length == 0) {
            return 0.0;
        }
        int rows = Math.min(features.length, labels.length);
        double[][] feat = new double[rows][INPUT_SIZE];
        double[][] lab = new double[rows][OUTPUT_SIZE];
        for (int i = 0; i < rows; i++) {
            feat[i] = features[i];
            lab[i] = labels[i];
        }
        DataSet evalSet = new DataSet(Nd4j.create(feat), Nd4j.create(lab));
        return network.score(evalSet);
    }

    public void save(File file) throws IOException {
        ModelSerializer.writeModel(network, file, true);
    }

    public static NeuralNetworkModel load(File file) throws IOException {
        MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(file);
        return new NeuralNetworkModel(restored);
    }
}
