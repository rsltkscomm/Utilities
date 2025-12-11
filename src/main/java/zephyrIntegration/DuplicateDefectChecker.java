package zephyrIntegration;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Utility class to check for duplicate defects before creating new ones.
 * Supports multiple detection strategies:
 * 1. Jira Query - Check Jira for existing open bugs
 * 2. Local Cache - Maintain local database of reported defects
 * 3. Hybrid - Combination of both (recommended)
 */
public class DuplicateDefectChecker {

    private static String JIRA_BASE_URL;
    private static String JIRA_EMAIL;
    private static String JIRA_API_KEY;
    private static String PROJECT_KEY;

    private static final String CACHE_FILE_PATH = "./logs/defect_cache.json";
    private static final int DUPLICATE_CHECK_DAYS = 7; // Check for duplicates in last N days

    // Duplicate detection strategy
    public enum DuplicateStrategy {
        JIRA_QUERY,    // Check Jira for existing bugs
        LOCAL_CACHE,   // Check local cache file
        HYBRID         // Check both (recommended)
    }

    private DuplicateStrategy strategy;

    public DuplicateDefectChecker() {
        this(DuplicateStrategy.HYBRID); // Default to hybrid approach
    }

    public DuplicateDefectChecker(DuplicateStrategy strategy) {
        this.strategy = strategy;
        loadConfig();
    }

    /**
     * Load configuration from system properties
     */
    private void loadConfig() {
        try  {
            JIRA_BASE_URL = System.getProperty("JIRA_BASE_URL");
            JIRA_EMAIL = System.getProperty("JIRA_EMAIL");
            JIRA_API_KEY = System.getProperty("JIRA_API_KEY");
            PROJECT_KEY = System.getProperty("PROJECT_KEY");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Jira configuration from system properties", e);
        }
    }

    /**
     * Check if a defect already exists for the given test case and failure
     *
     * @param testCaseKey The Jira test case key (e.g., "RS-T100")
     * @param failureReason The reason for test failure
     * @return DuplicateCheckResult with details
     */
    public DuplicateCheckResult checkForDuplicate(String testCaseKey, String failureReason) {

        System.out.println("\n🔍 Checking for duplicate defects...");
        System.out.println("   Test Case: " + testCaseKey);
        System.out.println("   Strategy: " + strategy);

        switch (strategy) {
            case JIRA_QUERY:
                return checkJiraForDuplicate(testCaseKey, failureReason);

            case LOCAL_CACHE:
                return checkLocalCacheForDuplicate(testCaseKey, failureReason);

            case HYBRID:
                // First check local cache (faster)
                DuplicateCheckResult cacheResult = checkLocalCacheForDuplicate(testCaseKey, failureReason);
                if (cacheResult.isDuplicate()) {
                    return cacheResult;
                }

                // If not in cache, check Jira (authoritative)
                DuplicateCheckResult jiraResult = checkJiraForDuplicate(testCaseKey, failureReason);
                if (jiraResult.isDuplicate()) {
                    // Update cache with Jira result
                    updateLocalCache(testCaseKey, failureReason, jiraResult.getExistingDefectId());
                }
                return jiraResult;

            default:
                return new DuplicateCheckResult(false, null, "Unknown strategy");
        }
    }

