package xray.api.config;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FeignController {
    private static <T> T getDefaultBuilder(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .errorDecoder(new XrayErrorDecoder())
                .retryer(new XrayRetryer())
                .target(clientClass, endPoint);
    }

    public static <T> T getFileBinaryBuilder(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new SimpleFileEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .errorDecoder(new XrayErrorDecoder())
                .retryer(new XrayRetryer())
                .options(new Request.Options(100, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, false))
                .target(clientClass, endPoint);
    }

    public static <T> T getFormBuilder(Class<T> clientClass, String endPoint) {
        return Feign.builder()
                .client(new OkHttpClient())
                .encoder(new FormEncoder(new GsonEncoder()))
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(clientClass))
                .logLevel(Logger.Level.FULL)
                .errorDecoder(new XrayErrorDecoder())
                .retryer(new XrayRetryer())
                .target(clientClass, endPoint);
    }

    public static <T> T getDefaultClient(Class<T> clientClass, String baseUrl) {
        return getDefaultBuilder(clientClass, baseUrl);
    }

    public static <T> T getFileClient(Class<T> clientClass, String baseUrl) {
        return getFileBinaryBuilder(clientClass, baseUrl);
    }

    public static <T> T getFormClient(Class<T> clientClass, String baseUrl) {
        return getFormBuilder(clientClass, baseUrl);
    }

    public static class SimpleFileEncoder implements Encoder {
        public void encode(Object object, Type type, RequestTemplate template)
                throws EncodeException {
            template.body((byte[]) ((Map<?, ?>) object).get("file"), UTF_8);
        }
    }
}