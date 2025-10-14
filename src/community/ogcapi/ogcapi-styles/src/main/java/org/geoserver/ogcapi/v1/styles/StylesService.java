/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.geoserver.ogcapi.v1.styles.StylesService.ValidationMode.only;
import static org.geoserver.ogcapi.v1.styles.StylesService.ValidationMode.yes;

import io.swagger.v3.oas.models.OpenAPI;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ogcapi.APIContentNegotiationManager;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.ResourceNotFoundException;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.StyleWriterConverter;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.xml.sax.EntityResolver;

@APIService(
        service = "Styles",
        version = "1.0.1",
        landingPage = "ogc/styles/v1",
        serviceClass = StylesServiceInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/styles/v1")
public class StylesService {

    private static final String INVALID_STYLE = "InvalidStyle";
    private static final String INVALID_METADATA = "InvalidMetadata";

    static enum ValidationMode {
        yes,
        no,
        only
    }

    public static final String CORE = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/core";
    public static final String HTML = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/html";
    public static final String JSON = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/json";
    public static final String MANAGE = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-styles";
    public static final String VALIDATION = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/style-validation";
    public static final String RESOURCES = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/resources";
    public static final String MANAGE_RESOURCES = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-resources";
    public static final String MAPBOX = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/mapbox-styles";
    public static final String SLD10 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-10";
    public static final String SLD11 = "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-11";
    public static final String CSS = "http://www.geoserver.org/opf-styles-1/1.0/conf/geocss";

    private static final String DISPLAY_NAME = "OGC API Styles";

    private final GeoServer geoServer;
    private final GeoServerDataDirectory dataDirectory;
    private final SampleDataSupport sampleDataSupport;
    private ThumbnailBuilder thumbnailBuilder;
    private Map<MediaType, StyleWriterConverter> writers = new HashMap<>();
    private List<MediaType> mediaTypes = new ArrayList<>();
    private APIContentNegotiationManager contentNegotiationManager = new APIContentNegotiationManager();

    public StylesService(
            GeoServer geoServer,
            GeoServerExtensions extensions,
            GeoServerDataDirectory dataDirectory,
            SampleDataSupport sampleDataSupport,
            ThumbnailBuilder thumbnailBuilder) {
        this.geoServer = geoServer;
        this.dataDirectory = dataDirectory;
        this.sampleDataSupport = sampleDataSupport;
        this.thumbnailBuilder = thumbnailBuilder;
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

    @SuppressWarnings("unused")
    public StylesServiceInfo getServiceInfo() {
        // required for DisabledServiceCheck class
        return getService();
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(CORE, HTML, JSON, MAPBOX, SLD10, SLD11);
        return new ConformanceDocument(DISPLAY_NAME, classes);
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {OPEN_API_MEDIA_TYPE_VALUE, APPLICATION_YAML_VALUE, MediaType.TEXT_XML_VALUE})
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new StylesAPIBuilder().build(getService());
    }

    @GetMapping(path = "styles", name = "getStyleSet")
    @ResponseBody
    @HTMLResponseBody(templateName = "styles.ftl", fileName = "styles.html")
    public StylesDocument getStyles() {
        return new StylesDocument(geoServer.getCatalog());
    }

    @GetMapping(path = "styles/{styleId}", name = "getStyle")
    public void getStyle(
            @PathVariable(name = "styleId") String styleId, NativeWebRequest request, HttpServletResponse response)
            throws HttpMediaTypeNotAcceptableException, IOException {
        StyleInfo styleInfo = getStyleInfo(styleId, true);

        // check the requested media types, if none specific was requested, just copy over the
        // native style
        MediaType nativeMediaType = getNativeMediaType(styleInfo);
        List<MediaType> requestedMediaTypes = contentNegotiationManager.resolveMediaTypes(request);
        if (requestedMediaTypes == null || ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST.equals(requestedMediaTypes)) {
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
                String fileName = styleId + "." + writer.getHandler().getFileExtension();
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);
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

    private StyleInfo getStyleInfo(String styleId, boolean failIfNotFound) {
        StyleInfo style = geoServer.getCatalog().getStyleByName(styleId);
        if (style == null) {
            if (failIfNotFound) {
                throw new ResourceNotFoundException("Could not locate style " + styleId);
            } else {
                return null;
            }
        }
        return style;
    }

    public void writeNativeToResponse(HttpServletResponse response, StyleInfo styleInfo, MediaType nativeMediaType)
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
        StyleHandler handler = writers.values().stream()
                .map(sw -> sw.getHandler())
                .filter(sh -> styleInfo.getFormat().equals(sh.getFormat()))
                .findFirst()
                .orElseThrow(() -> new RestException(
                        "Could not find style handler for style " + styleInfo.prefixedName(),
                        HttpStatus.INTERNAL_SERVER_ERROR));
        Version version = styleInfo.getFormatVersion();
        if (version == null) version = handler.getVersions().get(0);
        return MediaType.valueOf(handler.mimeType(version));
    }

    private Optional<StyleWriterConverter> getWriter(MediaType mediaType) {
        return writers.entrySet().stream()
                .filter(e -> e.getKey().isCompatibleWith(mediaType))
                .map(e -> e.getValue())
                .findFirst();
    }

    @GetMapping(path = "styles/{styleId}/metadata", name = "getStyleMetadata")
    @ResponseBody
    @HTMLResponseBody(templateName = "styleMetadata.ftl", fileName = "styleMetadata.html")
    public StyleMetadataDocument getStyleMetadata(@PathVariable(name = "styleId") String styleId) throws IOException {
        StyleInfo styleInfo = getStyleInfo(styleId, true);
        return new StyleMetadataDocument(styleInfo, geoServer, sampleDataSupport, thumbnailBuilder);
    }

    @GetMapping(path = "styles/{styleId}/thumbnail", name = "getStyleThumbnail", produces = "image/png")
    @ResponseBody
    public void getStyleThumbnail(@PathVariable(name = "styleId") String styleId, HttpServletResponse response)
            throws IOException {
        StyleInfo styleInfo = getStyleInfo(styleId, true);
        // TODO: return webmap instead and allow all GetMap encoders to work?
        RenderedImage image = thumbnailBuilder.buildThumbnailFor(styleInfo);
        if (image == null) {
            throw new APIException(
                    APIException.NO_APPLICABLE_CODE,
                    "Failed to build thumbnail, WMS returned no image",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // TODO: allow any other type of image, leverage the WMS output formats
        response.setHeader(HttpHeaders.CONTENT_TYPE, "image/png");
        ImageIO.write(image, "PNG", response.getOutputStream());
    }

    @PostMapping(
            path = "styles",
            name = "addStyle",
            consumes = {MediaType.ALL_VALUE})
    @ResponseBody
    public ResponseEntity postStyle(
            InputStream inputStream,
            @RequestParam(name = "validate", required = false, defaultValue = "no") ValidationMode validate,
            HttpServletRequest request)
            throws IOException {
        // Extracting mimeType and charset
        MediaType mediaType = MediaType.parseMediaType(request.getContentType());
        String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();

        byte[] rawData = org.geoserver.rest.util.IOUtils.toByteArray(inputStream);
        String charsetName = getCharacterEncoding(request);
        String content = new String(rawData, charsetName);
        StyleHandler handler = getHandler(mimeType);

        // validation
        if (validate == only || validate == yes) {
            validate(mimeType, content, handler);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            String styleId = getStyleId(mimeType, handler, content);
            StyleInfo styleInfo = getStyleInfo(styleId, false);
            if (styleInfo != null) {
                throw new APIException(
                        APIException.CONFLICT, "Style with id " + styleId + " already exists", HttpStatus.CONFLICT);
            }
            Catalog catalog = geoServer.getCatalog();
            styleInfo = catalog.getFactory().createStyle();
            styleInfo.setName(styleId); // assuming global styles?
            styleInfo.setFilename(styleId + "." + handler.getFileExtension());
            styleInfo.setFormat(handler.getFormat());
            styleInfo.setFormatVersion(handler.versionForMimeType(mimeType));
            if (LocalWorkspace.get() != null) {
                styleInfo.setWorkspace(
                        catalog.getWorkspaceByName(LocalWorkspace.get().getName()));
            }
            catalog.add(styleInfo);
            // write out the style body
            catalog.getResourcePool().writeStyle(styleInfo, new ByteArrayInputStream(rawData));

            MultiValueMap<String, String> headers = new HttpHeaders();
            String href = ResponseUtils.buildURL(
                    APIRequestInfo.get().getBaseURL(),
                    "ogc/styles/v1/styles/" + styleId,
                    null,
                    URLMangler.URLType.SERVICE);
            headers.set(HttpHeaders.LOCATION, href);
            return new ResponseEntity<>(headers, HttpStatus.CREATED);
        }
    }

    /**
     * Gets the charset from the content type, or uses the default configured charset, of if even that is missing,
     * returns UTF-8 as a default
     */
    private String getCharacterEncoding(HttpServletRequest request) {
        return Optional.ofNullable(request.getCharacterEncoding())
                .map(Object::toString)
                .orElseGet(() -> Optional.ofNullable(geoServer.getSettings().getCharset())
                        .orElse("UTF-8"));
    }

    /** Tries to get a style identifier from the style name itself, if not found, generates a random one */
    private String getStyleId(String mimeType, StyleHandler handler, String content) throws IOException {
        Catalog catalog = geoServer.getCatalog();
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        StyledLayerDescriptor sld = handler.parse(content, handler.versionForMimeType(mimeType), null, entityResolver);
        String name = sld.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "style-" + UUID.randomUUID();
    }

    @PutMapping(
            path = "styles/{styleId}",
            consumes = {MediaType.ALL_VALUE},
            name = "updateStyle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putStyle(
            InputStream inputStream,
            @PathVariable String styleId,
            @RequestParam(name = "validate", required = false, defaultValue = "no") ValidationMode validate,
            HttpServletRequest request)
            throws IOException {
        // Extracting mimeType and charset
        MediaType mediaType = MediaType.parseMediaType(request.getContentType());
        String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();

        byte[] rawData = org.geoserver.rest.util.IOUtils.toByteArray(inputStream);
        String charsetName = getCharacterEncoding(request);
        String content = new String(rawData, charsetName);
        StyleHandler handler = getHandler(mimeType);

        // validation
        if (validate == only || validate == yes) {
            validate(mimeType, content, handler);
        }

        // if not just validation was requested, save the file and its configuration
        if (validate != only) {
            StyleInfo styleInfo = getStyleInfo(styleId, false);
            Catalog catalog = geoServer.getCatalog();
            if (styleInfo == null) {
                styleInfo = catalog.getFactory().createStyle();
                styleInfo.setName(styleId); // assuming global styles?
                styleInfo.setFilename(styleId + "." + handler.getFileExtension());
                styleInfo.setFormat(handler.getFormat());
                styleInfo.setFormatVersion(handler.versionForMimeType(mimeType));
                if (LocalWorkspace.get() != null) {
                    styleInfo.setWorkspace(
                            catalog.getWorkspaceByName(LocalWorkspace.get().getName()));
                }
                catalog.add(styleInfo);
            } else {
                // the style could be in a different language now
                styleInfo.setFormat(handler.getFormat());
                styleInfo.setFormatVersion(handler.versionForMimeType(mimeType));
                catalog.save(styleInfo);
            }
            // write out the style body
            catalog.getResourcePool().writeStyle(styleInfo, new ByteArrayInputStream(rawData));
        }
    }

    public void validate(String mimeType, String content, StyleHandler handler) {
        try {
            Catalog catalog = geoServer.getCatalog();
            EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
            List<Exception> errors = handler.validate(content, handler.versionForMimeType(mimeType), entityResolver);
            if (errors != null && !errors.isEmpty()) {
                throw new APIException(INVALID_STYLE, "Invalid style:" + errors, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception invalid) {
            throw new APIException(
                    INVALID_STYLE, "Invalid style:" + invalid.getMessage(), HttpStatus.BAD_REQUEST, invalid);
        }
    }

    public StyleHandler getHandler(String mimeType) {
        // checking the style format is supported
        StyleHandler handler;
        try {
            handler = Styles.handler(mimeType);
        } catch (Exception e) {
            throw new APIException(
                    "Illegal input media type",
                    "Failed to lookup style support for media type " + mimeType,
                    HttpStatus.BAD_REQUEST,
                    e);
        }
        return handler;
    }

    @DeleteMapping(path = "styles/{styleId}", name = "deleteStyle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStyle(@PathVariable(name = "styleId") String styleId) {
        StyleInfo styleInfo = getStyleInfo(styleId, true);
        geoServer.getCatalog().remove(styleInfo);
    }

    @PutMapping(path = "styles/{styleId}/metadata", name = "updateStyleMetadata")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putStyleMetadata(@PathVariable("styleId") String styleId, @RequestBody StyleMetadataDocument metadata)
            throws IOException {
        StyleInfo styleInfo = getStyleInfo(styleId, true);
        StyleMetadataInfo metadataInfo = Optional.ofNullable(
                        styleInfo.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class))
                .orElse(new StyleMetadataInfo());

        if (metadata.getId() != null && !styleId.equals(metadata.getId())) {
            throw new APIException(INVALID_METADATA, "Style id must be " + styleId, HttpStatus.BAD_REQUEST);
        }

        // can only update the descriptive metadata, the rest is derived from the style contents
        // and the associations with layers (e.g., sample data)
        metadataInfo.setAbstract(metadata.getDescription());
        metadataInfo.setAccessConstraints(metadata.getAccessConstraints());
        metadataInfo.setKeywords(metadata.getKeywords());
        metadataInfo.setPointOfContact(metadata.getPointOfContact());
        metadataInfo.setTitle(metadata.getTitle());
        metadataInfo.setDates(metadata.getDates());
        styleInfo.getMetadata().put(StyleMetadataInfo.METADATA_KEY, metadataInfo);

        geoServer.getCatalog().save(styleInfo);
    }

    @PatchMapping(
            path = "styles/{styleId}/metadata",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            name = "patchStyleMetadata")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchMetadata(@PathVariable("styleId") String styleId, @RequestBody StyleMetadataDocument metadata) {
        StyleInfo styleInfo = getStyleInfo(styleId, true);
        StyleMetadataInfo metadataInfo = Optional.ofNullable(
                        styleInfo.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class))
                .orElse(new StyleMetadataInfo());

        if (metadata.isDescriptionSet()) {
            metadataInfo.setAbstract(metadata.getDescription());
        }
        if (metadata.isAccessConstraintsSet()) {
            metadataInfo.setAccessConstraints(metadata.getAccessConstraints());
        }
        if (metadata.isKeywordsSet()) {
            metadataInfo.setKeywords(metadata.getKeywords());
        }
        if (metadata.isPointOfContactSet()) {
            metadataInfo.setPointOfContact(metadata.getPointOfContact());
        }
        if (metadata.isTitleSet()) {
            metadataInfo.setTitle(metadata.getTitle());
        }
        if (metadata.isDatesSet() || metadata.getDates() != null) {
            StyleDates inputDates = metadata.getDates();
            StyleDates dates = Optional.ofNullable(metadataInfo.getDates()).orElse(new StyleDates());
            if (inputDates.isCreationSet()) {
                dates.setCreation(inputDates.getCreation());
            }
            if (inputDates.isPublicationSet()) {
                dates.setPublication(inputDates.getPublication());
            }
            if (inputDates.isReceivedOnSet()) {
                dates.setReceivedOn(inputDates.getReceivedOn());
            }
            if (inputDates.isRevisionSet()) {
                dates.setRevision(inputDates.getRevision());
            }
            if (inputDates.isValidTillSet()) {
                dates.setValidTill(inputDates.getValidTill());
            }

            metadataInfo.setDates(dates);
        }

        styleInfo.getMetadata().put(StyleMetadataInfo.METADATA_KEY, metadataInfo);
        geoServer.getCatalog().save(styleInfo);
    }
}
