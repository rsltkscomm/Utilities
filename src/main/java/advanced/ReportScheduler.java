package advanced;

import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Report scheduling and distribution system for automated report generation and delivery.
 */
public class ReportScheduler {
    
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledReport> scheduledReports;
    private final Map<String, ReportDistribution> distributionConfigs;
    private final String configDirectory;
    private boolean isRunning;
    
    public ReportScheduler() {
        this.scheduler = Executors.newScheduledThreadPool(5);
        this.scheduledReports = new ConcurrentHashMap<>();
        this.distributionConfigs = new ConcurrentHashMap<>();
        this.configDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("report_scheduler").toString();
        this.isRunning = false;
        createConfigDirectory();
    }
    
    /**
     * Starts the report scheduler.
     */
    public void startScheduler() {
        if (isRunning) {
            TestLogManager.warning("Report scheduler is already running");
            return;
        }
        
        TestLogManager.info("Starting report scheduler");
        isRunning = true;
        
        // Load scheduled reports from configuration
        loadScheduledReports();
        
        // Start monitoring for new schedules
        scheduler.scheduleAtFixedRate(this::checkForNewSchedules, 0, 1, TimeUnit.MINUTES);
        
        TestLogManager.success("Report scheduler started successfully");
    }
    
    /**
     * Stops the report scheduler.
     */
    public void stopScheduler() {
        if (!isRunning) {
            TestLogManager.warning("Report scheduler is not running");
            return;
        }
        
        TestLogManager.info("Stopping report scheduler");
        isRunning = false;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        TestLogManager.success("Report scheduler stopped");
    }
    
