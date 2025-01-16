package api.client;

import api.client.service.XrayCloud;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.form.spring.SpringFormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import commons.PropertyHandler;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static xray.Constants.XRAY_CLOUD_API_ENDPOINT;

public class FeignClientConfiguration {

    private final Properties props;

    public FeignClientConfiguration() {
        props = PropertyHandler.loadConfigPropertiesFile();
    }

    private <T> T getDefaultConfig(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .target(clientClass, endPoint);
    }

    public <T> T getFileBinaryBuilder(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new SimpleFileEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .target(clientClass, endPoint);
    }

    public <T> T getFormBuilder(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new FormEncoder(new GsonEncoder()))
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .target(clientClass, endPoint);
    }

    public XrayCloud getXrayClient() {
        return getDefaultConfig(XrayCloud.class, props.getProperty(XRAY_CLOUD_API_ENDPOINT));
    }

    public XrayCloud getXrayFileClient() {
        return getFileBinaryBuilder(XrayCloud.class, props.getProperty(XRAY_CLOUD_API_ENDPOINT));
    }

    public XrayCloud getXrayFormClient() {
        return getFormBuilder(XrayCloud.class, props.getProperty(XRAY_CLOUD_API_ENDPOINT));
    }

    public static class SimpleFileEncoder implements Encoder {
        public void encode(Object object, Type type, RequestTemplate template)
                throws EncodeException {
            template.body(Request.Body.encoded(
                    (byte[]) ((Map<?, ?>) object).get("file"), UTF_8));
        }
    }
}