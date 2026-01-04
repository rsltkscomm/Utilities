package ai.ml.integration;

import java.io.Serializable;
import java.util.*;

/**
 * Simplified decision tree model
 */
public class DecisionTreeModel implements MLModel, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private DecisionNode root;
    private double accuracy;
    
    public void train(List<MLModelTrainer.TrainingData> trainingData) {
        if (trainingData.isEmpty()) {
            throw new IllegalArgumentException("Training data cannot be empty");
        }
        
        root = buildTree(trainingData, 0, 5); // Max depth 5
        
        // Calculate accuracy
        accuracy = calculateAccuracy(trainingData);
    }
    
    /**
     * Build decision tree recursively
     */
    private DecisionNode buildTree(List<MLModelTrainer.TrainingData> data, 
                                   int depth, int maxDepth) {
        if (depth >= maxDepth || data.size() < 2) {
            return new DecisionNode(calculateAverageLabel(data), true);
        }
        
        // Find best split
        SplitResult bestSplit = findBestSplit(data);
        
        if (bestSplit == null) {
            return new DecisionNode(calculateAverageLabel(data), true);
        }
        
        DecisionNode node = new DecisionNode(bestSplit.getThreshold(), false);
        node.setFeatureIndex(bestSplit.getFeatureIndex());
        
        // Split data
        List<MLModelTrainer.TrainingData> leftData = new ArrayList<>();
        List<MLModelTrainer.TrainingData> rightData = new ArrayList<>();
        
        for (MLModelTrainer.TrainingData sample : data) {
            if (sample.getFeatures()[bestSplit.getFeatureIndex()] <= bestSplit.getThreshold()) {
                leftData.add(sample);
            } else {
                rightData.add(sample);
            }
        }
        
        // Recursively build children
        node.setLeft(buildTree(leftData, depth + 1, maxDepth));
        node.setRight(buildTree(rightData, depth + 1, maxDepth));
        
        return node;
    }
    
    /**
     * Find best split
     */
    private SplitResult findBestSplit(List<MLModelTrainer.TrainingData> data) {
        if (data.isEmpty()) {
            return null;
        }
        
        int featureCount = data.get(0).getFeatures().length;
        double bestGini = Double.MAX_VALUE;
        int bestFeature = -1;
        double bestThreshold = 0.0;
        
        for (int feature = 0; feature < featureCount; feature++) {
            // Try different thresholds
            for (double threshold = 0.0; threshold <= 1.0; threshold += 0.1) {
                double gini = calculateGini(data, feature, threshold);
                if (gini < bestGini) {
                    bestGini = gini;
                    bestFeature = feature;
                    bestThreshold = threshold;
                }
            }
        }
        
        if (bestFeature == -1) {
            return null;
        }
        
        return new SplitResult(bestFeature, bestThreshold);
    }
    
    /**
     * Calculate Gini impurity
     */
    private double calculateGini(List<MLModelTrainer.TrainingData> data, 
                                 int feature, double threshold) {
        int leftCount = 0;
        int rightCount = 0;
        int leftPositive = 0;
        int rightPositive = 0;
        
        for (MLModelTrainer.TrainingData sample : data) {
            if (sample.getFeatures()[feature] <= threshold) {
                leftCount++;
                if (sample.getLabel() >= 0.5) {
                    leftPositive++;
                }
            } else {
                rightCount++;
                if (sample.getLabel() >= 0.5) {
                    rightPositive++;
                }
            }
        }
        
        double leftGini = calculateNodeGini(leftCount, leftPositive);
        double rightGini = calculateNodeGini(rightCount, rightPositive);
        
        double leftWeight = (double) leftCount / data.size();
        double rightWeight = (double) rightCount / data.size();
        
        return leftWeight * leftGini + rightWeight * rightGini;
    }
    
    /**
     * Calculate Gini for a node
     */
    private double calculateNodeGini(int total, int positive) {
        if (total == 0) {
            return 0.0;
        }
        double p = (double) positive / total;
        return 1.0 - (p * p) - ((1.0 - p) * (1.0 - p));
    }
    
    /**
     * Calculate average label
     */
    private double calculateAverageLabel(List<MLModelTrainer.TrainingData> data) {
        return data.stream()
            .mapToDouble(MLModelTrainer.TrainingData::getLabel)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Calculate accuracy
     */
    public double calculateAccuracy(List<MLModelTrainer.TrainingData> testData) {
        int correct = 0;
        for (MLModelTrainer.TrainingData data : testData) {
            double prediction = predict(data.getFeatures());
            int predictedClass = prediction >= 0.5 ? 1 : 0;
            int actualClass = data.getLabel() >= 0.5 ? 1 : 0;
            if (predictedClass == actualClass) {
                correct++;
            }
        }
        return testData.isEmpty() ? 0.0 : (double) correct / testData.size();
    }
    
    @Override
    public double predict(double[] features) {
        return predictNode(root, features);
    }
    
    private double predictNode(DecisionNode node, double[] features) {
        if (node.isLeaf()) {
            return node.getThreshold();
        }
        
        if (features[node.getFeatureIndex()] <= node.getThreshold()) {
            return predictNode(node.getLeft(), features);
        } else {
            return predictNode(node.getRight(), features);
        }
    }
    
    @Override
    public PredictionResult predictWithMetadata(double[] features) {
        double prediction = predict(features);
        double confidence = calculateConfidence(prediction);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelType", "DecisionTree");
        metadata.put("featureCount", features.length);
        
        return new PredictionResult(prediction, confidence, metadata);
    }
    
    @Override
    public ModelMetadata getMetadata() {
        Map<String, Object> params = new HashMap<>();
        params.put("accuracy", accuracy);
        
        return new ModelMetadata(
            "DecisionTree",
            "1.0",
            params,
            System.currentTimeMillis(),
            accuracy
        );
    }
    
    private double calculateConfidence(double prediction) {
        return 1.0 - 2.0 * Math.abs(prediction - 0.5);
    }
    
    /**
     * Decision node
     */
    private static class DecisionNode implements Serializable {
        
        private static final long serialVersionUID = 1L;
        private final double threshold;
        private final boolean leaf;
        private int featureIndex;
        private DecisionNode left;
        private DecisionNode right;
        
        public DecisionNode(double threshold, boolean leaf) {
            this.threshold = threshold;
            this.leaf = leaf;
        }
        
        public double getThreshold() { return threshold; }
        public boolean isLeaf() { return leaf; }
        public int getFeatureIndex() { return featureIndex; }
        public void setFeatureIndex(int featureIndex) { this.featureIndex = featureIndex; }
        public DecisionNode getLeft() { return left; }
        public void setLeft(DecisionNode left) { this.left = left; }
        public DecisionNode getRight() { return right; }
        public void setRight(DecisionNode right) { this.right = right; }
    }
    
    /**
     * Split result
     */
    private static class SplitResult {
        private final int featureIndex;
        private final double threshold;
        
        public SplitResult(int featureIndex, double threshold) {
            this.featureIndex = featureIndex;
            this.threshold = threshold;
        }
        
        public int getFeatureIndex() { return featureIndex; }
        public double getThreshold() { return threshold; }
    }
}

