package performanceTracker;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Network Traffic Capture using BrowserMob Proxy.
 * 
 * This is a reliable alternative to CDP-based capture that works with ANY Chrome version.
 * 
 * Features:
 * - Captures all HTTP/HTTPS requests and responses
 * - Records request/response headers and bodies
 * - Tracks timing information
 * - Generates standard HAR (HTTP Archive) format
 * - No Chrome version dependency
 * - Works with Chrome, Firefox, Edge
 * 
 * Usage:
 * BrowserMobNetworkCapture capture = new BrowserMobNetworkCapture();
 * Proxy seleniumProxy = capture.startCapture();
 * // Configure WebDriver with seleniumProxy
 * // ... perform test actions ...
 * File harFile = capture.stopCaptureAndSave(testCaseKey);
 */
public class BrowserMobNetworkCapture {
    
    private BrowserMobProxy proxy;
    private final ConfigurationManager config;
    private boolean isCapturing = false;
    
    /**
     * Constructor
     */
    public BrowserMobNetworkCapture() {
        this.config = ConfigurationManager.getInstance();
    }
    
    /**
     * Start capturing network traffic and return Selenium Proxy
     * 
     * @return Selenium Proxy to configure WebDriver
     */
    public Proxy startCapture() {
        if (!config.isNetworkTrafficCaptureEnabled()) {
            return null;
        }
        
        try {
            // Create and start BrowserMob Proxy
            proxy = new BrowserMobProxyServer();
            proxy.start(0); // Start on any available port
            
            // Enable HAR capture with all details
            proxy.enableHarCaptureTypes(
                CaptureType.REQUEST_HEADERS,
                CaptureType.REQUEST_CONTENT,
                CaptureType.RESPONSE_HEADERS,
                CaptureType.RESPONSE_CONTENT
            );
            
            // Start new HAR
            proxy.newHar("TestExecution");
            
            // Create Selenium Proxy object
            Proxy seleniumProxy = new Proxy();
            String proxyAddress = "localhost:" + proxy.getPort();
            seleniumProxy.setHttpProxy(proxyAddress);
            seleniumProxy.setSslProxy(proxyAddress);
            
            isCapturing = true;
            System.out.println("🌐 Network traffic capture started on port: " + proxy.getPort());
            
            return seleniumProxy;
            
        } catch (Exception e) {
            System.err.println("⚠️  Failed to start network capture: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Stop capturing and save to HAR file
     * 
     * @param testCaseKey Test case key for file naming
     * @return HAR file, or null if failed
     */
    public File stopCaptureAndSave(String testCaseKey) {
        if (!isCapturing || proxy == null) {
            return null;
        }
        
        try {
            // Get HAR data
            Har har = proxy.getHar();
            
            // Generate filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = testCaseKey + "_network_traffic_" + timestamp + ".har";
            File harFile = new File(config.getLogPath() + fileName);
            
            // Create directory if doesn't exist
            harFile.getParentFile().mkdirs();
            
            // Write HAR to file
            har.writeTo(harFile);
            
            // Get statistics
            int totalRequests = har.getLog().getEntries().size();
            long failedRequests = har.getLog().getEntries().stream()
                .filter(entry -> entry.getResponse().getStatus() >= 400)
                .count();
            
            System.out.println("🌐 Network traffic captured:");
            System.out.println("   Total Requests: " + totalRequests);
            System.out.println("   Failed Requests: " + failedRequests);
            System.out.println("   File: " + fileName);
            
            return harFile;
            
        } catch (IOException e) {
            System.err.println("⚠️  Failed to save network traffic: " + e.getMessage());
            return null;
        } finally {
            // Stop proxy
            if (proxy != null) {
                try {
                    proxy.stop();
                    isCapturing = false;
                } catch (Exception e) {
                    // Ignore stop errors
                }
            }
        }
    }
    
    /**
     * Get summary of captured network traffic
     */
    public String getSummary() {
        if (proxy == null || !isCapturing) {
            return "No network traffic captured";
        }
        
        try {
            Har har = proxy.getHar();
            int totalRequests = har.getLog().getEntries().size();
            long failedRequests = har.getLog().getEntries().stream()
                .filter(entry -> entry.getResponse().getStatus() >= 400)
                .count();
            
            return String.format("Network Traffic: %d requests, %d errors", totalRequests, failedRequests);
        } catch (Exception e) {
            return "Network traffic capture in progress";
        }
    }
    
    /**
     * Check if there are network errors
     */
    public boolean hasNetworkErrors() {
        if (proxy == null || !isCapturing) {
            return false;
        }
        
        try {
            Har har = proxy.getHar();
            return har.getLog().getEntries().stream()
                .anyMatch(entry -> entry.getResponse().getStatus() >= 400);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get BrowserMob Proxy instance (for advanced usage)
     */
    public BrowserMobProxy getProxy() {
        return proxy;
    }
}

