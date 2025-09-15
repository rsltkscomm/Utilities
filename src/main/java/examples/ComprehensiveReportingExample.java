package examples;

import advanced.*;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive example demonstrating the usage of all reporting and analytics capabilities.
 */
public class ComprehensiveReportingExample {
    
    public static void main(String[] args) {
        TestLogManager.info("Starting Comprehensive Reporting Example");
        
        try {
            // Initialize reporting components
            ReportingEngine reportingEngine = new ReportingEngine();
            AnalyticsDashboard analyticsDashboard = new AnalyticsDashboard();
            ReportScheduler reportScheduler = new ReportScheduler();
            
            // Example 1: Generate comprehensive test report
            generateComprehensiveReport(reportingEngine);
            
            // Example 2: Create analytics dashboard
            createAnalyticsDashboard(analyticsDashboard);
            
            // Example 3: Generate trend analysis
            generateTrendAnalysis(reportingEngine);
            
            // Example 4: Generate executive report
            generateExecutiveReport(reportingEngine);
            
            // Example 5: Schedule automated reports
            scheduleAutomatedReports(reportScheduler);
            
            // Example 6: Configure report distribution
            configureReportDistribution(reportScheduler);
            
            // Example 7: Real-time dashboard monitoring
            demonstrateRealTimeDashboard(analyticsDashboard);
            
            TestLogManager.success("Comprehensive Reporting Example completed successfully");
            
        } catch (Exception e) {
            TestLogManager.error("Comprehensive Reporting Example failed", e);
        }
    }
    
    /**
     * Demonstrates comprehensive test report generation.
     */
    private static void generateComprehensiveReport(ReportingEngine reportingEngine) {
        TestLogManager.info("=== Generating Comprehensive Test Report ===");
        
        // Create test execution data
        ReportingEngine.TestExecutionData testData = new ReportingEngine.TestExecutionData();
        testData.setTotalTests(150);
        testData.setPassedTests(142);
        testData.setFailedTests(8);
        testData.setSuccessRate(94.67);
        testData.setAverageExecutionTime(1250.5);
        testData.setTotalExecutionTime(187575);
        testData.setExecutionTime(LocalDateTime.now());
        
        // Configure report generation
        ReportingEngine.ReportConfiguration reportConfig = new ReportingEngine.ReportConfiguration();
        reportConfig.setGenerateHTML(true);
        reportConfig.setGeneratePDF(false);
        reportConfig.setGenerateJSON(true);
        reportConfig.setGenerateExcel(false);
        reportConfig.setGenerateDashboard(true);
        reportConfig.setReportTitle("Comprehensive Test Execution Report");
        reportConfig.setReportDescription("Detailed analysis of test execution results with performance metrics");
        
        // Generate comprehensive report
        ReportingEngine.ReportResult result = reportingEngine.generateComprehensiveReport(testData, reportConfig);
        
        if (result.isSuccess()) {
            TestLogManager.success("Comprehensive report generated successfully");
            TestLogManager.info("Report ID: " + result.getReportId());
            TestLogManager.info("Generated files: " + result.getReportFiles().keySet());
        } else {
            TestLogManager.error("Failed to generate comprehensive report: " + result.getErrorMessage());
        }
    }
    
    /**
     * Demonstrates analytics dashboard creation and management.
     */
    private static void createAnalyticsDashboard(AnalyticsDashboard analyticsDashboard) {
        TestLogManager.info("=== Creating Analytics Dashboard ===");
        
        // Start the dashboard
        analyticsDashboard.startDashboard();
        
        // Add metrics to the dashboard
        addDashboardMetrics(analyticsDashboard);
        
        // Add widgets to the dashboard
        addDashboardWidgets(analyticsDashboard);
        
        // Generate the dashboard
        AnalyticsDashboard.DashboardResult result = analyticsDashboard.generateDashboard();
        
        if (result.isSuccess()) {
            TestLogManager.success("Analytics dashboard generated successfully");
            TestLogManager.info("Dashboard ID: " + result.getDashboardId());
            TestLogManager.info("Dashboard path: " + result.getDashboardPath());
        } else {
            TestLogManager.error("Failed to generate analytics dashboard: " + result.getErrorMessage());
        }
        
        // Export dashboard data
        AnalyticsDashboard.ExportResult exportResult = analyticsDashboard.exportDashboardData("JSON");
        if (exportResult.isSuccess()) {
            TestLogManager.success("Dashboard data exported: " + exportResult.getExportPath());
        }
        
        // Stop the dashboard after demonstration
        analyticsDashboard.stopDashboard();
    }
    
