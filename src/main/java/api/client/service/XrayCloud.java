package api.client.service;

import api.model.xray.AuthenticateReq;
import com.fasterxml.jackson.databind.JsonNode;
import feign.*;
import java.util.Map;

public interface XrayCloud {
    @RequestLine("POST /authenticate")
    @Headers("Content-Type: application/json")
    String getAuthToken(AuthenticateReq authenticate);

    @RequestLine("POST /import/execution")
    @Headers("Content-Type: application/octet-stream")
    Response importTestExecution(@HeaderMap Map<String, Object> headers,@Param("file") byte[] file);

    @RequestLine("POST")
    @Headers("Content-Type: application/json")
    JsonNode importMultiPartExecution();

}