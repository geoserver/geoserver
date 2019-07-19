/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.api.APIContentNegotiationManager;
import org.geoserver.api.APIDispatcher;
import org.geoserver.api.APIService;
import org.geoserver.api.ConformanceDocument;
import org.geoserver.api.HTMLResponseBody;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.api.OpenAPIMessageConverter;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.StyleWriterConverter;
import org.geotools.util.Version;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;

@APIService(
    service = "Style",
    version = "1.0",
    landingPage = "ogc/styles",
    serviceClass = StylesServiceInfo.class
)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/styles")
public class StylesService {

    public static final String CORE = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/core";
    public static final String HTML = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/html";
    public static final String JSON = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/json";
    public static final String MANAGE =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-styles";
    public static final String VALIDATION =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/style-validation";
    public static final String RESOURCES =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/resources";
    public static final String MANAGE_RESOURCES =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-resources";
    public static final String MAPBOX =
            "http://www.opengis.net/t15/opf-styles-1/1.0/conf/mapbox-styles";
    public static final String SLD10 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-10";
    public static final String SLD11 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-11";
    public static final String CSS = "http://www.geoserver.org/opf-styles-1/1.0/conf/geocss";

    private final GeoServer geoServer;
    private final GeoServerDataDirectory dataDirectory;
    Map<MediaType, StyleWriterConverter> writers = new HashMap<>();
    List<MediaType> mediaTypes = new ArrayList<>();
    APIContentNegotiationManager contentNegotiationManager = new APIContentNegotiationManager();

    public StylesService(
            GeoServer geoServer,
            GeoServerExtensions extensions,
            GeoServerDataDirectory dataDirectory) {
        this.geoServer = geoServer;
        this.dataDirectory = dataDirectory;
        List<StyleHandler> handlers = extensions.extensions(StyleHandler.class);
        for (StyleHandler sh : handlers) {
            for (Version ver : sh.getVersions()) {
                String mimeType = sh.mimeType(ver);
                MediaType mediaType = MediaType.valueOf(mimeType);
                writers.put(mediaType, new StyleWriterConverter(mimeType, ver, sh));
                mediaTypes.add(mediaType);
            }
        }
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public StylesLandingPage getLandingPage() {
        StylesServiceInfo styles = getService();
        return new StylesLandingPage(
                (styles.getTitle() == null) ? "Styles server" : styles.getTitle(),
                (styles.getAbstract() == null) ? "" : styles.getAbstract());
    }

    public StylesServiceInfo getService() {
        return geoServer.getService(StylesServiceInfo.class);
    }

    @GetMapping(path = "conformance", name = "getConformanceClasses")
    @ResponseBody
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE, HTML, JSON, MAPBOX, SLD10, SLD11);
        return new ConformanceDocument(classes);
    }

    @GetMapping(
        path = "api",
        name = "api",
        produces = {
            OpenAPIMessageConverter.OPEN_API_VALUE,
            "application/x-yaml",
            MediaType.TEXT_XML_VALUE
        }
    )
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() {
        return new StylesAPIBuilder().build(getService());
    }

    @GetMapping(path = "styles", name = "getStyleSet")
    @ResponseBody
    public StylesDocument getStyles() {
        List<String> classes = Arrays.asList(CORE, HTML, JSON, MAPBOX, SLD10, SLD11);
        return new StylesDocument(geoServer.getCatalog());
    }

    @GetMapping(path = "styles/{styleId}")
    public void getStyle(
            @PathVariable(name = "styleId") String styleId,
            NativeWebRequest request,
            HttpServletResponse response)
            throws HttpMediaTypeNotAcceptableException, IOException {
        StyleInfo styleInfo = getStyleInfo(styleId);

        // check the requested media types, if none specific was requested, just copy over the
        // native style
        MediaType nativeMediaType = getNativeMediaType(styleInfo);
        List<MediaType> requestedMediaTypes = contentNegotiationManager.resolveMediaTypes(request);
        if (requestedMediaTypes == null
                || ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST.equals(requestedMediaTypes)) {
            writeNativeToResponse(response, styleInfo, nativeMediaType);
            return;
        }

        // loop over the requested media
        for (MediaType requestedMediaType : requestedMediaTypes) {
            if (requestedMediaType.isCompatibleWith(nativeMediaType)) {
                writeNativeToResponse(response, styleInfo, nativeMediaType);
                return;
            }

            Optional<StyleWriterConverter> osh = getWriter(requestedMediaType);
            if (osh.isPresent()) {
                StyleWriterConverter writer = osh.get();
                writer.write(
                        styleInfo,
                        MediaType.valueOf(writer.getHandler().mimeType(writer.getVersion())),
                        new ServletServerHttpResponse(response));
                return;
            }
        }

        throw new RestException(
                "Could not find a stle encoder for requested media types " + requestedMediaTypes,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    private StyleInfo getStyleInfo(String styleId) {
        List<StyleInfo> styles = NCNameResourceCodec.getStyles(geoServer.getCatalog(), styleId);
        if (styles == null || styles.isEmpty()) {
            throw new RestException("Could not locate style " + styleId, HttpStatus.NOT_FOUND);
        }
        if (styles.size() > 1) {
            throw new RestException(
                    "More than one style can be matched to "
                            + styleId
                            + " please contact the service administrator about this",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return styles.get(0);
    }

    public void writeNativeToResponse(
            HttpServletResponse response, StyleInfo styleInfo, MediaType nativeMediaType)
            throws IOException {
        response.setContentType(nativeMediaType.toString());
        response.setStatus(200);
        Resource resource = dataDirectory.style(styleInfo);
        try (InputStream in = resource.in()) {
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    private MediaType getNativeMediaType(StyleInfo styleInfo) {
        String format = styleInfo.getFormat();
        if (format == null) {
            return MediaType.valueOf(SLDHandler.MIMETYPE_10);
        }
        StyleHandler handler =
                writers.values()
                        .stream()
                        .map(sw -> sw.getHandler())
                        .filter(sh -> styleInfo.getFormat().equals(sh.getFormat()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new RestException(
                                                "Could not find style handler for style "
                                                        + styleInfo.prefixedName(),
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        Version version = styleInfo.getFormatVersion();
        if (version == null) version = handler.getVersions().get(0);
        return MediaType.valueOf(handler.mimeType(version));
    }

    private Optional<StyleWriterConverter> getWriter(MediaType mediaType) {
        return writers.entrySet()
                .stream()
                .filter(e -> e.getKey().isCompatibleWith(mediaType))
                .map(e -> e.getValue())
                .findFirst();
    }

    @GetMapping(path = "styles/{styleId}/metadata")
    @ResponseBody
    public StyleMetadataDocument getStyleMetadata(@PathVariable(name = "styleId") String styleId)
            throws IOException {
        StyleInfo styleInfo = getStyleInfo(styleId);
        return new StyleMetadataDocument(styleInfo, geoServer);
    }
}
