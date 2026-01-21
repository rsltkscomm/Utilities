package reporting;

public class NetworkEntry {
    public String url;
    public String method;
    public int status;
    public String resourceType;
    public long responseTime;
    public String responseBody;

    public NetworkEntry(String url, String method, int status,
                        String resourceType, long responseTime,
                        String responseBody) {
        this.url = url;
        this.method = method;
        this.status = status;
        this.resourceType = resourceType;
        this.responseTime = responseTime;
        this.responseBody = responseBody;
    }
}
