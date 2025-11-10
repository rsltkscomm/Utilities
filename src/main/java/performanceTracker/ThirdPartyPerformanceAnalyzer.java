package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;
import config.ConfigurationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Third-Party Performance Impact Analyzer
 * 
 * Analyzes the performance impact of third-party scripts and resources:
 * - Third-party script identification and categorization
 * - Performance impact measurement
 * - Resource blocking analysis
 * - Security and privacy impact assessment
 * - Optimization recommendations
 */
public class ThirdPartyPerformanceAnalyzer {
    
    private final WebDriver driver;
    private final JavascriptExecutor jsExecutor;
    private final ConfigurationManager config;
    private final Map<String, ThirdPartyResource> thirdPartyResources;
    private final Map<String, ThirdPartyCategory> categoryDefinitions;
    
    public ThirdPartyPerformanceAnalyzer() {
    	WebDriver driver = DriverManager.getDriver();
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.config = ConfigurationManager.getInstance();
        this.thirdPartyResources = new ConcurrentHashMap<>();
        this.categoryDefinitions = initializeCategoryDefinitions();
    }
    
    /**
     * Initialize third-party category definitions
     */
    private Map<String, ThirdPartyCategory> initializeCategoryDefinitions() {
        Map<String, ThirdPartyCategory> categories = new ConcurrentHashMap<>();
        
        // Analytics
        categories.put("analytics", new ThirdPartyCategory(
            "Analytics",
            Arrays.asList("google-analytics.com", "googletagmanager.com", "mixpanel.com", "amplitude.com", "hotjar.com"),
            "Analytics and tracking scripts",
            Arrays.asList("gtag", "ga", "mixpanel", "amplitude", "hotjar")
        ));
        
        // Advertising
        categories.put("advertising", new ThirdPartyCategory(
            "Advertising",
            Arrays.asList("googlesyndication.com", "doubleclick.net", "facebook.com", "amazon-adsystem.com"),
            "Advertising and marketing scripts",
            Arrays.asList("adsbygoogle", "fbq", "amzn_assoc")
        ));
        
        // Social Media
        categories.put("social", new ThirdPartyCategory(
            "Social Media",
            Arrays.asList("facebook.com", "twitter.com", "linkedin.com", "instagram.com", "youtube.com"),
            "Social media widgets and embeds",
            Arrays.asList("fb-root", "twitter-widget", "linkedin-badge")
        ));
        
        // Content Delivery Networks
        categories.put("cdn", new ThirdPartyCategory(
            "Content Delivery Network",
            Arrays.asList("cloudflare.com", "jsdelivr.net", "unpkg.com", "cdnjs.cloudflare.com"),
            "CDN and content delivery services",
            Arrays.asList("cloudflare", "jsdelivr", "unpkg")
        ));
        
        // Payment Processing
        categories.put("payment", new ThirdPartyCategory(
            "Payment Processing",
            Arrays.asList("stripe.com", "paypal.com", "squareup.com", "braintreepayments.com"),
            "Payment processing services",
            Arrays.asList("stripe", "paypal", "square", "braintree")
        ));
        
        // Customer Support
        categories.put("support", new ThirdPartyCategory(
            "Customer Support",
            Arrays.asList("zendesk.com", "intercom.io", "freshdesk.com", "helpscout.com"),
            "Customer support and chat widgets",
            Arrays.asList("zendesk", "intercom", "freshdesk", "helpscout")
        ));
        
        // Maps and Location
        categories.put("maps", new ThirdPartyCategory(
            "Maps & Location",
            Arrays.asList("googleapis.com", "maps.google.com", "mapbox.com"),
            "Maps and location services",
            Arrays.asList("google-maps", "mapbox")
        ));
        
        // Video and Media
        categories.put("media", new ThirdPartyCategory(
            "Video & Media",
            Arrays.asList("youtube.com", "vimeo.com", "brightcove.com", "jwplayer.com"),
            "Video and media streaming services",
            Arrays.asList("youtube", "vimeo", "brightcove", "jwplayer")
        ));
        
        // Fonts and Typography
        categories.put("fonts", new ThirdPartyCategory(
            "Fonts & Typography",
            Arrays.asList("fonts.googleapis.com", "fonts.gstatic.com", "typekit.net"),
            "Web fonts and typography services",
            Arrays.asList("google-fonts", "adobe-fonts")
        ));
        
        // Security and Authentication
        categories.put("security", new ThirdPartyCategory(
            "Security & Authentication",
            Arrays.asList("recaptcha.net", "hcaptcha.com", "auth0.com", "okta.com"),
            "Security and authentication services",
            Arrays.asList("recaptcha", "hcaptcha", "auth0", "okta")
        ));
        
        return categories;
    }
    
