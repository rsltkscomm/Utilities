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

            long responseTime = System.currentTimeMillis() - startTime;
            String responseBody = "";

            try {
                if ("xhr".equals(request.resourceType()) ||
                    "fetch".equals(request.resourceType())) {
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
                    responseBody
                )
            );
        });
    }

    public void startTransaction(String pageName) {
        currentTransaction = new PageTransaction(pageName);
        currentTransaction.pageResponseTime = System.currentTimeMillis();
    }

    public PageTransaction endTransaction() {
        currentTransaction.pageResponseTime =
                System.currentTimeMillis() - currentTransaction.pageResponseTime;
        return currentTransaction;
    }
}
