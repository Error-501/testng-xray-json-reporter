package xray.json.model.misc;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import static org.apache.commons.lang3.StringUtils.getIfBlank;

/**
 * The Evidence Object attached to TestRun Objects.<p>
 * This will count towards Xray storage. -
 * <a href="https://docs.getxray.app/display/XRAYCLOUD/Tips+for+Managing+your+Xray+Storage">Ref</a>
 */
@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Evidence {

    @JsonIgnore
    private File attachment;
    @JsonProperty(value = "filename")
    private String fileName;
    @JsonProperty(value = "data")
    private String base64File;
    private String contentType;

    public Evidence (String filePath) throws IOException {
        attachment = new File(filePath);
        processAttachment();
    }

    public Evidence (String fileName, String filePath) throws IOException {
        attachment = new File(filePath);
        this.fileName = fileName;
        processAttachment();
    }

    private void processAttachment() throws IOException {
        if (attachment.exists() && attachment.isFile()) {
            fileName = getIfBlank(fileName, () -> attachment.getName());
            contentType = URLConnection.guessContentTypeFromName(attachment.getName());
            encodeFile();
        }
    }

    private void encodeFile() throws IOException {
        byte[] inFileBytes = Files.readAllBytes(Paths.get(attachment.toURI()));
        base64File = Base64.getMimeEncoder().encodeToString(inFileBytes);
    }
}