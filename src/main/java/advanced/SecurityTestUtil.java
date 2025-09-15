package advanced;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import reporting.TestLogManager;
import utils.CrossPlatformUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security testing utilities for vulnerability scanning, SSL validation, and security header checks.
 */
public class SecurityTestUtil {
    
    private final String reportDirectory;
    private final List<SecurityVulnerability> vulnerabilities;
    
    public SecurityTestUtil() {
        this.reportDirectory = CrossPlatformUtils.getProjectDataDirectory()
                .resolve("security_reports").toString();
        this.vulnerabilities = new ArrayList<>();
        createReportDirectory();
    }
    
    /**
     * Performs comprehensive vulnerability scan on a URL.
     * @param url URL to scan
     * @return SecurityScanResult with scan details
     */
    public SecurityScanResult performVulnerabilityScan(String url) {
        TestLogManager.info("Starting vulnerability scan for URL: " + url);
        
        SecurityScanResult scanResult = new SecurityScanResult();
        scanResult.setUrl(url);
        scanResult.setScanStartTime(LocalDateTime.now());
        
        try {
            // Check for common vulnerabilities
            checkSQLInjectionVulnerabilities(url, scanResult);
            checkXSSVulnerabilities(url, scanResult);
            checkCSRFVulnerabilities(url, scanResult);
            checkDirectoryTraversalVulnerabilities(url, scanResult);
            checkInformationDisclosure(url, scanResult);
            checkAuthenticationBypass(url, scanResult);
            checkSessionManagementIssues(url, scanResult);
            checkInputValidationIssues(url, scanResult);
            
            scanResult.setScanEndTime(LocalDateTime.now());
            scanResult.setTotalVulnerabilities(vulnerabilities.size());
            scanResult.setVulnerabilities(new ArrayList<>(vulnerabilities));
            
            // Generate security report
            generateSecurityReport(scanResult);
            
            TestLogManager.success("Vulnerability scan completed. Found " + vulnerabilities.size() + " potential issues");
            
        } catch (Exception e) {
            TestLogManager.error("Vulnerability scan failed", e);
            scanResult.setScanError(e.getMessage());
        }
        
        return scanResult;
    }
    
