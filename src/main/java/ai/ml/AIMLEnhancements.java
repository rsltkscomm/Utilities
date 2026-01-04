package ai.ml;

import listeners.retry.FlakyTestDetector;
import listeners.retry.TestStabilityScorer;
import reporting.TestLogManager;

import java.util.*;

/**
 * Main class for AI/ML enhancements
 * Provides unified access to all AI/ML features
 */
public class AIMLEnhancements {
    
    private final FlakyTestDetector flakyDetector;
    private final TestStabilityScorer stabilityScorer;
    private final PredictiveTestMaintenance predictiveMaintenance;
    private final IntelligentTestPrioritizer prioritizer;
    private final AnomalyDetector anomalyDetector;
    private final TestFailurePredictor failurePredictor;
    private final AITestOptimizer optimizer;
    private final MLElementDetector elementDetector;
    
    public AIMLEnhancements(FlakyTestDetector flakyDetector,
                           TestStabilityScorer stabilityScorer) {
        this.flakyDetector = flakyDetector;
        this.stabilityScorer = stabilityScorer;
        this.predictiveMaintenance = new PredictiveTestMaintenance(flakyDetector, stabilityScorer);
        this.prioritizer = new IntelligentTestPrioritizer(flakyDetector, stabilityScorer);
        this.anomalyDetector = new AnomalyDetector();
        this.failurePredictor = new TestFailurePredictor(flakyDetector, stabilityScorer);
        this.optimizer = new AITestOptimizer(flakyDetector, stabilityScorer);
        this.elementDetector = new MLElementDetector();
    }
    
    /**
     * Get predictive test maintenance
     */
    public PredictiveTestMaintenance getPredictiveMaintenance() {
        return predictiveMaintenance;
    }
    
    /**
     * Get intelligent test prioritizer
     */
    public IntelligentTestPrioritizer getPrioritizer() {
        return prioritizer;
    }
    
    /**
     * Get anomaly detector
     */
    public AnomalyDetector getAnomalyDetector() {
        return anomalyDetector;
    }
    
    /**
     * Get test failure predictor
     */
    public TestFailurePredictor getFailurePredictor() {
        return failurePredictor;
    }
    
    /**
     * Get test optimizer
     */
    public AITestOptimizer getOptimizer() {
        return optimizer;
    }
    
    /**
     * Get ML element detector
     */
    public MLElementDetector getElementDetector() {
        return elementDetector;
    }
    
    /**
     * Run comprehensive AI/ML analysis
     */
    public MLAnalysisReport runComprehensiveAnalysis(Set<String> testKeys) {
        TestLogManager.info("Running comprehensive AI/ML analysis");
        
        MLAnalysisReport report = new MLAnalysisReport();
        
        // Predictive maintenance
        List<PredictiveTestMaintenance.TestPrediction> predictions = 
            predictiveMaintenance.predictFailures();
        report.setFailurePredictions(predictions);
        
        // Test prioritization
        List<IntelligentTestPrioritizer.TestPriority> priorities = 
            prioritizer.prioritizeTests(testKeys);
        report.setTestPriorities(priorities);
        
        // Failure risk prediction
        List<TestFailurePredictor.FailureRisk> risks = 
            failurePredictor.predictFailureRisks(testKeys);
        report.setFailureRisks(risks);
        
        // Optimization
        AITestOptimizer.OptimizationReport optReport = 
            optimizer.analyzeTestSuite(testKeys);
        report.setOptimizationReport(optReport);
        
        return report;
    }
    
    /**
     * ML Analysis Report
     */
    public static class MLAnalysisReport {
        private List<PredictiveTestMaintenance.TestPrediction> failurePredictions;
        private List<IntelligentTestPrioritizer.TestPriority> testPriorities;
        private List<TestFailurePredictor.FailureRisk> failureRisks;
        private AITestOptimizer.OptimizationReport optimizationReport;
        
        public List<PredictiveTestMaintenance.TestPrediction> getFailurePredictions() { 
            return failurePredictions; 
        }
        public void setFailurePredictions(List<PredictiveTestMaintenance.TestPrediction> predictions) {
            this.failurePredictions = predictions;
        }
        
        public List<IntelligentTestPrioritizer.TestPriority> getTestPriorities() { 
            return testPriorities; 
        }
        public void setTestPriorities(List<IntelligentTestPrioritizer.TestPriority> priorities) {
            this.testPriorities = priorities;
        }
        
        public List<TestFailurePredictor.FailureRisk> getFailureRisks() { 
            return failureRisks; 
        }
        public void setFailureRisks(List<TestFailurePredictor.FailureRisk> risks) {
            this.failureRisks = risks;
        }
        
        public AITestOptimizer.OptimizationReport getOptimizationReport() { 
            return optimizationReport; 
        }
        public void setOptimizationReport(AITestOptimizer.OptimizationReport report) {
            this.optimizationReport = report;
        }
    }
}

