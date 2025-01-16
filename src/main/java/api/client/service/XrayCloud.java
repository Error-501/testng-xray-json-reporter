package api.client.service;

import api.model.xray.AuthenticateReq;
import com.fasterxml.jackson.databind.JsonNode;
import feign.*;

import java.io.File;
import java.util.Map;

public interface XrayCloud {
    @RequestLine("POST /authenticate")
    @Headers("Content-Type: application/json")
    String getAuthToken(AuthenticateReq authenticate);

    @RequestLine("POST /import/execution")
    @Headers("Content-Type: application/octet-stream")
    Response importTestExecution(@HeaderMap Map<String, Object> headers,@Param("file") byte[] file);

    @RequestLine("POST /import/execution/multipart")
    @Headers("Content-Type: multipart/form-data")
    Response importMultiPartExecution(@HeaderMap Map<String, Object> headers, @Param("results") File testExec, @Param("info") File testExecInfo);

}