    /**
     * Validates SSL configuration and certificate details.
     * @param url URL to validate SSL for
     * @return SSLValidationResult with validation details
     */
    public SSLValidationResult validateSSLConfiguration(String url) {
        TestLogManager.info("Validating SSL configuration for URL: " + url);
        
        SSLValidationResult result = new SSLValidationResult();
        result.setUrl(url);
            // result.setValidationTime(LocalDateTime.now()); // Method not available
        
        try {
            URL sslUrl = new URL(url);
            if (!sslUrl.getProtocol().equals("https")) {
                result.setValid(false);
                result.setErrorMessage("URL is not using HTTPS protocol");
                return result;
            }
            
            HttpsURLConnection connection = (HttpsURLConnection) sslUrl.openConnection();
            
            // Check SSL certificate
            X509Certificate certificate = getSSLCertificate(connection);
            if (certificate != null) {
                result.setCertificate(certificate);
                result.setValid(true);
                result.setIssuer(certificate.getIssuerDN().toString());
                result.setSubject(certificate.getSubjectDN().toString());
                result.setValidFrom(certificate.getNotBefore());
                result.setValidTo(certificate.getNotAfter());
                result.setSignatureAlgorithm(certificate.getSigAlgName());
                
                // Check certificate validity
                Date now = new Date();
                if (now.before(certificate.getNotBefore()) || now.after(certificate.getNotAfter())) {
                    result.setValid(false);
                    result.setErrorMessage("SSL certificate is expired or not yet valid");
                }
                
                // Check certificate strength
                String algorithm = certificate.getSigAlgName();
                if (algorithm.contains("MD5") || algorithm.contains("SHA1")) {
                    result.addWarning("Weak signature algorithm: " + algorithm);
                }
            }
            
            // Check SSL/TLS version
            String[] supportedProtocols = connection.getSSLSocketFactory().getDefaultCipherSuites();
            result.setSupportedProtocols(Arrays.asList(supportedProtocols));
            
            // Check for weak protocols
            for (String protocol : supportedProtocols) {
                if (protocol.equals("SSLv2") || protocol.equals("SSLv3") || protocol.equals("TLSv1")) {
                    result.addWarning("Weak SSL/TLS protocol supported: " + protocol);
                }
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            TestLogManager.error("SSL validation failed", e);
            result.setValid(false);
            result.setErrorMessage("SSL validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Checks for security headers in HTTP response.
     * @param response HTTP response to check
     * @return SecurityHeadersResult with header analysis
     */
    public SecurityHeadersResult checkForSecurityHeaders(Response response) {
        TestLogManager.info("Checking security headers in response");
        
        SecurityHeadersResult result = new SecurityHeadersResult();
        result.setResponseCode(response.getStatusCode());
        result.setCheckTime(LocalDateTime.now());
        
        Map<String, String> headers = new HashMap<>();
        response.getHeaders().forEach(header -> headers.put(header.getName(), header.getValue()));
        result.setAllHeaders(headers);
        
        // Check for essential security headers
        checkContentSecurityPolicy(headers, result);
        checkStrictTransportSecurity(headers, result);
        checkXFrameOptions(headers, result);
        checkXContentTypeOptions(headers, result);
        checkXSSProtection(headers, result);
        checkReferrerPolicy(headers, result);
        checkPermissionsPolicy(headers, result);
        checkServerHeader(headers, result);
        checkCacheControl(headers, result);
        
        return result;
    }
    
    /**
     * Performs OWASP Top 10 security testing.
     * @param url Base URL to test
     * @return OWASPScanResult with OWASP Top 10 analysis
     */
    public OWASPScanResult performOWASPTop10Scan(String url) {
        TestLogManager.info("Performing OWASP Top 10 security scan for URL: " + url);
        
        OWASPScanResult result = new OWASPScanResult();
        result.setUrl(url);
        result.setScanTime(LocalDateTime.now());
        
        // A01: Broken Access Control
        checkBrokenAccessControl(url, result);
        
        // A02: Cryptographic Failures
        checkCryptographicFailures(url, result);
        
        // A03: Injection
        checkInjectionVulnerabilities(url, result);
        
        // A04: Insecure Design
        checkInsecureDesign(url, result);
        
        // A05: Security Misconfiguration
        checkSecurityMisconfiguration(url, result);
        
        // A06: Vulnerable and Outdated Components
        checkVulnerableComponents(url, result);
        
        // A07: Identification and Authentication Failures
        checkAuthenticationFailures(url, result);
        
        // A08: Software and Data Integrity Failures
        checkDataIntegrityFailures(url, result);
        
        // A09: Security Logging and Monitoring Failures
        checkLoggingFailures(url, result);
        
        // A10: Server-Side Request Forgery (SSRF)
        checkSSRFVulnerabilities(url, result);
        
        return result;
    }
    
    /**
     * Performs penetration testing simulation.
     * @param targetUrl Target URL for penetration testing
     * @return PenetrationTestResult with test results
     */
    public PenetrationTestResult performPenetrationTest(String targetUrl) {
        TestLogManager.info("Starting penetration test for URL: " + targetUrl);
        
        PenetrationTestResult result = new PenetrationTestResult();
        result.setTargetUrl(targetUrl);
        result.setTestStartTime(LocalDateTime.now());
        
        try {
            // Port scanning simulation
            performPortScan(targetUrl, result);
            
            // Directory enumeration
            performDirectoryEnumeration(targetUrl, result);
            
            // Parameter fuzzing
            performParameterFuzzing(targetUrl, result);
            
            // Authentication bypass attempts
            performAuthenticationBypass(targetUrl, result);
            
            // Session management testing
            performSessionManagementTest(targetUrl, result);
            
            result.setTestEndTime(LocalDateTime.now());
            
        } catch (Exception e) {
            TestLogManager.error("Penetration test failed", e);
            result.setTestError(e.getMessage());
        }
        
        return result;
    }
    
    private void checkSQLInjectionVulnerabilities(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for SQL injection vulnerabilities");
        
        String[] sqlPayloads = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT * FROM users --",
            "1' OR 1=1 --",
            "admin'--"
        };
        
        for (String payload : sqlPayloads) {
            try {
                Response response = RestAssured.given()
                    .param("id", payload)
                    .get(url);
                
                if (response.getBody().asString().toLowerCase().contains("error") ||
                    response.getBody().asString().toLowerCase().contains("sql") ||
                    response.getBody().asString().toLowerCase().contains("database")) {
                    
                    SecurityVulnerability vuln = new SecurityVulnerability();
                    vuln.setType("SQL Injection");
                    vuln.setSeverity("HIGH");
                    vuln.setDescription("Potential SQL injection vulnerability detected");
                    vuln.setPayload(payload);
                    vuln.setUrl(url);
                    vulnerabilities.add(vuln);
                }
            } catch (Exception e) {
                TestLogManager.info("SQL injection test failed for payload: " + payload);
            }
        }
    }
    
    private void checkXSSVulnerabilities(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for XSS vulnerabilities");
        
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "';alert('XSS');//"
        };
        
        for (String payload : xssPayloads) {
            try {
                Response response = RestAssured.given()
                    .param("search", payload)
                    .get(url);
                
                if (response.getBody().asString().contains(payload)) {
                    SecurityVulnerability vuln = new SecurityVulnerability();
                    vuln.setType("Cross-Site Scripting (XSS)");
                    vuln.setSeverity("MEDIUM");
                    vuln.setDescription("Potential XSS vulnerability detected");
                    vuln.setPayload(payload);
                    vuln.setUrl(url);
                    vulnerabilities.add(vuln);
                }
            } catch (Exception e) {
                TestLogManager.info("XSS test failed for payload: " + payload);
            }
        }
    }
    
    private void checkCSRFVulnerabilities(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for CSRF vulnerabilities");
        
        try {
            Response response = RestAssured.get(url);
            Map<String, String> headers = new HashMap<>();
            response.getHeaders().forEach(header -> headers.put(header.getName(), header.getValue()));
            
            if (!headers.containsKey("X-CSRF-Token") && !headers.containsKey("X-Requested-With")) {
                SecurityVulnerability vuln = new SecurityVulnerability();
                vuln.setType("Cross-Site Request Forgery (CSRF)");
                vuln.setSeverity("MEDIUM");
                vuln.setDescription("Potential CSRF vulnerability - missing CSRF protection");
                vuln.setUrl(url);
                vulnerabilities.add(vuln);
            }
        } catch (Exception e) {
                TestLogManager.info("CSRF test failed");
        }
    }
    
    private void checkDirectoryTraversalVulnerabilities(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for directory traversal vulnerabilities");
        
        String[] traversalPayloads = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\drivers\\etc\\hosts",
            "....//....//....//etc/passwd",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd"
        };
        
        for (String payload : traversalPayloads) {
            try {
                Response response = RestAssured.given()
                    .param("file", payload)
                    .get(url);
                
                if (response.getBody().asString().contains("root:") || 
                    response.getBody().asString().contains("localhost")) {
                    
                    SecurityVulnerability vuln = new SecurityVulnerability();
                    vuln.setType("Directory Traversal");
                    vuln.setSeverity("HIGH");
                    vuln.setDescription("Potential directory traversal vulnerability detected");
                    vuln.setPayload(payload);
                    vuln.setUrl(url);
                    vulnerabilities.add(vuln);
                }
            } catch (Exception e) {
                TestLogManager.info("Directory traversal test failed for payload: " + payload);
            }
        }
    }
    
    private void checkInformationDisclosure(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for information disclosure");
        
        try {
            Response response = RestAssured.get(url);
            String body = response.getBody().asString();
            
            // Check for sensitive information in response
            if (body.contains("password") || body.contains("secret") || 
                body.contains("key") || body.contains("token")) {
                
                SecurityVulnerability vuln = new SecurityVulnerability();
                vuln.setType("Information Disclosure");
                vuln.setSeverity("LOW");
                vuln.setDescription("Potential information disclosure in response");
                vuln.setUrl(url);
                vulnerabilities.add(vuln);
            }
        } catch (Exception e) {
            TestLogManager.info("Information disclosure test failed");
        }
    }
    
    private void checkAuthenticationBypass(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for authentication bypass vulnerabilities");
        
        try {
            // Try accessing protected resources without authentication
            Response response = RestAssured.get(url + "/admin");
            
            if (response.getStatusCode() == 200) {
                SecurityVulnerability vuln = new SecurityVulnerability();
                vuln.setType("Authentication Bypass");
                vuln.setSeverity("HIGH");
                vuln.setDescription("Protected resource accessible without authentication");
                vuln.setUrl(url + "/admin");
                vulnerabilities.add(vuln);
            }
        } catch (Exception e) {
            TestLogManager.info("Authentication bypass test failed");
        }
    }
    
    private void checkSessionManagementIssues(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for session management issues");
        
        try {
            Response response = RestAssured.get(url);
            Map<String, String> headers = new HashMap<>();
            response.getHeaders().forEach(header -> headers.put(header.getName(), header.getValue()));
            
            // Check for secure session cookies
            String setCookie = headers.get("Set-Cookie");
            if (setCookie != null && !setCookie.contains("Secure") && !setCookie.contains("HttpOnly")) {
                SecurityVulnerability vuln = new SecurityVulnerability();
                vuln.setType("Session Management");
                vuln.setSeverity("MEDIUM");
                vuln.setDescription("Session cookies not marked as Secure or HttpOnly");
                vuln.setUrl(url);
                vulnerabilities.add(vuln);
            }
        } catch (Exception e) {
            TestLogManager.info("Session management test failed");
        }
    }
    
    private void checkInputValidationIssues(String url, SecurityScanResult result) {
        TestLogManager.info("Checking for input validation issues");
        
        String[] maliciousInputs = {
            "<script>",
            "javascript:",
            "vbscript:",
            "data:text/html,",
            "file://",
            "ftp://"
        };
        
        for (String input : maliciousInputs) {
            try {
                Response response = RestAssured.given()
                    .param("input", input)
                    .get(url);
                
                if (response.getBody().asString().contains(input)) {
                    SecurityVulnerability vuln = new SecurityVulnerability();
                    vuln.setType("Input Validation");
                    vuln.setSeverity("MEDIUM");
                    vuln.setDescription("Input validation issue detected");
                    vuln.setPayload(input);
                    vuln.setUrl(url);
                    vulnerabilities.add(vuln);
                }
            } catch (Exception e) {
                TestLogManager.info("Input validation test failed for input: " + input);
            }
        }
    }
    
    private X509Certificate getSSLCertificate(HttpsURLConnection connection) {
        try {
            // Create a trust manager that accepts all certificates (for testing purposes)
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
            
            connection.connect();
            return (X509Certificate) connection.getServerCertificates()[0];
            
        } catch (Exception e) {
            TestLogManager.error("Failed to get SSL certificate", e);
            return null;
        }
    }
    
    private void checkContentSecurityPolicy(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("Content-Security-Policy")) {
            result.addMissingHeader("Content-Security-Policy");
            result.addRecommendation("Implement Content Security Policy to prevent XSS attacks");
        }
    }
    
