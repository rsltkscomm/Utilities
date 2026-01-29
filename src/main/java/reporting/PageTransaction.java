package reporting;

import java.util.ArrayList;
import java.util.List;

public class PageTransaction {
    public String pageName;
    public long pageResponseTime;
    public long transactionStartTime;
    public List<NetworkEntry> networkEntries = new ArrayList<>();

    public PageTransaction(String pageName) {
        this.pageName = pageName;
    }
}
