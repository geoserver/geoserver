/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ManifestLoader;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;

/**
 * Base builder for an OpenAPI specification whose template is read on creation, and can be
 * customized on the fly based on the server configuration, status, request context
 */
public class OpenAPIBuilder<T extends ServiceInfo> {

    private final String apiSpecification;
    private final String defaultTitle;
    private final String serviceBase;

    public OpenAPIBuilder(Class clazz, String location, String defaultTitle, String serviceBase) {
        try (InputStream is = clazz.getResourceAsStream(location)) {
            if (is == null) {
                throw new RuntimeException(
                        "Could not find API definition at " + location + " from class " + clazz);
            }
            apiSpecification = IOUtils.toString(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the api template", e);
        }
        this.defaultTitle = defaultTitle;
        this.serviceBase = serviceBase;
    }

    /**
     * Build the document based on request, current service configuration, and list of available
     * extensions. The default implementation adds the "Info" and "Servers" elements, subclasses can
     * override
     *
     * @param service The service configuration
     */
    public OpenAPI build(T service) {
        OpenAPI api = readTemplate();
        addAPIInfo(service, api);
        addServers(api);
        addBasePathFormats(api);

        return api;
    }

    protected void addBasePathFormats(OpenAPI api) {
        declareGetResponseFormats(api, "/", getLandingPageDocumentClass());
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
    }

    /**
     * Returns the landing page document class. By default it returns {@link
     * AbstractLandingPageDocument} in case a service has more representations than usual
     */
    protected Class getLandingPageDocumentClass() {
        return AbstractLandingPageDocument.class;
    }

    public void addServers(OpenAPI api) {
        // the servers
        String wfsUrl =
                ResponseUtils.buildURL(
                        APIRequestInfo.get().getBaseURL(),
                        serviceBase,
                        null,
                        URLMangler.URLType.SERVICE);
        api.servers(Arrays.asList(new Server().description("This server").url(wfsUrl)));
    }

    public void addAPIInfo(T service, OpenAPI api) {
        // build "info"
        ContactInfo contactInfo = service.getGeoServer().getGlobal().getSettings().getContact();
        Contact contact =
                new Contact()
                        .email(contactInfo.getContactEmail())
                        .name(
                                Stream.of(
                                                contactInfo.getContactPerson(),
                                                contactInfo.getContactOrganization())
                                        .filter(s -> s != null)
                                        .collect(Collectors.joining(" - ")))
                        .url(contactInfo.getOnlineResource());
        String title = service.getTitle() == null ? defaultTitle : service.getTitle();
        String version = getGeoServerVersion();
        Info info =
                new Info()
                        .contact(contact)
                        .title(title)
                        .description(service.getAbstract())
                        .version(version);
        api.info(info);
    }

    protected void declareGetResponseFormats(OpenAPI api, String path, Class<?> binding) {
        PathItem pi = api.getPaths().get(path);
        Operation get = pi.getGet();
        Content content = get.getResponses().get("200").getContent();
        List<String> formats =
                APIRequestInfo.get()
                        .getProducibleMediaTypes(binding, true)
                        .stream()
                        .map(mt -> mt.toString())
                        .collect(Collectors.toList());
        // first remove the ones missing
        Set<String> missingFormats = new HashSet<>(content.keySet());
        missingFormats.removeAll(formats);
        missingFormats.forEach(f -> content.remove(f));
        // then add the ones not already declared
        Set<String> extraFormats = new HashSet<>(formats);
        extraFormats.removeAll(content.keySet());
        for (String extraFormat : extraFormats) {
            MediaType mediaType = new MediaType();
            if (extraFormat.contains("yaml") && content.get("application/json") != null) {
                // same schema as JSON
                mediaType.schema(content.get("application/json").getSchema());
            } else if (extraFormat.contains("text")) {
                mediaType.schema(new StringSchema());
            } else {
                mediaType.schema(new BinarySchema());
            }
            content.addMediaType(extraFormat, mediaType);
        }
    }

    /**
     * Reads the template to customize (each time, as the object tree is not thread safe nor
     * cloneable not serializable)
     */
    protected OpenAPI readTemplate() {
        try {
            return Yaml.mapper().readValue(apiSpecification, OpenAPI.class);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected String getGeoServerVersion() {
        ManifestLoader.AboutModel versions = ManifestLoader.getVersions();
        TreeSet<ManifestLoader.AboutModel.ManifestModel> manifests = versions.getManifests();
        return manifests
                .stream()
                .filter(m -> m.getName().equalsIgnoreCase("GeoServer"))
                .map(m -> m.getEntries().get("Version"))
                .findFirst()
                .orElse("1.0.0");
    }
}
