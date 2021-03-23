package org.geoserver.restconfig.client;

import java.util.Collection;
import java.util.Map;
import lombok.Getter;

public class ServerException extends RuntimeException {
    private static final long serialVersionUID = 220100165000333400L;

    private @Getter int status;
    private @Getter String reason;
    private @Getter Map<String, Collection<String>> responseHeaders;
    private @Getter String request;

    public ServerException(
            int status, String reason, Map<String, Collection<String>> headers, String request) {
        super(String.format("Status %d: %s\nRequest: %s", status, reason, request));
        this.status = status;
        this.reason = reason;
        this.responseHeaders = headers;
        this.request = request;
    }

    public @Override String toString() {
        return String.format(
                "%s\n------\nERROR: %d '%s'\nHeaders: %s",
                request, status, reason, responseHeaders);
    }

    public static ServerException of(
            int status, String reason, Map<String, Collection<String>> headers, String request) {
        switch (status) {
            case 400:
                return new BadRequest(reason, headers, request);
            case 401:
                return new Unauthorized(reason, headers, request);
            case 403:
                return new Forbidden(reason, headers, request);
            case 404:
                return new NotFound(reason, headers, request);
            case 405:
                return new MethodNotAllowed(reason, headers, request);
            case 406:
                return new NotAcceptable(reason, headers, request);
            case 409:
                return new Conflict(reason, headers, request);
            case 410:
                return new Gone(reason, headers, request);
            case 415:
                return new UnsupportedMediaType(reason, headers, request);
            case 429:
                return new TooManyRequests(reason, headers, request);
            case 422:
                return new UnprocessableEntity(reason, headers, request);
            case 500:
                return new InternalServerError(reason, headers, request);
            case 501:
                return new NotImplemented(reason, headers, request);
            case 502:
                return new BadGateway(reason, headers, request);
            case 503:
                return new ServiceUnavailable(reason, headers, request);
            case 504:
                return new GatewayTimeout(reason, headers, request);
            default:
                return new ServerException(status, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class BadRequest extends ServerException {
        public BadRequest(String reason, Map<String, Collection<String>> headers, String request) {
            super(400, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class Unauthorized extends ServerException {
        public Unauthorized(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(401, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class Forbidden extends ServerException {
        public Forbidden(String reason, Map<String, Collection<String>> headers, String request) {
            super(403, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class NotFound extends ServerException {
        public NotFound(String reason, Map<String, Collection<String>> headers, String request) {
            super(404, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class MethodNotAllowed extends ServerException {
        public MethodNotAllowed(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(405, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class NotAcceptable extends ServerException {
        public NotAcceptable(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(406, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class Conflict extends ServerException {
        public Conflict(String reason, Map<String, Collection<String>> headers, String request) {
            super(409, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class Gone extends ServerException {
        public Gone(String reason, Map<String, Collection<String>> headers, String request) {
            super(410, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class UnsupportedMediaType extends ServerException {
        public UnsupportedMediaType(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(415, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class TooManyRequests extends ServerException {
        public TooManyRequests(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(429, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class UnprocessableEntity extends ServerException {
        public UnprocessableEntity(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(422, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class InternalServerError extends ServerException {
        public InternalServerError(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(500, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class NotImplemented extends ServerException {
        public NotImplemented(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(501, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class BadGateway extends ServerException {
        public BadGateway(String reason, Map<String, Collection<String>> headers, String request) {
            super(502, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class ServiceUnavailable extends ServerException {
        public ServiceUnavailable(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(503, reason, headers, request);
        }
    }

    @SuppressWarnings("serial")
    public static class GatewayTimeout extends ServerException {
        public GatewayTimeout(
                String reason, Map<String, Collection<String>> headers, String request) {
            super(504, reason, headers, request);
        }
    }
}
