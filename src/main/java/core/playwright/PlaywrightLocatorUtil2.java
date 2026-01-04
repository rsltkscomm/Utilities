package core.playwright;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import base.DriverContext;
import core.interfaces.LocatorInterface;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import reporting.TestLogManager;

/**
 * Playwright Locator Utility with Self-Healing + AI Healing
 *
 * Resolution order: 1. Cached healed locator 2. Original locator 3. Rule-based healing 4. File-based healed locator 5. AI-based healing (LAST)
 *
 * ❌ No wildcard fallback ❌ No unsafe locator caching
 */
public class PlaywrightLocatorUtil2 implements LocatorInterface
{

	protected final DriverContext driverContext;

	private static final boolean SELF_HEALING_ENABLED = true;
	private static final String OPENAI_API_KEY = System.getProperty("Apikey");

	private static final String DESCRIPTION_FILE = System.getProperty("user.dir") + "/src/main/resources/data/element_descriptions.properties";
	private static final String LOCATOR_FILE = System.getProperty("user.dir") + "/src/main/resources/data/healed_locators.properties";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Map<String, String> healedLocatorCache = new ConcurrentHashMap<>();
	private final Map<String, String> elementDescriptions = new HashMap<>();
	private final Map<String, String> healedLocators = new HashMap<>();

	public static ThreadLocal<String> logName = new ThreadLocal<>();

	public PlaywrightLocatorUtil2(DriverContext driverContext) {
		this.driverContext = driverContext;
		loadDescriptionsFromFile();
		loadHealedLocatorsFromFile();
	}

	protected Page page()
	{
		return driverContext.getPage();
	}

	// ====================================================================================
	// SELENIUM-COMPATIBLE AUTOLOCATOR
	// ====================================================================================
	@Override
	public By autolocator(String key)
	{

		String[] parts = key.split(",", 3);
		logName.set(parts[0].trim());

		String type = parts[1].trim().toLowerCase(Locale.ROOT);
		String value = parts[2].trim();

		return switch (type)
		{
		case "id" -> By.id(value);
		case "name" -> By.name(value);
		case "xpath" -> By.xpath(value);
		case "css", "cssselector" -> By.cssSelector(value);
		case "class", "classname" -> By.className(value);
		case "tag", "tagname" -> By.tagName(value);
		case "link", "linktext" -> By.linkText(value);
		case "parlink", "partiallinktext" -> By.partialLinkText(value);
		default -> throw new IllegalArgumentException("Unsupported locator type: " + type);
		};
	}

	// ====================================================================================
	// MAIN LOCATOR RESOLUTION
	// ====================================================================================
	public Locator getLocator(String key)
	{

		ParsedLocator parsed = parse(key);
		logName.set(parsed.elementName);

		// 1️⃣ Cached healed locator
		if (SELF_HEALING_ENABLED && healedLocatorCache.containsKey(parsed.elementName))
		{
			Locator cached = page().locator(healedLocatorCache.get(parsed.elementName));
			if (isValid(cached))
				return cached;
		}

		// 2️⃣ Original locator
		Locator original = page().locator(toPlaywrightSelector(parsed));
		if (isValid(original))
			return original;

		// 3️⃣ Rule-based healing
		if (SELF_HEALING_ENABLED)
		{
			Locator healed = heal(parsed);
			if (isValid(healed))
			{
				cacheStableLocator(parsed.elementName, healed);
				return healed;
			}
		}

		// 4️⃣ File-based healed locator
		if (healedLocators.containsKey(parsed.elementName))
		{
			Locator fileHealed = page().locator(healedLocators.get(parsed.elementName));
			if (isValid(fileHealed))
				return fileHealed;
		}

		// 5️⃣ AI-based healing (LAST)
		try
		{
			String description = elementDescriptions.computeIfAbsent(parsed.elementName, k -> generateDescription(original));
			saveDescriptionsToFile();

			String aiResponse = suggestLocator(page().content(), description);
			String xpath = sanitizeXPath(extractXPathFromAIResponse(aiResponse));;

			Locator aiLocator = page().locator("xpath=" + xpath);
			if (isValid(aiLocator))
			{
				healedLocators.put(parsed.elementName, "xpath=" + xpath);
				saveHealedLocatorsToFile();
				return aiLocator;
			}

		} catch (Exception e)
		{
			TestLogManager.warning("AI healing failed for " + parsed.elementName + ": " + e.getMessage());
		}

		return original;
	}

