package xray.api.client;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import xray.json.model.api.CreateIssueResp;
import xray.json.model.api.XrayAuthenticateReq;

import java.io.File;
import java.util.Map;

public interface XrayCloud {
    @RequestLine("POST /authenticate")
    @Headers("Content-Type: application/json")
    String getAuthToken(XrayAuthenticateReq authenticate);

    @RequestLine("POST /import/execution")
    @Headers("Content-Type: application/octet-stream")
    CreateIssueResp importTestExecution(@HeaderMap Map<String, Object> headers, @Param("file") byte[] file);

    @RequestLine("POST /import/execution/multipart")
    @Headers("Content-Type: multipart/form-data")
    CreateIssueResp importMultiPartExecution(@HeaderMap Map<String, Object> headers, @Param("results") File testExec, @Param("info") File testExecInfo);

}