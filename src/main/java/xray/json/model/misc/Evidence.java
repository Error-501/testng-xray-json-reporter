package xray.json.model.misc;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class Evidence {

    private File attachment;
    @Getter
    private String fileName;
    @Getter
    private byte[] base64File;
    @Getter
    private String contentType;

    public Evidence (String fileName, String filePath) throws IOException {
        attachment = new File(filePath);
        this.fileName = fileName;
        processAttachment();
    }

    public Evidence (String fileName, String filePath, String contentType) throws IOException {
        attachment = new File(filePath);
        this.fileName = fileName;
        this.contentType = contentType;
        processAttachment();
    }

    private void processAttachment() throws IOException {
        if (attachment.exists()) {
            encodeFile();
            if(StringUtils.isBlank(contentType))
                contentType =
                URLConnection.guessContentTypeFromName(attachment.getName());
        }
    }

    private void encodeFile() throws IOException {
        byte[] inFileBytes = Files.readAllBytes(Paths.get(attachment.toURI()));
        base64File = java.util.Base64.getEncoder().encode(inFileBytes);
    }
}
