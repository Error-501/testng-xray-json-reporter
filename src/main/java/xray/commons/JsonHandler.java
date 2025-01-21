package xray.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static xray.commons.Constants.DEFAULT_XRAY_PROPERTIES_FILE;

public class JsonHandler {

    public static final ObjectMapper objectMapper = new ObjectMapper();
    static Properties props;

    public static Properties loadConfigPropertiesFile() {
        try {
            InputStream stream = JsonHandler.class.getClassLoader().getResourceAsStream(DEFAULT_XRAY_PROPERTIES_FILE);
            if (stream == null) {
                throw new IOException("Could not find " + DEFAULT_XRAY_PROPERTIES_FILE + " in classpath");
            }
            props = new Properties();
            props.load(stream);
        } catch (Exception e) {
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

    public static void writeJsonFileFromObject(String outputJson, Object obj) {
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputJson))) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, obj);
        } catch (IOException e) {
            //TODO Logger
            System.err.println("Error in BufferedWriter due to " + e);
        }
    }

}