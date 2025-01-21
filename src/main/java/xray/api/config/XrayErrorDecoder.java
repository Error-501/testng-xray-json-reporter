package xray.api.config;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class XrayErrorDecoder implements ErrorDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(XrayErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
    private final List<Integer> retryableStatus = Arrays.asList(429, 500, 503);

    @Override
    public Exception decode(String methodKey, Response response) {
        Response.Body responseBody = response.body();
        int status = response.status();
        boolean isRetryableError = retryableStatus.contains(status);
        Exception defaultException = defaultDecoder.decode(methodKey, response);
        try {
            LOG.error(
                    "Got {} response from {}, response body: {}",
                    status,
                    methodKey,
                    responseBody.asReader(Charset.defaultCharset()).toString());
        } catch (IOException e) {
            LOG.error(
                    "Got {} response from {}, response body could not be read",
                    status,
                    methodKey
            );
        }

        if (isRetryableError) {
            return new RetryableException(
                    status,
                    defaultException.getMessage(),
                    response.request().httpMethod(),
                    (Long) null,
                    response.request(),
                    null,
                    response.headers());
        }
        return defaultException;
    }


}