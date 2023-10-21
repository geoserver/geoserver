package org.geoserver.restconfig.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign.Builder;
import feign.FeignException;
import feign.Logger;
import feign.Logger.Level;
import feign.Request.Body;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.codec.StringDecoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.openapi.client.internal.ApiClient;
import org.geoserver.openapi.client.internal.ApiClient.Api;
import org.geoserver.openapi.client.internal.auth.HttpBasicAuth;
import org.geoserver.openapi.v1.client.ManifestsApi;
import org.geoserver.openapi.v1.model.ComponentVersion;
import org.geoserver.openapi.v1.model.VersionResponse;

/**
 * A client for GeoServer REST config API
 *
 * <p>Usage:
 *
 * <pre>
 * <code>
 * GeoServerClient client = new GeoServerClient()
 * 							.setBasePath("http://localhost:8080/geoserver/rest")
 * 							.setBasicAuth("admin", "geoserver");
 * List<String> workspaceNames = client.workspaces().findAllNames();
 * </code>
 * </pre>
 *
 * Note: all API calls that receive a server side error throw {@link ServerException}
 *
 * @see WorkspacesClient
 * @see DataStoresClient
 * @see FeatureTypesClient
 */
public class GeoServerClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/geoserver/rest";
    final /* VisibleForTests */ ApiClient apiClient;

    public GeoServerClient() {
        this(DEFAULT_BASE_URL);
    }

    private static class ByContentTypeStringDecoder extends StringDecoder {

        private final Set<String> supportedMimeTypes;

        ByContentTypeStringDecoder(String... supportedMimeTypes) {
            this.supportedMimeTypes = new HashSet<>(Arrays.asList(supportedMimeTypes));
        }

        public boolean canHandle(String contentType) {
            return contentType != null
                    && (contentType.startsWith("text/plain")
                            || supportedMimeTypes.contains(contentType));
        }
    }

    private static @RequiredArgsConstructor class BinaryEncoder implements feign.codec.Encoder {

        private final Encoder delegate;

        public @Override void encode(Object object, Type bodyType, RequestTemplate template)
                throws EncodeException {
            if (byte[].class.equals(bodyType)) {
                if (object != null) {
                    byte[] bytes;
                    if (object instanceof String) {
                        bytes = ((String) object).getBytes(UTF_8);
                    } else if (object instanceof byte[]) {
                        bytes = (byte[]) object;
                    } else {
                        throw new IllegalArgumentException(
                                "Don't know how to convert "
                                        + object.getClass().getName()
                                        + " to byte[]");
                    }
                    Body body = Body.encoded(bytes, UTF_8);
                    template.body(body);
                }
            } else {
                delegate.encode(object, bodyType, template);
            }
        }
    }

    public static ApiClient newApiClient() {
        ApiClient client = new ApiClient();
        final ObjectMapper objectMapper = client.getObjectMapper();
        // do not serialize null or empty collections. This is important for GeoServer
        // not trying to override/parse *Info attributes when not needed
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return client;
    }

    public GeoServerClient(@NonNull String restApiEntryPoint) {
        apiClient = newApiClient();
        setBasePath(restApiEntryPoint);
        final ObjectMapper objectMapper = apiClient.getObjectMapper();
        Builder feignBuilder = apiClient.getFeignBuilder();
        feignBuilder.errorDecoder(new GeoServerFeignErrorDecoder());
        // use okhttp client, the default one doesn't send request headers correctly
        feignBuilder.client(new OkHttpClient());
        // but avoid error "feign.FeignException: source exhausted prematurely reading POST" due to
        // server sending incorrect content-length
        feignBuilder.requestInterceptor(
                new RequestInterceptor() {
                    public @Override void apply(RequestTemplate template) {
                        template.header("Accept-Encoding", "identity");
                    }
                });

        Decoder decoder =
                new Decoder() {

                    final JacksonDecoder jacksonDecoder = new JacksonDecoder(objectMapper);
                    final ByContentTypeStringDecoder stringDecoder =
                            new ByContentTypeStringDecoder( //
                                    StylesClient.StyleFormat.MAPBOX.getMimeType(), //
                                    StylesClient.StyleFormat.SLD_1_0_0.getMimeType(), //
                                    StylesClient.StyleFormat.SLD_1_1_0.getMimeType(), //
                                    StylesClient.StyleFormat.GEOCSS.getMimeType(), //
                                    StylesClient.StyleFormat.YSLD.getMimeType() //
                                    );

                    @Override
                    public Object decode(Response response, Type type)
                            throws IOException, DecodeException, FeignException {
                        Collection<String> contentType = response.headers().get("Content-Type");
                        String mime =
                                contentType == null || contentType.isEmpty()
                                        ? null
                                        : contentType.iterator().next();
                        if (stringDecoder.canHandle(mime)) {
                            return stringDecoder.decode(response, type);
                        }
                        return jacksonDecoder.decode(response, type);
                    }
                };
        feignBuilder.decoder(decoder);
        feignBuilder.encoder(new BinaryEncoder(new FormEncoder(new JacksonEncoder(objectMapper))));
    }

    public List<ComponentVersion> getComponentsVersion() {
        VersionResponse response = api(ManifestsApi.class).getComponentVersions(null, null, null);
        return response.getAbout().getResource();
    }

    public void setDebugRequests(boolean debug) {
        api().getFeignBuilder().logLevel(debug ? Level.FULL : Level.NONE);
    }

    public void logToStdErr() {
        api().getFeignBuilder().logger(new Logger.ErrorLogger());
    }

    /**
     * Utility method to clone a model object to overcome the issue that openapi-codegen generated
     * models don't provide a copy constructor
     */
    @SuppressWarnings("unchecked")
    public @NonNull <T> T clone(@NonNull T modelObject) {
        try {
            ObjectMapper objectMapper = apiClient.getObjectMapper();
            String serialized = objectMapper.writeValueAsString(modelObject);
            Object readValue = objectMapper.readValue(serialized, modelObject.getClass());
            return (T) readValue;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public <API extends Api> API api(@NonNull Class<API> apiClass) {
        return api().buildClient(apiClass);
    }

    ApiClient api() {
        return apiClient;
    }

    public GeoServerClient setBasePath(@NonNull String restApiEntryPoint) {
        apiClient.setBasePath(restApiEntryPoint);
        return this;
    }

    public String getBaseURL() {
        return apiClient.getBasePath();
    }

    public GeoServerClient setBasicAuth(@NonNull String userName, @NonNull String password) {
        if (!apiClient.getApiAuthorizations().containsKey("basicAuth")) {
            apiClient.addAuthorization("basicAuth", new HttpBasicAuth());
        }
        apiClient.setCredentials(userName, password);
        return this;
    }

    public GeoServerClient setRequestHeaderAuth(
            @NonNull String authName, @NonNull Map<String, String> authHeaders) {
        RequestInterceptor interceptor =
                new RequestInterceptor() {
                    private final Map<String, String> headers = new HashMap<>(authHeaders);

                    public @Override void apply(RequestTemplate template) {
                        for (String name : headers.keySet()) {
                            String value = headers.get(name);
                            template.header(name, value);
                        }
                    }
                };
        apiClient.getApiAuthorizations().remove("basicAuth");
        apiClient.addAuthorization(authName, interceptor);
        return this;
    }

    public SettingsClient settings() {
        return new SettingsClient(this);
    }

    public OwsServicesClient ows() {
        return new OwsServicesClient(this);
    }

    public WorkspacesClient workspaces() {
        return new WorkspacesClient(this);
    }

    public NamespacesClient namespaces() {
        return new NamespacesClient(this);
    }

    public CoverageStoresClient coverageStores() {
        return new CoverageStoresClient(this);
    }

    public CoveragesClient coverages() {
        return new CoveragesClient(this);
    }

    public DataStoresClient dataStores() {
        return new DataStoresClient(this);
    }

    public FeatureTypesClient featureTypes() {
        return new FeatureTypesClient(this);
    }

    public LayersClient layers() {
        return new LayersClient(this);
    }

    public StylesClient styles() {
        return new StylesClient(this);
    }
}