    /**
     * Check Jira for existing open bugs related to the test case
     */
    private DuplicateCheckResult checkJiraForDuplicate(String testCaseKey, String failureReason) {
        try {
            System.out.println("   🌐 Querying Jira for existing defects...");

            // Build JQL query to find open bugs for this test case
            String jql = buildJqlQuery(testCaseKey);

            // Execute Jira search
            List<JiraIssue> existingBugs = searchJira(jql);

            if (existingBugs == null || existingBugs.isEmpty()) {
                System.out.println("   ✅ No duplicate defects found in Jira");
                return new DuplicateCheckResult(false, null, "No existing bugs found");
            }

            // Check if any bug has similar failure reason
            for (JiraIssue bug : existingBugs) {
                if (isSimilarFailure(bug.description, failureReason)) {
                    System.out.println("   ⚠️  DUPLICATE FOUND: " + bug.key);
                    System.out.println("   📋 Existing Bug: " + JIRA_BASE_URL + "/browse/" + bug.key);
                    System.out.println("   📅 Created: " + bug.created);
                    System.out.println("   📊 Status: " + bug.status);

                    return new DuplicateCheckResult(true, bug.key,
                            "Duplicate found in Jira: " + bug.key + " (Status: " + bug.status + ")");
                }
            }

            // Found bugs but none are similar
            System.out.println("   ✅ Found " + existingBugs.size() + " bug(s) but none are duplicates");
            return new DuplicateCheckResult(false, null,
                    "Found bugs for test case but different failure types");

        } catch (Exception e) {
            System.err.println("   ⚠️  Failed to check Jira: " + e.getMessage());
            return new DuplicateCheckResult(false, null,
                    "Failed to check Jira: " + e.getMessage());
        }
    }

    /**
     * Check local cache for duplicate defects
     */
    private DuplicateCheckResult checkLocalCacheForDuplicate(String testCaseKey, String failureReason) {
        try {
            System.out.println("   📁 Checking local cache...");

            File cacheFile = new File(CACHE_FILE_PATH);
            if (!cacheFile.exists()) {
                System.out.println("   ✅ No local cache found (first run)");
                return new DuplicateCheckResult(false, null, "No cache file");
            }

            // Read cache file
            String cacheContent = readFile(cacheFile);
            JSONObject cache = new JSONObject(cacheContent);

            // Generate signature for this failure
            String failureSignature = generateFailureSignature(testCaseKey, failureReason);

            // Check if signature exists in cache
            if (cache.has(failureSignature)) {
                JSONObject cachedDefect = cache.getJSONObject(failureSignature);
                String defectId = cachedDefect.getString("defectId");
                String createdDate = cachedDefect.getString("created");

                // Check if defect is recent (within duplicate check window)
                if (isRecentDefect(createdDate)) {
                    System.out.println("   ⚠️  DUPLICATE FOUND IN CACHE: " + defectId);
                    System.out.println("   📅 Original Report Date: " + createdDate);

                    return new DuplicateCheckResult(true, defectId,
                            "Duplicate found in cache: " + defectId + " (Created: " + createdDate + ")");
                } else {
                    System.out.println("   ℹ️  Found old defect (older than " + DUPLICATE_CHECK_DAYS + " days), will create new one");
                    return new DuplicateCheckResult(false, null, "Old defect found but expired");
                }
            }

            System.out.println("   ✅ No duplicate found in cache");
            return new DuplicateCheckResult(false, null, "Not in cache");

        } catch (Exception e) {
            System.err.println("   ⚠️  Failed to check cache: " + e.getMessage());
            return new DuplicateCheckResult(false, null, "Failed to check cache: " + e.getMessage());
        }
    }

    /**
     * Update local cache with new defect information
     */
    public void updateLocalCache(String testCaseKey, String failureReason, String defectId) {
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            cacheFile.getParentFile().mkdirs();

            JSONObject cache;
            if (cacheFile.exists()) {
                String cacheContent = readFile(cacheFile);
                cache = new JSONObject(cacheContent);
            } else {
                cache = new JSONObject();
            }

            // Generate signature
            String failureSignature = generateFailureSignature(testCaseKey, failureReason);

            // Add to cache
            JSONObject defectInfo = new JSONObject();
            defectInfo.put("defectId", defectId);
            defectInfo.put("testCaseKey", testCaseKey);
            defectInfo.put("failureReason", failureReason);
            defectInfo.put("created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            cache.put(failureSignature, defectInfo);

            // Write to file
            try (FileWriter writer = new FileWriter(cacheFile)) {
                writer.write(cache.toString(2)); // Pretty print with indent
            }

            System.out.println("   ✅ Cache updated with defect: " + defectId);

        } catch (Exception e) {
            System.err.println("   ⚠️  Failed to update cache: " + e.getMessage());
        }
    }

