/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.xml.resolver.apps.resolver;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.SLDNamedLayerValidator;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.EntityResolver;

/** Example style resource controller */
@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH,
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class StyleController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        value = {"/styles", "/layers/{layerName}/styles", "/workspaces/{workspaceName}/styles"}
    )
    public RestWrapper<?> stylesGet(
            @PathVariable(required = false) String layerName,
            @PathVariable(required = false) String workspaceName) {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }

        if (layerName != null) {
            return wrapList(catalog.getLayerByName(layerName).getStyles(), StyleInfo.class);
        } else if (workspaceName != null) {
            return wrapList(catalog.getStylesByWorkspace(workspaceName), StyleInfo.class);
        }
        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return wrapList(styles, StyleInfo.class);
    }

    @PostMapping(
        value = {"/styles", "/layers/{layerName}/styles", "/workspaces/{workspaceName}/styles"},
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        },
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public String stylePost(
            @RequestBody StyleInfo style,
            @PathVariable(required = false) String layerName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(defaultValue = "false", name = "default") boolean makeDefault) {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);

        if (layerName != null) {
            StyleInfo existing = catalog.getStyleByName(style.getName());
            if (existing == null) {
                throw new ResourceNotFoundException();
            }

            LayerInfo l = catalog.getLayerByName(layerName);
            l.getStyles().add(existing);

            // check for default
            if (makeDefault) {
                l.setDefaultStyle(existing);
            }
            catalog.save(l);
            LOGGER.info("POST style " + style.getName() + " to layer " + layerName);
        } else {

            if (workspaceName != null) {
                style.setWorkspace(catalog.getWorkspaceByName(workspaceName));
            }

            catalog.add(style);
            LOGGER.info("POST style " + style.getName());
        }

        return style.getName();
    }

    @PostMapping(
        value = {"/styles", "/workspaces/{workspaceName}/styles"},
        consumes = {MediaTypeExtensions.APPLICATION_ZIP_VALUE}
    )
    public ResponseEntity<String> stylePost(
            InputStream stream,
            @RequestParam(required = false) String name,
            @PathVariable(required = false) String workspaceName,
            UriComponentsBuilder builder)
            throws IOException {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);

        File directory = unzipSldPackage(stream);
        File uploadedFile = getSldFileFromDirectory(directory);

        Style styleSld = parseSld(uploadedFile);

        if (name == null) {
            name = getNameFromStyle(styleSld);
        }
        checkStyleNotExists(workspaceName, name);

        saveImageResources(directory, workspaceName);

        StyleHandler handler = Styles.handler("sld");
        Version version = handler.version(uploadedFile);
        StyleInfo styleInfo =
                createStyleInfo(workspaceName, name, handler, handler.mimeType(version));

        checkStyleResourceNotExists(styleInfo);
        writeStyleRaw(styleInfo, new FileInputStream(uploadedFile));

        catalog.add(styleInfo);

        LOGGER.info("POST Style Package: " + name + ", workspace: " + workspaceName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(name, workspaceName, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(name, headers, HttpStatus.CREATED);
    }

    @PostMapping(
        value = {"/styles", "/workspaces/{workspaceName}/styles"},
        consumes = {MediaType.ALL_VALUE}
    )
    public ResponseEntity<String> styleSLDPost(
            InputStream inputStream,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(required = false) String name,
            @RequestHeader("Content-Type") String contentType,
            @RequestParam(name = "raw", required = false, defaultValue = "false") boolean raw,
            UriComponentsBuilder builder)
            throws IOException {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);

        String mimeType = getMimeTypeFromContentType(contentType);
        String charset = getCharsetFromContentType(contentType);

        byte[] rawData = IOUtils.toByteArray(inputStream);
        String content = new String(rawData, charset);
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();

        StyleHandler handler = Styles.handler(mimeType);
        Version version = handler.versionForMimeType(mimeType);

        StyledLayerDescriptor sld = handler.parse(content, version, null, entityResolver);
        Style style = Styles.style(sld);

        if (name == null) {
            name = getNameFromStyle(style);
        }
        checkStyleNotExists(workspaceName, name);
        StyleInfo sinfo = createStyleInfo(workspaceName, name, handler, mimeType);
        checkStyleResourceNotExists(sinfo);

        try {
            writeStyle(raw, sinfo, sld, rawData, handler, version);
        } catch (Exception e) {
            throw new RestException("Error writing style", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        catalog.add(sinfo);
        LOGGER.info("POST Style " + name);
        // build the new path
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(name, workspaceName, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(name, headers, HttpStatus.CREATED);
    }

    @GetMapping(
        path = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"},
        produces = {MediaType.ALL_VALUE}
    )
    protected RestWrapper<StyleInfo> styleGet(
            @PathVariable String styleName, @PathVariable(required = false) String workspaceName) {

        return wrapObject(getStyleInternal(styleName, workspaceName), StyleInfo.class);
    }

    @GetMapping(
        path = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"},
        produces = {SLDHandler.MIMETYPE_10, SLDHandler.MIMETYPE_11}
    )
    protected StyleInfo styleSLDGet(
            @PathVariable String styleName, @PathVariable(required = false) String workspaceName) {

        return getStyleInternal(styleName, workspaceName);
    }

    protected StyleInfo getStyleInternal(String styleName, String workspace) {
        LOGGER.fine("GET style " + styleName);
        StyleInfo sinfo = catalog.getStyleByName(workspace, styleName);

        if (sinfo == null) {
            String message = "No such style: " + styleName;
            if (workspace != null) {
                message = "No such style " + styleName + " in workspace " + workspace;
            }
            throw new ResourceNotFoundException(message);
        } else {
            return sinfo;
        }
    }

    @DeleteMapping(path = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"})
    protected void styleDelete(
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(required = false, defaultValue = "false") boolean recurse,
            @RequestParam(required = false, defaultValue = "false") boolean purge)
            throws IOException {

        checkWorkspaceName(workspaceName);

        StyleInfo style =
                workspaceName != null
                        ? catalog.getStyleByName(workspaceName, styleName)
                        : catalog.getStyleByName(styleName);

        if (style == null) {
            throw new ResourceNotFoundException("Style " + styleName + " not found");
        }

        if (recurse) {
            new CascadeDeleteVisitor(catalog).visit(style);
        } else {
            // ensure that no layers reference the style
            List<LayerInfo> layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                throw new RestException(
                        "Can't delete style referenced by existing layers.", HttpStatus.FORBIDDEN);
            }
            catalog.remove(style);
        }

        catalog.getResourcePool().deleteStyle(style, purge);

        LOGGER.info("DELETE style " + styleName);
    }

    @PutMapping(
        value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"},
        consumes = {MediaTypeExtensions.APPLICATION_ZIP_VALUE}
    )
    public void styleZipPut(
            InputStream is,
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);

        File directory = null;
        try {
            directory = unzipSldPackage(is);
            File uploadedFile = getSldFileFromDirectory(directory);

            Style styleSld = parseSld(uploadedFile);

            if (styleName == null) {
                styleName = getNameFromStyle(styleSld);
            }

            if (styleName == null) {
                throw new RestException("Style must have a name.", HttpStatus.BAD_REQUEST);
            }

            // ensure that the style already exists
            if (!existsStyleInCatalog(workspaceName, styleName)) {
                throw new RestException(
                        "Style " + styleName + " doesn't exist.", HttpStatus.FORBIDDEN);
            }

            // save image resources
            saveImageResources(directory, workspaceName);

            // Save the style: serialize the style out into the data directory
            StyleInfo styleInfo = catalog.getStyleByName(workspaceName, styleName);
            writeStyleRaw(styleInfo, new FileInputStream(uploadedFile));
            catalog.save(styleInfo);

            LOGGER.info("PUT Style Package: " + styleName + ", workspace: " + workspaceName);

        } catch (Exception e) {
            LOGGER.severe("Error processing the style package (PUT): " + e.getMessage());
            throw new RestException(
                    "Error processing the style", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } finally {
            FileUtils.deleteQuietly(directory);
        }
    }

    /** Workaround to support regular response content type when extension is in path */
    @Configuration
    static class StyleControllerConfiguration {
        @Bean
        PutIgnoringExtensionContentNegotiationStrategy stylePutContentNegotiationStrategy() {
            return new PutIgnoringExtensionContentNegotiationStrategy(
                    new PatternsRequestCondition(
                            "/styles/{styleName}",
                            "/workspaces/{workspaceName}/styles/{styleName}"),
                    Arrays.asList(
                            MediaType.APPLICATION_JSON,
                            MediaType.APPLICATION_XML,
                            MediaType.TEXT_HTML));
        }
    }

    @PutMapping(
        value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"},
        consumes = {MediaType.ALL_VALUE}
    )
    public void stylePut(
            InputStream inputStream,
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(name = "raw", required = false, defaultValue = "false") boolean raw,
            @RequestHeader("Content-Type") String contentType)
            throws IOException {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);
        StyleInfo info = catalog.getStyleByName(workspaceName, styleName);

        // Extracting mimeType and charset from content type formated as "Content-Type: text/html;
        // charset=utf-8"
        String mimeType = getMimeTypeFromContentType(contentType);
        String charset = getCharsetFromContentType(contentType);

        byte[] rawData = IOUtils.toByteArray(inputStream);
        String content = new String(rawData, charset);
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        if (raw) {
            writeStyleRaw(info, new ByteArrayInputStream(rawData));

            try {
                // figure out if we need a version switch
                for (StyleHandler handler : Styles.handlers()) {
                    if (Objects.equals(info.getFormat(), handler.getFormat())) {
                        Version version = Styles.handler(info.getFormat()).version(content);
                        if (version != null) {
                            info.setFormatVersion(version);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not determine the version of the raw style, the previous one was "
                                + "retained",
                        e);
            }
            catalog.save(info);
        } else {
            try {
                StyleHandler handler = Styles.handler(mimeType);
                Version version = handler.versionForMimeType(mimeType);

                StyledLayerDescriptor sld = handler.parse(content, version, null, entityResolver);
                writeStyle(false, info, sld, rawData, handler, version);
                catalog.save(info);
            } catch (Exception invalid) {
                throw new RestException(
                        "Invalid style:" + invalid.getMessage(), HttpStatus.BAD_REQUEST, invalid);
            }
        }
    }

    @PutMapping(
        value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"},
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        }
    )
    public void stylePut(
            @RequestBody StyleInfo info,
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        checkWorkspaceName(workspaceName);
        checkFullAdminRequired(workspaceName);

        StyleInfo original = catalog.getStyleByName(workspaceName, styleName);
        new CatalogBuilder(catalog).updateStyle(original, info);

        catalog.save(original);
    }

    /* Style parsing and encoding utilities ******************************************************/

    /**
     * Writes a valid StyledLayerDescriptor to a style resource in the requested format. If the
     * requested format does not support encoding from a StyledLayerDescriptor, instead writes the
     * raw style to the style resource.
     *
     * <p>If the StyledLayerDescriptor contains multiple StyledLayers, assumes it represents a style
     * group, and verifies that all StyledLayers contain valid layer references.
     *
     * @param info Style info object, containing details about the style format and location
     * @param sld StyledLayerDescriptor representing the style
     * @param rawData Raw bytes of the original style, before it was parsed to a
     *     StyledLayerDescriptor
     * @param handler A {@link StyleHandler} compatible with the format of the style
     * @param version The version of the style format.
     * @throws Exception if there was an error persisting the style, or if there was a validation
     *     error.
     */
    private void writeStyle(
            boolean raw,
            StyleInfo info,
            StyledLayerDescriptor sld,
            byte[] rawData,
            StyleHandler handler,
            Version version)
            throws Exception {
        ResourcePool resourcePool = catalog.getResourcePool();

        // If there is more than one layer, assume this is a style group and validate accordingly.
        if (sld.getStyledLayers().length > 1) {
            List<Exception> validationErrors = SLDNamedLayerValidator.validate(catalog, sld);
            if (validationErrors.size() > 0) {
                throw validationErrors.get(0);
            }
        }

        // only SLD 1.0 handler can write out a pretty printed SLD, and only under certain
        // conditions
        Style style = Styles.style(sld);
        if (!raw
                && handler instanceof SLDHandler
                && sld.getStyledLayers().length <= 1
                && SLDHandler.VERSION_10.equals(version)) {
            info.setFormat(handler.getFormat());
            resourcePool.writeStyle(info, style, true);
        } else {
            info.setFormat(handler.getFormat());
            info.setFormatVersion(version);
            writeStyleRaw(info, new ByteArrayInputStream(rawData));
        }
    }

    /**
     * Writes the content of an input stream to a style resource, without validation
     *
     * @param info Style info object, containing details about the style format and location
     * @param input The style contents
     * @throws IOException if there was an error persisting the style
     */
    private void writeStyleRaw(StyleInfo info, InputStream input) throws IOException {
        try {
            catalog.getResourcePool().writeStyle(info, input);
        } finally {
            org.geoserver.util.IOUtils.closeQuietly(input);
        }
    }

    /**
     * Parses the sld file from a zipfile upload, returning it as a Style object
     *
     * @param sldFile The sld file to parse
     * @return The parsed style
     * @throws RestException if there was an error parsing the style.
     */
    private Style parseSld(File sldFile) throws RestException {
        Style style = null;
        try (InputStream is = new FileInputStream(sldFile)) {
            SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null), is);
            EntityResolver resolver = catalog.getResourcePool().getEntityResolver();
            if (resolver != null) {
                parser.setEntityResolver(resolver);
            }

            Style[] styles = parser.readXML();
            if (styles.length > 0) {
                style = styles[0];
            }
            if (style == null) {
                throw new RestException("Style error.", HttpStatus.BAD_REQUEST);
            }
            return style;

        } catch (Exception ex) {
            LOGGER.severe(ex.getMessage());
            throw new RestException("Style error. " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /* Parameter/request parsing utilities *******************************************************/

    /**
     * Extracts an input stream representing a zipped directory containing an sld file and any
     * number of image files to a temporary location on the filesystem.
     *
     * @param object The input stream containing the zipped directory
     * @return A file pointing to the (temporary) unzipped directory
     * @throws IOException if there was an error extracting the archive
     */
    private File unzipSldPackage(InputStream object) throws IOException {
        File tempDir = Files.createTempDir();

        org.geoserver.util.IOUtils.decompress(object, tempDir);

        return tempDir;
    }

    /**
     * Returns the sld file in the given directory. If no sld file, throws an appropriate exception
     *
     * @param directory The directory containing the sld file
     * @throws RestException it the sld file does not exist
     */
    private File getSldFileFromDirectory(File directory) throws RestException {
        File[] matchingFiles = directory.listFiles((dir, name) -> name.endsWith("sld"));

        if (matchingFiles == null || matchingFiles.length == 0) {
            throw new RestException("No sld file provided:", HttpStatus.FORBIDDEN);
        }

        LOGGER.fine("getSldFileFromDirectory (sldFile): " + matchingFiles[0].getAbsolutePath());

        return matchingFiles[0];
    }

    /**
     * Returns an array of image files (svg, png, or jpg) in the given directory
     *
     * @param directory The directory containing the image files
     * @return an array of image files
     */
    private File[] listImageFiles(File directory) {
        return directory.listFiles(
                (dir, name) ->
                        validImageFileExtensions.contains(
                                FilenameUtils.getExtension(name).toLowerCase()));
    }

    /**
     * Save the image resources in the styles folder
     *
     * @param directory Temporary directory containing the image files to save
     * @param workspaceName Name of the workspace of the style (or null for a global style)
     * @throws IOException if there was an error saving the image resources
     */
    private void saveImageResources(File directory, String workspaceName) throws IOException {
        Resource stylesDir =
                workspaceName == null
                        ? dataDir.getStyles()
                        : dataDir.getStyles(catalog.getWorkspaceByName(workspaceName));

        File[] imageFiles = listImageFiles(directory);

        for (File imageFile : imageFiles) {
            IOUtils.copyStream(
                    new FileInputStream(imageFile),
                    stylesDir.get(imageFile.getName()).out(),
                    true,
                    true);
        }
    }

    /**
     * Generates a name from a style object. If {@link Style#getName()} is not null, returns that.
     * Otherwise generates a unique identifier of the form "style-UUID", and returns that.
     *
     * @param style The style to get the name from
     * @return A unique name for the style
     * @throws RestException if there was an error generating a unique name
     */
    private String getNameFromStyle(Style style) throws RestException {
        String name = null;
        if (style != null) {
            name = style.getName();
        }
        if (name == null) {
            // generate a random one
            for (int i = 0; name == null && i < 100; i++) {
                String candidate = "style-" + UUID.randomUUID().toString().substring(0, 7);
                if (catalog.getStyleByName(candidate) == null) {
                    name = candidate;
                }
            }
        }
        if (name == null) {
            throw new RestException(
                    "Unable to generate style name, specify one with 'name' " + "parameter",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return name;
    }

    /**
     * Constructs a new StyleInfo object, given a style name and format.
     *
     * <p>Note: It is up to the caller to add this style info to the catalog.
     *
     * @param workspaceName The workspace of the style, or null for a global style
     * @param name The name of the style
     * @param handler StyleHandler, containing format information of the style
     * @param mimeType Declared mime type of the style
     * @return A new StyleInfo object, constructed from the provided data
     * @throws RestException if a style of the given name and workspace already exists in the
     *     catalog.
     */
    private StyleInfo createStyleInfo(
            String workspaceName, String name, StyleHandler handler, String mimeType)
            throws RestException {

        // ensure that the style does not already exist
        if (catalog.getStyleByName(workspaceName, name) != null) {
            throw new RestException("Style " + name + " already exists.", HttpStatus.FORBIDDEN);
        }

        StyleInfo sinfo = catalog.getFactory().createStyle();
        sinfo.setName(name);
        sinfo.setFilename(name + "." + handler.getFileExtension());
        sinfo.setFormat(handler.getFormat());
        sinfo.setFormatVersion(handler.versionForMimeType(mimeType));

        if (workspaceName != null) {
            sinfo.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }

        return sinfo;
    }

    /**
     * Builds the REST URI to a style of the provided name.
     *
     * @param name Name of the style
     * @param workspace Workspace of the style, or null for a global style
     * @param builder The {@link UriComponentsBuilder} for the request
     * @return the {@link URI} to the named style
     */
    private URI getUri(String name, String workspace, UriComponentsBuilder builder) {
        UriComponents uriComponents;
        if (workspace != null) {
            uriComponents =
                    builder.path("/workspaces/{workspaceName}/styles/{styleName}")
                            .buildAndExpand(workspace, name);
        } else {
            uriComponents = builder.path("/styles/{id}").buildAndExpand(name);
        }
        return uriComponents.toUri();
    }

    /**
     * Extracts the MimeType from the HTTP Content-Type header value. Example: For a Content-Type of
     * "text/html; charset=utf-8", would return "text/html".
     *
     * @param contentType Content-Type header value
     */
    private String getMimeTypeFromContentType(String contentType) {
        if (contentType != null) {
            return contentType.split(";")[0];
        }
        return null;
    }

    /**
     * Extracts Charset from HTTP Content-Type header value. Example: For a Content-Type of
     * "text/html; charset=utf-8", would return "utf-8".
     *
     * <p>If the charset is not included in the Content-Type, returns the default charset of the
     * JVM.
     *
     * @param contentType Content-Type header value
     */
    private String getCharsetFromContentType(String contentType) {
        if (contentType != null && contentType.split(";").length > 1) {
            String charsetRaw = contentType.split(";")[1];
            if (charsetRaw.contains("=") && charsetRaw.split("=").length > 1) {
                return charsetRaw.split("=")[1];
            }
        }
        // For retrocompatibility sake
        return Charset.defaultCharset().name();
    }

    /* Validation / Verification utilities *******************************************************/

    /**
     * Checks if style is in the catalog.
     *
     * @param workspaceName The name of the workspace, or null for a global style
     * @param styleName The name of the style
     * @return true if the style exists in the catalog, false otherwise.
     */
    private boolean existsStyleInCatalog(String workspaceName, String styleName) {
        return (catalog.getStyleByName(workspaceName, styleName) != null);
    }

    /**
     * Verifies the workspace name (if not null) for a REST request, throwing an appropriate
     * exception if invalid
     *
     * @param workspaceName The workspace name. Ignored if null.
     * @throws RestException if the workspace name is not null and the workspace doesn't exist in
     *     the catalog
     */
    private void checkWorkspaceName(String workspaceName) throws RestException {
        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
    }

    /**
     * Verifies that a style with the given name doesn't exist, throwing an appropriate exception if
     * it does
     *
     * @param workspaceName The name of the workspace (or null for a global style)
     * @param styleName The name of the style
     * @throws RestException if the style exists
     */
    private void checkStyleNotExists(String workspaceName, String styleName) throws RestException {
        if (existsStyleInCatalog(workspaceName, styleName)) {
            throw new RestException(
                    "Style " + styleName + " already exists.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Verifies that the style resource for the passed style does not yet exist, throwing an
     * appropriate exception if it does
     *
     * @param info The style info to test. Filename should be set.
     * @throws RestException if the style resource associated with the style info exists.
     */
    private void checkStyleResourceNotExists(StyleInfo info) throws RestException {
        // ensure that a existing resource does not already exist, because we may not want to
        // overwrite it
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        if (dataDir.style(info).getType() != Resource.Type.UNDEFINED) {
            throw new RestException(
                    "Style resource " + info.getFilename() + " already exists.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
