package ai.ml.integration;

import reporting.TestLogManager;

import java.util.*;

/**
 * ML Model Trainer
 * Trains machine learning models using collected data
 */
public class MLModelTrainer {
    
    /**
     * Train a simple linear regression model
     */
    public SimpleLinearModel trainLinearModel(List<TrainingData> trainingData) {
        TestLogManager.info("Training linear model with " + trainingData.size() + " samples");
        
        if (trainingData.isEmpty()) {
            throw new IllegalArgumentException("Training data cannot be empty");
        }
        
        int featureCount = trainingData.get(0).getFeatures().length;
        
        // Initialize weights
        double[] weights = new double[featureCount];
        double bias = 0.0;
        
        // Simple gradient descent training
        double learningRate = 0.01;
        int epochs = 100;
        
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0.0;
            
            for (TrainingData data : trainingData) {
                double prediction = predict(weights, bias, data.getFeatures());
                double error = data.getLabel() - prediction;
                
                // Update weights
                for (int i = 0; i < featureCount; i++) {
                    weights[i] += learningRate * error * data.getFeatures()[i];
                }
                bias += learningRate * error;
                
                totalError += Math.abs(error);
            }
            
            double avgError = totalError / trainingData.size();
            if (epoch % 20 == 0) {
                TestLogManager.info("Epoch " + epoch + ", Average Error: " + 
                    String.format("%.4f", avgError));
            }
        }
        
        // Calculate accuracy
        double accuracy = calculateAccuracy(weights, bias, trainingData);
        
        TestLogManager.info("Model training completed. Accuracy: " + 
            String.format("%.2f%%", accuracy * 100));
        
        return new SimpleLinearModel(weights, bias, accuracy);
    }
    
    /**
     * Train a decision tree model (simplified)
     */
    public DecisionTreeModel trainDecisionTree(List<TrainingData> trainingData) {
        TestLogManager.info("Training decision tree with " + trainingData.size() + " samples");
        
        // Simplified decision tree implementation
        // In production, use a proper ML library like Weka, DL4J, or scikit-learn via Python
        
        DecisionTreeModel model = new DecisionTreeModel();
        model.train(trainingData);
        
        double accuracy = model.calculateAccuracy(trainingData);
        TestLogManager.info("Decision tree training completed. Accuracy: " + 
            String.format("%.2f%%", accuracy * 100));
        
        return model;
    }
    
    /**
     * Cross-validate model
     */
    public CrossValidationResult crossValidate(List<TrainingData> trainingData, 
                                              int folds) {
        TestLogManager.info("Cross-validating with " + folds + " folds");
        
        Collections.shuffle(trainingData);
        int foldSize = trainingData.size() / folds;
        
        List<Double> accuracies = new ArrayList<>();
        
        for (int fold = 0; fold < folds; fold++) {
            int start = fold * foldSize;
            int end = Math.min(start + foldSize, trainingData.size());
            
            List<TrainingData> testSet = trainingData.subList(start, end);
            List<TrainingData> trainSet = new ArrayList<>();
            trainSet.addAll(trainingData.subList(0, start));
            trainSet.addAll(trainingData.subList(end, trainingData.size()));
            
            SimpleLinearModel model = trainLinearModel(trainSet);
            double accuracy = calculateAccuracy(model.getWeights(), model.getBias(), testSet);
            accuracies.add(accuracy);
            
            TestLogManager.info("Fold " + (fold + 1) + " accuracy: " + 
                String.format("%.2f%%", accuracy * 100));
        }
        
        double avgAccuracy = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(accuracies, avgAccuracy);
        
        return new CrossValidationResult(avgAccuracy, stdDev, accuracies);
    }
    
    /**
     * Predict using weights and bias
     */
    private double predict(double[] weights, double bias, double[] features) {
        double sum = bias;
        for (int i = 0; i < weights.length && i < features.length; i++) {
            sum += weights[i] * features[i];
        }
        return sum;
    }
    
    /**
     * Calculate accuracy
     */
    private double calculateAccuracy(double[] weights, double bias, 
                                    List<TrainingData> testData) {
        int correct = 0;
        for (TrainingData data : testData) {
            double prediction = predict(weights, bias, data.getFeatures());
            // For binary classification, threshold at 0.5
            int predictedClass = prediction >= 0.5 ? 1 : 0;
            int actualClass = data.getLabel() >= 0.5 ? 1 : 0;
            if (predictedClass == actualClass) {
                correct++;
            }
        }
        return testData.isEmpty() ? 0.0 : (double) correct / testData.size();
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStandardDeviation(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    /**
     * Training data
     */
    public static class TrainingData {
        private final double[] features;
        private final double label;
        
        public TrainingData(double[] features, double label) {
            this.features = features;
            this.label = label;
        }
        
        public double[] getFeatures() { return features; }
        public double getLabel() { return label; }
    }
    
    /**
     * Cross-validation result
     */
    public static class CrossValidationResult {
        private final double averageAccuracy;
        private final double standardDeviation;
        private final List<Double> foldAccuracies;
        
        public CrossValidationResult(double averageAccuracy, double standardDeviation,
                                   List<Double> foldAccuracies) {
            this.averageAccuracy = averageAccuracy;
            this.standardDeviation = standardDeviation;
            this.foldAccuracies = new ArrayList<>(foldAccuracies);
        }
        
        public double getAverageAccuracy() { return averageAccuracy; }
        public double getStandardDeviation() { return standardDeviation; }
        public List<Double> getFoldAccuracies() { return new ArrayList<>(foldAccuracies); }
    }
}

