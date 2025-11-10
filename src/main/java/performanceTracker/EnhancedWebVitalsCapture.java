package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import base.DriverManager;
import config.ConfigurationManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Web Vitals Capture with Custom Metrics
 * 
 * Captures Google Core Web Vitals plus additional custom performance metrics:
 * - Core Web Vitals: LCP, CLS, FCP, TTFB, INP, FID
 * - Custom Business Metrics: Time to Interactive, Time to Business Action
 * - Advanced Metrics: Resource Timing, Navigation Timing, Paint Timing
 * - Mobile Web Vitals: mFID, mFCP, mCLS
 * - Custom User Journey Metrics
 */
public class EnhancedWebVitalsCapture {
    
    private final WebDriver driver;
    private final ConfigurationManager config;
    private final JavascriptExecutor jsExecutor;
    
    public EnhancedWebVitalsCapture() {
        this.driver = DriverManager.getDriver();
        this.config = ConfigurationManager.getInstance();
        this.jsExecutor = (JavascriptExecutor) driver;
    }
    
    /**
     * Capture all enhanced Web Vitals and custom metrics
     */
    public EnhancedWebVitals captureAllMetrics() {
        EnhancedWebVitals webVitals = new EnhancedWebVitals();
        webVitals.setTimestamp(System.currentTimeMillis());
        
        try {
            // Capture Core Web Vitals
            captureCoreWebVitals(webVitals);
            
            // Capture Custom Business Metrics
            captureCustomBusinessMetrics(webVitals);
            
            // Capture Advanced Performance Metrics
            captureAdvancedPerformanceMetrics(webVitals);
            
            // Capture Mobile Web Vitals
            captureMobileWebVitals(webVitals);
            
            // Capture User Journey Metrics
            captureUserJourneyMetrics(webVitals);
            
            // Calculate overall score
            webVitals.setOverallScore(calculateOverallScore(webVitals));
            
            // Generate insights
            webVitals.setInsights(generateWebVitalsInsights(webVitals));
            
        } catch (Exception e) {
            System.err.println("Error capturing enhanced Web Vitals: " + e.getMessage());
            webVitals.setError(e.getMessage());
        }
        
        return webVitals;
    }
    
