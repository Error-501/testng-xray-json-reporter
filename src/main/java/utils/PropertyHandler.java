package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static xray.Constants.DEFAULT_XRAY_PROPERTIES_FILE;

public class PropertyHandler {

    static Properties props;

    public static Properties loadConfigPropertiesFile() {
        try {
            InputStream stream = PropertyHandler.class.getClassLoader().getResourceAsStream(DEFAULT_XRAY_PROPERTIES_FILE);
            if (stream == null) {
                throw new IOException("Could not find " + DEFAULT_XRAY_PROPERTIES_FILE + " in classpath");
            }
            props = new Properties();
            props.load(stream);
        }
        catch (Exception e) {
            throw new RuntimeException("Error loading Xray configuration from properties files " + e);
        }
        return props;
    }
}