package core.playwright;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.fasterxml.jackson.databind.*;
import com.microsoft.playwright.*;

import base.DriverContext;
import core.interfaces.LocatorInterface;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import reporting.TestLogManager;

public class PlaywrightLocatorUtil implements LocatorInterface {

    protected final DriverContext driverContext;

    private static final boolean SELF_HEALING_ENABLED = Boolean.parseBoolean(System.getProperty("selfhealing"));
    private static final boolean AI_HEALING_ENABLED = Boolean.parseBoolean(System.getProperty("aihealing"));;

    private static final String OPENAI_API_KEY = System.getProperty("Apikey");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DESCRIPTION_FILE =
            System.getProperty("user.dir") + "/src/main/resources/data/element_descriptions.properties";
    private static final String LOCATOR_FILE =
            System.getProperty("user.dir") + "/src/main/resources/data/healed_locators.properties";

    private static final Map<String, String> healedLocatorCache = new ConcurrentHashMap<>();
    private final Map<String, String> healedLocators = new HashMap<>();
    private final Map<String, String> elementDescriptions = new HashMap<>();

    public static ThreadLocal<String> logName = new ThreadLocal<>();

    public PlaywrightLocatorUtil(DriverContext driverContext) {
        this.driverContext = driverContext;
        loadProps(DESCRIPTION_FILE, elementDescriptions);
        loadProps(LOCATOR_FILE, healedLocators);
    }

    protected Page page() {
        return driverContext.getPage();
    }

    // ============================================================
    // PLAYWRIGHT LOCATOR RESOLUTION
    // ============================================================
    public Locator getLocator(String key) {

        ParsedLocator parsed = parse(key);
        logName.set(parsed.elementName);

        // 1️⃣ Cached healed locator
        if (SELF_HEALING_ENABLED && healedLocatorCache.containsKey(parsed.elementName)) {
            Locator cached = page().locator(healedLocatorCache.get(parsed.elementName));
            if (isVisibleOnly(cached)) return cached;
        }

        // 2️⃣ Original locator (MOST TRUSTED)
        Locator original = page().locator(toPlaywrightSelector(parsed));
        String description = elementDescriptions.computeIfAbsent(
                parsed.elementName,
                k -> generateDescription(original)
        );
        saveProps(DESCRIPTION_FILE, elementDescriptions);
        if (isVisibleOnly(original)) return original;

        // 3️⃣ Rule-based healing
        if (SELF_HEALING_ENABLED) {
            Locator healed = heal(parsed);
            if (isCorrectElement(healed, parsed)) {
                healedLocatorCache.put(parsed.elementName, healed.toString());
                return healed;
            }
        }

        // 4️⃣ File-based healing
        if (healedLocators.containsKey(parsed.elementName)) {
            Locator fileHealed = page().locator(healedLocators.get(parsed.elementName));
            if (isVisibleOnly(fileHealed)) return fileHealed;
        }

        // 5️⃣ AI-based healing (LAST, NOT cached blindly)
        if (AI_HEALING_ENABLED) {
            try {
                String aiResponse = suggestLocator(page().content(), description);;
                String xpath = sanitizeXPath(extractXPathFromAIResponse(aiResponse));

                Locator aiLocator = page().locator("xpath=" + xpath);
                if (isCorrectElement(aiLocator, parsed)) {
                    healedLocators.put(parsed.elementName, "xpath=" + xpath);
                    saveProps(LOCATOR_FILE, healedLocators);
                    return aiLocator;
                }
            } catch (Exception e) {
                TestLogManager.warning("AI healing failed: " + e.getMessage());
            }
        }

        return original;
    }

