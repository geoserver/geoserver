/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.ManifestLoader;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.ContactInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.response.CollectionsDocument;
import org.geoserver.wfs3.response.ConformanceDocument;

/** Builds the OpenAPI document that will be returned to the clients */
public class OpenAPIBuilder {

    static final String OPENAPI_SPECIFICATION;

    static {
        try (InputStream is = OpenAPIBuilder.class.getResourceAsStream("openapi.yaml")) {
            OPENAPI_SPECIFICATION = IOUtils.toString(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the openapi.yaml template", e);
        }
    }

    /**
     * Build the document based on request, current WFS configuration, and list of available
     * extensions
     *
     * @param request The incoming request
     * @param wfs The WFS configuration
     * @param extensions The list of WFS 3 extensions
     */
    public OpenAPI build(BaseRequest request, WFSInfo wfs, List<WFS3Extension> extensions) {
        OpenAPI api = readTemplate();

        // build "info"
        ContactInfo contactInfo = wfs.getGeoServer().getGlobal().getSettings().getContact();
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
        String title = wfs.getTitle() == null ? "WFS 3.0 server" : wfs.getTitle();
        String version = getGeoServerVersion();
        Info info =
                new Info()
                        .contact(contact)
                        .title(title)
                        .description(wfs.getAbstract())
                        .version(version);
        api.info(info);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation()
                        .description("WFS specification")
                        .url("https://github.com/opengeospatial/WFS_FES"));

        // the servers
        String wfsUrl =
                ResponseUtils.buildURL(
                        request.getBaseUrl(), "wfs3", null, URLMangler.URLType.SERVICE);
        api.servers(Arrays.asList(new Server().description("This server").url(wfsUrl)));

        // adjust path output formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionsDocument.class);
        declareGetResponseFormats(
                api, "/collections/{collectionId}/items", FeatureCollectionResponse.class);
        declareGetResponseFormats(
                api,
                "/collections/{collectionId}/items/{featureId}",
                FeatureCollectionResponse.class);

        // provide a list of valid values for collectionId
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter collectionId = parameters.get("collectionId");
        Catalog catalog = wfs.getGeoServer().getCatalog();
        List<String> validCollectionIds =
                catalog.getFeatureTypes()
                        .stream()
                        .map(ft -> NCNameResourceCodec.encode(ft))
                        .collect(Collectors.toList());
        collectionId.getSchema().setEnum(validCollectionIds);

        // provide actual values for limit
        Parameter limit = parameters.get("limit");
        BigDecimal limitMax;
        if (wfs.getMaxFeatures() > 0) {
            limitMax = BigDecimal.valueOf(wfs.getMaxFeatures());
        } else {
            limitMax = BigDecimal.valueOf(Integer.MAX_VALUE);
        }
        limit.getSchema().setMaximum(limitMax);
        // for the moment we don't have a setting for the default, keep it same as max
        limit.getSchema().setDefault(limitMax);

        // handle the extensions
        for (WFS3Extension extension : extensions) {
            extension.extendAPI(api);
        }

        return api;
    }

    private void declareGetResponseFormats(OpenAPI api, String path, Class<?> binding) {
        PathItem pi = api.getPaths().get(path);
        Operation get = pi.getGet();
        Content content = get.getResponses().get("200").getContent();
        List<String> formats = DefaultWebFeatureService30.getAvailableFormats(binding);
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
    private OpenAPI readTemplate() {
        try {
            return Yaml.mapper().readValue(OPENAPI_SPECIFICATION, OpenAPI.class);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private String getGeoServerVersion() {
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
