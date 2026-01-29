package reporting;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PageLevelHtmlReport
{

	public static String html = "";
	private static final long API_SLA_MS = 3000;
	
	private static String getSlaColor(long responseTime) {
	    return responseTime <= API_SLA_MS
	            ? "#059669"   // ✅ Green
	            : "#dc2626";  // ❌ Red
	}


	public static String generate(List<PageTransaction> pages)
	{
		// Calculate summary statistics
		int totalPages = pages.size();
		long totalTime = pages.stream().mapToLong(p -> p.pageResponseTime).sum();
		long avgTime = totalPages > 0 ? totalTime / totalPages : 0;
		int totalAPIs = pages.stream().mapToInt(p -> p.networkEntries.size()).sum();

		// Generate performance summary cards with blue shades
		StringBuilder summaryCards = new StringBuilder();
		String[] blueColors = { "#1e40af", "#2563eb", "#3b82f6", "#60a5fa" };

		summaryCards.append(String.format("""
				    <div class="summary-grid">
				        <div class="summary-card" style="background: linear-gradient(135deg, %s, %s);">
				            <div class="summary-icon">📊</div>
				            <div class="summary-value">%d</div>
				            <div class="summary-label">Total Pages</div>
				        </div>
				        <div class="summary-card" style="background: linear-gradient(135deg, %s, %s);">
				            <div class="summary-icon">⚡</div>
				            <div class="summary-value">%d ms</div>
				            <div class="summary-label">Avg Response Time</div>
				        </div>
				        <div class="summary-card" style="background: linear-gradient(135deg, %s, %s);">
				            <div class="summary-icon">🔗</div>
				            <div class="summary-value">%d</div>
				            <div class="summary-label">Total API Calls</div>
				        </div>
				        <div class="summary-card" style="background: linear-gradient(135deg, %s, %s);">
				            <div class="summary-icon">⏱️</div>
				            <div class="summary-value">%d ms</div>
				            <div class="summary-label">Total Time</div>
				        </div>
				    </div>
				""", blueColors[0], blueColors[1], totalPages, blueColors[1], blueColors[2], avgTime, blueColors[2], blueColors[3], totalAPIs, blueColors[3], blueColors[0], totalTime));

		// Generate page blocks with enhanced styling
		StringBuilder pageBlocks = new StringBuilder();
		int pageIndex = 1;

		for (PageTransaction page : pages)
		{
			String statusColor = getStatusColor(page.pageResponseTime);
			String progressWidth = Math.min(page.pageResponseTime / 10, 100) + "%";

			pageBlocks.append(String.format("""
					<div class="page-card" data-page-index="%d">
					    <div class="page-header" onclick="togglePageDetails(this)">
					        <div class="page-indicator">%d</div>
					        <div class="page-info">
					            <div class="page-title">
					                <span class="page-name">%s</span>
					                <span class="page-time-badge" style="background: %s">%d ms</span>
					            </div>
					            <div class="progress-bar">
					                <div class="progress-fill" style="width: %s; background: %s"></div>
					            </div>
					            <div class="page-stats">
					                <span class="api-count">📡 %d API calls</span>
					                <span class="toggle-icon">▶</span>
					            </div>
					        </div>
					    </div>
					    <div class="page-details" style="display: none;">
					""", pageIndex, pageIndex, page.pageName, statusColor, page.pageResponseTime, progressWidth, statusColor, page.networkEntries.size()));

			// Add API entries
			int apiIndex = 1;
			for (NetworkEntry api : page.networkEntries)
			{
				String apiStatusColor = getApiStatusColor(api.status);
				String methodColor = getMethodColor(api.method);
				String slaColor = getSlaColor(api.responseTime);

				pageBlocks.append(String.format("""
					    <div class="api-card" style="border-left: 6px solid %s;">
					        <div class="api-header" onclick="toggleApiDetails(this)">
					            <div class="api-method" style="background: %s">%s</div>
					            <div class="api-url">%s</div>
					            <div class="api-status" style="color: %s">%d</div>
					            <div class="api-time" style="color: %s; font-weight: 600;">%d ms</div>
					            <div class="api-toggle">▼</div>
					        </div>

					        <div class="api-details" style="display: none;">

					            <!-- REQUEST PAYLOAD -->
					            <div class="response-section">
					                <div class="section-title">Request Payload</div>
					                <div class="code-block">
					                    <pre><code class="json">%s</code></pre>
					                    <button class="copy-btn" onclick="copyToClipboard(this)">📋 Copy</button>
					                </div>
					            </div>

					            <!-- RESPONSE BODY -->
					            <div class="response-section">
					                <div class="section-title">Response Body</div>
					                <div class="code-block">
					                    <pre><code class="json">%s</code></pre>
					                    <button class="copy-btn" onclick="copyToClipboard(this)">📋 Copy</button>
					                </div>
					            </div>

					            <div class="api-meta">
					                <span class="meta-item">Index: %d</span>
					                <span class="meta-item">Payload: %d chars</span>
					                <span class="meta-item">Response: %d chars</span>
					            </div>
					        </div>
					    </div>
					""",
					    slaColor, 
					    methodColor,
					    api.method,
					    truncateUrl(api.url, 60),
					    apiStatusColor,
					    api.status,
					    slaColor,
					    api.responseTime,

					    // ✅ PAYLOAD
					    formatJson(escapeHtml(api.requestPayload)),

					    // ✅ RESPONSE
					    formatJson(escapeHtml(api.responseBody)),

					    apiIndex,
					    api.requestPayload != null ? api.requestPayload.length() : 0,
					    api.responseBody != null ? api.responseBody.length() : 0
					));
				apiIndex++;
			}

			pageBlocks.append("</div></div>");
			pageIndex++;
		}

		// Build the HTML template with blue and white theme
		html = String.format("""
				            <!DOCTYPE html>
				            <html>
				            <head>
				                <meta charset="UTF-8">
				                <meta name="viewport" content="width=device-width, initial-scale=1.0">
				                <title>📊 Performance Analytics Dashboard</title>
				                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
				                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/atom-one-light.min.css">
				                <style>
				                    :root {
				                        --primary-blue: #1e40af;
				                        --secondary-blue: #3b82f6;
				                        --light-blue: #dbeafe;
				                        --accent-blue: #2563eb;
				                        --success: #059669;
				                        --warning: #d97706;
				                        --error: #dc2626;
				                        --dark-gray: #374151;
				                        --medium-gray: #6b7280;
				                        --light-gray: #f3f4f6;
				                        --white: #ffffff;
				                    }

				                    * {
				                        margin: 0;
				                        padding: 0;
				                        box-sizing: border-box;
				                        font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
				                    }

				                    body {
				                        background: linear-gradient(135deg, #f0f9ff 0%%, #e0f2fe 50%%, #f0f9ff 100%%);
				                        color: #1e293b;
				                        min-height: 100vh;
				                        padding: 20px;
				                        animation: fadeIn 0.8s ease-out;
				                    }

				                    .container {
				                        max-width: 1400px;
				                        margin: 0 auto;
				                        padding: 20px;
				                    }

				                    /* Header with Back Button */
				                    .header-container {
				                        display: flex;
				                        justify-content: space-between;
				                        align-items: center;
				                        margin-bottom: 40px;
				                        animation: slideDown 0.6s ease-out;
				                        background: var(--white);
				                        padding: 25px 30px;
				                        border-radius: 16px;
				                        box-shadow: 0 4px 20px rgba(30, 64, 175, 0.1);
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                    }

				                    .back-btn {
				                        background: linear-gradient(135deg, var(--primary-blue), var(--accent-blue));
				                        color: white;
				                        border: none;
				                        padding: 12px 24px;
				                        border-radius: 8px;
				                        font-weight: 600;
				                        font-size: 0.95rem;
				                        cursor: pointer;
				                        display: flex;
				                        align-items: center;
				                        gap: 8px;
				                        transition: all 0.3s ease;
				                        text-decoration: none;
				                        box-shadow: 0 4px 12px rgba(30, 64, 175, 0.2);
				                    }

				                    .back-btn:hover {
				                        transform: translateY(-2px);
				                        box-shadow: 0 6px 20px rgba(30, 64, 175, 0.3);
				                        background: linear-gradient(135deg, var(--accent-blue), var(--secondary-blue));
				                    }

				                    .back-btn i {
				                        font-size: 1.1rem;
				                    }

				                    .header-content {
				                        text-align: center;
				                        flex: 1;
				                    }

				                    .header-content h1 {
				                        font-size: 2.8rem;
				                        background: linear-gradient(90deg, var(--primary-blue), var(--accent-blue));
				                        -webkit-background-clip: text;
				                        background-clip: text;
				                        color: transparent;
				                        margin-bottom: 10px;
				                        font-weight: 700;
				                    }

				                    .header-content p {
				                        color: var(--medium-gray);
				                        font-size: 1.1rem;
				                        max-width: 600px;
				                        margin: 0 auto 15px;
				                    }

				                    .timestamp {
				                        color: var(--medium-gray);
				                        font-size: 0.9rem;
				                        background: var(--light-gray);
				                        padding: 8px 16px;
				                        border-radius: 20px;
				                        display: inline-block;
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                    }

				                    /* Right side placeholder for future buttons */
				                    .header-actions {
				                        min-width: 140px;
				                        text-align: right;
				                    }

				                    .summary-grid {
				                        display: grid;
				                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
				                        gap: 20px;
				                        margin-bottom: 40px;
				                        animation: fadeInUp 0.8s ease-out 0.2s both;
				                    }

				                    .summary-card {
				                        background: var(--white);
				                        border-radius: 16px;
				                        padding: 25px;
				                        text-align: center;
				                        border: 2px solid var(--light-blue);
				                        transition: all 0.3s ease;
				                        box-shadow: 0 4px 15px rgba(30, 64, 175, 0.08);
				                    }

				                    .summary-card:hover {
				                        transform: translateY(-5px);
				                        box-shadow: 0 8px 25px rgba(30, 64, 175, 0.15);
				                        border-color: var(--accent-blue);
				                    }

				                    .summary-icon {
				                        font-size: 2.5rem;
				                        margin-bottom: 15px;
				                        opacity: 0.9;
				                        color: var(--primary-blue);
				                    }

				                    .summary-value {
				                        font-size: 2.2rem;
				                        font-weight: 700;
				                        margin-bottom: 5px;
				                        color: var(--primary-blue);
				                    }

				                    .summary-label {
				                        font-size: 0.9rem;
				                        color: var(--medium-gray);
				                        text-transform: uppercase;
				                        letter-spacing: 1px;
				                        font-weight: 600;
				                    }

				                    .pages-section {
				                        background: var(--white);
				                        padding: 30px;
				                        border-radius: 16px;
				                        box-shadow: 0 4px 20px rgba(30, 64, 175, 0.1);
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                        margin-bottom: 40px;
				                    }

				                    .pages-section h2 {
				                        color: var(--primary-blue);
				                        font-size: 1.8rem;
				                        margin-bottom: 25px;
				                        padding-bottom: 15px;
				                        border-bottom: 2px solid var(--light-blue);
				                        display: flex;
				                        align-items: center;
				                        gap: 10px;
				                    }

				                    .page-card {
				                        background: var(--white);
				                        border-radius: 12px;
				                        margin-bottom: 20px;
				                        overflow: hidden;
				                        border: 1px solid var(--light-blue);
				                        animation: fadeInUp 0.6s ease-out;
				                        transition: all 0.3s ease;
				                    }

				                    .page-card:hover {
				                        border-color: var(--accent-blue);
				                        box-shadow: 0 6px 20px rgba(30, 64, 175, 0.1);
				                    }

				                    .page-header {
				                        padding: 20px;
				                        cursor: pointer;
				                        display: flex;
				                        align-items: center;
				                        gap: 20px;
				                        transition: background-color 0.3s ease;
				                        background: linear-gradient(to right, #f8fafc, var(--white));
				                    }

				                    .page-header:hover {
				                        background: linear-gradient(to right, #f0f9ff, #e0f2fe);
				                    }

				                    .page-indicator {
				                        width: 40px;
				                        height: 40px;
				                        background: linear-gradient(135deg, var(--primary-blue), var(--accent-blue));
				                        border-radius: 10px;
				                        display: flex;
				                        align-items: center;
				                        justify-content: center;
				                        font-weight: bold;
				                        font-size: 1.2rem;
				                        color: white;
				                        flex-shrink: 0;
				                    }

				                    .page-info {
				                        flex: 1;
				                    }

				                    .page-title {
				                        display: flex;
				                        align-items: center;
				                        gap: 15px;
				                        margin-bottom: 10px;
				                        flex-wrap: wrap;
				                    }

				                    .page-name {
				                        font-size: 1.3rem;
				                        font-weight: 600;
				                        color: var(--dark-gray);
				                    }

				                    .page-time-badge {
				                        padding: 6px 14px;
				                        border-radius: 20px;
				                        font-size: 0.85rem;
				                        font-weight: 600;
				                        color: white;
				                        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
				                    }

				                    .progress-bar {
				                        height: 8px;
				                        background: var(--light-gray);
				                        border-radius: 4px;
				                        overflow: hidden;
				                        margin: 15px 0;
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                    }

				                    .progress-fill {
				                        height: 100%%;
				                        border-radius: 4px;
				                        transition: width 1s ease-out;
				                        box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
				                    }

				                    .page-stats {
				                        display: flex;
				                        justify-content: space-between;
				                        align-items: center;
				                        color: var(--medium-gray);
				                        font-size: 0.9rem;
				                    }

				                    .api-count {
				                        background: var(--light-blue);
				                        padding: 6px 12px;
				                        border-radius: 20px;
				                        color: var(--primary-blue);
				                        font-weight: 500;
				                    }

				                    .toggle-icon {
				                        transition: transform 0.3s ease;
				                        font-size: 0.9rem;
				                        color: var(--primary-blue);
				                        background: var(--light-blue);
				                        width: 28px;
				                        height: 28px;
				                        border-radius: 50%%;
				                        display: flex;
				                        align-items: center;
				                        justify-content: center;
				                    }

				                    .page-details {
				                        padding: 0 20px 20px;
				                        border-top: 1px solid var(--light-blue);
				                    }

				                    .api-card {
				                        background: var(--white);
				                        border-radius: 10px;
				                        margin: 10px 0;
				                        border: 1px solid var(--light-blue);
				                        overflow: hidden;
				                        transition: all 0.3s ease;
				                    }

				                    .api-card:hover {
				                        border-color: var(--accent-blue);
				                        transform: translateX(5px);
				                        box-shadow: 0 4px 12px rgba(30, 64, 175, 0.1);
				                    }

				                    .api-header {
				                        padding: 15px;
				                        display: flex;
				                        align-items: center;
				                        gap: 15px;
				                        cursor: pointer;
				                        flex-wrap: wrap;
				                        background: linear-gradient(to right, #f8fafc, var(--white));
				                    }

				                    .api-method {
				                        padding: 6px 14px;
				                        border-radius: 6px;
				                        font-weight: 600;
				                        font-size: 0.85rem;
				                        color: white;
				                        min-width: 70px;
				                        text-align: center;
				                        text-transform: uppercase;
				                        letter-spacing: 0.5px;
				                        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
				                    }

				                    .api-url {
				                        flex: 1;
				                        font-family: 'Monaco', 'Courier New', monospace;
				                        font-size: 0.9rem;
				                        color: var(--dark-gray);
				                        overflow: hidden;
				                        text-overflow: ellipsis;
				                        white-space: nowrap;
				                        padding: 4px 0;
				                    }

				                    .api-status {
				                        font-weight: 600;
				                        font-size: 0.95rem;
				                        padding: 4px 10px;
				                        border-radius: 4px;
				                        background: var(--light-gray);
				                    }

				                    .api-time {
				                        color: var(--medium-gray);
				                        font-size: 0.85rem;
				                        font-weight: 500;
				                        background: var(--light-gray);
				                        padding: 4px 10px;
				                        border-radius: 4px;
				                    }

				                    .api-toggle {
				                        transition: transform 0.3s ease;
				                        font-size: 0.8rem;
				                        color: var(--primary-blue);
				                        width: 24px;
				                        height: 24px;
				                        border-radius: 50%%;
				                        display: flex;
				                        align-items: center;
				                        justify-content: center;
				                        background: var(--light-blue);
				                    }

				                    .api-details {
				                        padding: 0 15px 15px;
				                        border-top: 1px solid var(--light-blue);
				                    }

				                    .response-section {
				                        margin-top: 15px;
				                    }

				                    .section-title {
				                        font-size: 0.9rem;
				                        color: var(--primary-blue);
				                        margin-bottom: 10px;
				                        text-transform: uppercase;
				                        letter-spacing: 1px;
				                        font-weight: 600;
				                        display: flex;
				                        align-items: center;
				                        gap: 8px;
				                    }

				                    .section-title:before {
				                        content: "📄";
				                        font-size: 1rem;
				                    }

				                    .code-block {
				                        position: relative;
				                        background: var(--light-gray);
				                        border-radius: 8px;
				                        overflow: hidden;
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                    }

				                    .code-block pre {
				                        margin: 0;
				                        padding: 20px;
				                        max-height: 400px;
				                        overflow-y: auto;
				                        font-size: 0.85rem;
				                        line-height: 1.5;
				                        background: var(--white);
				                    }

				                    .code-block code {
				                        font-family: 'Monaco', 'Courier New', monospace;
				                        color: var(--dark-gray);
				                    }

				                    .copy-btn {
				                        position: absolute;
				                        top: 10px;
				                        right: 10px;
				                        background: var(--primary-blue);
				                        color: var(--white);
				                        border: none;
				                        padding: 8px 16px;
				                        border-radius: 6px;
				                        cursor: pointer;
				                        font-size: 0.8rem;
				                        font-weight: 500;
				                        transition: all 0.3s ease;
				                        display: flex;
				                        align-items: center;
				                        gap: 6px;
				                        box-shadow: 0 2px 8px rgba(30, 64, 175, 0.2);
				                    }

				                    .copy-btn:hover {
				                        background: var(--accent-blue);
				                        transform: translateY(-1px);
				                        box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
				                    }

				                    .api-meta {
				                        display: flex;
				                        gap: 20px;
				                        margin-top: 15px;
				                        font-size: 0.85rem;
				                        color: var(--medium-gray);
				                    }

				                    .meta-item {
				                        background: var(--light-gray);
				                        padding: 6px 12px;
				                        border-radius: 6px;
				                        border: 1px solid rgba(30, 64, 175, 0.1);
				                        display: flex;
				                        align-items: center;
				                        gap: 5px;
				                    }

				                    .controls {
				                        position: fixed;
				                        bottom: 30px;
				                        right: 30px;
				                        display: flex;
				                        gap: 12px;
				                        z-index: 1000;
				                    }

				                    .control-btn {
				                        width: 56px;
				                        height: 56px;
				                        border-radius: 50%%;
				                        background: linear-gradient(135deg, var(--primary-blue), var(--accent-blue));
				                        border: none;
				                        color: white;
				                        font-size: 1.3rem;
				                        cursor: pointer;
				                        display: flex;
				                        align-items: center;
				                        justify-content: center;
				                        transition: all 0.3s ease;
				                        box-shadow: 0 6px 20px rgba(30, 64, 175, 0.3);
				                        border: 2px solid white;
				                    }

				                    .control-btn:hover {
				                        transform: scale(1.1) translateY(-3px);
				                        box-shadow: 0 8px 25px rgba(30, 64, 175, 0.4);
				                    }

				                    /* Scrollbar styling */
				                    ::-webkit-scrollbar {
				                        width: 8px;
				                        height: 8px;
				                    }

				                    ::-webkit-scrollbar-track {
				                        background: var(--light-gray);
				                        border-radius: 4px;
				                    }

				                    ::-webkit-scrollbar-thumb {
				                        background: var(--secondary-blue);
				                        border-radius: 4px;
				                    }

				                    ::-webkit-scrollbar-thumb:hover {
				                        background: var(--primary-blue);
				                    }

				                    @keyframes fadeIn {
				                        from { opacity: 0; }
				                        to { opacity: 1; }
				                    }

				                    @keyframes slideDown {
				                        from { transform: translateY(-30px); opacity: 0; }
				                        to { transform: translateY(0); opacity: 1; }
				                    }

				                    @keyframes fadeInUp {
				                        from { transform: translateY(20px); opacity: 0; }
				                        to { transform: translateY(0); opacity: 1; }
				                    }

				                    @media (max-width: 768px) {
				                        .container {
				                            padding: 10px;
				                        }

				                        .header-container {
				                            flex-direction: column;
				                            gap: 20px;
				                            padding: 20px;
				                            text-align: center;
				                        }

				                        .header-content h1 {
				                            font-size: 2rem;
				                        }

				                        .header-actions {
				                            width: 100%%;
				                            text-align: center;
				                        }

				                        .back-btn {
				                            width: 100%%;
				                            justify-content: center;
				                        }

				                        .pages-section {
				                            padding: 20px;
				                        }

				                        .page-header {
				                            flex-direction: column;
				                            gap: 10px;
				                            text-align: center;
				                        }

				                        .page-title {
				                            justify-content: center;
				                        }

				                        .api-header {
				                            flex-direction: column;
				                            align-items: flex-start;
				                            gap: 10px;
				                        }

				                        .api-url {
				                            width: 100%%;
				                        }

				                        .controls {
				                            bottom: 20px;
				                            right: 20px;
				                        }

				                        .control-btn {
				                            width: 48px;
				                            height: 48px;
				                            font-size: 1.1rem;
				                        }
				                    }
				                </style>
				            </head>
				            <body>
				                <div class="container">
				                    <!-- Header with Back Button -->
				                    <div class="header-container">
				                        <div class="header-content">
				                            <h1><i class="fas fa-chart-line"></i> Performance Analytics Dashboard</h1>
				                            <p>Interactive performance report with detailed API call analysis</p>
				                            <div class="timestamp">Generated: %s</div>
				                        </div>

				                        <div class="header-actions">
				                            <!-- Placeholder for future buttons if needed -->
				                        </div>
				                    </div>

				                    %s

				                    <div class="pages-section">
				                        <h2><i class="fas fa-file-alt"></i> Transactions </h2>
				                        %s
				                    </div>
				                </div>

				                <div class="controls">
				                    <button class="control-btn" onclick="expandAll()" title="Expand All">
				                        <i class="fas fa-expand-alt"></i>
				                    </button>
				                    <button class="control-btn" onclick="collapseAll()" title="Collapse All">
				                        <i class="fas fa-compress-alt"></i>
				                    </button>
				                    <button class="control-btn" onclick="scrollToTop()" title="Scroll to Top">
				                        <i class="fas fa-arrow-up"></i>
				                    </button>
				                    <button class="control-btn" onclick="printReport()" title="Print Report">
				                        <i class="fas fa-print"></i>
				                    </button>
				                </div>

				                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js"></script>
				                <script>
				document.addEventListener('DOMContentLoaded', function () {

				    /* =========================
				       SAFE SYNTAX HIGHLIGHT
				    ========================== */
				    if (window.hljs) {
				        hljs.highlightAll();
				    }

				    /* =========================
				       BACK TO SUMMARY FUNCTION
				       This function tries to call the parent window's showSummaryReport()
				       If not in an iframe, it shows a fallback message
				    ========================== */
				    window.showSummaryReport = function() {
				        // Try to call parent window function first (when embedded in iframe)
				        if (window.parent && typeof window.parent.showSummaryReport === 'function') {
				            window.parent.showSummaryReport();
				            return;
				        }

				        // Fallback: If not in iframe, show a message or navigate
				        if (window !== window.parent) {
				            // We're in an iframe but parent doesn't have the function
				            showNavigationOptions();
				        } else {
				            // We're not in an iframe at all
				            showStandaloneMessage();
				        }
				    };

				    function showNavigationOptions() {
				        const modal = document.createElement('div');
				        modal.style.cssText = `
				            position: fixed;
				            top: 0;
				            left: 0;
				            width: 100%%;
				            height: 100%%;
				            background: rgba(0, 0, 0, 0.5);
				            display: flex;
				            justify-content: center;
				            align-items: center;
				            z-index: 2000;
				        `;

				        modal.innerHTML = `
				            <div style="
				                background: white;
				                padding: 30px;
				                border-radius: 12px;
				                box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
				                max-width: 400px;
				                width: 90%%;
				            ">
				                <h3 style="color: var(--primary-blue); margin-bottom: 15px;">
				                    <i class="fas fa-info-circle"></i> Summary Report Options
				                </h3>
				                <p style="color: var(--medium-gray); margin-bottom: 20px;">
				                    This report is designed to be viewed from the summary page. What would you like to do?
				                </p>
				                <div style="display: flex; gap: 10px; flex-wrap: wrap;">
				                    <button onclick="window.open('../test-summary-report.html', '_self')" style="
				                        background: var(--primary-blue);
				                        color: white;
				                        border: none;
				                        padding: 10px 20px;
				                        border-radius: 6px;
				                        cursor: pointer;
				                        flex: 1;
				                        font-weight: 500;
				                    ">
				                        <i class="fas fa-external-link-alt"></i> Open Summary Report
				                    </button>
				                    <button onclick="this.closest('div[style*=\"position: fixed\"]').remove()" style="
				                        background: var(--light-gray);
				                        color: var(--dark-gray);
				                        border: none;
				                        padding: 10px 20px;
				                        border-radius: 6px;
				                        cursor: pointer;
				                        flex: 1;
				                        font-weight: 500;
				                    ">
				                        <i class="fas fa-times"></i> Cancel
				                    </button>
				                </div>
				            </div>
				        `;

				        document.body.appendChild(modal);
				    }

				    function showStandaloneMessage() {
				        alert("This is a standalone performance report. To view it within the summary report, please open it from the summary page.");
				    }

				    /* =========================
				       PAGE TOGGLE
				    ========================== */
				    window.togglePageDetails = function (header) {
				        const details = header.closest('.page-card')?.querySelector('.page-details');
				        const icon = header.querySelector('.toggle-icon');

				        if (!details || !icon) return;

				        const isOpen = details.style.display === 'block';
				        details.style.display = isOpen ? 'none' : 'block';
				        icon.textContent = isOpen ? '▶' : '▼';
				        icon.style.transform = isOpen ? 'rotate(0deg)' : 'rotate(90deg)';
				    };

				    /* =========================
				       API TOGGLE
				    ========================== */
				    window.toggleApiDetails = function (header) {
				        const details = header.closest('.api-card')?.querySelector('.api-details');
				        const icon = header.querySelector('.api-toggle');

				        if (!details || !icon) return;

				        const isOpen = details.style.display === 'block';
				        details.style.display = isOpen ? 'none' : 'block';
				        icon.textContent = isOpen ? '▼' : '▲';
				        icon.style.transform = isOpen ? 'rotate(0deg)' : 'rotate(180deg)';
				    };

				    /* =========================
				       COPY TO CLIPBOARD
				    ========================== */
				    window.copyToClipboard = function (btn) {
				        const code = btn.parentElement.querySelector('code');
				        if (!code) return;

				        const textToCopy = code.textContent;
				        navigator.clipboard.writeText(textToCopy).then(() => {
				            const original = btn.innerHTML;
				            btn.innerHTML = '<i class="fas fa-check"></i> Copied';
				            btn.style.background = 'var(--success)';

				            setTimeout(() => {
				                btn.innerHTML = original;
				                btn.style.background = 'var(--primary-blue)';
				            }, 2000);
				        }).catch(err => {
				            console.error('Failed to copy: ', err);
				        });
				    };

				    /* =========================
				       EXPAND / COLLAPSE ALL
				    ========================== */
				    window.expandAll = function () {
				        document.querySelectorAll('.page-details, .api-details').forEach(el => {
				            el.style.display = 'block';
				        });
				        document.querySelectorAll('.toggle-icon').forEach(i => {
				            i.textContent = '▼';
				            i.style.transform = 'rotate(90deg)';
				        });
				        document.querySelectorAll('.api-toggle').forEach(i => {
				            i.textContent = '▲';
				            i.style.transform = 'rotate(180deg)';
				        });
				    };

				    window.collapseAll = function () {
				        document.querySelectorAll('.page-details, .api-details').forEach(el => {
				            el.style.display = 'none';
				        });
				        document.querySelectorAll('.toggle-icon').forEach(i => {
				            i.textContent = '▶';
				            i.style.transform = 'rotate(0deg)';
				        });
				        document.querySelectorAll('.api-toggle').forEach(i => {
				            i.textContent = '▼';
				            i.style.transform = 'rotate(0deg)';
				        });
				    };

				    /* =========================
				       SCROLL TO TOP
				    ========================== */
				    window.scrollToTop = function () {
				        window.scrollTo({
				            top: 0,
				            behavior: 'smooth'
				        });
				    };

				    /* =========================
				       PRINT REPORT
				    ========================== */
				    window.printReport = function () {
				        window.print();
				    };

				    /* =========================
				       AUTO-EXPAND FIRST PAGE
				    ========================== */
				    const firstPage = document.querySelector('.page-card');
				    if (firstPage) {
				        const details = firstPage.querySelector('.page-details');
				        const icon = firstPage.querySelector('.toggle-icon');
				        if (details && icon) {
				            details.style.display = 'block';
				            icon.textContent = '▼';
				            icon.style.transform = 'rotate(90deg)';
				        }
				    }

				    /* =========================
				       INTERSECTION OBSERVER
				    ========================== */
				    const observer = new IntersectionObserver(entries => {
				        entries.forEach(entry => {
				            if (entry.isIntersecting) {
				                entry.target.style.animationPlayState = 'running';
				                observer.unobserve(entry.target);
				            }
				        });
				    }, { threshold: 0.1 });

				    document.querySelectorAll('.page-card').forEach(card => {
				        card.style.animationPlayState = 'paused';
				        observer.observe(card);
				    });

				    /* =========================
				       PROGRESS BAR ANIMATION
				    ========================== */
				    document.querySelectorAll('.progress-fill').forEach(bar => {
				        const width = bar.style.width;
				        bar.style.width = '0';
				        setTimeout(() => {
				            bar.style.width = width;
				        }, 300);
				    });
				});
				</script>
				            </body>
				            </html>
				        """, new java.util.Date().toString(), summaryCards.toString(), pageBlocks.toString());

		String fileName = "performance-dashboard" + getCurrentDate() + ".html";
		System.setProperty("performanceReportPath", fileName);

		try (FileWriter fw = new FileWriter(fileName))
		{
			fw.write(html);
			System.out.println("📊 Professional blue/white dashboard generated: performance-dashboard.html");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return html;
	}

	public static String getCurrentDate()
	{
		String date = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
		TestLogManager.dataInfo("Current date", date);
		return date;
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	private static String getStatusColor(long responseTime)
	{
		if (responseTime < 500)
			return "#059669"; // Green
		if (responseTime < 2000)
			return "#d97706"; // Orange/amber
		return "#dc2626"; // Red
	}

	private static String getApiStatusColor(int status)
	{
		if (status >= 400)
			return "#dc2626"; // Red for errors
		if (status >= 300)
			return "#d97706"; // Amber for redirects
		return "#059669"; // Green for success
	}

	private static String getMethodColor(String method)
	{
		if (method == null)
			return "#6b7280";
		switch (method.toUpperCase())
		{
		case "GET":
			return "#3b82f6"; // Blue
		case "POST":
			return "#10b981"; // Green
		case "PUT":
			return "#f59e0b"; // Yellow
		case "DELETE":
			return "#ef4444"; // Red
		case "PATCH":
			return "#8b5cf6"; // Purple
		default:
			return "#6b7280"; // Gray
		}
	}

	private static String truncateUrl(String url, int maxLength)
	{
		if (url == null)
			return "";
		if (url.length() <= maxLength)
			return url;
		return url.substring(0, Math.min(url.length(), maxLength - 3)) + "...";
	}

	private static String formatJson(String json)
	{
		if (json == null || json.trim().isEmpty())
			return "";
		try
		{
			if (json.trim().startsWith("{") || json.trim().startsWith("["))
			{
				// Basic JSON formatting
				return json.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"");
			}
		} catch (Exception e)
		{
			// If not JSON, return as-is
		}
		return json;
	}
}