    /**
     * Demonstrates trend analysis and predictive analytics.
     */
    private static void generateTrendAnalysis(ReportingEngine reportingEngine) {
        TestLogManager.info("=== Generating Trend Analysis ===");
        
        // Create historical test data
        List<ReportingEngine.TestExecutionData> historicalData = createHistoricalTestData();
        
        // Configure trend analysis
        ReportingEngine.TrendAnalysisConfig trendConfig = new ReportingEngine.TrendAnalysisConfig();
        trendConfig.setLookbackPeriod(30);
        trendConfig.setConfidenceLevel(0.95);
        trendConfig.setIncludePredictions(true);
        
        // Generate trend analysis
        ReportingEngine.TrendAnalysisResult result = reportingEngine.generateTrendAnalysis(historicalData, trendConfig);
        
        if (result.isSuccess()) {
            TestLogManager.success("Trend analysis generated successfully");
            TestLogManager.info("Analysis ID: " + result.getAnalysisId());
            TestLogManager.info("Trends identified: " + result.getTrends().keySet());
            TestLogManager.info("Predictions made: " + result.getPredictions().keySet());
            TestLogManager.info("Key insights: " + result.getInsights());
        } else {
            TestLogManager.error("Failed to generate trend analysis: " + result.getErrorMessage());
        }
    }
    
    /**
     * Demonstrates executive reporting and KPI tracking.
     */
    private static void generateExecutiveReport(ReportingEngine reportingEngine) {
        TestLogManager.info("=== Generating Executive Report ===");
        
        // Create executive data
        ReportingEngine.ExecutiveData executiveData = new ReportingEngine.ExecutiveData();
        executiveData.setOverallSuccessRate(94.5);
        executiveData.setAverageExecutionTime(1250.0);
        executiveData.setTestCoverage(87.3);
        executiveData.setDefectDetectionRate(12.8);
        executiveData.setAutomationPercentage(78.5);
        
        // Generate executive report
        ReportingEngine.ExecutiveReportResult result = reportingEngine.generateExecutiveReport(executiveData);
        
        if (result.isSuccess()) {
            TestLogManager.success("Executive report generated successfully");
            TestLogManager.info("Report ID: " + result.getReportId());
            TestLogManager.info("KPIs calculated: " + result.getKpis().keySet());
            TestLogManager.info("Recommendations: " + result.getRecommendations());
        } else {
            TestLogManager.error("Failed to generate executive report: " + result.getErrorMessage());
        }
    }
    