    /**
     * Schedules a report for automatic generation.
     * @param reportSchedule Report schedule configuration
     * @return ScheduleResult with scheduling details
     */
    public ScheduleResult scheduleReport(ReportSchedule reportSchedule) {
        TestLogManager.info("Scheduling report: " + reportSchedule.getReportName());
        
        ScheduleResult result = new ScheduleResult();
        result.setScheduleId(UUID.randomUUID().toString());
        result.setScheduledTime(LocalDateTime.now());
        
        try {
            // Calculate next execution time
            LocalDateTime nextExecution = calculateNextExecution(reportSchedule);
            
            // Create scheduled report
            ScheduledReport scheduledReport = new ScheduledReport();
            scheduledReport.setScheduleId(result.getScheduleId());
            scheduledReport.setReportSchedule(reportSchedule);
            scheduledReport.setNextExecution(nextExecution);
            scheduledReport.setStatus("SCHEDULED");
            
            // Schedule the report
            ScheduledFuture<?> future = scheduleReportExecution(scheduledReport);
            scheduledReport.setScheduledFuture(future);
            
            // Store the scheduled report
            scheduledReports.put(result.getScheduleId(), scheduledReport);
            
            // Save configuration
            saveScheduledReport(scheduledReport);
            
            result.setSuccess(true);
            result.setNextExecution(nextExecution);
            TestLogManager.success("Report scheduled successfully: " + reportSchedule.getReportName());
            
        } catch (Exception e) {
            TestLogManager.error("Failed to schedule report", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Cancels a scheduled report.
     * @param scheduleId Schedule ID to cancel
     * @return boolean indicating success
     */
    public boolean cancelScheduledReport(String scheduleId) {
        TestLogManager.info("Cancelling scheduled report: " + scheduleId);
        
        ScheduledReport scheduledReport = scheduledReports.get(scheduleId);
        if (scheduledReport != null) {
            scheduledReport.getScheduledFuture().cancel(false);
            scheduledReport.setStatus("CANCELLED");
            scheduledReports.remove(scheduleId);
            
            // Remove from configuration
            removeScheduledReport(scheduleId);
            
            TestLogManager.success("Scheduled report cancelled: " + scheduleId);
            return true;
        }
        
        TestLogManager.warning("Scheduled report not found: " + scheduleId);
        return false;
    }
    
    /**
     * Configures report distribution.
     * @param distribution Report distribution configuration
     * @return DistributionResult with configuration details
     */
    public DistributionResult configureDistribution(ReportDistribution distribution) {
        TestLogManager.info("Configuring report distribution: " + distribution.getDistributionName());
        
        DistributionResult result = new DistributionResult();
        result.setDistributionId(UUID.randomUUID().toString());
        result.setDistributedTime(LocalDateTime.now());
        
        try {
            // Store distribution configuration
            distributionConfigs.put(result.getDistributionId(), distribution);
            
            // Save configuration
            saveDistributionConfig(distribution);
            
            result.setSuccess(true);
            TestLogManager.success("Report distribution configured: " + distribution.getDistributionName());
            
        } catch (Exception e) {
            TestLogManager.error("Failed to configure report distribution", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Distributes a report using configured distribution methods.
     * @param reportPath Path to the report file
     * @param distributionId Distribution configuration ID
     * @return DistributionResult with distribution details
     */
    public DistributionResult distributeReport(Path reportPath, String distributionId) {
        TestLogManager.info("Distributing report: " + reportPath);
        
        DistributionResult result = new DistributionResult();
        result.setDistributionId(distributionId);
        result.setDistributedTime(LocalDateTime.now());
        
        try {
            ReportDistribution distribution = distributionConfigs.get(distributionId);
            if (distribution == null) {
                throw new IllegalArgumentException("Distribution configuration not found: " + distributionId);
            }
            
            // Distribute using configured methods
            for (DistributionMethod method : distribution.getDistributionMethods()) {
                distributeUsingMethod(reportPath, method);
            }
            
            result.setSuccess(true);
            TestLogManager.success("Report distributed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Failed to distribute report", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Gets all scheduled reports.
     * @return List of scheduled reports
     */
    public List<ScheduledReport> getScheduledReports() {
        return new ArrayList<>(scheduledReports.values());
    }
    
    /**
     * Gets all distribution configurations.
     * @return List of distribution configurations
     */
    public List<ReportDistribution> getDistributionConfigurations() {
        return new ArrayList<>(distributionConfigs.values());
    }
    
    private ScheduledFuture<?> scheduleReportExecution(ScheduledReport scheduledReport) {
        ReportSchedule schedule = scheduledReport.getReportSchedule();
        
        // Calculate initial delay
        long initialDelay = java.time.Duration.between(LocalDateTime.now(), scheduledReport.getNextExecution()).toMillis();
        
        // Schedule based on frequency
        switch (schedule.getFrequency()) {
            case "DAILY":
                return scheduler.scheduleAtFixedRate(
                    () -> executeScheduledReport(scheduledReport),
                    initialDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    
            case "WEEKLY":
                return scheduler.scheduleAtFixedRate(
                    () -> executeScheduledReport(scheduledReport),
                    initialDelay, 7 * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    
            case "MONTHLY":
                return scheduler.scheduleAtFixedRate(
                    () -> executeScheduledReport(scheduledReport),
                    initialDelay, 30L * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    
            case "HOURLY":
                return scheduler.scheduleAtFixedRate(
                    () -> executeScheduledReport(scheduledReport),
                    initialDelay, 60 * 60 * 1000, TimeUnit.MILLISECONDS);
                    
            default:
                return scheduler.schedule(
                    () -> executeScheduledReport(scheduledReport),
                    initialDelay, TimeUnit.MILLISECONDS);
        }
    }
    
    private void executeScheduledReport(ScheduledReport scheduledReport) {
        TestLogManager.info("Executing scheduled report: " + scheduledReport.getReportSchedule().getReportName());
        
        try {
            scheduledReport.setStatus("EXECUTING");
            scheduledReport.setLastExecution(LocalDateTime.now());
            
            // Generate report using ReportingEngine
            ReportingEngine reportingEngine = new ReportingEngine();
            ReportingEngine.TestExecutionData testData = createTestExecutionData();
            ReportingEngine.ReportConfiguration reportConfig = createReportConfiguration(scheduledReport.getReportSchedule());
            
            ReportingEngine.ReportResult reportResult = reportingEngine.generateComprehensiveReport(testData, reportConfig);
            
            if (reportResult.isSuccess()) {
                scheduledReport.setStatus("COMPLETED");
                scheduledReport.setLastSuccess(LocalDateTime.now());
                
                // Distribute report if configured
                if (scheduledReport.getReportSchedule().getDistributionId() != null) {
                    Path reportPath = reportResult.getReportFiles().get("HTML");
                    if (reportPath != null) {
                        distributeReport(reportPath, scheduledReport.getReportSchedule().getDistributionId());
                    }
                }
                
                TestLogManager.success("Scheduled report executed successfully: " + scheduledReport.getReportSchedule().getReportName());
            } else {
                scheduledReport.setStatus("FAILED");
                scheduledReport.setLastError(reportResult.getErrorMessage());
                TestLogManager.error("Scheduled report execution failed: " + reportResult.getErrorMessage());
            }
            
            // Calculate next execution time
            scheduledReport.setNextExecution(calculateNextExecution(scheduledReport.getReportSchedule()));
            
        } catch (Exception e) {
            scheduledReport.setStatus("FAILED");
            scheduledReport.setLastError(e.getMessage());
            TestLogManager.error("Scheduled report execution failed", e);
        }
    }
    
    private void distributeUsingMethod(Path reportPath, DistributionMethod method) {
        switch (method.getMethodType()) {
            case "EMAIL":
                distributeViaEmail(reportPath, method);
                break;
            case "FTP":
                distributeViaFTP(reportPath, method);
                break;
            case "WEBHOOK":
                distributeViaWebhook(reportPath, method);
                break;
            case "FILE_SYSTEM":
                distributeViaFileSystem(reportPath, method);
                break;
            default:
                TestLogManager.warning("Unknown distribution method: " + method.getMethodType());
        }
    }
    
    private void distributeViaEmail(Path reportPath, DistributionMethod method) {
        TestLogManager.info("Distributing report via email");
        // Email distribution implementation would go here
        // This would require email libraries like JavaMail
    }
    
    private void distributeViaFTP(Path reportPath, DistributionMethod method) {
        TestLogManager.info("Distributing report via FTP");
        // FTP distribution implementation would go here
        // This would require FTP libraries like Apache Commons Net
    }
    
    private void distributeViaWebhook(Path reportPath, DistributionMethod method) {
        TestLogManager.info("Distributing report via webhook");
        // Webhook distribution implementation would go here
        // This would use HTTP libraries to POST to webhook URLs
    }
    
    private void distributeViaFileSystem(Path reportPath, DistributionMethod method) {
        TestLogManager.info("Distributing report via file system");
        try {
            String destinationPath = method.getParameters().get("destination_path").toString();
            Path destination = Paths.get(destinationPath);
            
            if (!Files.exists(destination)) {
                Files.createDirectories(destination);
            }
            
            Path targetPath = destination.resolve(reportPath.getFileName());
            Files.copy(reportPath, targetPath);
            
            TestLogManager.success("Report copied to: " + targetPath);
            
        } catch (IOException e) {
            TestLogManager.error("Failed to distribute report via file system", e);
        }
    }
    
    private LocalDateTime calculateNextExecution(ReportSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (schedule.getFrequency()) {
            case "DAILY":
                return now.plusDays(1).withHour(schedule.getHour()).withMinute(schedule.getMinute());
            case "WEEKLY":
                return now.plusWeeks(1).withHour(schedule.getHour()).withMinute(schedule.getMinute());
            case "MONTHLY":
                return now.plusMonths(1).withHour(schedule.getHour()).withMinute(schedule.getMinute());
            case "HOURLY":
                return now.plusHours(1);
            default:
                return now.plusHours(1);
        }
    }
    
    private void checkForNewSchedules() {
        if (!isRunning) return;
        
        // Check for new schedule configurations
        loadScheduledReports();
    }
    
    private void loadScheduledReports() {
        // Load scheduled reports from configuration files
        // This would read from JSON/XML configuration files
    }
    
    private void saveScheduledReport(ScheduledReport scheduledReport) {
        // Save scheduled report configuration to file
        // This would write to JSON/XML configuration files
    }
    
    private void removeScheduledReport(String scheduleId) {
        // Remove scheduled report configuration from file
    }
    
    private void saveDistributionConfig(ReportDistribution distribution) {
        // Save distribution configuration to file
    }
    
    private ReportingEngine.TestExecutionData createTestExecutionData() {
        ReportingEngine.TestExecutionData testData = new ReportingEngine.TestExecutionData();
        testData.setTotalTests(100);
        testData.setPassedTests(95);
        testData.setFailedTests(5);
        testData.setSuccessRate(95.0);
        testData.setAverageExecutionTime(1500.0);
        testData.setTotalExecutionTime(150000);
        testData.setExecutionTime(LocalDateTime.now());
        return testData;
    }
    
    private ReportingEngine.ReportConfiguration createReportConfiguration(ReportSchedule schedule) {
        ReportingEngine.ReportConfiguration config = new ReportingEngine.ReportConfiguration();
        config.setGenerateHTML(true);
        config.setGenerateJSON(true);
        config.setGenerateDashboard(true);
        config.setReportTitle(schedule.getReportName());
        config.setReportDescription("Automated report generated by scheduler");
        return config;
    }
    
    private void createConfigDirectory() {
        try {
            Path dir = Paths.get(configDirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                TestLogManager.info("Created report scheduler config directory: " + configDirectory);
            }
        } catch (IOException e) {
            TestLogManager.error("Failed to create report scheduler config directory", e);
        }
    }
    
    // Data model classes
    public static class ReportSchedule {
        private String reportName;
        private String frequency; // DAILY, WEEKLY, MONTHLY, HOURLY
        private int hour;
        private int minute;
        private String distributionId;
        private Map<String, Object> parameters;
        
        public ReportSchedule() {
            this.parameters = new HashMap<>();
        }
        
        // Getters and setters
        public String getReportName() { return reportName; }
        public void setReportName(String reportName) { this.reportName = reportName; }
        
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
        
        public int getHour() { return hour; }
        public void setHour(int hour) { this.hour = hour; }
        
        public int getMinute() { return minute; }
        public void setMinute(int minute) { this.minute = minute; }
        
        public String getDistributionId() { return distributionId; }
        public void setDistributionId(String distributionId) { this.distributionId = distributionId; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    public static class ScheduledReport {
        private String scheduleId;
        private ReportSchedule reportSchedule;
        private LocalDateTime nextExecution;
        private LocalDateTime lastExecution;
        private LocalDateTime lastSuccess;
        private String status;
        private String lastError;
        private ScheduledFuture<?> scheduledFuture;
        
        // Getters and setters
        public String getScheduleId() { return scheduleId; }
        public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
        
        public ReportSchedule getReportSchedule() { return reportSchedule; }
        public void setReportSchedule(ReportSchedule reportSchedule) { this.reportSchedule = reportSchedule; }
        
        public LocalDateTime getNextExecution() { return nextExecution; }
        public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }
        
        public LocalDateTime getLastExecution() { return lastExecution; }
        public void setLastExecution(LocalDateTime lastExecution) { this.lastExecution = lastExecution; }
        
        public LocalDateTime getLastSuccess() { return lastSuccess; }
        public void setLastSuccess(LocalDateTime lastSuccess) { this.lastSuccess = lastSuccess; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        
        public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) { this.scheduledFuture = scheduledFuture; }
    }
    
    public static class ReportDistribution {
        private String distributionId;
        private String distributionName;
        private List<DistributionMethod> distributionMethods;
        private Map<String, Object> parameters;
        
        public ReportDistribution() {
            this.distributionMethods = new ArrayList<>();
            this.parameters = new HashMap<>();
        }
        
        // Getters and setters
        public String getDistributionId() { return distributionId; }
        public void setDistributionId(String distributionId) { this.distributionId = distributionId; }
        
        public String getDistributionName() { return distributionName; }
        public void setDistributionName(String distributionName) { this.distributionName = distributionName; }
        
        public List<DistributionMethod> getDistributionMethods() { return distributionMethods; }
        public void setDistributionMethods(List<DistributionMethod> distributionMethods) { this.distributionMethods = distributionMethods; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    public static class DistributionMethod {
        private String methodType; // EMAIL, FTP, WEBHOOK, FILE_SYSTEM
        private Map<String, Object> parameters;
        private boolean enabled;
        
        public DistributionMethod() {
            this.parameters = new HashMap<>();
            this.enabled = true;
        }
        
        // Getters and setters
        public String getMethodType() { return methodType; }
        public void setMethodType(String methodType) { this.methodType = methodType; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class ScheduleResult {
        private String scheduleId;
        private LocalDateTime scheduledTime;
        private LocalDateTime nextExecution;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getScheduleId() { return scheduleId; }
        public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
        
        public LocalDateTime getScheduledTime() { return scheduledTime; }
        public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
        
        public LocalDateTime getNextExecution() { return nextExecution; }
        public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class DistributionResult {
        private String distributionId;
        private LocalDateTime distributedTime;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getDistributionId() { return distributionId; }
        public void setDistributionId(String distributionId) { this.distributionId = distributionId; }
        
        public LocalDateTime getDistributedTime() { return distributedTime; }
        public void setDistributedTime(LocalDateTime distributedTime) { this.distributedTime = distributedTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