	// ====================================================================================
	// RULE-BASED HEALING
	// ====================================================================================
	private Locator heal(ParsedLocator parsed)
	{

		String intent = parsed.elementName;
		if (intent == null || intent.isBlank())
			return null;

		// Role-based (best)
		for (AriaRole role : new AriaRole[] { AriaRole.BUTTON, AriaRole.LINK, AriaRole.TAB, AriaRole.MENUITEM })
		{
			Locator byRole = page().getByRole(role, new Page.GetByRoleOptions().setName(intent).setExact(false));
			if (isValid(byRole))
				return byRole.first();
		}

		// Text-based
		Locator byText = page().getByText(intent, new Page.GetByTextOptions().setExact(false));
		if (isValid(byText))
			return byText.first();

		// Attribute-based
		Locator byAttr = page().locator("[aria-label*='" + intent + "'], " + "[placeholder*='" + intent + "'], " + "[data-testid*='" + intent + "']");
		if (isValid(byAttr))
			return byAttr.first();

		return null;
	}

	// ====================================================================================
	// VALIDATION (CRITICAL)
	// ====================================================================================
	private boolean isValid(Locator locator) {
	    try {
	        if (locator == null || locator.count() == 0)
	            return false;

	        Locator el = locator.first();

	        if (!el.isVisible())
	            return false;

	        // Convert Locator → ElementHandle
	        ElementHandle handle = el.elementHandle();
	        if (handle == null)
	            return false;

	        Boolean uncovered = (Boolean) page().evaluate(
	                "(e) => {" +
	                        "const r = e.getBoundingClientRect();" +
	                        "const x = r.left + r.width / 2;" +
	                        "const y = r.top + r.height / 2;" +
	                        "return document.elementFromPoint(x, y) === e;" +
	                        "}",
	                handle
	        );

	        return Boolean.TRUE.equals(uncovered);

	    } catch (Exception e) {
	        return false;
	    }
	}


	// ====================================================================================
	// HELPER METHODS (FIXES YOUR ERRORS)
	// ====================================================================================
	private ParsedLocator parse(String key)
	{
		if (key == null || key.isBlank())
		{
			throw new IllegalArgumentException("Locator key cannot be null or empty");
		}

		String[] parts = key.split(",", 3);
		if (parts.length < 3)
		{
			throw new IllegalArgumentException("Invalid locator format: " + key + " | Expected: ElementName,locatorType,locatorValue");
		}

		return new ParsedLocator(parts[0].trim(), parts[1].trim(), parts[2].trim());
	}

	private String toPlaywrightSelector(ParsedLocator parsed)
	{

		String type = parsed.locatorType.toLowerCase(Locale.ROOT);
		String value = parsed.locatorValue;

		return switch (type)
		{
		case "id" -> "#" + value;
		case "name" -> "[name='" + value + "']";
		case "xpath" -> "xpath=" + value;
		case "css", "cssselector" -> value;
		case "class", "classname" -> "." + value.replace(" ", ".");
		case "tag", "tagname" -> value;
		default -> value;
		};
	}

	private void cacheStableLocator(String elementName, Locator locator)
	{
		try
		{
			String stableSelector = locator.evaluate("el => el.id ? '#'+el.id : el.getAttribute('data-testid')").toString();

			if (stableSelector != null && !stableSelector.isBlank())
			{
				healedLocatorCache.put(elementName, stableSelector);
			}
		} catch (Exception ignored)
		{
		}
	}

	// ====================================================================================
	// WEBELEMENT BRIDGE
	// ====================================================================================
	@Override
	public WebElement getElement(Object pr)
	{
		if (pr instanceof String)
		{
			return new PlaywrightWebElement(getLocator(pr.toString()).first());
		}
		return (WebElement) pr;
	}

