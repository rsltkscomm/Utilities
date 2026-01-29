package reporting;

import com.microsoft.playwright.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionPerformanceTracker {

    private final Page page;
    private final Map<Request, Long> requestStartTimes = new ConcurrentHashMap<>();
    private PageTransaction currentTransaction;

    public TransactionPerformanceTracker(Page page) {
        this.page = page;

        page.onRequest(request ->
                requestStartTimes.put(request, System.currentTimeMillis())
        );

        page.onResponse(response -> {
            if (currentTransaction == null) return;

            Request request = response.request();
            Long startTime = requestStartTimes.get(request);
            if (startTime == null) return;

            if (startTime < currentTransaction.transactionStartTime) return;

            long responseTime = System.currentTimeMillis() - startTime;

            String responseBody = "";
            String requestPayload = "";

            try {
                if ("xhr".equals(request.resourceType()) ||
                    "fetch".equals(request.resourceType())) {

                    // ✅ REQUEST PAYLOAD
                    requestPayload = request.postData();
                    if (requestPayload == null) {
                        requestPayload = "";
                    }

                    // ✅ RESPONSE BODY
                    responseBody = response.text();
                }
            } catch (Exception e) {
                responseBody = "Unable to capture response body";
            }

            currentTransaction.networkEntries.add(
                new NetworkEntry(
                    request.url(),
                    request.method(),
                    response.status(),
                    request.resourceType(),
                    responseTime,
                    requestPayload,   // 👈 NEW
                    responseBody
                )
            );
        });

    }

    public void startTransaction(String pageName) {
        currentTransaction = new PageTransaction(pageName);

        // ✅ mark exact transaction start
        currentTransaction.transactionStartTime = System.currentTimeMillis();
        currentTransaction.pageResponseTime = currentTransaction.transactionStartTime;

        // ✅ clear old request cache
        requestStartTimes.clear();
    }

    public PageTransaction endTransaction() {
        currentTransaction.pageResponseTime =
                System.currentTimeMillis() - currentTransaction.pageResponseTime;
        return currentTransaction;
    }
}
