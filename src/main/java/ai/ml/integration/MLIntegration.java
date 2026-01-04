package ai.ml.integration;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;
import reporting.TestLogManager;

import java.util.*;

/**
 * Main ML Integration class
 * Provides unified interface for all ML capabilities
 */
public class MLIntegration {
    
    private final MLModelManager modelManager;
    private final FeatureExtractor featureExtractor;
    private final MLModelTrainer trainer;
    private final FlakyTestDetector flakyDetector;
    
    public MLIntegration(FlakyTestDetector flakyDetector,
                        TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.modelManager = new MLModelManager();
        this.featureExtractor = new FeatureExtractor(flakyDetector, stabilityScorer);
        this.trainer = new MLModelTrainer();
    }
    
    /**
     * Train failure prediction model
     */
    public MLModel trainFailurePredictionModel(String modelName, 
                                               List<String> testKeys) {
        TestLogManager.info("Training failure prediction model: " + modelName);
        
        // Prepare training data
        List<MLModelTrainer.TrainingData> trainingData = new ArrayList<>();
        
        for (String testKey : testKeys) {
            double[] features = featureExtractor.extractFailurePredictionFeatures(testKey);
            
            // Label: 1 if test is flaky or has high failure rate, 0 otherwise
            FlakyTestDetector.TestExecutionHistory history = 
                flakyDetector.getHistory(testKey);
            double label = 0.0;
            if (flakyDetector.isFlaky(testKey)) {
                label = 1.0;
            } else if (history != null && history.getFailureRate() > 0.3) {
                label = 1.0;
            }
            
            trainingData.add(new MLModelTrainer.TrainingData(features, label));
        }
        
        // Train model
        return modelManager.trainAndSave(modelName, "linear", trainingData);
    }
    
    /**
     * Predict test failure using ML model
     */
    public MLModel.PredictionResult predictFailure(String modelName, String testKey) {
        double[] features = featureExtractor.extractFailurePredictionFeatures(testKey);
        return modelManager.predict(modelName, features);
    }
    
    /**
     * Train locator success prediction model
     */
    public MLModel trainLocatorModel(String modelName, 
                                    List<LocatorTrainingData> trainingData) {
        TestLogManager.info("Training locator success model: " + modelName);
        
        // Convert to ML training data
        List<MLModelTrainer.TrainingData> mlData = new ArrayList<>();
        
        for (LocatorTrainingData data : trainingData) {
            double[] features = featureExtractor.extractLocatorFeatures(
                data.getLocator(), data.getLocatorType(), data.getElementDescription());
            mlData.add(new MLModelTrainer.TrainingData(features, data.isSuccess() ? 1.0 : 0.0));
        }
        
        // Train model
        return modelManager.trainAndSave(modelName, "decisiontree", mlData);
    }
    
    /**
     * Predict locator success
     */
    public MLModel.PredictionResult predictLocatorSuccess(String modelName,
                                                          String locator,
                                                          String locatorType,
                                                          String elementDescription) {
        double[] features = featureExtractor.extractLocatorFeatures(
            locator, locatorType, elementDescription);
        return modelManager.predict(modelName, features);
    }
    
    /**
     * Cross-validate model
     */
    public MLModelTrainer.CrossValidationResult crossValidateModel(
            String modelName, List<MLModelTrainer.TrainingData> trainingData, int folds) {
        TestLogManager.info("Cross-validating model: " + modelName);
        return trainer.crossValidate(trainingData, folds);
    }
    
    /**
     * Get model manager
     */
    public MLModelManager getModelManager() {
        return modelManager;
    }
    
    /**
     * Get feature extractor
     */
    public FeatureExtractor getFeatureExtractor() {
        return featureExtractor;
    }
    
    /**
     * Locator training data
     */
    public static class LocatorTrainingData {
        private final String locator;
        private final String locatorType;
        private final String elementDescription;
        private final boolean success;
        
        public LocatorTrainingData(String locator, String locatorType,
                                 String elementDescription, boolean success) {
            this.locator = locator;
            this.locatorType = locatorType;
            this.elementDescription = elementDescription;
            this.success = success;
        }
        
        public String getLocator() { return locator; }
        public String getLocatorType() { return locatorType; }
        public String getElementDescription() { return elementDescription; }
        public boolean isSuccess() { return success; }
    }
}