	@Override
	public List<WebElement> getElements(Object pr)
	{
		List<WebElement> list = new ArrayList<>();
		Locator loc = getLocator(pr.toString());
		for (int i = 0; i < loc.count(); i++)
		{
			list.add(new PlaywrightWebElement(loc.nth(i)));
		}
		return list;
	}

	// ====================================================================================
	// PLACEHOLDER SUPPORT (BACKWARD COMPATIBLE)
	// ====================================================================================
	@Override
	public String replacePlaceHolder(String locator, String value)
	{
		return locator.replace("PLACE_HOLDER", value).replace("{0}", value);
	}

	@Override
	public String replacePlaceHolder(String locator, int value)
	{
		return replacePlaceHolder(locator, String.valueOf(value));
	}

	@Override
	public String replacePlaceHolder(String locator, String v1, String v2)
	{
		return locator.replace("PLACE_HOLDER", v1).replace("TEMP", v2).replace("{0}", v1).replace("{1}", v2);
	}

	// ====================================================================================
	// SUPPORT CLASSES
	// ====================================================================================
	private static class ParsedLocator
	{
		final String elementName;
		final String locatorType;
		final String locatorValue;

		ParsedLocator(String n, String t, String v) {
			elementName = n;
			locatorType = t;
			locatorValue = v;
		}
	}

	// ====================================================================================
	// DESCRIPTION + AI HELPERS
	// ====================================================================================
	public static String generateDescription(Locator element)
	{
		try
		{
			Locator el = element.first();
			String tag = el.evaluate("e=>e.tagName.toLowerCase()").toString();
			return "Element of type '" + tag + "'";
		} catch (Exception e)
		{
			return "Unknown element";
		}
	}

	public static String sanitizeXPath(String xpath)
	{
		return xpath.replaceAll("\\s+and\\s+@placeholder=''", "").trim();
	}

	public static String extractXPathFromAIResponse(String aiResponse)
	{
		Matcher m = Pattern.compile("(//[^\\n\\r]*)").matcher(aiResponse);
		if (m.find())
			return m.group(1).trim();
		throw new RuntimeException("No valid XPath found in AI response");
	}

	public static String suggestLocator(String html, String description) throws Exception
	{
		String prompt = "Given the HTML:\n" + html + "\nLocate element: '" + description + "'. Return ONLY XPath.";
		return callOpenAI(prompt);
	}


	public static String callOpenAI(String prompt) {

	    try {
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

	        if (response.statusCode() != 200) {
	            throw new RuntimeException(
	                    "OpenAI API error: " + response.statusCode() + " → " + response.getBody().asString()
	            );
	        }

	        JsonNode root = MAPPER.readTree(response.getBody().asString());
	        return root.at("/choices/0/message/content").asText();

	    } catch (Exception e) {
	        throw new RuntimeException("OpenAI call failed", e);
	    }
	}


	// ====================================================================================
	// FILE IO
	// ====================================================================================
	public void saveDescriptionsToFile()
	{
		saveProps(DESCRIPTION_FILE, elementDescriptions);
	}

	public void saveHealedLocatorsToFile()
	{
		saveProps(LOCATOR_FILE, healedLocators);
	}

	public void loadDescriptionsFromFile()
	{
		loadProps(DESCRIPTION_FILE, elementDescriptions);
	}

	public void loadHealedLocatorsFromFile()
	{
		loadProps(LOCATOR_FILE, healedLocators);
	}

	private void saveProps(String file, Map<String, String> map)
	{
		try (FileOutputStream fos = new FileOutputStream(file))
		{
			Properties p = new Properties();
			p.putAll(map);
			p.store(fos, "");
		} catch (Exception ignored)
		{
		}
	}

	private void loadProps(String file, Map<String, String> map)
	{
		try (FileInputStream fis = new FileInputStream(file))
		{
			Properties p = new Properties();
			p.load(fis);
			p.forEach((k, v) -> map.put(k.toString(), v.toString()));
		} catch (Exception ignored)
		{
		}
	}
}
