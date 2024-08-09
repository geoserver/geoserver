/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(
        service = "3D-GeoVolumes",
        version = "1.0.0",
        landingPage = "ogc/3dgeovolumes/v1",
        serviceClass = GeoVolumesServiceInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/3dgeovolumes/v1")
public class GeoVolumesService {

    private static final String DISPLAY_NAME = "OGC API 3D GeoVolumes";
    private static final String GEOVOLUME_CONF_BASE =
            "http://www.opengis.net/spec/ogcapi-geovolumes-1/1.0";
    public static final String CORE = GEOVOLUME_CONF_BASE + "req/core";
    public static final String OAS = CORE + "/conf";
    private final GeoVolumesProvider geoVolumesProvider;

    GeoServer geoServer;

    public GeoVolumesService(GeoServer geoServer, GeoVolumesProvider geoVolumesProvider) {
        this.geoServer = geoServer;
        this.geoVolumesProvider = geoVolumesProvider;
    }

    public GeoVolumesServiceInfo getService() {
        return geoServer.getService(GeoVolumesServiceInfo.class);
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public GeoVolumesLandingPage getLandingPage() {
        GeoVolumesServiceInfo info = getService();
        return new GeoVolumesLandingPage(
                (info.getTitle() == null) ? "3D Geovolumes service" : info.getTitle(),
                (info.getAbstract() == null) ? "" : info.getAbstract());
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {
                OPEN_API_MEDIA_TYPE_VALUE,
                APPLICATION_YAML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new GeoVolumesAPIBuilder(geoVolumesProvider.getGeoVolumes()).build(getService());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes =
                Arrays.asList(
                        // generic conformance classes
                        ConformanceClass.CORE,
                        ConformanceClass.COLLECTIONS,
                        ConformanceClass.JSON,
                        ConformanceClass.HTML,
                        // 3D geovolumes specific conformance classes
                        CORE);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(path = "collections", name = "describe3DContainers")
    @ResponseBody
    @HTMLResponseBody(templateName = "collections.ftl", fileName = "collections.html")
    public GeoVolumes getCollections() throws IOException {
        GeoVolumes result = new GeoVolumes(geoVolumesProvider.getGeoVolumes());
        result.updateLinks();
        return result;
    }

    @GetMapping(path = "collections/{3d-containerID}", name = "describe3DContainer")
    @ResponseBody
    @HTMLResponseBody(templateName = "collection.ftl", fileName = "collection.html")
    public GeoVolume collection(@PathVariable(name = "3d-containerID") String collectionId)
            throws IOException {
        GeoVolumes geoVolumes = geoVolumesProvider.getGeoVolumes();
        Optional<GeoVolume> first =
                geoVolumes.getCollections().stream()
                        .filter(gv -> Objects.equals(gv.getId(), collectionId))
                        .findFirst();
        if (first.isPresent()) {
            // update the links to reflec the current request, self/alternate depend on what was
            // requested
            GeoVolume geoVolume = first.get();
            GeoVolume result = new GeoVolume(geoVolume);
            result.updateLinks();
            return result;
        } else {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "Collection not found: " + collectionId,
                    HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
            path = "cesium",
            produces = {MediaType.TEXT_HTML_VALUE})
    @ResponseBody
    @HTMLResponseBody(templateName = "cesium.ftl", fileName = "cesium.html")
    public Object getCesiumViewer() {
        return "cesium";
    }

    @GetMapping(
            path = "i3s",
            produces = {MediaType.TEXT_HTML_VALUE})
    @ResponseBody
    @HTMLResponseBody(templateName = "i3sclient.ftl", fileName = "i3sclient.html")
    public Object geti3sViewer() {
        return "i3s";
    }

    @GetMapping(path = "collections/{3d-containerID}/**", name = "getResource")
    public void resource(
            @PathVariable(name = "3d-containerID") String collectionId,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        String pathInfo = request.getPathInfo();
        String path = getRelativePath(pathInfo);

        Resource resource = geoVolumesProvider.getResource(path);
        if (resource.getType() != Resource.Type.RESOURCE) {
            throw new APIException(
                    APIException.NOT_FOUND, "No resource found at " + path, HttpStatus.NOT_FOUND);
        }

        String contentType = getContentType(resource, request.getServletContext());
        response.setContentType(contentType);
        try (InputStream is = resource.in()) {
            IOUtils.copy(is, response.getOutputStream());
        }
    }

    private String getRelativePath(String pathInfo) throws IOException {
        // it starts with the well known base
        String base = "/3dgeovolumes/v1/collections/";
        int idxBase = pathInfo.indexOf(base);
        if (idxBase == -1) return null;

        // it has a collection name
        return pathInfo.substring(idxBase + base.length());
    }

    private String getContentType(Resource resource, ServletContext servletContext) {
        String contentType = servletContext.getMimeType(resource.name());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }
}
