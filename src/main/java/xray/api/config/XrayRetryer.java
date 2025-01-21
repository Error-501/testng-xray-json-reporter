package xray.api.config;

import feign.RetryableException;
import feign.Retryer;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;

@NoArgsConstructor
public class XrayRetryer implements Retryer {
    private static final Logger LOG = LoggerFactory.getLogger(XrayRetryer.class);

    private int maxRetries = 5;
    private long maxRetryDelayMillis = 300000;
    private long initialRetryDelayMillis = 10000;
    private final double[] maxJitterMultiplier = {0.5, 1.1};
    private int attempt = 0;


    public XrayRetryer(int maxRetries, long initialRetryDelayMillis, long maxRetryDelayMillis) {
        this.maxRetries = maxRetries;
        this.initialRetryDelayMillis = initialRetryDelayMillis;
        this.maxRetryDelayMillis = maxRetryDelayMillis;
    }

    @Override
    public void continueOrPropagate(RetryableException exception) {
        if (++attempt > maxRetries) {
            throw exception;
        }
        //System.out.println("Retrying the connection attempt :" + attempt);
        LOG.debug("Retrying the connection attempt : {} ", attempt );
        long retryAfter = getRetryHeaderValue(exception.responseHeaders());
        if (retryAfter < 0 && exception.status() == 429) {
            retryAfter = min(2 * initialRetryDelayMillis, maxRetryDelayMillis);
        }
        if (retryAfter > 0) {
            Random random = new Random();
            retryAfter += (long) (retryAfter *
                    random.doubles(maxJitterMultiplier[0], maxJitterMultiplier[1])
                            .findFirst()
                            .getAsDouble());
            //System.out.println("Retry After Millis :" + retryAfter);
            LOG.debug("Retry After Millis : {}", retryAfter);
            try {
                Thread.sleep(retryAfter);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw exception;
        }
    }

    private long getRetryHeaderValue(Map<String, Collection<String>> responseHeaders) {
        Collection<String> retryMap = responseHeaders.get("retry-after");
        if (retryMap != null) {
            if (retryMap.stream().findFirst().isPresent()) {
                return TimeUnit.MILLISECONDS.convert(
                        Long.parseLong(retryMap.stream().findFirst().get()), TimeUnit.SECONDS);
            }
        }
        return -1;
    }

    @Override
    public Retryer clone() {
        return new XrayRetryer();
    }
}