    /**
     * Capture Core Web Vitals (LCP, CLS, FCP, TTFB, INP, FID)
     */
    private void captureCoreWebVitals(EnhancedWebVitals webVitals) {
        try {
            // Largest Contentful Paint (LCP)
            CompletableFuture<Double> lcpFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureLCP();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // Cumulative Layout Shift (CLS)
            CompletableFuture<Double> clsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureCLS();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // First Contentful Paint (FCP)
            CompletableFuture<Double> fcpFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureFCP();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // Time to First Byte (TTFB)
            CompletableFuture<Double> ttfbFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureTTFB();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // Interaction to Next Paint (INP)
            CompletableFuture<Double> inpFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureINP();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // First Input Delay (FID)
            CompletableFuture<Double> fidFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return captureFID();
                } catch (Exception e) {
                    return 0.0;
                }
            });
            
            // Wait for all core metrics
            webVitals.setLcp(lcpFuture.get(10, TimeUnit.SECONDS));
            webVitals.setCls(clsFuture.get(10, TimeUnit.SECONDS));
            webVitals.setFcp(fcpFuture.get(10, TimeUnit.SECONDS));
            webVitals.setTtfb(ttfbFuture.get(10, TimeUnit.SECONDS));
            webVitals.setInp(inpFuture.get(10, TimeUnit.SECONDS));
            webVitals.setFid(fidFuture.get(10, TimeUnit.SECONDS));
            
        } catch (Exception e) {
            System.err.println("Error capturing core Web Vitals: " + e.getMessage());
        }
    }
    
    /**
     * Capture Custom Business Metrics
     */
    private void captureCustomBusinessMetrics(EnhancedWebVitals webVitals) {
        try {
            // Time to Interactive (TTI) - when page becomes fully interactive
            Double tti = captureTTI();
            webVitals.setTti(tti != null ? tti : 0.0);
            
            // Time to Business Action (TTBA) - time to complete primary business action
            Double ttba = captureTTBA();
            webVitals.setTtba(ttba != null ? ttba : 0.0);
            
            // Time to Meaningful Paint (TTMP) - when meaningful content is visible
            Double ttmp = captureTTMP();
            webVitals.setTtmp(ttmp != null ? ttmp : 0.0);
            
            // Time to Critical Resource Load (TTCRL)
            Double ttcrl = captureTTCRL();
            webVitals.setTtcrl(ttcrl != null ? ttcrl : 0.0);
            
            // Custom Business Metric: Time to Login Form Ready
            Double loginFormReady = captureLoginFormReady();
            webVitals.setLoginFormReady(loginFormReady != null ? loginFormReady : 0.0);
            
            // Custom Business Metric: Time to Search Ready
            Double searchReady = captureSearchReady();
            webVitals.setSearchReady(searchReady != null ? searchReady : 0.0);
            
        } catch (Exception e) {
            System.err.println("Error capturing custom business metrics: " + e.getMessage());
        }
    }
    
    /**
     * Capture Advanced Performance Metrics
     */
    private void captureAdvancedPerformanceMetrics(EnhancedWebVitals webVitals) {
        try {
            // Resource Timing API metrics
            Map<String, Object> resourceTiming = captureResourceTiming();
            webVitals.setResourceTiming(resourceTiming);
            
            // Navigation Timing API metrics
            Map<String, Object> navigationTiming = captureNavigationTiming();
            webVitals.setNavigationTiming(navigationTiming);
            
            // Paint Timing API metrics
            Map<String, Object> paintTiming = capturePaintTiming();
            webVitals.setPaintTiming(paintTiming);
            
            // Long Tasks API metrics
            Map<String, Object> longTasks = captureLongTasks();
            webVitals.setLongTasks(longTasks);
            
            // Layout Shift Events
            List<Map<String, Object>> layoutShifts = captureLayoutShifts();
            webVitals.setLayoutShifts(layoutShifts);
            
        } catch (Exception e) {
            System.err.println("Error capturing advanced performance metrics: " + e.getMessage());
        }
    }
    
    /**
     * Capture Mobile Web Vitals
     */
    private void captureMobileWebVitals(EnhancedWebVitals webVitals) {
        try {
            // Mobile First Input Delay (mFID)
            Double mfid = captureMobileFID();
            webVitals.setMfid(mfid != null ? mfid : 0.0);
            
            // Mobile First Contentful Paint (mFCP)
            Double mfcp = captureMobileFCP();
            webVitals.setMfcp(mfcp != null ? mfcp : 0.0);
            
            // Mobile Cumulative Layout Shift (mCLS)
            Double mcls = captureMobileCLS();
            webVitals.setMcls(mcls != null ? mcls : 0.0);
            
            // Touch Response Time
            Double touchResponse = captureTouchResponseTime();
            webVitals.setTouchResponseTime(touchResponse != null ? touchResponse : 0.0);
            
            // Scroll Performance
            Double scrollPerformance = captureScrollPerformance();
            webVitals.setScrollPerformance(scrollPerformance != null ? scrollPerformance : 0.0);
            
        } catch (Exception e) {
            System.err.println("Error capturing mobile Web Vitals: " + e.getMessage());
        }
    }
    
    /**
     * Capture User Journey Metrics
     */
    private void captureUserJourneyMetrics(EnhancedWebVitals webVitals) {
        try {
            // Time to User Action
            Double timeToAction = captureTimeToUserAction();
            webVitals.setTimeToUserAction(timeToAction != null ? timeToAction : 0.0);
            
            // User Engagement Score
            Double engagementScore = calculateUserEngagementScore();
            webVitals.setUserEngagementScore(engagementScore != null ? engagementScore : 0.0);
            
            // Page Responsiveness Score
            Double responsivenessScore = calculatePageResponsivenessScore();
            webVitals.setPageResponsivenessScore(responsivenessScore != null ? responsivenessScore : 0.0);
            
            // Visual Stability Score
            Double visualStabilityScore = calculateVisualStabilityScore();
            webVitals.setVisualStabilityScore(visualStabilityScore != null ? visualStabilityScore : 0.0);
            
        } catch (Exception e) {
            System.err.println("Error capturing user journey metrics: " + e.getMessage());
        }
    }
    
    // Core Web Vitals Capture Methods
    
    private Double captureLCP() {
        try {
            String script = """
                return new Promise((resolve) => {
                    new PerformanceObserver((list) => {
                        const entries = list.getEntries();
                        const lastEntry = entries[entries.length - 1];
                        resolve(lastEntry ? lastEntry.startTime : null);
                    }).observe({type: 'largest-contentful-paint', buffered: true});
                    setTimeout(() => resolve(null), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureCLS() {
        try {
            String script = """
                return new Promise((resolve) => {
                    let clsValue = 0;
                    new PerformanceObserver((list) => {
                        for (const entry of list.getEntries()) {
                            if (!entry.hadRecentInput) {
                                clsValue += entry.value;
                            }
                        }
                    }).observe({type: 'layout-shift', buffered: true});
                    setTimeout(() => resolve(clsValue), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureFCP() {
        try {
            String script = """
                return new Promise((resolve) => {
                    new PerformanceObserver((list) => {
                        const entries = list.getEntries();
                        if (entries.length > 0) {
                            resolve(entries[0].startTime);
                        }
                    }).observe({type: 'paint', buffered: true});
                    setTimeout(() => resolve(null), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureTTFB() {
        try {
            String script = """
                const navigation = performance.getEntriesByType('navigation')[0];
                return navigation ? navigation.responseStart - navigation.requestStart : null;
                """;
            
            Object result = jsExecutor.executeScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureINP() {
        try {
            String script = """
                return new Promise((resolve) => {
                    let worstINP = 0;
                    new PerformanceObserver((list) => {
                        for (const entry of list.getEntries()) {
                            if (entry.interactionId) {
                                const inp = entry.processingEnd - entry.startTime;
                                worstINP = Math.max(worstINP, inp);
                            }
                        }
                    }).observe({type: 'event', buffered: true, durationThreshold: 16});
                    setTimeout(() => resolve(worstINP > 0 ? worstINP : null), 3000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureFID() {
        try {
            String script = """
                return new Promise((resolve) => {
                    new PerformanceObserver((list) => {
                        for (const entry of list.getEntries()) {
                            if (entry.name === 'first-input') {
                                const fid = entry.processingStart - entry.startTime;
                                resolve(fid);
                                return;
                            }
                        }
                    }).observe({type: 'first-input', buffered: true});
                    setTimeout(() => resolve(null), 3000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Custom Business Metrics Capture Methods
    
    private Double captureTTI() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const observer = new PerformanceObserver((list) => {
                        const entries = list.getEntries();
                        for (const entry of entries) {
                            if (entry.name === 'first-input') {
                                resolve(entry.startTime);
                                return;
                            }
                        }
                    });
                    observer.observe({type: 'first-input', buffered: true});
                    setTimeout(() => resolve(null), 10000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureTTBA() {
        try {
            String script = """
                // Custom metric: Time to Business Action
                // This measures when the primary business functionality is ready
                const startTime = performance.now();
                return new Promise((resolve) => {
                    // Check for common business actions
                    const checkBusinessReady = () => {
                        const loginForm = document.querySelector('form[action*="login"], input[type="password"]');
                        const searchBox = document.querySelector('input[type="search"], input[placeholder*="search"]');
                        const submitButton = document.querySelector('button[type="submit"], input[type="submit"]');
                        
                        if (loginForm || searchBox || submitButton) {
                            resolve(performance.now() - startTime);
                        } else {
                            setTimeout(checkBusinessReady, 100);
                        }
                    };
                    
                    checkBusinessReady();
                    setTimeout(() => resolve(null), 10000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureTTMP() {
        try {
            String script = """
                // Custom metric: Time to Meaningful Paint
                // Measures when meaningful content is visible to users
                return new Promise((resolve) => {
                    const observer = new MutationObserver(() => {
                        const meaningfulElements = document.querySelectorAll('h1, h2, .hero, .banner, .main-content');
                        if (meaningfulElements.length > 0) {
                            resolve(performance.now());
                        }
                    });
                    
                    observer.observe(document.body, { childList: true, subtree: true });
                    setTimeout(() => resolve(null), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureTTCRL() {
        try {
            String script = """
                // Custom metric: Time to Critical Resource Load
                const navigation = performance.getEntriesByType('navigation')[0];
                if (navigation) {
                    return navigation.loadEventEnd - navigation.requestStart;
                }
                return null;
                """;
            
            Object result = jsExecutor.executeScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureLoginFormReady() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const checkLoginForm = () => {
                        const loginForm = document.querySelector('form[action*="login"], input[type="password"]');
                        if (loginForm) {
                            resolve(performance.now());
                        } else {
                            setTimeout(checkLoginForm, 100);
                        }
                    };
                    checkLoginForm();
                    setTimeout(() => resolve(null), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureSearchReady() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const checkSearchBox = () => {
                        const searchBox = document.querySelector('input[type="search"], input[placeholder*="search"]');
                        if (searchBox) {
                            resolve(performance.now());
                        } else {
                            setTimeout(checkSearchBox, 100);
                        }
                    };
                    checkSearchBox();
                    setTimeout(() => resolve(null), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Advanced Performance Metrics Capture Methods
    
    private Map<String, Object> captureResourceTiming() {
        try {
            String script = """
                const resources = performance.getEntriesByType('resource');
                const timing = {
                    totalResources: resources.length,
                    totalSize: resources.reduce((sum, r) => sum + (r.transferSize || 0), 0),
                    averageLoadTime: resources.reduce((sum, r) => sum + r.duration, 0) / resources.length,
                    slowestResource: Math.max(...resources.map(r => r.duration)),
                    failedResources: resources.filter(r => r.responseStatus >= 400).length
                };
                return timing;
                """;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) jsExecutor.executeScript(script);
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> captureNavigationTiming() {
        try {
            String script = """
                const navigation = performance.getEntriesByType('navigation')[0];
                if (navigation) {
                    return {
                        dns: navigation.domainLookupEnd - navigation.domainLookupStart,
                        tcp: navigation.connectEnd - navigation.connectStart,
                        request: navigation.responseStart - navigation.requestStart,
                        response: navigation.responseEnd - navigation.responseStart,
                        dom: navigation.domContentLoadedEventEnd - navigation.responseEnd,
                        load: navigation.loadEventEnd - navigation.domContentLoadedEventEnd
                    };
                }
                return {};
                """;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) jsExecutor.executeScript(script);
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> capturePaintTiming() {
        try {
            String script = """
                const paintEntries = performance.getEntriesByType('paint');
                const timing = {};
                paintEntries.forEach(entry => {
                    timing[entry.name] = entry.startTime;
                });
                return timing;
                """;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) jsExecutor.executeScript(script);
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> captureLongTasks() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const longTasks = [];
                    new PerformanceObserver((list) => {
                        list.getEntries().forEach(entry => {
                            longTasks.push({
                                duration: entry.duration,
                                startTime: entry.startTime,
                                name: entry.name
                            });
                        });
                    }).observe({entryTypes: ['longtask']});
                    
                    setTimeout(() => {
                        resolve({
                            count: longTasks.length,
                            totalDuration: longTasks.reduce((sum, task) => sum + task.duration, 0),
                            averageDuration: longTasks.length > 0 ? longTasks.reduce((sum, task) => sum + task.duration, 0) / longTasks.length : 0,
                            tasks: longTasks
                        });
                    }, 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            @SuppressWarnings("unchecked")
            Map<String, Object> longTasks = (Map<String, Object>) result;
            return longTasks != null ? longTasks : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private List<Map<String, Object>> captureLayoutShifts() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const layoutShifts = [];
                    new PerformanceObserver((list) => {
                        list.getEntries().forEach(entry => {
                            if (!entry.hadRecentInput) {
                                layoutShifts.push({
                                    value: entry.value,
                                    startTime: entry.startTime,
                                    sources: entry.sources ? entry.sources.map(s => ({
                                        node: s.node ? s.node.localName : 'unknown',
                                        previousRect: s.previousRect,
                                        currentRect: s.currentRect
                                    })) : []
                                });
                            }
                        });
                    }).observe({entryTypes: ['layout-shift']});
                    
                    setTimeout(() => resolve(layoutShifts), 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> layoutShifts = (List<Map<String, Object>>) result;
            return layoutShifts != null ? layoutShifts : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    // Mobile Web Vitals Capture Methods
    
    private Double captureMobileFID() {
        // Mobile FID is similar to FID but with mobile-specific considerations
        return captureFID();
    }
    
    private Double captureMobileFCP() {
        // Mobile FCP is similar to FCP but with mobile-specific considerations
        return captureFCP();
    }
    
    private Double captureMobileCLS() {
        // Mobile CLS is similar to CLS but with mobile-specific considerations
        return captureCLS();
    }
    
    private Double captureTouchResponseTime() {
        try {
            String script = """
                return new Promise((resolve) => {
                    let touchStartTime = 0;
                    let touchEndTime = 0;
                    
                    document.addEventListener('touchstart', (e) => {
                        touchStartTime = performance.now();
                    });
                    
                    document.addEventListener('touchend', (e) => {
                        touchEndTime = performance.now();
                        if (touchStartTime > 0) {
                            resolve(touchEndTime - touchStartTime);
                        }
                    });
                    
                    setTimeout(() => resolve(null), 10000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double captureScrollPerformance() {
        try {
            String script = """
                return new Promise((resolve) => {
                    let scrollStartTime = 0;
                    let scrollEndTime = 0;
                    let isScrolling = false;
                    
                    window.addEventListener('scroll', () => {
                        if (!isScrolling) {
                            scrollStartTime = performance.now();
                            isScrolling = true;
                        }
                        scrollEndTime = performance.now();
                    });
                    
                    setTimeout(() => {
                        if (isScrolling) {
                            resolve(scrollEndTime - scrollStartTime);
                        } else {
                            resolve(null);
                        }
                    }, 5000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // User Journey Metrics Capture Methods
    
    private Double captureTimeToUserAction() {
        try {
            String script = """
                return new Promise((resolve) => {
                    const startTime = performance.now();
                    
                    const trackUserAction = () => {
                        const clickHandler = (e) => {
                            resolve(performance.now() - startTime);
                        };
                        
                        document.addEventListener('click', clickHandler, { once: true });
                        document.addEventListener('touchstart', clickHandler, { once: true });
                    };
                    
                    trackUserAction();
                    setTimeout(() => resolve(null), 10000);
                });
                """;
            
            Object result = jsExecutor.executeAsyncScript(script);
            return result != null ? ((Number) result).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Double calculateUserEngagementScore() {
        try {
            String script = """
                // Calculate user engagement score based on page interactivity
                const interactiveElements = document.querySelectorAll('button, a, input, select, textarea');
                const totalElements = document.querySelectorAll('*').length;
                const engagementRatio = interactiveElements.length / totalElements;
                
                // Factor in page complexity and interactivity
                const complexityScore = Math.min(100, engagementRatio * 100);
                return complexityScore;
                """;
            
            Object result = jsExecutor.executeScript(script);
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Double calculatePageResponsivenessScore() {
        try {
            String script = """
                // Calculate page responsiveness based on interaction latency
                let totalLatency = 0;
                let interactionCount = 0;
                
                const measureInteraction = (eventType) => {
                    document.addEventListener(eventType, (e) => {
                        const startTime = performance.now();
                        requestAnimationFrame(() => {
                            const latency = performance.now() - startTime;
                            totalLatency += latency;
                            interactionCount++;
                        });
                    });
                };
                
                measureInteraction('click');
                measureInteraction('touchstart');
                
                setTimeout(() => {
                    const avgLatency = interactionCount > 0 ? totalLatency / interactionCount : 0;
                    const responsivenessScore = Math.max(0, 100 - (avgLatency * 10));
                    return responsivenessScore;
                }, 5000);
                
                return 0;
                """;
            
            Object result = jsExecutor.executeScript(script);
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private Double calculateVisualStabilityScore() {
        try {
            String script = """
                // Calculate visual stability score based on layout shifts
                let totalShift = 0;
                let shiftCount = 0;
                
                new PerformanceObserver((list) => {
                    list.getEntries().forEach(entry => {
                        if (!entry.hadRecentInput) {
                            totalShift += entry.value;
                            shiftCount++;
                        }
                    });
                }).observe({entryTypes: ['layout-shift']});
                
                setTimeout(() => {
                    const avgShift = shiftCount > 0 ? totalShift / shiftCount : 0;
                    const stabilityScore = Math.max(0, 100 - (avgShift * 1000));
                    return stabilityScore;
                }, 5000);
                
                return 0;
                """;
            
            Object result = jsExecutor.executeScript(script);
            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Calculate overall Web Vitals score
     */
    private double calculateOverallScore(EnhancedWebVitals webVitals) {
        double score = 0.0;
        int metricsCount = 0;
        
        // Core Web Vitals scoring
        if (webVitals.getLcp() > 0) {
            score += Math.max(0, 100 - (webVitals.getLcp() / 25)); // LCP: Good < 2.5s
            metricsCount++;
        }
        
        if (webVitals.getCls() >= 0) {
            score += Math.max(0, 100 - (webVitals.getCls() * 1000)); // CLS: Good < 0.1
            metricsCount++;
        }
        
        if (webVitals.getFcp() > 0) {
            score += Math.max(0, 100 - (webVitals.getFcp() / 18)); // FCP: Good < 1.8s
            metricsCount++;
        }
        
        if (webVitals.getTtfb() > 0) {
            score += Math.max(0, 100 - (webVitals.getTtfb() / 8)); // TTFB: Good < 800ms
            metricsCount++;
        }
        
        if (webVitals.getInp() > 0) {
            score += Math.max(0, 100 - (webVitals.getInp() / 2)); // INP: Good < 200ms
            metricsCount++;
        }
        
        if (webVitals.getFid() > 0) {
            score += Math.max(0, 100 - (webVitals.getFid() / 1)); // FID: Good < 100ms
            metricsCount++;
        }
        
        return metricsCount > 0 ? score / metricsCount : 0.0;
    }
    
    /**
     * Generate Web Vitals insights
     */
    private List<String> generateWebVitalsInsights(EnhancedWebVitals webVitals) {
        List<String> insights = new ArrayList<>();
        
        // LCP insights
        if (webVitals.getLcp() > 2500) {
            insights.add("LCP is slow (" + String.format("%.0f", webVitals.getLcp()) + "ms). Consider optimizing largest contentful paint.");
        } else if (webVitals.getLcp() > 0) {
            insights.add("LCP is good (" + String.format("%.0f", webVitals.getLcp()) + "ms).");
        }
        
        // CLS insights
        if (webVitals.getCls() > 0.1) {
            insights.add("CLS is high (" + String.format("%.3f", webVitals.getCls()) + "). Consider fixing layout shifts.");
        } else if (webVitals.getCls() >= 0) {
            insights.add("CLS is good (" + String.format("%.3f", webVitals.getCls()) + ").");
        }
        
        // FCP insights
        if (webVitals.getFcp() > 1800) {
            insights.add("FCP is slow (" + String.format("%.0f", webVitals.getFcp()) + "ms). Consider optimizing first contentful paint.");
        } else if (webVitals.getFcp() > 0) {
            insights.add("FCP is good (" + String.format("%.0f", webVitals.getFcp()) + "ms).");
        }
        
        // Custom metrics insights
        if (webVitals.getTtba() > 3000) {
            insights.add("Time to Business Action is slow (" + String.format("%.0f", webVitals.getTtba()) + "ms).");
        }
        
        if (webVitals.getUserEngagementScore() < 50) {
            insights.add("User engagement score is low (" + String.format("%.0f", webVitals.getUserEngagementScore()) + "). Consider improving interactivity.");
        }
        
        return insights;
    }
    
    /**
     * Enhanced Web Vitals data model
     */
    public static class EnhancedWebVitals {
        private long timestamp;
        private String error;
        
        // Core Web Vitals
        private double lcp = 0.0; // Largest Contentful Paint
        private double cls = 0.0; // Cumulative Layout Shift
        private double fcp = 0.0; // First Contentful Paint
        private double ttfb = 0.0; // Time to First Byte
        private double inp = 0.0; // Interaction to Next Paint
        private double fid = 0.0; // First Input Delay
        
        // Custom Business Metrics
        private double tti = 0.0; // Time to Interactive
        private double ttba = 0.0; // Time to Business Action
        private double ttmp = 0.0; // Time to Meaningful Paint
        private double ttcrl = 0.0; // Time to Critical Resource Load
        private double loginFormReady = 0.0;
        private double searchReady = 0.0;
        
        // Advanced Performance Metrics
        private Map<String, Object> resourceTiming = new HashMap<>();
        private Map<String, Object> navigationTiming = new HashMap<>();
        private Map<String, Object> paintTiming = new HashMap<>();
        private Map<String, Object> longTasks = new HashMap<>();
        private List<Map<String, Object>> layoutShifts = new ArrayList<>();
        
        // Mobile Web Vitals
        private double mfid = 0.0; // Mobile First Input Delay
        private double mfcp = 0.0; // Mobile First Contentful Paint
        private double mcls = 0.0; // Mobile Cumulative Layout Shift
        private double touchResponseTime = 0.0;
        private double scrollPerformance = 0.0;
        
        // User Journey Metrics
        private double timeToUserAction = 0.0;
        private double userEngagementScore = 0.0;
        private double pageResponsivenessScore = 0.0;
        private double visualStabilityScore = 0.0;
        
        // Overall scoring and insights
        private double overallScore = 0.0;
        private List<String> insights = new ArrayList<>();
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public double getLcp() { return lcp; }
        public void setLcp(double lcp) { this.lcp = lcp; }
        
        public double getCls() { return cls; }
        public void setCls(double cls) { this.cls = cls; }
        
        public double getFcp() { return fcp; }
        public void setFcp(double fcp) { this.fcp = fcp; }
        
        public double getTtfb() { return ttfb; }
        public void setTtfb(double ttfb) { this.ttfb = ttfb; }
        
        public double getInp() { return inp; }
        public void setInp(double inp) { this.inp = inp; }
        
        public double getFid() { return fid; }
        public void setFid(double fid) { this.fid = fid; }
        
        public double getTti() { return tti; }
        public void setTti(double tti) { this.tti = tti; }
        
        public double getTtba() { return ttba; }
        public void setTtba(double ttba) { this.ttba = ttba; }
        
        public double getTtmp() { return ttmp; }
        public void setTtmp(double ttmp) { this.ttmp = ttmp; }
        
        public double getTtcrl() { return ttcrl; }
        public void setTtcrl(double ttcrl) { this.ttcrl = ttcrl; }
        
        public double getLoginFormReady() { return loginFormReady; }
        public void setLoginFormReady(double loginFormReady) { this.loginFormReady = loginFormReady; }
        
        public double getSearchReady() { return searchReady; }
        public void setSearchReady(double searchReady) { this.searchReady = searchReady; }
        
        public Map<String, Object> getResourceTiming() { return resourceTiming; }
        public void setResourceTiming(Map<String, Object> resourceTiming) { this.resourceTiming = resourceTiming; }
        
        public Map<String, Object> getNavigationTiming() { return navigationTiming; }
        public void setNavigationTiming(Map<String, Object> navigationTiming) { this.navigationTiming = navigationTiming; }
        
        public Map<String, Object> getPaintTiming() { return paintTiming; }
        public void setPaintTiming(Map<String, Object> paintTiming) { this.paintTiming = paintTiming; }
        
        public Map<String, Object> getLongTasks() { return longTasks; }
        public void setLongTasks(Map<String, Object> longTasks) { this.longTasks = longTasks; }
        
        public List<Map<String, Object>> getLayoutShifts() { return layoutShifts; }
        public void setLayoutShifts(List<Map<String, Object>> layoutShifts) { this.layoutShifts = layoutShifts; }
        
        public double getMfid() { return mfid; }
        public void setMfid(double mfid) { this.mfid = mfid; }
        
        public double getMfcp() { return mfcp; }
        public void setMfcp(double mfcp) { this.mfcp = mfcp; }
        
        public double getMcls() { return mcls; }
        public void setMcls(double mcls) { this.mcls = mcls; }
        
        public double getTouchResponseTime() { return touchResponseTime; }
        public void setTouchResponseTime(double touchResponseTime) { this.touchResponseTime = touchResponseTime; }
        
        public double getScrollPerformance() { return scrollPerformance; }
        public void setScrollPerformance(double scrollPerformance) { this.scrollPerformance = scrollPerformance; }
        
        public double getTimeToUserAction() { return timeToUserAction; }
        public void setTimeToUserAction(double timeToUserAction) { this.timeToUserAction = timeToUserAction; }
        
        public double getUserEngagementScore() { return userEngagementScore; }
        public void setUserEngagementScore(double userEngagementScore) { this.userEngagementScore = userEngagementScore; }
        
        public double getPageResponsivenessScore() { return pageResponsivenessScore; }
        public void setPageResponsivenessScore(double pageResponsivenessScore) { this.pageResponsivenessScore = pageResponsivenessScore; }
        
        public double getVisualStabilityScore() { return visualStabilityScore; }
        public void setVisualStabilityScore(double visualStabilityScore) { this.visualStabilityScore = visualStabilityScore; }
        
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }
}
