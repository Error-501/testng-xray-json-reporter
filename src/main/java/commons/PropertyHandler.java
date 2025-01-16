package commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static xray.Constants.DEFAULT_XRAY_PROPERTIES_FILE;
import static xray.listeners.XrayJsonReporter.objectMapper;

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

    public static void validateXrayJsonSchema(String jsonFilePath) throws IOException {
        Path jsonFile = Path.of(jsonFilePath);
        if (Files.exists(jsonFile)) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            InputStream schemaFileStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("xray-schema.json");
            JsonSchema jsonSchema = factory.getSchema(schemaFileStream);
            JsonNode jsonNode = objectMapper.readTree(jsonFile.toFile());
            Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
            if (!errors.isEmpty()) {
                throw new InvalidPropertiesFormatException(
                        errors.stream().
                                map(ValidationMessage::getMessage).
                                collect(Collectors.joining(System.lineSeparator()))
                );
            }
        }
    }
}