    /**
     * Analyze all third-party resources on the current page
     */
    public ThirdPartyAnalysisResult analyzeThirdPartyImpact() {
        ThirdPartyAnalysisResult result = new ThirdPartyAnalysisResult();
        result.setTimestamp(System.currentTimeMillis());
        
        try {
            // Identify third-party resources
            List<ThirdPartyResource> resources = identifyThirdPartyResources();
            result.setThirdPartyResources(resources);
            
            // Categorize resources
            categorizeResources(resources);
            
            // Measure performance impact
            measurePerformanceImpact(resources);
            
            // Analyze blocking resources
            analyzeBlockingResources(resources);
            
            // Calculate impact metrics
            calculateImpactMetrics(result, resources);
            
            // Generate recommendations
            result.setRecommendations(generateOptimizationRecommendations(resources));
            
            // Assess security and privacy impact
            result.setSecurityImpact(assessSecurityImpact(resources));
            result.setPrivacyImpact(assessPrivacyImpact(resources));
            
        } catch (Exception e) {
            result.setError("Error analyzing third-party impact: " + e.getMessage());
            System.err.println("Error in third-party analysis: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Identify third-party resources using Resource Timing API
     */
    private List<ThirdPartyResource> identifyThirdPartyResources() {
        List<ThirdPartyResource> resources = new ArrayList<>();
        
        try {
            String script = """
                const resources = performance.getEntriesByType('resource');
                const currentDomain = window.location.hostname;
                const thirdPartyResources = [];
                
                resources.forEach(resource => {
                    try {
                        const resourceUrl = new URL(resource.name);
                        const resourceDomain = resourceUrl.hostname;
                        
                        // Check if it's a third-party resource
                        if (resourceDomain !== currentDomain && 
                            !resourceDomain.endsWith('.' + currentDomain)) {
                            
                            thirdPartyResources.push({
                                url: resource.name,
                                domain: resourceDomain,
                                size: resource.transferSize || 0,
                                duration: resource.duration,
                                startTime: resource.startTime,
                                responseEnd: resource.responseEnd,
                                initiatorType: resource.initiatorType,
                                responseStatus: resource.responseStatus || 0,
                                isBlocking: resource.renderBlockingStatus === 'blocking',
                                protocol: resourceUrl.protocol,
                                path: resourceUrl.pathname,
                                query: resourceUrl.search,
                                fragment: resourceUrl.hash
                            });
                        }
                    } catch (e) {
                        // Skip invalid URLs
                    }
                });
                
                return thirdPartyResources;
                """;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resourceData = (List<Map<String, Object>>) jsExecutor.executeScript(script);
            
            for (Map<String, Object> data : resourceData) {
                ThirdPartyResource resource = new ThirdPartyResource();
                resource.setUrl(data.get("url").toString());
                resource.setDomain(data.get("domain").toString());
                resource.setSize(((Number) data.get("size")).longValue());
                resource.setDuration(((Number) data.get("duration")).doubleValue());
                resource.setStartTime(((Number) data.get("startTime")).doubleValue());
                resource.setResponseEnd(((Number) data.get("responseEnd")).doubleValue());
                resource.setInitiatorType(data.get("initiatorType").toString());
                resource.setResponseStatus(((Number) data.get("responseStatus")).intValue());
                resource.setBlocking((Boolean) data.get("isBlocking"));
                resource.setProtocol(data.get("protocol").toString());
                resource.setPath(data.get("path").toString());
                resource.setQuery(data.get("query").toString());
                resource.setFragment(data.get("fragment").toString());
                
                resources.add(resource);
            }
            
        } catch (Exception e) {
            System.err.println("Error identifying third-party resources: " + e.getMessage());
        }
        
        return resources;
    }
    
    /**
     * Categorize third-party resources
     */
    private void categorizeResources(List<ThirdPartyResource> resources) {
        for (ThirdPartyResource resource : resources) {
            String category = determineCategory(resource);
            resource.setCategory(category);
            
            ThirdPartyCategory categoryInfo = categoryDefinitions.get(category);
            if (categoryInfo != null) {
                resource.setCategoryDescription(categoryInfo.getDescription());
            }
        }
    }
    
    /**
     * Determine category for a third-party resource
     */
    private String determineCategory(ThirdPartyResource resource) {
        String domain = resource.getDomain().toLowerCase();
        String url = resource.getUrl().toLowerCase();
        
        for (Map.Entry<String, ThirdPartyCategory> entry : categoryDefinitions.entrySet()) {
            ThirdPartyCategory category = entry.getValue();
            
            // Check domain matches
            for (String domainPattern : category.getDomainPatterns()) {
                if (domain.contains(domainPattern.toLowerCase())) {
                    return entry.getKey();
                }
            }
            
            // Check script identifiers
            for (String scriptPattern : category.getScriptPatterns()) {
                if (url.contains(scriptPattern.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        
        return "unknown";
    }
    
    /**
     * Measure performance impact of third-party resources
     */
    private void measurePerformanceImpact(List<ThirdPartyResource> resources) {
        for (ThirdPartyResource resource : resources) {
            // Calculate performance impact score
            double impactScore = calculatePerformanceImpactScore(resource);
            resource.setPerformanceImpact(impactScore);
            
            // Determine if resource is critical
            boolean isCritical = isResourceCritical(resource);
            resource.setCritical(isCritical);
            
            // Calculate blocking time
            double blockingTime = calculateBlockingTime(resource);
            resource.setBlockingTime(blockingTime);
        }
    }
    
    /**
     * Calculate performance impact score for a resource
     */
    private double calculatePerformanceImpactScore(ThirdPartyResource resource) {
        double score = 0.0;
        
        // Size impact (larger resources have higher impact)
        if (resource.getSize() > 0) {
            score += Math.log10(resource.getSize() / 1024) * 10; // KB to score
        }
        
        // Duration impact (slower resources have higher impact)
        if (resource.getDuration() > 0) {
            score += resource.getDuration() / 10; // ms to score
        }
        
        // Blocking impact
        if (resource.isBlocking()) {
            score += 20; // Additional penalty for blocking resources
        }
        
        // Critical resource impact
        if (resource.isCritical()) {
            score += 15; // Additional penalty for critical resources
        }
        
        // Network timing impact
        double networkTime = resource.getResponseEnd() - resource.getStartTime();
        if (networkTime > 0) {
            score += networkTime / 5; // Network time to score
        }
        
        return Math.min(100.0, Math.max(0.0, score));
    }
    
    /**
     * Determine if a resource is critical
     */
    private boolean isResourceCritical(ThirdPartyResource resource) {
        String category = resource.getCategory();
        
        // Critical categories
        if ("payment".equals(category) || "security".equals(category)) {
            return true;
        }
        
        // Blocking resources are critical
        if (resource.isBlocking()) {
            return true;
        }
        
        // Large resources are critical
        if (resource.getSize() > 100000) { // 100KB
            return true;
        }
        
        // Slow resources are critical
        if (resource.getDuration() > 1000) { // 1 second
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculate blocking time for a resource
     */
    private double calculateBlockingTime(ThirdPartyResource resource) {
        if (!resource.isBlocking()) {
            return 0.0;
        }
        
        // For blocking resources, blocking time is roughly the duration
        return resource.getDuration();
    }
    
    /**
     * Analyze blocking resources
     */
    private void analyzeBlockingResources(List<ThirdPartyResource> resources) {
        List<ThirdPartyResource> blockingResources = resources.stream()
            .filter(ThirdPartyResource::isBlocking)
            .sorted((a, b) -> Double.compare(b.getDuration(), a.getDuration()))
            .collect(Collectors.toList());
        
        // Store blocking resources analysis
        for (ThirdPartyResource resource : blockingResources) {
            resource.setBlockingAnalysis(generateBlockingAnalysis(resource));
        }
    }
    
    /**
     * Generate blocking analysis for a resource
     */
    private String generateBlockingAnalysis(ThirdPartyResource resource) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("Blocking resource: ").append(resource.getDomain()).append("\n");
        analysis.append("Duration: ").append(String.format("%.2f", resource.getDuration())).append("ms\n");
        analysis.append("Size: ").append(formatBytes(resource.getSize())).append("\n");
        
        if (resource.getDuration() > 2000) {
            analysis.append("⚠️ High blocking time - consider async loading\n");
        }
        
        if (resource.getSize() > 500000) { // 500KB
            analysis.append("⚠️ Large resource size - consider optimization\n");
        }
        
        if ("analytics".equals(resource.getCategory()) || "advertising".equals(resource.getCategory())) {
            analysis.append("💡 Consider lazy loading for non-critical tracking\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Calculate impact metrics
     */
    private void calculateImpactMetrics(ThirdPartyAnalysisResult result, List<ThirdPartyResource> resources) {
        // Total impact
        double totalImpact = resources.stream()
            .mapToDouble(ThirdPartyResource::getPerformanceImpact)
            .sum();
        result.setTotalPerformanceImpact(totalImpact);
        
        // Average impact
        double averageImpact = resources.isEmpty() ? 0.0 : totalImpact / resources.size();
        result.setAveragePerformanceImpact(averageImpact);
        
        // Blocking time
        double totalBlockingTime = resources.stream()
            .mapToDouble(ThirdPartyResource::getBlockingTime)
            .sum();
        result.setTotalBlockingTime(totalBlockingTime);
        
        // Resource count by category
        Map<String, Long> categoryCounts = resources.stream()
            .collect(Collectors.groupingBy(ThirdPartyResource::getCategory, Collectors.counting()));
        result.setCategoryCounts(categoryCounts);
        
        // Size by category
        Map<String, Long> categorySizes = resources.stream()
            .collect(Collectors.groupingBy(
                ThirdPartyResource::getCategory,
                Collectors.summingLong(ThirdPartyResource::getSize)
            ));
        result.setCategorySizes(categorySizes);
        
        // Critical resources
        long criticalCount = resources.stream()
            .mapToLong(r -> r.isCritical() ? 1 : 0)
            .sum();
        result.setCriticalResourceCount(criticalCount);
        
        // Failed resources
        long failedCount = resources.stream()
            .mapToLong(r -> r.getResponseStatus() >= 400 ? 1 : 0)
            .sum();
        result.setFailedResourceCount(failedCount);
    }
    
    /**
     * Generate optimization recommendations
     */
    private List<String> generateOptimizationRecommendations(List<ThirdPartyResource> resources) {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze by category
        Map<String, List<ThirdPartyResource>> resourcesByCategory = resources.stream()
            .collect(Collectors.groupingBy(ThirdPartyResource::getCategory));
        
        for (Map.Entry<String, List<ThirdPartyResource>> entry : resourcesByCategory.entrySet()) {
            String category = entry.getKey();
            List<ThirdPartyResource> categoryResources = entry.getValue();
            
            // Analytics recommendations
            if ("analytics".equals(category)) {
                recommendations.add("Analytics: Consider lazy loading Google Analytics and other tracking scripts");
                recommendations.add("Analytics: Use Google Analytics 4 (GA4) for better performance than Universal Analytics");
            }
            
            // Advertising recommendations
            if ("advertising".equals(category)) {
                recommendations.add("Advertising: Implement lazy loading for ad scripts to improve initial page load");
                recommendations.add("Advertising: Consider using Google AdSense auto ads for better performance");
            }
            
            // Social media recommendations
            if ("social".equals(category)) {
                recommendations.add("Social Media: Load social widgets only when they come into viewport");
                recommendations.add("Social Media: Consider using lightweight social sharing buttons instead of full widgets");
            }
            
            // CDN recommendations
            if ("cdn".equals(category)) {
                recommendations.add("CDN: Ensure CDN resources are properly cached and compressed");
                recommendations.add("CDN: Consider using a single CDN provider to reduce DNS lookups");
            }
            
            // Font recommendations
            if ("fonts".equals(category)) {
                recommendations.add("Fonts: Use font-display: swap for Google Fonts to improve loading performance");
                recommendations.add("Fonts: Preload critical fonts and use font subsetting");
            }
        }
        
        // General recommendations based on resource analysis
        long blockingCount = resources.stream().mapToLong(r -> r.isBlocking() ? 1 : 0).sum();
        if (blockingCount > 3) {
            recommendations.add("General: Too many blocking resources (" + blockingCount + ") - consider async loading");
        }
        
        long largeResourceCount = resources.stream().mapToLong(r -> r.getSize() > 100000 ? 1 : 0).sum();
        if (largeResourceCount > 0) {
            recommendations.add("General: " + largeResourceCount + " large resources detected - consider optimization");
        }
        
        double totalBlockingTime = resources.stream().mapToDouble(ThirdPartyResource::getBlockingTime).sum();
        if (totalBlockingTime > 5000) { // 5 seconds
            recommendations.add("General: High total blocking time (" + String.format("%.0f", totalBlockingTime) + "ms) - optimize critical rendering path");
        }
        
        return recommendations;
    }
    
    /**
     * Assess security impact
     */
    private SecurityImpact assessSecurityImpact(List<ThirdPartyResource> resources) {
        SecurityImpact securityImpact = new SecurityImpact();
        
        // Count resources by security risk level
        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;
        
        for (ThirdPartyResource resource : resources) {
            String category = resource.getCategory();
            
            if ("security".equals(category) || "payment".equals(category)) {
                highRiskCount++;
            } else if ("analytics".equals(category) || "advertising".equals(category)) {
                mediumRiskCount++;
            } else {
                lowRiskCount++;
            }
        }
        
        securityImpact.setHighRiskCount(highRiskCount);
        securityImpact.setMediumRiskCount(mediumRiskCount);
        securityImpact.setLowRiskCount(lowRiskCount);
        
        // Generate security recommendations
        List<String> securityRecommendations = new ArrayList<>();
        
        if (highRiskCount > 0) {
            securityRecommendations.add("Review high-risk third-party integrations for security compliance");
        }
        
        if (mediumRiskCount > 5) {
            securityRecommendations.add("Consider implementing Content Security Policy (CSP) for analytics and advertising scripts");
        }
        
        securityRecommendations.add("Regularly audit third-party scripts for security vulnerabilities");
        securityRecommendations.add("Use Subresource Integrity (SRI) for third-party scripts when possible");
        
        securityImpact.setRecommendations(securityRecommendations);
        
        return securityImpact;
    }
    
    /**
     * Assess privacy impact
     */
    private PrivacyImpact assessPrivacyImpact(List<ThirdPartyResource> resources) {
        PrivacyImpact privacyImpact = new PrivacyImpact();
        
        // Count tracking resources
        long trackingCount = resources.stream()
            .mapToLong(r -> "analytics".equals(r.getCategory()) || "advertising".equals(r.getCategory()) ? 1 : 0)
            .sum();
        
        privacyImpact.setTrackingResourceCount(trackingCount);
        
        // Check for GDPR compliance
        boolean hasAnalytics = resources.stream().anyMatch(r -> "analytics".equals(r.getCategory()));
        boolean hasAdvertising = resources.stream().anyMatch(r -> "advertising".equals(r.getCategory()));
        
        privacyImpact.setHasAnalytics(hasAnalytics);
        privacyImpact.setHasAdvertising(hasAdvertising);
        
        // Generate privacy recommendations
        List<String> privacyRecommendations = new ArrayList<>();
        
        if (hasAnalytics || hasAdvertising) {
            privacyRecommendations.add("Implement cookie consent management for GDPR compliance");
            privacyRecommendations.add("Provide clear privacy policy and cookie policy");
        }
        
        if (trackingCount > 3) {
            privacyRecommendations.add("Consider reducing number of tracking services to improve privacy");
        }
        
        privacyRecommendations.add("Implement privacy-focused analytics alternatives where possible");
        privacyRecommendations.add("Regularly review and audit third-party data collection practices");
        
        privacyImpact.setRecommendations(privacyRecommendations);
        
        return privacyImpact;
    }
    
    /**
     * Format bytes to human readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Third-Party Category data model
     */
    public static class ThirdPartyCategory {
        private final String name;
        private final List<String> domainPatterns;
        private final String description;
        private final List<String> scriptPatterns;
        
        public ThirdPartyCategory(String name, List<String> domainPatterns, String description, List<String> scriptPatterns) {
            this.name = name;
            this.domainPatterns = domainPatterns;
            this.description = description;
            this.scriptPatterns = scriptPatterns;
        }
        
        // Getters
        public String getName() { return name; }
        public List<String> getDomainPatterns() { return domainPatterns; }
        public String getDescription() { return description; }
        public List<String> getScriptPatterns() { return scriptPatterns; }
    }
    
    /**
     * Third-Party Resource data model
     */
    public static class ThirdPartyResource {
        private String url;
        private String domain;
        private long size;
        private double duration;
        private double startTime;
        private double responseEnd;
        private String initiatorType;
        private int responseStatus;
        private boolean blocking;
        private String protocol;
        private String path;
        private String query;
        private String fragment;
        private String category;
        private String categoryDescription;
        private double performanceImpact;
        private boolean critical;
        private double blockingTime;
        private String blockingAnalysis;
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
        
        public double getStartTime() { return startTime; }
        public void setStartTime(double startTime) { this.startTime = startTime; }
        
        public double getResponseEnd() { return responseEnd; }
        public void setResponseEnd(double responseEnd) { this.responseEnd = responseEnd; }
        
        public String getInitiatorType() { return initiatorType; }
        public void setInitiatorType(String initiatorType) { this.initiatorType = initiatorType; }
        
        public int getResponseStatus() { return responseStatus; }
        public void setResponseStatus(int responseStatus) { this.responseStatus = responseStatus; }
        
        public boolean isBlocking() { return blocking; }
        public void setBlocking(boolean blocking) { this.blocking = blocking; }
        
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getFragment() { return fragment; }
        public void setFragment(String fragment) { this.fragment = fragment; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getCategoryDescription() { return categoryDescription; }
        public void setCategoryDescription(String categoryDescription) { this.categoryDescription = categoryDescription; }
        
        public double getPerformanceImpact() { return performanceImpact; }
        public void setPerformanceImpact(double performanceImpact) { this.performanceImpact = performanceImpact; }
        
        public boolean isCritical() { return critical; }
        public void setCritical(boolean critical) { this.critical = critical; }
        
        public double getBlockingTime() { return blockingTime; }
        public void setBlockingTime(double blockingTime) { this.blockingTime = blockingTime; }
        
        public String getBlockingAnalysis() { return blockingAnalysis; }
        public void setBlockingAnalysis(String blockingAnalysis) { this.blockingAnalysis = blockingAnalysis; }
    }
    
    /**
     * Third-Party Analysis Result
     */
    public static class ThirdPartyAnalysisResult {
        private long timestamp;
        private List<ThirdPartyResource> thirdPartyResources;
        private double totalPerformanceImpact;
        private double averagePerformanceImpact;
        private double totalBlockingTime;
        private Map<String, Long> categoryCounts;
        private Map<String, Long> categorySizes;
        private long criticalResourceCount;
        private long failedResourceCount;
        private List<String> recommendations;
        private SecurityImpact securityImpact;
        private PrivacyImpact privacyImpact;
        private String error;
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public List<ThirdPartyResource> getThirdPartyResources() { return thirdPartyResources; }
        public void setThirdPartyResources(List<ThirdPartyResource> thirdPartyResources) { this.thirdPartyResources = thirdPartyResources; }
        
        public double getTotalPerformanceImpact() { return totalPerformanceImpact; }
        public void setTotalPerformanceImpact(double totalPerformanceImpact) { this.totalPerformanceImpact = totalPerformanceImpact; }
        
        public double getAveragePerformanceImpact() { return averagePerformanceImpact; }
        public void setAveragePerformanceImpact(double averagePerformanceImpact) { this.averagePerformanceImpact = averagePerformanceImpact; }
        
        public double getTotalBlockingTime() { return totalBlockingTime; }
        public void setTotalBlockingTime(double totalBlockingTime) { this.totalBlockingTime = totalBlockingTime; }
        
        public Map<String, Long> getCategoryCounts() { return categoryCounts; }
        public void setCategoryCounts(Map<String, Long> categoryCounts) { this.categoryCounts = categoryCounts; }
        
        public Map<String, Long> getCategorySizes() { return categorySizes; }
        public void setCategorySizes(Map<String, Long> categorySizes) { this.categorySizes = categorySizes; }
        
        public long getCriticalResourceCount() { return criticalResourceCount; }
        public void setCriticalResourceCount(long criticalResourceCount) { this.criticalResourceCount = criticalResourceCount; }
        
        public long getFailedResourceCount() { return failedResourceCount; }
        public void setFailedResourceCount(long failedResourceCount) { this.failedResourceCount = failedResourceCount; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public SecurityImpact getSecurityImpact() { return securityImpact; }
        public void setSecurityImpact(SecurityImpact securityImpact) { this.securityImpact = securityImpact; }
        
        public PrivacyImpact getPrivacyImpact() { return privacyImpact; }
        public void setPrivacyImpact(PrivacyImpact privacyImpact) { this.privacyImpact = privacyImpact; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    /**
     * Security Impact data model
     */
    public static class SecurityImpact {
        private long highRiskCount;
        private long mediumRiskCount;
        private long lowRiskCount;
        private List<String> recommendations;
        
        // Getters and setters
        public long getHighRiskCount() { return highRiskCount; }
        public void setHighRiskCount(long highRiskCount) { this.highRiskCount = highRiskCount; }
        
        public long getMediumRiskCount() { return mediumRiskCount; }
        public void setMediumRiskCount(long mediumRiskCount) { this.mediumRiskCount = mediumRiskCount; }
        
        public long getLowRiskCount() { return lowRiskCount; }
        public void setLowRiskCount(long lowRiskCount) { this.lowRiskCount = lowRiskCount; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    /**
     * Privacy Impact data model
     */
    public static class PrivacyImpact {
        private long trackingResourceCount;
        private boolean hasAnalytics;
        private boolean hasAdvertising;
        private List<String> recommendations;
        
        // Getters and setters
        public long getTrackingResourceCount() { return trackingResourceCount; }
        public void setTrackingResourceCount(long trackingResourceCount) { this.trackingResourceCount = trackingResourceCount; }
        
        public boolean isHasAnalytics() { return hasAnalytics; }
        public void setHasAnalytics(boolean hasAnalytics) { this.hasAnalytics = hasAnalytics; }
        
        public boolean isHasAdvertising() { return hasAdvertising; }
        public void setHasAdvertising(boolean hasAdvertising) { this.hasAdvertising = hasAdvertising; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}
