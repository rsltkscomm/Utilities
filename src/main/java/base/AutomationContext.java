package base;

import java.util.Map;

import core.interfaces.EngineType;


public interface AutomationContext {

    EngineType getEngineType();
    
    String getLoginURL();

    String getEnvironment();

    String getNormalizedPath(String path);

    String detectFilePath(String path);

    Map<String, String> getDeviceSpecs();

}
