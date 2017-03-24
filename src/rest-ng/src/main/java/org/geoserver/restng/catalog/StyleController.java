package org.geoserver.restng.catalog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.restng.ResourceNotFoundException;
import org.geoserver.restng.RestException;
import org.geoserver.restng.catalog.wrapper.XStreamListWrapper;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geoserver.restng.wrapper.FreemarkerConfigurationWrapper;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.EntityResolver;

import com.google.common.io.Files;

/**
 * Example style resource controller
 */
@RestController @RequestMapping(path = "/restng", produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE,
    MediaType.TEXT_HTML_VALUE})
public class StyleController extends CatalogController {



    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(Catalog catalog) {
        super(catalog);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public XStreamListWrapper getStyles() {

        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return toXStreamList(styles, StyleInfo.class);
    }

    @RequestMapping(value = "/workspaces/{workspaceName}/styles", method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public XStreamListWrapper getStylesFromWorkspace(@PathVariable String workspaceName) {
        LOGGER.fine("GET styles for workspace " + workspaceName);
        return toXStreamList(catalog.getStylesByWorkspace(workspaceName), StyleInfo.class);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    public FreemarkerConfigurationWrapper getStylesFreemarker() {
        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return toFreemarkerList(styles, StyleInfo.class);
    }

    @RequestMapping(value = "/workspaces/{workspaceName}/styles", method = RequestMethod.GET,
            produces = {MediaType.TEXT_HTML_VALUE})
    public FreemarkerConfigurationWrapper getStylesFromWorkspaceFreemarker(@PathVariable String workspaceName) {
        LOGGER.fine("GET styles for workspace " + workspaceName);
        return toFreemarkerList(catalog.getStylesByWorkspace(workspaceName));
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyle(@RequestBody StyleInfo style) {
        return postStyleInfoInternal(style, null, null, false);
    }

    @RequestMapping(value = "/layers/{layerName}/styles", method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyle(@RequestBody StyleInfo style, @PathVariable String layerName,
        @RequestParam(defaultValue = "false", name = "default") boolean makeDefault)
    {
        return postStyleInfoInternal(style, null, layerName, makeDefault);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST,
        consumes = {SLDHandler.MIMETYPE_11, SLDHandler.MIMETYPE_10})
    public ResponseEntity<String> postStyle(@RequestBody Style style, @RequestParam(required = false) String name,
        @RequestHeader("Content-Type") String contentType, UriComponentsBuilder builder)
    {
        StyleHandler handler = org.geoserver.catalog.Styles.handler(contentType);
        return postStyleInternal(style, name, null, handler, contentType, builder);
    }

    @RequestMapping(
        value = "/workspaces/{workspaceName}/styles",
        method = RequestMethod.POST,
        consumes = {SLDHandler.MIMETYPE_11, SLDHandler.MIMETYPE_10})
    public ResponseEntity<String> postStyle(
        @RequestBody Style style,
        @RequestParam(required = false) String name,
        @PathVariable String workspaceName,
        @RequestHeader("Content-Type") String contentType,
        UriComponentsBuilder builder)
    {
        StyleHandler handler = org.geoserver.catalog.Styles.handler(contentType);
        return postStyleInternal(style, name, workspaceName, handler, contentType, builder);
    }

    public ResponseEntity<String> postStylePackageInternal(InputStream stream, String workspace,
        String name, UriComponentsBuilder builder)
        throws IOException
    {
        File directory = unzipSldPackage(stream);
        File uploadedFile = retrieveSldFile(directory);

        Style styleSld = parseSld(uploadedFile);

        if (name == null) {
            name = findNameFromObject(styleSld);
        }

        //ensure that the style does not already exist
        if (catalog.getStyleByName(workspace, name) != null) {
            throw new RestException("Style " + name + " already exists.", HttpStatus.FORBIDDEN);
        }

        // save image resources
        saveImageResources(directory, workspace);

        //create a style info object
        StyleInfo styleInfo = catalog.getFactory().createStyle();
        styleInfo.setName(name);
        styleInfo.setFilename(name + ".sld");

        if (workspace != null) {
            styleInfo.setWorkspace(catalog.getWorkspaceByName(workspace));
        }

        Resource style = dataDir.style(styleInfo);
        // ensure that a existing resource does not already exist, because we may not want to overwrite it
        if (dataDir.style(styleInfo).getType() != Resource.Type.UNDEFINED) {
            String msg = "Style resource " + styleInfo.getFilename() + " already exists.";
            throw new RestException(msg, HttpStatus.FORBIDDEN);
        }

        serializeSldFileInCatalog(style, uploadedFile);

        catalog.add(styleInfo);

        LOGGER.info("POST Style Package: " + name + ", workspace: " + workspace);
        UriComponents uriComponents = getUriComponents(name, workspace, builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(name, headers, HttpStatus.CREATED);
    }

    public ResponseEntity<String> postStyleInternal(Object object, String name, String workspace,
        StyleHandler styleFormat, String mediaType, UriComponentsBuilder builder)
    {

        if (name == null) {
            name = findNameFromObject(object);
        }

        //ensure that the style does not already exist
        if (catalog.getStyleByName(workspace, name) != null) {
            throw new RestException("Style " + name + " already exists.",
                HttpStatus.FORBIDDEN);
        }

        StyleInfo sinfo = catalog.getFactory().createStyle();
        sinfo.setName(name);
        sinfo.setFilename(name + "." + styleFormat.getFileExtension());
        sinfo.setFormat(styleFormat.getFormat());
        sinfo.setFormatVersion(styleFormat.versionForMimeType(mediaType));

        if (workspace != null) {
            sinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
        }

        // ensure that a existing resource does not already exist, because we may not want to overwrite it
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        if (dataDir.style(sinfo).getType() != Resource.Type.UNDEFINED) {
            String msg = "Style resource " + sinfo.getFilename() + " already exists.";
            throw new RestException(msg, HttpStatus.FORBIDDEN);
        }


        ResourcePool resourcePool = catalog.getResourcePool();
        try {
            if (object instanceof Style) {
                resourcePool.writeStyle(sinfo, (Style) object);
            } else {
                resourcePool.writeStyle(sinfo, (InputStream) object);
            }
        } catch (IOException e) {
            throw new RestException("Error writing style", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        catalog.add(sinfo);
        LOGGER.info("POST Style " + name);
        //build the new path
        UriComponents uriComponents = getUriComponents(name, workspace, builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(name, headers, HttpStatus.CREATED);
    }

    private UriComponents getUriComponents(String name, String workspace,
        UriComponentsBuilder builder) {
        UriComponents uriComponents;
        if (workspace != null) {
            uriComponents = builder.path("/workspaces/{workspaceName}/styles/{styleName}")
                .buildAndExpand(workspace, name);
        } else {
            uriComponents = builder.path("/styles/{id}").buildAndExpand(name);
        }
        return uriComponents;
    }

    @RequestMapping(value = "/workspaces/{workspaceName}/styles",
        method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyleInfoToWorkspace(@RequestBody StyleInfo styleInfo,
        @PathVariable String workspaceName) {
        return postStyleInfoInternal(styleInfo, workspaceName, null, false);
    }

    public String postStyleInfoInternal(StyleInfo style, String workspace, String layer, boolean makeDefault)
    {
        if (layer != null) {
            StyleInfo existing = catalog.getStyleByName(style.getName());
            if (existing == null) {
                throw new ResourceNotFoundException();
            }

            LayerInfo l = catalog.getLayerByName(layer);
            l.getStyles().add(existing);

            //check for default
            if (makeDefault) {
                l.setDefaultStyle(existing);
            }
            catalog.save(l);
            LOGGER.info("POST style " + style.getName() + " to layer " + layer);
        } else {

            if (workspace != null) {
                style.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            catalog.add(style);
            LOGGER.info("POST style " + style.getName());
        }

        return style.getName();
    }

    @RequestMapping(path = "/workspaces/{workspaceName}/styles/{styleName}", method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            SLDHandler.MIMETYPE_10, SLDHandler.MIMETYPE_11})
    protected StyleInfo getStyleFromWorkspace(
        @PathVariable String styleName,
        @PathVariable String workspaceName) {
        return getStyleInternal(styleName, workspaceName);
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.GET,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            SLDHandler.MIMETYPE_10, SLDHandler.MIMETYPE_11})
    protected StyleInfo getStyle(
        @PathVariable String styleName) {
        return getStyleInternal(styleName, null);
    }

    @RequestMapping(path = "/workspaces/{workspaceName}/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    protected FreemarkerConfigurationWrapper getStyleFromWorkspaceFreemarker(
            @PathVariable String styleName,
            @PathVariable String workspaceName) {
        return toFreemarkerMap(getStyleInternal(styleName, workspaceName));
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    protected FreemarkerConfigurationWrapper getStyleFreemarker(
            @PathVariable String styleName) {
        return toFreemarkerMap(getStyleInternal(styleName, null));
    }

    protected StyleInfo getStyleInternal(String styleName, String workspace) {
        LOGGER.fine("GET style " + styleName);
        StyleInfo sinfo = workspace == null ?
            catalog.getStyleByName(styleName) :
            catalog.getStyleByName(workspace, styleName);

        if (sinfo == null) {
            String message = "No such style: " + styleName;
            if (workspace != null) {
                message = "No such style "+ styleName +" in workspace " + workspace;
            }
            throw new ResourceNotFoundException(message);
        } else {
            return sinfo;
        }
    }

    @RequestMapping(
        path = "/workspaces/{workspaceName}/styles/{styleName}",
        method = RequestMethod.DELETE)
    protected void deleteStyleWithWorkspace(
        @PathVariable String styleName,
        @PathVariable String workspaceName,
        @RequestParam(required = false, defaultValue = "false") boolean recurse,
        @RequestParam(required = false, defaultValue = "false") boolean purge)
        throws IOException {
        deleteStyleInternal(styleName, recurse, purge, workspaceName);
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.DELETE)
    protected void deleteStyle(
        @PathVariable String styleName,
        @RequestParam(required = false, defaultValue = "false") boolean recurse,
        @RequestParam(required = false, defaultValue = "false") boolean purge)
        throws IOException {
        deleteStyleInternal(styleName, recurse, purge, null);
    }

    protected void deleteStyleInternal(String styleName, boolean recurse, boolean purge,
        String workspace)
        throws IOException {

        StyleInfo style = workspace != null ? catalog.getStyleByName(workspace, styleName) :
            catalog.getStyleByName(styleName);

        if (recurse) {
            new CascadeDeleteVisitor(catalog).visit(style);
        } else {
            // ensure that no layers reference the style
            List<LayerInfo> layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                throw new RestException("Can't delete style referenced by existing layers.", HttpStatus.FORBIDDEN);
            }
            catalog.remove(style);
        }

        catalog.getResourcePool().deleteStyle(style, purge);

        LOGGER.info("DELETE style " + styleName);
    }

    String findNameFromObject(Object object) {
        String name = null;
        if (object instanceof Style) {
            name = ((Style)object).getName();
        }

        if (name == null) {
            // generate a random one
            for (int i = 0; name == null && i < 100; i++) {
                String candidate = "style-"+ UUID.randomUUID().toString().substring(0, 7);
                if (catalog.getStyleByName(candidate) == null) {
                    name = candidate;
                }
            }
        }

        if (name == null) {
            throw new RestException("Unable to generate style name, specify one with 'name' "
                + "parameter", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return name;
    }

    @RequestMapping(value = "/styles/{styleName}", method = RequestMethod.PUT,
        consumes = { "text/xml", "application/xml" })
    public void
    putStyleInfo(@RequestBody StyleInfo info, @PathVariable String styleName) {
        handleStyleInfoPutInternal(info, null, styleName);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST,
        consumes = {"application/zip"})
    public ResponseEntity<String> postStyle(InputStream stream,
        @RequestParam(required = false) String name, UriComponentsBuilder builder)
        throws IOException {
        return postStylePackageInternal(stream, null, name, builder);
    }

    @RequestMapping(
        value = "/workspaces/{workspaceName}/styles",
        method = RequestMethod.POST,
        consumes = {"application/zip"})
    public ResponseEntity<String> postStyleToWorkspace(
        InputStream stream,
        @RequestParam(required = false) String name,
        @PathVariable String workspaceName,
        UriComponentsBuilder builder)
        throws IOException
    {
        return postStylePackageInternal(stream, workspaceName, name, builder);
    }

    @RequestMapping(
        value = "/workspaces/{workspaceName}/styles/{styleName}",
        method = RequestMethod.PUT,
        consumes = {"application/zip"})
    public void putStyleInfo(
        InputStream is,
        @PathVariable String styleName,
        @PathVariable String workspaceName,
        @RequestParam(required = false) String name)
    {
        putZipInternal(is, workspaceName, name, styleName);
    }

    @RequestMapping(
        value = "/styles/{styleName}",
        method = RequestMethod.PUT,
        consumes = {"application/zip"})
    public void putStyleInfoToWorkspace(
        InputStream is,
        @PathVariable String styleName,
        @RequestParam(required = false) String name)
    {
        putZipInternal(is, null, name, styleName);
    }

    @RequestMapping(value = "/workspaces/{workspaceName}/styles/{styleName}",
        method = RequestMethod.PUT, consumes = { "text/xml", "application/xml" })
    public void putStyleInfoToWorkspace(@RequestBody StyleInfo info, @PathVariable String styleName,
        @PathVariable String workspaceName) {
        handleStyleInfoPutInternal(info, workspaceName, styleName);
    }

    @RequestMapping(value = "/styles/{styleName}", method = RequestMethod.PUT,
        consumes = {SLDHandler.MIMETYPE_11, SLDHandler.MIMETYPE_10})
    public void putStyleInfo(@RequestBody Style style, @PathVariable String styleName)
        throws IOException {
        handleStylePutInternal(style, null, styleName);
    }

    public void handleStyleInfoPutInternal(StyleInfo info, String workspace, String styleName) {
        StyleInfo original = catalog.getStyleByName(workspace, styleName);

        //ensure no workspace change
        if (info.getWorkspace() != null) {
            if (!info.getWorkspace().equals(original.getWorkspace())) {
                throw new RestException( "Can't change the workspace of a style, instead " +
                    "DELETE from existing workspace and POST to new workspace", HttpStatus.FORBIDDEN);
            }
        }

        new CatalogBuilder(catalog).updateStyle(original, info);
        catalog.save(original);
    }

    public void handleStylePutInternal(Object object, String workspace, String style)
        throws IOException {
        StyleInfo s = catalog.getStyleByName( workspace, style );

        ResourcePool resourcePool = catalog.getResourcePool();
        if (object instanceof Style) {
            resourcePool.writeStyle(s, (Style) object, true);
        }
        else {
            resourcePool.writeStyle(s, (InputStream)object);
        }

        catalog.save(s);
    }

    /**
     * Unzips the ZIP stream.
     *
     */
    private File unzipSldPackage(InputStream object) throws IOException {
        File tempDir = Files.createTempDir();

        org.geoserver.util.IOUtils.decompress(object, tempDir);

        return tempDir;
    }

    /**
     * Returns the sld file in the given directory. If no sld file, throws an exception
     *
     * @param directory
     *
     */
    private File retrieveSldFile(File directory) {
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("sld");
            }
        });

        if (matchingFiles.length == 0) {
            throw new RestException("No sld file provided:", HttpStatus.FORBIDDEN);
        }

        LOGGER.fine("retrieveSldFile (sldFile): " + matchingFiles[0].getAbsolutePath());

        return matchingFiles[0];
    }

    /**
     * Parses the sld file.
     *
     * @param sldFile
     *
     */
    private Style parseSld(File sldFile) {
        Style style = null;
        InputStream is = null;

        try {
            is = new FileInputStream(sldFile);

            SLDParser parser
                = new SLDParser(CommonFactoryFinder.getStyleFactory(null), is);
            EntityResolver resolver = catalog.getResourcePool().getEntityResolver();
            if(resolver != null) {
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

        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Save the image resources in the styles folder
     *
     * @param directory     Temporary directory with images from SLD package
     * @param workspace     Geoserver workspace name for the style
     * @throws java.io.IOException
     */
    private void saveImageResources(File directory, String workspace) throws IOException {
        Resource stylesDir = workspace == null ? dataDir.getStyles() : dataDir.getStyles(catalog.getWorkspaceByName(workspace));

        File[] imageFiles = retrieveImageFiles(directory);

        for (int i = 0; i < imageFiles.length; i++) {
            IOUtils.copyStream(new FileInputStream(imageFiles[i]),
                stylesDir.get(imageFiles[i].getName()).out(), true, true);
        }
    }

    /**
     * Returns a list of image files in the given directory
     *
     * @param directory
     *
     */
    private File[] retrieveImageFiles(File directory) {
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return validImageFileExtensions.contains(FilenameUtils.getExtension(name).toLowerCase());
            }
        });

        return matchingFiles;
    }

    /**
     * Serializes the uploaded sld file in the catalog
     *
     * @param sldFile
     * @param uploadedSldFile
     */
    private void serializeSldFileInCatalog(Resource sldFile, File uploadedSldFile) {
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(sldFile.out());
            byte[] sldContent = FileUtils.readFileToByteArray(uploadedSldFile);
            out.write(sldContent);
            out.flush();
        } catch (IOException e) {
            throw new RestException("Error creating file", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void putZipInternal(InputStream is, String workspace, String name, String style) {
        File directory = null;
        try {
            directory = unzipSldPackage((InputStream) is);
            File uploadedFile = retrieveSldFile(directory);

            Style styleSld = parseSld(uploadedFile);

            if (name == null) {
                name = findNameFromObject(styleSld);
            }

            if (name == null) {
                throw new RestException("Style must have a name.", HttpStatus.BAD_REQUEST);
            }

            //ensure that the style does already exist
            if (!existsStyleInCatalog(workspace, name)) {
                throw new RestException("Style " + name + " doesn't exists.", HttpStatus.FORBIDDEN);
            }

            // save image resources
            saveImageResources(directory, workspace);

            // Save the style: serialize the style out into the data directory
            StyleInfo styleInfo = catalog.getStyleByName(workspace, style);
            serializeSldFileInCatalog(dataDir.style(styleInfo), uploadedFile);

            LOGGER.info("PUT Style Package: " + name + ", workspace: " + workspace);

        } catch (Exception e) {
            LOGGER.severe("Error processing the style package (PUT): " + e.getMessage());
            throw new RestException("Error processing the style", HttpStatus.INTERNAL_SERVER_ERROR, e);
        } finally {
            FileUtils.deleteQuietly(directory);
        }
    }

    /**
     * Checks if style is in the catalog.
     *
     * @param workspaceName     Workspace name
     * @param name              Style name
     */
    private boolean existsStyleInCatalog(String workspaceName, String name) {
        return (catalog.getStyleByName(workspaceName, name ) != null);
    }

}
