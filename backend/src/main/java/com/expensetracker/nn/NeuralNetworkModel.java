package com.expensetracker.nn;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Builds a 3-layer neural network for predicting applicable saving-tip types:
 *
 * <pre>
 *   Input  Layer :  20 neurons  (20 expense features)
 *   Hidden Layer 1:  64 neurons  (ReLU)
 *   Hidden Layer 2:  32 neurons  (ReLU)
 *   Output Layer :   8 neurons  (Sigmoid — one per tip type)
 * </pre>
 *
 * Optimiser : Adam (lr = 0.001)
 * Loss      : Binary cross-entropy
 */
public class NeuralNetworkModel {

    public static final int INPUT_SIZE   = 20;
    public static final int HIDDEN_1     = 64;
    public static final int HIDDEN_2     = 32;
    public static final int OUTPUT_SIZE  = 8;
    public static final double LEARNING_RATE = 0.001;

    private final MultiLayerNetwork network;
    private final MultiLayerConfiguration config;

    public NeuralNetworkModel() {
        this.config = buildConfig();
        this.network = new MultiLayerNetwork(config);
        this.network.init();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private MultiLayerConfiguration buildConfig() {
        return new NeuralNetConfiguration.Builder()
                .seed(42)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(LEARNING_RATE))
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
    }

    /** Returns the initialised (but not yet trained) network. */
    public MultiLayerNetwork getNetwork() {
        return network;
    }

    /** Returns the raw configuration used to build the network. */
    public MultiLayerConfiguration getConfig() {
        return config;
    }
}