    /**
     * Demonstrates automated report scheduling.
     */
    private static void scheduleAutomatedReports(ReportScheduler reportScheduler) {
        TestLogManager.info("=== Scheduling Automated Reports ===");
        
        // Start the scheduler
        reportScheduler.startScheduler();
        
        // Schedule daily test report
        ReportScheduler.ReportSchedule dailyReport = new ReportScheduler.ReportSchedule();
        dailyReport.setReportName("Daily Test Execution Report");
        dailyReport.setFrequency("DAILY");
        dailyReport.setHour(9);
        dailyReport.setMinute(0);
        dailyReport.setDistributionId("daily_distribution");
        
        ReportScheduler.ScheduleResult dailyResult = reportScheduler.scheduleReport(dailyReport);
        if (dailyResult.isSuccess()) {
            TestLogManager.success("Daily report scheduled: " + dailyResult.getScheduleId());
        }
        
        // Schedule weekly trend analysis
        ReportScheduler.ReportSchedule weeklyReport = new ReportScheduler.ReportSchedule();
        weeklyReport.setReportName("Weekly Trend Analysis");
        weeklyReport.setFrequency("WEEKLY");
        weeklyReport.setHour(10);
        weeklyReport.setMinute(30);
        
        ReportScheduler.ScheduleResult weeklyResult = reportScheduler.scheduleReport(weeklyReport);
        if (weeklyResult.isSuccess()) {
            TestLogManager.success("Weekly report scheduled: " + weeklyResult.getScheduleId());
        }
        
        // Schedule monthly executive report
        ReportScheduler.ReportSchedule monthlyReport = new ReportScheduler.ReportSchedule();
        monthlyReport.setReportName("Monthly Executive Summary");
        monthlyReport.setFrequency("MONTHLY");
        monthlyReport.setHour(8);
        monthlyReport.setMinute(0);
        monthlyReport.setDistributionId("executive_distribution");
        
        ReportScheduler.ScheduleResult monthlyResult = reportScheduler.scheduleReport(monthlyReport);
        if (monthlyResult.isSuccess()) {
            TestLogManager.success("Monthly report scheduled: " + monthlyResult.getScheduleId());
        }
        
        // Display all scheduled reports
        List<ReportScheduler.ScheduledReport> scheduledReports = reportScheduler.getScheduledReports();
        TestLogManager.info("Total scheduled reports: " + scheduledReports.size());
        
        // Stop the scheduler
        reportScheduler.stopScheduler();
    }
    
    /**
     * Demonstrates report distribution configuration.
     */
    private static void configureReportDistribution(ReportScheduler reportScheduler) {
        TestLogManager.info("=== Configuring Report Distribution ===");
        
        // Configure email distribution
        ReportScheduler.ReportDistribution emailDistribution = new ReportScheduler.ReportDistribution();
        emailDistribution.setDistributionName("Email Distribution");
        
        ReportScheduler.DistributionMethod emailMethod = new ReportScheduler.DistributionMethod();
        emailMethod.setMethodType("EMAIL");
        emailMethod.getParameters().put("smtp_host", "smtp.company.com");
        emailMethod.getParameters().put("smtp_port", "587");
        emailMethod.getParameters().put("recipients", "team@company.com,manager@company.com");
        emailMethod.getParameters().put("subject", "Automated Test Report");
        
        emailDistribution.getDistributionMethods().add(emailMethod);
        
        ReportScheduler.DistributionResult emailResult = reportScheduler.configureDistribution(emailDistribution);
        if (emailResult.isSuccess()) {
            TestLogManager.success("Email distribution configured: " + emailResult.getDistributionId());
        }
        
        // Configure file system distribution
        ReportScheduler.ReportDistribution fileDistribution = new ReportScheduler.ReportDistribution();
        fileDistribution.setDistributionName("File System Distribution");
        
        ReportScheduler.DistributionMethod fileMethod = new ReportScheduler.DistributionMethod();
        fileMethod.setMethodType("FILE_SYSTEM");
        fileMethod.getParameters().put("destination_path", CrossPlatformUtils.getProjectDataDirectory().resolve("shared_reports").toString());
        
        fileDistribution.getDistributionMethods().add(fileMethod);
        
        ReportScheduler.DistributionResult fileResult = reportScheduler.configureDistribution(fileDistribution);
        if (fileResult.isSuccess()) {
            TestLogManager.success("File system distribution configured: " + fileResult.getDistributionId());
        }
        
        // Configure webhook distribution
        ReportScheduler.ReportDistribution webhookDistribution = new ReportScheduler.ReportDistribution();
        webhookDistribution.setDistributionName("Webhook Distribution");
        
        ReportScheduler.DistributionMethod webhookMethod = new ReportScheduler.DistributionMethod();
        webhookMethod.setMethodType("WEBHOOK");
        webhookMethod.getParameters().put("webhook_url", "https://hooks.slack.com/services/xxx/yyy/zzz");
        webhookMethod.getParameters().put("content_type", "application/json");
        
        webhookDistribution.getDistributionMethods().add(webhookMethod);
        
        ReportScheduler.DistributionResult webhookResult = reportScheduler.configureDistribution(webhookDistribution);
        if (webhookResult.isSuccess()) {
            TestLogManager.success("Webhook distribution configured: " + webhookResult.getDistributionId());
        }
        
        // Display all distribution configurations
        List<ReportScheduler.ReportDistribution> distributions = reportScheduler.getDistributionConfigurations();
        TestLogManager.info("Total distribution configurations: " + distributions.size());
    }
    