    // ============================================================
    // VALIDATION
    // ============================================================
    private boolean isVisibleOnly(Locator locator) {
        try {
        	boolean val = locator.count() > 0;
        	boolean visible = locator.first().isVisible();
            return locator != null && locator.count() > 0 && locator.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }
    
    public Locator getRawLocator(String key) {
        ParsedLocator parsed = parse(key);
        return page().locator(toPlaywrightSelector(parsed));
    }

    private boolean isCorrectElement(Locator locator, ParsedLocator parsed) {
        try {
            if (!isVisibleOnly(locator)) return false;

            Locator el = locator.first();

            // ID match if original was ID-based
            if ("id".equals(parsed.locatorType)) {
                return parsed.locatorValue.equals(el.getAttribute("id"));
            }

            // Text intent match
            String text = el.innerText();
            return text != null && parsed.elementName.toLowerCase().contains(text.toLowerCase());

        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // RULE-BASED HEALING
    // ============================================================
    private Locator heal(ParsedLocator parsed) {

        String intent = parsed.elementName;
        if (intent == null || intent.isBlank()) return null;

        // Role-based (BEST)
        for (com.microsoft.playwright.options.AriaRole role :
                List.of(
                        com.microsoft.playwright.options.AriaRole.BUTTON,
                        com.microsoft.playwright.options.AriaRole.LINK,
                        com.microsoft.playwright.options.AriaRole.MENUITEM
                )) {
            try {
                Locator l = page().getByRole(role,
                        new Page.GetByRoleOptions().setName(intent).setExact(false));
                if (isVisibleOnly(l)) return l.first();
            } catch (Exception ignored) {}
        }

        // Text-based
        try {
            Locator byText = page().getByText(intent,
                    new Page.GetByTextOptions().setExact(false));
            if (isVisibleOnly(byText)) return byText.first();
        } catch (Exception ignored) {}

        return null;
    }

    // ============================================================
    // PARSING
    // ============================================================
    private ParsedLocator parse(String key) {
        String[] parts = key.split(",", 3);
        if (parts.length < 3)
            throw new IllegalArgumentException("Invalid locator format: " + key);
        return new ParsedLocator(parts[0].trim(), parts[1].trim(), parts[2].trim());
    }

    private String toPlaywrightSelector(ParsedLocator p) {
        switch (p.locatorType) {
            case "id": return "#" + p.locatorValue;
            case "xpath": return "xpath=" + p.locatorValue;
            case "css": return p.locatorValue;
            case "name": return "[name='" + p.locatorValue + "']";
            default: return p.locatorValue;
        }
    }

    // ============================================================
    // AI SUPPORT
    // ============================================================
    public static String generateDescription(Locator element) {
        try {
            return "Element tag: " +
                    element.first().evaluate("e=>e.tagName.toLowerCase()");
        } catch (Exception e) {
            return "Unknown element";
        }
    }

    public static String suggestLocator(String html, String description) throws Exception {
        String prompt =
                "Given the HTML below, return ONLY an XPath for: " + description + "\n\n" + html;
        return callOpenAI(prompt);
    }

    public static String callOpenAI(String prompt) throws Exception {

        RestAssured.baseURI = System.getProperty("BaseURI");

        Map<String, Object> body = Map.of(
                "model", System.getProperty("model"),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .header("Content-Type", "application/json")
                .body(MAPPER.writeValueAsString(body))
                .post("/chat/completions");

        JsonNode root = MAPPER.readTree(response.getBody().asString());
        return root.at("/choices/0/message/content").asText();
    }

    // ============================================================
    // UTIL
    // ============================================================
    private static String extractXPathFromAIResponse(String r) {
        Matcher m = Pattern.compile("(//[^\\n\\r]*)").matcher(r);
        if (m.find()) return m.group(1);
        throw new RuntimeException("No XPath found");
    }

    private static String sanitizeXPath(String x) {
        return x.replaceAll("\\s+", " ").trim();
    }

    private void saveProps(String file, Map<String, String> map) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Properties p = new Properties();
            p.putAll(map);
            p.store(fos, "");
        } catch (Exception ignored) {}
    }

    private void loadProps(String file, Map<String, String> map) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties p = new Properties();
            p.load(fis);
            p.forEach((k, v) -> map.put(k.toString(), v.toString()));
        } catch (Exception ignored) {}
    }

    private static class ParsedLocator {
        final String elementName, locatorType, locatorValue;
        ParsedLocator(String n, String t, String v) {
            elementName = n; locatorType = t; locatorValue = v;
        }
    }

    @Override
    public By autolocator(String key) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Locator string cannot be null or empty.");
        }

        String[] parts = key.split(",", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                "Invalid locator format: " + key +
                " | Expected format: 'ElementName,locatorType,value'"
            );
        }

        logName.set(parts[0].trim());
        String locatorType = parts[1].trim().toLowerCase(Locale.ROOT);
        String locatorValue = parts[2].trim();

        switch (locatorType) {
            case "id": return By.id(locatorValue);
            case "name": return By.name(locatorValue);
            case "xpath": return By.xpath(locatorValue);
            case "css":
            case "cssselector": return By.cssSelector(locatorValue);
            case "link":
            case "linktext": return By.linkText(locatorValue);
            case "parlink":
            case "partiallinktext": return By.partialLinkText(locatorValue);
            case "class":
            case "classname": return By.className(locatorValue);
            case "tag":
            case "tagname": return By.tagName(locatorValue);
            default:
                throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        }
    }

	@Override
	public WebElement getElement(Object pr) {

	    if (pr instanceof PlaywrightWebElement)
	        return (WebElement) pr;

	    if (pr instanceof String) {
	        Locator loc = getLocator(pr.toString());
	        if (!loc.first().isVisible()) {
	            throw new RuntimeException("Element not visible: " + pr);
	        }
	        return new PlaywrightWebElement(loc.first());
	    }

	    if (pr instanceof WebElement)
	        return (WebElement) pr;

	    throw new IllegalArgumentException("getElement expects String or WebElement");
	}
	
	@Override
	public List<WebElement> getElements(Object pr) {

	    if (pr instanceof PlaywrightWebElement)
	        return List.of((WebElement) pr);

	    if (pr instanceof String) {
	        Locator locator = getLocator(pr.toString());
	        List<WebElement> list = new ArrayList<>();

	        int count = locator.count();
	        for (int i = 0; i < count; i++) {
	            Locator nth = locator.nth(i);
	            if (nth.isVisible()) {
	                list.add(new PlaywrightWebElement(nth));
	            }
	        }
	        return list;
	    }

	    if (pr instanceof WebElement)
	        return List.of((WebElement) pr);

	    throw new IllegalArgumentException("getElements expects String or WebElement");
	}
	
    @Override
    public String replacePlaceHolder(String locator, String placeHolder) {
        return locator.replace("PLACE_HOLDER", placeHolder);
    }

    @Override
    public String replacePlaceHolder(String locator, int placeHolder) {
        return locator.replace("PLACE_HOLDER", Integer.toString(placeHolder));
    }

    @Override
    public String replacePlaceHolder(String locator, String placeHolder, String placeHolder1) {
        return locator.replace("PLACE_HOLDER", placeHolder)
                      .replace("TEMP", placeHolder1);
    }

}
