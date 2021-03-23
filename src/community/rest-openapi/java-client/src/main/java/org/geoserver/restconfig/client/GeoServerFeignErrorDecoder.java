package org.geoserver.restconfig.client;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.BufferedReader;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Feign {@link ErrorDecoder} that returns application specific {@link ServerException} instead of
 * {@link FeignException}
 */
public class GeoServerFeignErrorDecoder implements ErrorDecoder {

    public @Override ServerException decode(String methodKey, Response response) {
        int status = response.status();
        Map<String, Collection<String>> headers = response.headers();
        String reason = response.reason();
        String request = response.request().toString();
        if (headers.containsKey("Content-Type")
                && headers.get("Content-Type").contains("text/plain")) {
            try {
                String errorText =
                        new BufferedReader(response.body().asReader())
                                .lines()
                                .collect(Collectors.joining("\n"));
                if (errorText != null && !errorText.trim().isEmpty()) {
                    reason = errorText;
                }
            } catch (Exception ignore) {
            }
        }
        ServerException exception = ServerException.of(status, reason, headers, request);
        return exception;
    }
}