    /**
     * Demonstrates real-time dashboard monitoring.
     */
    private static void demonstrateRealTimeDashboard(AnalyticsDashboard analyticsDashboard) {
        TestLogManager.info("=== Demonstrating Real-time Dashboard ===");
        
        // Start the dashboard
        analyticsDashboard.startDashboard();
        
        // Simulate real-time metric updates
        for (int i = 0; i < 10; i++) {
            // Update various metrics
            analyticsDashboard.updateMetric("tests_executed", Math.random() * 100);
            analyticsDashboard.updateMetric("success_rate", 90 + Math.random() * 10);
            analyticsDashboard.updateMetric("response_time", 100 + Math.random() * 500);
            analyticsDashboard.updateMetric("memory_used", Math.random() * 1000);
            
            try {
                Thread.sleep(2000); // Wait 2 seconds between updates
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Generate final dashboard
        AnalyticsDashboard.DashboardResult result = analyticsDashboard.generateDashboard();
        if (result.isSuccess()) {
            TestLogManager.success("Real-time dashboard updated: " + result.getDashboardPath());
        }
        
        // Stop the dashboard
        analyticsDashboard.stopDashboard();
    }
    
    /**
     * Adds sample metrics to the analytics dashboard.
     */
    private static void addDashboardMetrics(AnalyticsDashboard analyticsDashboard) {
        // Test execution metrics
        AnalyticsDashboard.DashboardMetric testMetric = new AnalyticsDashboard.DashboardMetric();
        testMetric.setMetricId("tests_executed");
        testMetric.setMetricName("Tests Executed");
        testMetric.setValue(150);
        testMetric.setMinValue(0);
        testMetric.setMaxValue(200);
        testMetric.setUnit("count");
        testMetric.setLastUpdated(LocalDateTime.now());
        testMetric.setCategory("Test Execution");
        analyticsDashboard.addMetric(testMetric);
        
        // Success rate metric
        AnalyticsDashboard.DashboardMetric successMetric = new AnalyticsDashboard.DashboardMetric();
        successMetric.setMetricId("success_rate");
        successMetric.setMetricName("Success Rate");
        successMetric.setValue(94.67);
        successMetric.setMinValue(0);
        successMetric.setMaxValue(100);
        successMetric.setUnit("%");
        successMetric.setLastUpdated(LocalDateTime.now());
        successMetric.setCategory("Quality");
        analyticsDashboard.addMetric(successMetric);
        
        // Response time metric
        AnalyticsDashboard.DashboardMetric responseMetric = new AnalyticsDashboard.DashboardMetric();
        responseMetric.setMetricId("response_time");
        responseMetric.setMetricName("Average Response Time");
        responseMetric.setValue(1250.5);
        responseMetric.setMinValue(0);
        responseMetric.setMaxValue(5000);
        responseMetric.setUnit("ms");
        responseMetric.setLastUpdated(LocalDateTime.now());
        responseMetric.setCategory("Performance");
        analyticsDashboard.addMetric(responseMetric);
        
        // Memory usage metric
        AnalyticsDashboard.DashboardMetric memoryMetric = new AnalyticsDashboard.DashboardMetric();
        memoryMetric.setMetricId("memory_used");
        memoryMetric.setMetricName("Memory Usage");
        memoryMetric.setValue(512);
        memoryMetric.setMinValue(0);
        memoryMetric.setMaxValue(1024);
        memoryMetric.setUnit("MB");
        memoryMetric.setLastUpdated(LocalDateTime.now());
        memoryMetric.setCategory("System");
        analyticsDashboard.addMetric(memoryMetric);
    }
    
    /**
     * Adds sample widgets to the analytics dashboard.
     */
    private static void addDashboardWidgets(AnalyticsDashboard analyticsDashboard) {
        // Metric card widget
        AnalyticsDashboard.DashboardWidget metricWidget = new AnalyticsDashboard.DashboardWidget();
        metricWidget.setWidgetId("metric_card_1");
        metricWidget.setWidgetName("Test Execution Summary");
        metricWidget.setWidgetType("metric_card");
        
        AnalyticsDashboard.WidgetConfiguration metricConfig = new AnalyticsDashboard.WidgetConfiguration();
        metricConfig.setWidgetId("metric_card_1");
        metricConfig.setWidgetName("Test Execution Summary");
        metricConfig.setMetricId("tests_executed");
        metricWidget.setConfiguration(metricConfig);
        metricWidget.setCreatedTime(LocalDateTime.now());
        
        analyticsDashboard.addWidget(metricWidget);
        
        // Line chart widget
        AnalyticsDashboard.DashboardWidget lineChartWidget = new AnalyticsDashboard.DashboardWidget();
        lineChartWidget.setWidgetId("line_chart_1");
        lineChartWidget.setWidgetName("Success Rate Trend");
        lineChartWidget.setWidgetType("line_chart");
        
        AnalyticsDashboard.WidgetConfiguration lineConfig = new AnalyticsDashboard.WidgetConfiguration();
        lineConfig.setWidgetId("line_chart_1");
        lineConfig.setWidgetName("Success Rate Trend");
        lineConfig.setMetricId("success_rate");
        lineChartWidget.setConfiguration(lineConfig);
        lineChartWidget.setCreatedTime(LocalDateTime.now());
        
        analyticsDashboard.addWidget(lineChartWidget);
        
        // Gauge widget
        AnalyticsDashboard.DashboardWidget gaugeWidget = new AnalyticsDashboard.DashboardWidget();
        gaugeWidget.setWidgetId("gauge_1");
        gaugeWidget.setWidgetName("Memory Usage Gauge");
        gaugeWidget.setWidgetType("gauge");
        
        AnalyticsDashboard.WidgetConfiguration gaugeConfig = new AnalyticsDashboard.WidgetConfiguration();
        gaugeConfig.setWidgetId("gauge_1");
        gaugeConfig.setWidgetName("Memory Usage Gauge");
        gaugeConfig.setMetricId("memory_used");
        gaugeWidget.setConfiguration(gaugeConfig);
        gaugeWidget.setCreatedTime(LocalDateTime.now());
        
        analyticsDashboard.addWidget(gaugeWidget);
        
        // Table widget
        AnalyticsDashboard.DashboardWidget tableWidget = new AnalyticsDashboard.DashboardWidget();
        tableWidget.setWidgetId("table_1");
        tableWidget.setWidgetName("Performance Metrics Table");
        tableWidget.setWidgetType("table");
        
        AnalyticsDashboard.WidgetConfiguration tableConfig = new AnalyticsDashboard.WidgetConfiguration();
        tableConfig.setWidgetId("table_1");
        tableConfig.setWidgetName("Performance Metrics Table");
        tableWidget.setConfiguration(tableConfig);
        tableWidget.setCreatedTime(LocalDateTime.now());
        
        analyticsDashboard.addWidget(tableWidget);
    }
    
    /**
     * Creates sample historical test data for trend analysis.
     */
    private static List<ReportingEngine.TestExecutionData> createHistoricalTestData() {
        List<ReportingEngine.TestExecutionData> historicalData = new ArrayList<>();
        
        for (int i = 0; i < 30; i++) {
            ReportingEngine.TestExecutionData testData = new ReportingEngine.TestExecutionData();
            testData.setTotalTests(100 + (int)(Math.random() * 50));
            testData.setPassedTests((int)(testData.getTotalTests() * (0.9 + Math.random() * 0.1)));
            testData.setFailedTests(testData.getTotalTests() - testData.getPassedTests());
            testData.setSuccessRate((double)testData.getPassedTests() / testData.getTotalTests() * 100);
            testData.setAverageExecutionTime(1000 + Math.random() * 1000);
            testData.setTotalExecutionTime((long)(testData.getAverageExecutionTime() * testData.getTotalTests()));
            testData.setExecutionTime(LocalDateTime.now().minusDays(30 - i));
            
            historicalData.add(testData);
        }
        
        return historicalData;
    }
}