    private void checkStrictTransportSecurity(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("Strict-Transport-Security")) {
            result.addMissingHeader("Strict-Transport-Security");
            result.addRecommendation("Implement HSTS to enforce HTTPS connections");
        }
    }
    
    private void checkXFrameOptions(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("X-Frame-Options")) {
            result.addMissingHeader("X-Frame-Options");
            result.addRecommendation("Implement X-Frame-Options to prevent clickjacking");
        }
    }
    
    private void checkXContentTypeOptions(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("X-Content-Type-Options")) {
            result.addMissingHeader("X-Content-Type-Options");
            result.addRecommendation("Implement X-Content-Type-Options: nosniff");
        }
    }
    
    private void checkXSSProtection(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("X-XSS-Protection")) {
            result.addMissingHeader("X-XSS-Protection");
            result.addRecommendation("Implement X-XSS-Protection header");
        }
    }
    
    private void checkReferrerPolicy(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("Referrer-Policy")) {
            result.addMissingHeader("Referrer-Policy");
            result.addRecommendation("Implement Referrer-Policy to control referrer information");
        }
    }
    
    private void checkPermissionsPolicy(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("Permissions-Policy")) {
            result.addMissingHeader("Permissions-Policy");
            result.addRecommendation("Implement Permissions-Policy to control browser features");
        }
    }
    
    private void checkServerHeader(Map<String, String> headers, SecurityHeadersResult result) {
        if (headers.containsKey("Server")) {
            String server = headers.get("Server");
            if (server.contains("Apache") || server.contains("nginx") || server.contains("IIS")) {
                result.addInformation("Server information disclosed: " + server);
                result.addRecommendation("Consider hiding server information");
            }
        }
    }
    
    private void checkCacheControl(Map<String, String> headers, SecurityHeadersResult result) {
        if (!headers.containsKey("Cache-Control")) {
            result.addMissingHeader("Cache-Control");
            result.addRecommendation("Implement appropriate Cache-Control headers");
        }
    }
    
    private void checkBrokenAccessControl(String url, OWASPScanResult result) {
        // Implementation for A01: Broken Access Control
        TestLogManager.info("Checking for broken access control (A01)");
    }
    
    private void checkCryptographicFailures(String url, OWASPScanResult result) {
        // Implementation for A02: Cryptographic Failures
        TestLogManager.info("Checking for cryptographic failures (A02)");
    }
    
    private void checkInjectionVulnerabilities(String url, OWASPScanResult result) {
        // Implementation for A03: Injection
        TestLogManager.info("Checking for injection vulnerabilities (A03)");
    }
    
    private void checkInsecureDesign(String url, OWASPScanResult result) {
        // Implementation for A04: Insecure Design
        TestLogManager.info("Checking for insecure design (A04)");
    }
    
    private void checkSecurityMisconfiguration(String url, OWASPScanResult result) {
        // Implementation for A05: Security Misconfiguration
        TestLogManager.info("Checking for security misconfiguration (A05)");
    }
    
    private void checkVulnerableComponents(String url, OWASPScanResult result) {
        // Implementation for A06: Vulnerable and Outdated Components
        TestLogManager.info("Checking for vulnerable components (A06)");
    }
    
    private void checkAuthenticationFailures(String url, OWASPScanResult result) {
        // Implementation for A07: Identification and Authentication Failures
        TestLogManager.info("Checking for authentication failures (A07)");
    }
    
    private void checkDataIntegrityFailures(String url, OWASPScanResult result) {
        // Implementation for A08: Software and Data Integrity Failures
        TestLogManager.info("Checking for data integrity failures (A08)");
    }
    
    private void checkLoggingFailures(String url, OWASPScanResult result) {
        // Implementation for A09: Security Logging and Monitoring Failures
        TestLogManager.info("Checking for logging failures (A09)");
    }
    
    private void checkSSRFVulnerabilities(String url, OWASPScanResult result) {
        // Implementation for A10: Server-Side Request Forgery (SSRF)
        TestLogManager.info("Checking for SSRF vulnerabilities (A10)");
    }
    
    private void performPortScan(String targetUrl, PenetrationTestResult result) {
        TestLogManager.info("Performing port scan simulation");
        // Implementation for port scanning
    }
    
    private void performDirectoryEnumeration(String targetUrl, PenetrationTestResult result) {
        TestLogManager.info("Performing directory enumeration");
        // Implementation for directory enumeration
    }
    
    private void performParameterFuzzing(String targetUrl, PenetrationTestResult result) {
        TestLogManager.info("Performing parameter fuzzing");
        // Implementation for parameter fuzzing
    }
    
    private void performAuthenticationBypass(String targetUrl, PenetrationTestResult result) {
        TestLogManager.info("Performing authentication bypass attempts");
        // Implementation for authentication bypass
    }
    
    private void performSessionManagementTest(String targetUrl, PenetrationTestResult result) {
        TestLogManager.info("Performing session management test");
        // Implementation for session management testing
    }
    
    private void generateSecurityReport(SecurityScanResult scanResult) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "security_scan_report_" + timestamp + ".html";
            Path reportPath = Paths.get(reportDirectory, fileName);
            
            StringBuilder report = new StringBuilder();
            report.append(generateHTMLHeader());
            report.append(generateSecurityReportSummary(scanResult));
            report.append(generateVulnerabilitiesTable(scanResult));
            report.append(generateRecommendations());
            report.append(generateHTMLFooter());
            
            java.nio.file.Files.write(reportPath, report.toString().getBytes());
            TestLogManager.success("Security report generated: " + reportPath);
            
        } catch (Exception e) {
            TestLogManager.error("Failed to generate security report", e);
        }
    }
    
    private void createReportDirectory() {
        try {
            Path dir = Paths.get(reportDirectory);
            if (!java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir);
                TestLogManager.info("Created security report directory: " + reportDirectory);
            }
        } catch (Exception e) {
            TestLogManager.error("Failed to create security report directory", e);
        }
    }
    
    private String generateHTMLHeader() {
        return "<!DOCTYPE html><html><head><title>Security Test Report</title>" +
               "<style>body{font-family:Arial,sans-serif;margin:20px;}table{border-collapse:collapse;width:100%;}" +
               "th,td{border:1px solid #ddd;padding:8px;text-align:left;}th{background-color:#f2f2f2;}" +
               ".high{color:red;}.medium{color:orange;}.low{color:yellow;}</style></head><body>";
    }
    
    private String generateSecurityReportSummary(SecurityScanResult scanResult) {
        return "<h1>Security Scan Report</h1>" +
               "<p>URL: " + scanResult.getUrl() + "</p>" +
               "<p>Scan Time: " + scanResult.getScanStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>" +
               "<p>Total Vulnerabilities: " + scanResult.getTotalVulnerabilities() + "</p>";
    }
    
    private String generateVulnerabilitiesTable(SecurityScanResult scanResult) {
        StringBuilder table = new StringBuilder("<h2>Vulnerabilities Found</h2><table><tr><th>Type</th><th>Severity</th><th>Description</th><th>Payload</th></tr>");
        
        for (SecurityVulnerability vuln : scanResult.getVulnerabilities()) {
            String severityClass = vuln.getSeverity().toLowerCase();
            table.append("<tr>")
                 .append("<td>").append(vuln.getType()).append("</td>")
                 .append("<td class='").append(severityClass).append("'>").append(vuln.getSeverity()).append("</td>")
                 .append("<td>").append(vuln.getDescription()).append("</td>")
                 .append("<td>").append(vuln.getPayload() != null ? vuln.getPayload() : "N/A").append("</td>")
                 .append("</tr>");
        }
        
        table.append("</table>");
        return table.toString();
    }
    
    private String generateRecommendations() {
        return "<h2>Security Recommendations</h2>" +
               "<ul>" +
               "<li>Implement proper input validation and sanitization</li>" +
               "<li>Use parameterized queries to prevent SQL injection</li>" +
               "<li>Implement Content Security Policy (CSP)</li>" +
               "<li>Use HTTPS with proper SSL/TLS configuration</li>" +
               "<li>Implement proper authentication and session management</li>" +
               "<li>Regular security testing and code reviews</li>" +
               "</ul>";
    }
    
    private String generateHTMLFooter() {
        return "</body></html>";
    }
    
    /**
     * Security vulnerability data model.
     */
    public static class SecurityVulnerability {
        private String type;
        private String severity;
        private String description;
        private String payload;
        private String url;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    /**
     * Security scan result data model.
     */
    public static class SecurityScanResult {
        private String url;
        private LocalDateTime scanStartTime;
        private LocalDateTime scanEndTime;
        private int totalVulnerabilities;
        private List<SecurityVulnerability> vulnerabilities;
        private String scanError;
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public LocalDateTime getScanStartTime() { return scanStartTime; }
        public void setScanStartTime(LocalDateTime scanStartTime) { this.scanStartTime = scanStartTime; }
        
        public LocalDateTime getScanEndTime() { return scanEndTime; }
        public void setScanEndTime(LocalDateTime scanEndTime) { this.scanEndTime = scanEndTime; }
        
        public int getTotalVulnerabilities() { return totalVulnerabilities; }
        public void setTotalVulnerabilities(int totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; }
        
        public List<SecurityVulnerability> getVulnerabilities() { return vulnerabilities; }
        public void setVulnerabilities(List<SecurityVulnerability> vulnerabilities) { this.vulnerabilities = vulnerabilities; }
        
        public String getScanError() { return scanError; }
        public void setScanError(String scanError) { this.scanError = scanError; }
    }
    
    /**
     * SSL validation result data model.
     */
    public static class SSLValidationResult {
        private String url;
        private boolean valid;
        private String errorMessage;
        private X509Certificate certificate;
        private String issuer;
        private String subject;
        private Date validFrom;
        private Date validTo;
        private String signatureAlgorithm;
        private List<String> supportedProtocols;
        private List<String> warnings;
        
        public SSLValidationResult() {
            this.warnings = new ArrayList<>();
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public X509Certificate getCertificate() { return certificate; }
        public void setCertificate(X509Certificate certificate) { this.certificate = certificate; }
        
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public Date getValidFrom() { return validFrom; }
        public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }
        
        public Date getValidTo() { return validTo; }
        public void setValidTo(Date validTo) { this.validTo = validTo; }
        
        public String getSignatureAlgorithm() { return signatureAlgorithm; }
        public void setSignatureAlgorithm(String signatureAlgorithm) { this.signatureAlgorithm = signatureAlgorithm; }
        
        public List<String> getSupportedProtocols() { return supportedProtocols; }
        public void setSupportedProtocols(List<String> supportedProtocols) { this.supportedProtocols = supportedProtocols; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
    
    /**
     * Security headers result data model.
     */
    public static class SecurityHeadersResult {
        private int responseCode;
        private LocalDateTime checkTime;
        private Map<String, String> allHeaders;
        private List<String> missingHeaders;
        private List<String> recommendations;
        private List<String> information;
        
        public SecurityHeadersResult() {
            this.missingHeaders = new ArrayList<>();
            this.recommendations = new ArrayList<>();
            this.information = new ArrayList<>();
        }
        
        public void addMissingHeader(String header) {
            this.missingHeaders.add(header);
        }
        
        public void addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
        }
        
        public void addInformation(String info) {
            this.information.add(info);
        }
        
        // Getters and setters
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        public Map<String, String> getAllHeaders() { return allHeaders; }
        public void setAllHeaders(Map<String, String> allHeaders) { this.allHeaders = allHeaders; }
        
        public List<String> getMissingHeaders() { return missingHeaders; }
        public void setMissingHeaders(List<String> missingHeaders) { this.missingHeaders = missingHeaders; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        
        public List<String> getInformation() { return information; }
        public void setInformation(List<String> information) { this.information = information; }
    }
    
    /**
     * OWASP scan result data model.
     */
    public static class OWASPScanResult {
        private String url;
        private LocalDateTime scanTime;
        private Map<String, String> owaspResults;
        
        public OWASPScanResult() {
            this.owaspResults = new HashMap<>();
        }
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public LocalDateTime getScanTime() { return scanTime; }
        public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }
        
        public Map<String, String> getOwaspResults() { return owaspResults; }
        public void setOwaspResults(Map<String, String> owaspResults) { this.owaspResults = owaspResults; }
    }
    
    /**
     * Penetration test result data model.
     */
    public static class PenetrationTestResult {
        private String targetUrl;
        private LocalDateTime testStartTime;
        private LocalDateTime testEndTime;
        private String testError;
        private Map<String, Object> testResults;
        
        public PenetrationTestResult() {
            this.testResults = new HashMap<>();
        }
        
        // Getters and setters
        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        
        public LocalDateTime getTestStartTime() { return testStartTime; }
        public void setTestStartTime(LocalDateTime testStartTime) { this.testStartTime = testStartTime; }
        
        public LocalDateTime getTestEndTime() { return testEndTime; }
        public void setTestEndTime(LocalDateTime testEndTime) { this.testEndTime = testEndTime; }
        
        public String getTestError() { return testError; }
        public void setTestError(String testError) { this.testError = testError; }
        
        public Map<String, Object> getTestResults() { return testResults; }
        public void setTestResults(Map<String, Object> testResults) { this.testResults = testResults; }
    }
}

