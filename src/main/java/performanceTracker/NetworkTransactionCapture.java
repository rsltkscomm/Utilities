package performanceTracker;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JS-based API transaction capture (Fetch/XHR) with same-origin response constraints.
 * Avoids DevTools dependency; works headless. Captures headers/payload when accessible,
 * and response bodies where CORS permits. Size-limited and configurable.
 */
public class NetworkTransactionCapture {

    public static class ApiTransaction {
        public String requestId;
        public String url;
        public String path;
        public String method;
        public Map<String, Object> requestHeaders;
        public String requestBody;
        public int status;
        public Map<String, Object> responseHeaders;
        public String responseBody;
        public String mimeType;
        public long timestamp;
        public long durationMs;
    }

    private final ConfigurationManager config;
    private final WebDriver driver;
    private boolean initialized;
    private final List<ApiTransaction> buffer = new ArrayList<>();

    public NetworkTransactionCapture(WebDriver driver) {
        this.driver = driver;
        this.config = ConfigurationManager.getInstance();
        init();
    }

    private void init() {
        if (!config.isCaptureApiDetailsEnabled()) return;
        if (!(driver instanceof JavascriptExecutor)) return;
        if (initialized) return;
        initialized = true;

        String script = "(function(){\n" +
                "if(window.__apiTxInstalled){return;}\n" +
                "window.__apiTxInstalled=true;\n" +
                "window.__apiTx = [];\n" +
                "let __id=0;\n" +
                "// Wrap fetch\n" +
                "const __origFetch = window.fetch;\n" +
                "window.fetch = function(input, init){\n" +
                "  const start = performance.now();\n" +
                "  const id = (++__id)+'';\n" +
                "  const url = (typeof input === 'string') ? input : (input && input.url) ? input.url : '' ;\n" +
                "  const method = (init && init.method) ? init.method : 'GET';\n" +
                "  const reqHeaders = (init && init.headers) ? init.headers : {};\n" +
                "  const body = (init && init.body) ? init.body : null;\n" +
                "  return __origFetch.apply(this, arguments).then(function(resp){\n" +
                "    const end = performance.now();\n" +
                "    const clone = resp.clone();\n" +
                "    let headersObj = {};\n" +
                "    try{ clone.headers && clone.headers.forEach((v,k)=>{headersObj[k]=v}); }catch(e){}\n" +
                "    return clone.text().then(function(txt){\n" +
                "      window.__apiTx.push({\n" +
                "        requestId: id,\n" +
                "        url: url,\n" +
                "        path: (function(){try{var u=new URL(url);return u.pathname;}catch(e){return url}})(),\n" +
                "        method: method,\n" +
                "        requestHeaders: reqHeaders,\n" +
                "        requestBody: body,\n" +
                "        status: resp.status,\n" +
                "        responseHeaders: headersObj,\n" +
                "        responseBody: txt,\n" +
                "        mimeType: (resp.headers && resp.headers.get && resp.headers.get('content-type'))||'',\n" +
                "        timestamp: Date.now(),\n" +
                "        durationMs: (end - start)\n" +
                "      });\n" +
                "      return resp;\n" +
                "    });\n" +
                "  });\n" +
                "};\n" +
                "// Wrap XHR\n" +
                "(function(){\n" +
                "  const open = XMLHttpRequest.prototype.open;\n" +
                "  const send = XMLHttpRequest.prototype.send;\n" +
                "  XMLHttpRequest.prototype.open = function(method, url){\n" +
                "    this.__id=(++__id)+''; this.__method=method; this.__url=url;\n" +
                "    open.apply(this, arguments);\n" +
                "  };\n" +
                "  XMLHttpRequest.prototype.send = function(body){\n" +
                "    const start = performance.now();\n" +
                "    const xhr=this;\n" +
                "    xhr.addEventListener('loadend', function(){\n" +
                "      const end = performance.now();\n" +
                "      let headersObj={};\n" +
                "      try{ var raw=xhr.getAllResponseHeaders(); raw.split(/\r?\n/).forEach(function(l){if(!l) return; var p=l.split(': '); headersObj[p[0]]=p.slice(1).join(': ')});}catch(e){}\n" +
                "      window.__apiTx.push({\n" +
                "        requestId: xhr.__id,\n" +
                "        url: xhr.__url,\n" +
                "        path: (function(){try{var u=new URL(xhr.__url);return u.pathname;}catch(e){return xhr.__url}})(),\n" +
                "        method: xhr.__method,\n" +
                "        requestHeaders: {},\n" +
                "        requestBody: body||null,\n" +
                "        status: xhr.status||0,\n" +
                "        responseHeaders: headersObj,\n" +
                "        responseBody: (xhr.responseText||''),\n" +
                "        mimeType: (xhr.getResponseHeader && xhr.getResponseHeader('content-type'))||'',\n" +
                "        timestamp: Date.now(),\n" +
                "        durationMs: (end - start)\n" +
                "      });\n" +
                "    });\n" +
                "    send.apply(this, arguments);\n" +
                "  };\n" +
                "})();\n" +
                "})();";

        ((JavascriptExecutor) driver).executeScript(script);
    }

    public List<ApiTransaction> getTransactions() {
        if (!config.isCaptureApiDetailsEnabled()) return Collections.emptyList();
        if (!(driver instanceof JavascriptExecutor)) return Collections.emptyList();
        try {
            Object res = ((JavascriptExecutor) driver).executeScript("return window.__apiTx || [];");
            List<ApiTransaction> out = new ArrayList<>();
            if (res instanceof List) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> list = (List<Map<String, Object>>) res;
                int maxBytes = Math.max(1, config.getApiBodyMaxKb() * 1024);
                for (Map<String, Object> m : list) {
                    ApiTransaction tx = new ApiTransaction();
                    tx.requestId = String.valueOf(m.get("requestId"));
                    tx.url = String.valueOf(m.get("url"));
                    tx.path = String.valueOf(m.get("path"));
                    tx.method = String.valueOf(m.get("method"));
                    tx.requestHeaders = asMap(m.get("requestHeaders"));
                    tx.requestBody = limitSize(asString(m.get("requestBody")), maxBytes);
                    tx.status = parseInt(m.get("status"));
                    tx.responseHeaders = asMap(m.get("responseHeaders"));
                    tx.responseBody = limitSize(asString(m.get("responseBody")), maxBytes);
                    tx.mimeType = String.valueOf(m.get("mimeType"));
                    tx.timestamp = parseLong(m.get("timestamp"));
                    tx.durationMs = parseLong(m.get("durationMs"));
                    out.add(tx);
                }
            }
            buffer.clear();
            buffer.addAll(out);
            return new ArrayList<>(buffer);
        } catch (Exception e) {
            return new ArrayList<>(buffer);
        }
    }

    private Map<String, Object> asMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        return new LinkedHashMap<>();
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private int parseInt(Object o) { try { return (o==null)?0:Integer.parseInt(String.valueOf(o)); } catch(Exception e){ return 0; } }
    private long parseLong(Object o) { try { return (o==null)?0L:Long.parseLong(String.valueOf(o)); } catch(Exception e){ return 0L; } }

    private String limitSize(String s, int maxBytes) {
        if (s == null) return null;
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        if (data.length <= maxBytes) return s;
        return new String(Arrays.copyOfRange(data, 0, maxBytes), StandardCharsets.UTF_8) + "\n/* truncated */";
    }
}


