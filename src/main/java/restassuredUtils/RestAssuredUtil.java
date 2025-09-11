package restassuredUtils;

import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import reporting.ExtentManager;
import reporting.TestLogManager;

public class RestAssuredUtil {

    private static ThreadLocal<RequestSpecification> requestSpec = new ThreadLocal<>();
    private static ThreadLocal<Response> response = new ThreadLocal<>();

    // Set Base URI
    public static void setBaseURI(String baseURI) {
        RestAssured.baseURI = baseURI;
        TestLogManager.info("Base URI set to: " + baseURI);
    }

    // Reset Base URI
    public static void resetBaseURI() {
        RestAssured.baseURI = null;
        TestLogManager.info("Base URI reset.");
    }

    // Initialize request with default headers
    public static void initRequest() {
        requestSpec.set(RestAssured.given().contentType(ContentType.JSON));
        TestLogManager.info("Initialized new request with Content-Type: JSON");
    }

    // Add headers
    public static void addHeaders(Map<String, String> headers) {
        requestSpec.set(requestSpec.get().headers(headers));
        TestLogManager.info("Headers added: " + headers);
    }

    // Add query params
    public static void addQueryParams(Map<String, ?> queryParams) {
        requestSpec.set(requestSpec.get().queryParams(queryParams));
        TestLogManager.info("Query Params added: " + queryParams);
    }

    // Add body
    public static void addBody(Object body) {
        requestSpec.set(requestSpec.get().body(body));
        TestLogManager.info("Request body added.");
    }

    // GET request
    public static Response get(String endpoint) {
        response.set(requestSpec.get().when().get(endpoint));
        logResponse("GET", endpoint);
        return response.get();
    }

    // POST request
    public static Response post(String endpoint) {
        response.set(requestSpec.get().when().post(endpoint));
        logResponse("POST", endpoint);
        return response.get();
    }

    // PUT request
    public static Response put(String endpoint) {
        response.set(requestSpec.get().when().put(endpoint));
        logResponse("PUT", endpoint);
        return response.get();
    }

    // DELETE request
    public static Response delete(String endpoint) {
        response.set(requestSpec.get().when().delete(endpoint));
        logResponse("DELETE", endpoint);
        return response.get();
    }

    // Get latest response
    public static Response getResponse() {
        return response.get();
    }

    // Logging to Extent report
    private static void logResponse(String method, String endpoint) {
        Response res = response.get();
        String logMessage = method + " " + endpoint +
                " | Status: " + res.getStatusCode() +
                " | Response: " + res.getBody().asString();

        TestLogManager.info(logMessage);
        ExtentManager.getTest().info(logMessage);
    }
}
