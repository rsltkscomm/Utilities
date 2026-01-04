package ai.ml.integration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple linear regression model
 */
public class SimpleLinearModel implements MLModel, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final double[] weights;
    private final double bias;
    private final double accuracy;
    private final ModelMetadata metadata;
    
    public SimpleLinearModel(double[] weights, double bias, double accuracy) {
        this.weights = weights;
        this.bias = bias;
        this.accuracy = accuracy;
        
        Map<String, Object> params = new HashMap<>();
        params.put("weights", weights);
        params.put("bias", bias);
        params.put("featureCount", weights.length);
        
        this.metadata = new ModelMetadata(
            "LinearRegression",
            "1.0",
            params,
            System.currentTimeMillis(),
            accuracy
        );
    }
    
    @Override
    public double predict(double[] features) {
        double sum = bias;
        for (int i = 0; i < weights.length && i < features.length; i++) {
            sum += weights[i] * features[i];
        }
        // Apply sigmoid for binary classification
        return sigmoid(sum);
    }
    
    @Override
    public PredictionResult predictWithMetadata(double[] features) {
        double prediction = predict(features);
        double confidence = calculateConfidence(prediction);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelType", "LinearRegression");
        metadata.put("featureCount", features.length);
        
        return new PredictionResult(prediction, confidence, metadata);
    }
    
    @Override
    public ModelMetadata getMetadata() {
        return metadata;
    }
    
    public double[] getWeights() {
        return weights.clone();
    }
    
    public double getBias() {
        return bias;
    }
    
    public double getAccuracy() {
        return accuracy;
    }
    
    /**
     * Sigmoid function for binary classification
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    /**
     * Calculate confidence based on prediction distance from 0.5
     */
    private double calculateConfidence(double prediction) {
        return 1.0 - 2.0 * Math.abs(prediction - 0.5);
    }
}

