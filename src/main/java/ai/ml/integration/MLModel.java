package ai.ml.integration;

import java.util.Map;

/**
 * ML Model interface
 * Represents a machine learning model
 */
public interface MLModel {
    
    /**
     * Predict using the model
     */
    double predict(double[] features);
    
    /**
     * Predict with metadata
     */
    PredictionResult predictWithMetadata(double[] features);
    
    /**
     * Get model metadata
     */
    ModelMetadata getMetadata();
    
    /**
     * Model metadata
     */
    class ModelMetadata {
        private final String modelType;
        private final String version;
        private final Map<String, Object> parameters;
        private final long trainingTimestamp;
        private final double accuracy;
        
        public ModelMetadata(String modelType, String version, 
                           Map<String, Object> parameters, 
                           long trainingTimestamp, double accuracy) {
            this.modelType = modelType;
            this.version = version;
            this.parameters = parameters;
            this.trainingTimestamp = trainingTimestamp;
            this.accuracy = accuracy;
        }
        
        public String getModelType() { return modelType; }
        public String getVersion() { return version; }
        public Map<String, Object> getParameters() { return parameters; }
        public long getTrainingTimestamp() { return trainingTimestamp; }
        public double getAccuracy() { return accuracy; }
    }
    
    /**
     * Prediction result
     */
    class PredictionResult {
        private final double prediction;
        private final double confidence;
        private final Map<String, Object> metadata;
        
        public PredictionResult(double prediction, double confidence, 
                              Map<String, Object> metadata) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.metadata = metadata;
        }
        
        public double getPrediction() { return prediction; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}

