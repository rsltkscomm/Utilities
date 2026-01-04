package ai.ml.integration;

import config.ConfigurationManager;
import reporting.TestLogManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ML Model Manager
 * Manages ML models: training, saving, loading, and prediction
 */
public class MLModelManager {
    
    private final ConfigurationManager config;
    private final Map<String, MLModel> loadedModels;
    private final String modelsDirectory;
    
    public MLModelManager() {
        this.config = ConfigurationManager.getInstance();
        this.loadedModels = new ConcurrentHashMap<>();
        this.modelsDirectory = config.getString("ml.models.directory", 
            System.getProperty("user.dir") + File.separator + "models");
        
        // Create models directory if it doesn't exist
        createModelsDirectory();
    }
    
    /**
     * Train and save a model
     */
    public MLModel trainAndSave(String modelName, String modelType,
                                List<MLModelTrainer.TrainingData> trainingData) {
        TestLogManager.info("Training model: " + modelName + " (type: " + modelType + ")");
        
        MLModelTrainer trainer = new MLModelTrainer();
        MLModel model;
        
        switch (modelType.toLowerCase()) {
            case "linear":
            case "linearregression":
                model = trainer.trainLinearModel(trainingData);
                break;
            case "decisiontree":
            case "tree":
                model = trainer.trainDecisionTree(trainingData);
                break;
            default:
                throw new IllegalArgumentException("Unknown model type: " + modelType);
        }
        
        // Save model
        saveModel(modelName, model);
        
        // Load into memory
        loadedModels.put(modelName, model);
        
        TestLogManager.info("Model trained and saved: " + modelName);
        return model;
    }
    
    /**
     * Save model to disk
     */
    public void saveModel(String modelName, MLModel model) {
        try {
            Path modelPath = Paths.get(modelsDirectory, modelName + ".model");
            
            // Serialize model
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    Files.newOutputStream(modelPath))) {
                oos.writeObject(model);
            }
            
            // Save metadata
            saveModelMetadata(modelName, model.getMetadata());
            
            TestLogManager.info("Model saved: " + modelPath);
        } catch (IOException e) {
            TestLogManager.error("Failed to save model: " + modelName, e);
            throw new RuntimeException("Failed to save model", e);
        }
    }
    
    /**
     * Load model from disk
     */
    public MLModel loadModel(String modelName) {
        // Check if already loaded
        if (loadedModels.containsKey(modelName)) {
            return loadedModels.get(modelName);
        }
        
        try {
            Path modelPath = Paths.get(modelsDirectory, modelName + ".model");
            
            if (!Files.exists(modelPath)) {
                throw new FileNotFoundException("Model not found: " + modelName);
            }
            
            // Deserialize model
            MLModel model;
            try (ObjectInputStream ois = new ObjectInputStream(
                    Files.newInputStream(modelPath))) {
                model = (MLModel) ois.readObject();
            }
            
            // Load into memory
            loadedModels.put(modelName, model);
            
            TestLogManager.info("Model loaded: " + modelName);
            return model;
        } catch (IOException | ClassNotFoundException e) {
            TestLogManager.error("Failed to load model: " + modelName, e);
            throw new RuntimeException("Failed to load model", e);
        }
    }
    
    /**
     * Predict using a model
     */
    public MLModel.PredictionResult predict(String modelName, double[] features) {
        MLModel model = getModel(modelName);
        return model.predictWithMetadata(features);
    }
    
    /**
     * Get model (load if not in memory)
     */
    public MLModel getModel(String modelName) {
        if (!loadedModels.containsKey(modelName)) {
            return loadModel(modelName);
        }
        return loadedModels.get(modelName);
    }
    
    /**
     * List all available models
     */
    public List<String> listModels() {
        List<String> models = new ArrayList<>();
        
        try {
            Path modelsPath = Paths.get(modelsDirectory);
            if (Files.exists(modelsPath)) {
                Files.list(modelsPath)
                    .filter(path -> path.toString().endsWith(".model"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        models.add(fileName.substring(0, fileName.length() - 6)); // Remove .model
                    });
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to list models", e);
        }
        
        return models;
    }
    
    /**
     * Delete a model
     */
    public boolean deleteModel(String modelName) {
        try {
            Path modelPath = Paths.get(modelsDirectory, modelName + ".model");
            Path metadataPath = Paths.get(modelsDirectory, modelName + ".metadata");
            
            boolean deleted = false;
            if (Files.exists(modelPath)) {
                Files.delete(modelPath);
                deleted = true;
            }
            if (Files.exists(metadataPath)) {
                Files.delete(metadataPath);
            }
            
            // Remove from memory
            loadedModels.remove(modelName);
            
            if (deleted) {
                TestLogManager.info("Model deleted: " + modelName);
            }
            
            return deleted;
        } catch (IOException e) {
            TestLogManager.error("Failed to delete model: " + modelName, e);
            return false;
        }
    }
    
    /**
     * Get model metadata
     */
    public MLModel.ModelMetadata getModelMetadata(String modelName) {
        MLModel model = getModel(modelName);
        return model.getMetadata();
    }
    
    /**
     * Save model metadata
     */
    private void saveModelMetadata(String modelName, MLModel.ModelMetadata metadata) {
        try {
            Path metadataPath = Paths.get(modelsDirectory, modelName + ".metadata");
            
            try (PrintWriter writer = new PrintWriter(
                    Files.newBufferedWriter(metadataPath))) {
                writer.println("Model: " + modelName);
                writer.println("Type: " + metadata.getModelType());
                writer.println("Version: " + metadata.getVersion());
                writer.println("Accuracy: " + String.format("%.2f%%", 
                    metadata.getAccuracy() * 100));
                writer.println("Training Timestamp: " + 
                    new Date(metadata.getTrainingTimestamp()));
            }
        } catch (IOException e) {
            TestLogManager.warning("Failed to save model metadata: " + modelName);
        }
    }
    
    /**
     * Create models directory
     */
    private void createModelsDirectory() {
        try {
            Path dir = Paths.get(modelsDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created models directory: " + modelsDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create models directory", e);
        }
    }
}