    /**
     * Build JQL query to find existing bugs
     */
    private String buildJqlQuery(String testCaseKey) {
        // Search for bugs that:
        // 1. Are in the same project
        // 2. Have automation-bug label
        // 3. Are not closed/resolved
        // 4. Mention the test case key in summary or description
        // 5. Were created in the last N days

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -DUPLICATE_CHECK_DAYS);
        String startDate = dateFormat.format(cal.getTime());

        return String.format(
                "project = %s AND " +
                        "issuetype = Bug AND " +
                        "labels = automation-bug AND " +
                        "status not in (Closed, Resolved, Done) AND " +
                        "(summary ~ \"%s\" OR description ~ \"%s\") AND " +
                        "created >= \"%s\"",
                PROJECT_KEY, testCaseKey, testCaseKey, startDate
        );
    }

    /**
     * Execute Jira search with JQL (updated for API v3 POST request)
     */
    private List<JiraIssue> searchJira(String jql) throws IOException {
        return searchJira(jql, 50);
    }

    /**
     * Execute Jira search with JQL and explicit maxResults
     */
    private List<JiraIssue> searchJira(String jql, int maxResults) throws IOException {
        String apiUrl = JIRA_BASE_URL + "/rest/api/3/search/jql";

        // Build JSON payload for POST request
        JSONObject payload = new JSONObject();
        payload.put("jql", jql);
        payload.put("maxResults", maxResults <= 0 ? 50 : maxResults); // adjust as needed
        payload.put("fields", new JSONArray()
                .put("key")
                .put("summary")
                .put("description")
                .put("status")
                .put("created"));

        // Open connection
        HttpsURLConnection conn = createJiraConnection(apiUrl, "POST"); // your existing method
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response = readResponse(conn);

        List<JiraIssue> issues = new ArrayList<>();

        if (responseCode == 200) {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray issuesArray = jsonResponse.getJSONArray("issues");

            for (int i = 0; i < issuesArray.length(); i++) {
                JSONObject issue = issuesArray.getJSONObject(i);
                JSONObject fields = issue.getJSONObject("fields");

                JiraIssue jiraIssue = new JiraIssue();
                jiraIssue.key = issue.getString("key");
                jiraIssue.summary = fields.optString("summary", "");
                jiraIssue.description = fields.optString("description", "");
                jiraIssue.status = fields.getJSONObject("status").getString("name");
                jiraIssue.created = fields.getString("created");

                issues.add(jiraIssue);
            }

            System.out.println("   📊 Found " + issues.size() + " existing bug(s)");
        } else {
            System.err.println("❌ Jira search API failed: " + responseCode + " Response: " + response);
        }

        return issues;
    }


    /**
     * Check if two failures are similar
     */
    private boolean isSimilarFailure(String existingDescription, String newFailureReason) {
        if (existingDescription == null || newFailureReason == null) {
            return false;
        }

        // Extract key error information
        String existingError = extractErrorSignature(existingDescription);
        String newError = extractErrorSignature(newFailureReason);

        // Check similarity (can be enhanced with fuzzy matching)
        return existingError.equalsIgnoreCase(newError) ||
                (existingDescription != null && existingDescription.contains(newFailureReason)) ||
                (newFailureReason != null && newFailureReason.contains(existingError));
    }

    /**
     * Extract error signature from failure message
     */
    private String extractErrorSignature(String failureMessage) {
        // Extract exception type (e.g., "ElementNotFoundException")
        String[] parts = failureMessage.split(":");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        return failureMessage.trim();
    }

    /**
     * Generate unique signature for a failure
     */
    private String generateFailureSignature(String testCaseKey, String failureReason) {
        String errorType = extractErrorSignature(failureReason);
        return testCaseKey + "_" + errorType.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Check if defect was created recently
     */
    private boolean isRecentDefect(String createdDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date createdDate = sdf.parse(createdDateStr);

            Calendar cutoffDate = Calendar.getInstance();
            cutoffDate.add(Calendar.DAY_OF_MONTH, -DUPLICATE_CHECK_DAYS);

            return createdDate.after(cutoffDate.getTime());
        } catch (Exception e) {
            return true; // Assume recent if parsing fails
        }
    }

    /**
     * Create Jira API connection
     */
    private HttpsURLConnection createJiraConnection(String apiUrl, String method) throws IOException {
        URL url = new URL(apiUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoOutput(true);

        String auth = JIRA_EMAIL + ":" + JIRA_API_KEY;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        // Set timeouts (configurable from properties). Provide defaults to avoid NPE.
        int connTimeout = Integer.parseInt(System.getProperty("CONNECTION_TIMEOUT_MS", "10000"));
        int readTimeout = Integer.parseInt(System.getProperty("READ_TIMEOUT_MS", "10000"));
        conn.setConnectTimeout(connTimeout);
        conn.setReadTimeout(readTimeout);

        return conn;
    }

    /**
     * Read response from HTTP connection
     */
    private String readResponse(HttpsURLConnection conn) throws IOException {
        InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Read file contents
     */
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    /**
     * Clear old entries from cache
     */
    public void cleanupCache() {
        try {
            File cacheFile = new File(CACHE_FILE_PATH);
            if (!cacheFile.exists()) {
                return;
            }

            String cacheContent = readFile(cacheFile);
            JSONObject cache = new JSONObject(cacheContent);
            JSONObject cleanedCache = new JSONObject();

            int removedCount = 0;
            for (String key : cache.keySet()) {
                JSONObject defect = cache.getJSONObject(key);
                String createdDate = defect.getString("created");

                if (isRecentDefect(createdDate)) {
                    cleanedCache.put(key, defect);
                } else {
                    removedCount++;
                }
            }

            // Write cleaned cache
            try (FileWriter writer = new FileWriter(cacheFile)) {
                writer.write(cleanedCache.toString(2));
            }

            if (removedCount > 0) {
                System.out.println("🧹 Cleaned up " + removedCount + " old cache entries");
            }

        } catch (Exception e) {
            System.err.println("⚠️  Failed to cleanup cache: " + e.getMessage());
        }
    }

    /**
     * Inner class to represent Jira issue
     */
    private static class JiraIssue {
        String key;
        String summary;
        String description;
        String status;
        String created;
    }

    /**
     * Result of duplicate check
     */
    public static class DuplicateCheckResult {
        private final boolean isDuplicate;
        private final String existingDefectId;
        private final String message;

        public DuplicateCheckResult(boolean isDuplicate, String existingDefectId, String message) {
            this.isDuplicate = isDuplicate;
            this.existingDefectId = existingDefectId;
            this.message = message;
        }

        public boolean isDuplicate() {
            return isDuplicate;
        }

        public String getExistingDefectId() {
            return existingDefectId;
        }

        public String getMessage() {
            return message;
        }

        public String getJiraUrl(String baseUrl) {
            if (existingDefectId != null) {
                return baseUrl + "/browse/" + existingDefectId;
            }
            return null;
        }
    }

    /**
     * Try to find an existing Jira bug that matches the failure reason.
     * Returns the top-most matching issue key (e.g. "PROJ-123") or null if nothing found.
     *
     * @param failureReason the text to search (short exception message / failure summary)
     * @param maxResults maximum issues to return from Jira search (use 1 for fastest)
     */
    public String findBugKeyByFailure(String failureReason, int maxResults) {
        if (failureReason == null || failureReason.trim().isEmpty()) return null;

        try {
            // 1) sanitize input for JQL (escape backslashes and double quotes)
            String cleaned = failureReason.replace("\\", "\\\\").replace("\"", "\\\"");
            if (cleaned.length() > 400) cleaned = cleaned.substring(0, 400);

            // Helper: limit results safely
            Function<List<JiraIssue>, List<JiraIssue>> limit = (list) -> {
                if (list == null) return Collections.emptyList();
                if (maxResults <= 0 || list.size() <= maxResults) return list;
                return list.subList(0, maxResults);
            };

            // Helper: normalization function
            Function<String, String> normalize = (s) -> {
                if (s == null) return "";
                return s.toLowerCase().replaceAll("[^a-z0-9]", "");
            };

            // Helper: strong client-side matcher that normalizes and compares
            BiPredicate<String, String> strongMatch = (existingText, newFailure) -> {
                if (existingText == null || newFailure == null) return false;
                try {
                    if (isSimilarFailure(existingText, newFailure)) return true;
                } catch (Throwable ignored) {}
                String a = normalize.apply(existingText);
                String b = normalize.apply(newFailure);
                return a.equals(b) || a.contains(b) || b.contains(a);
            };

            // 2) FIRST: try phrase search (closest to exact)
            String phraseJql = String.format(
                    "project = %s AND issuetype = Bug AND status not in (Closed, Resolved, Done) " +
                            "AND (summary ~ \"\\\"%s\\\"\" OR description ~ \"\\\"%s\\\"\") ORDER BY created DESC",
                    PROJECT_KEY, cleaned, cleaned
            );

            System.out.println("[DuplicateChecker] Phrase JQL: " + phraseJql);
            List<JiraIssue> phraseCandidates = limit.apply(searchJira(phraseJql, Math.max(1, maxResults)));
            System.out.println("[DuplicateChecker] Phrase candidates: " + (phraseCandidates == null ? 0 : phraseCandidates.size()));

            if (phraseCandidates != null && !phraseCandidates.isEmpty()) {
                for (JiraIssue c : phraseCandidates) {
                    // check summary and description for a strong match
                    if (strongMatch.test(c.summary, failureReason) || strongMatch.test(c.description, failureReason)) {
                        System.out.println("[DuplicateChecker] Phrase-match found: " + c.key);
                        return c.key;
                    }
                }
                // If none of the phrase candidates matched strongly, continue to fuzzy fallback
                System.out.println("[DuplicateChecker] No strong match found among phrase candidates, falling back to fuzzy search");
            }

            // 3) SECOND: broader fuzzy search (tokenized) but then filter locally
            String fuzzyJql = String.format(
                    "project = %s AND issuetype = Bug AND status not in (Closed, Resolved, Done) " +
                            "AND (summary ~ \"%s\" OR description ~ \"%s\") ORDER BY created DESC",
                    PROJECT_KEY, cleaned, cleaned
            );

            System.out.println("[DuplicateChecker] Fuzzy JQL: " + fuzzyJql);
            List<JiraIssue> fuzzyCandidates = limit.apply(searchJira(fuzzyJql, Math.max(5, maxResults)));
            System.out.println("[DuplicateChecker] Fuzzy candidates: " + (fuzzyCandidates == null ? 0 : fuzzyCandidates.size()));

            if (fuzzyCandidates != null && !fuzzyCandidates.isEmpty()) {
                // Try to find the best candidate by applying strongMatch
                for (JiraIssue c : fuzzyCandidates) {
                    if (strongMatch.test(c.summary, failureReason) || strongMatch.test(c.description, failureReason)) {
                        System.out.println("[DuplicateChecker] Fuzzy-match found: " + c.key);
                        return c.key;
                    }
                }
            }

            // Nothing found
            System.out.println("[DuplicateChecker] No matching bug found for failure: " + failureReason);
        } catch (Exception e) {
            System.err.println("⚠️  findBugKeyByFailure failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
