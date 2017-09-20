/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.PutIgnoringExtensionContentNegotiationStrategy;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
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

import com.google.common.io.Files;

/**
 * Example style resource controller
 */
@RestController @RequestMapping(path = RestBaseController.ROOT_PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE,
    MediaType.TEXT_HTML_VALUE})
public class StyleController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(value = {"/styles", "/layers/{layerName}/styles", "/workspaces/{workspaceName}/styles"})
    public RestWrapper<?> stylesGet(
            @PathVariable(required = false) String layerName,
            @PathVariable(required = false) String workspaceName) {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
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

    @PostMapping(value = {"/styles", "/layers/{layerName}/styles", "/workspaces/{workspaceName}/styles"}, consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public String stylePost(
            @RequestBody StyleInfo style,
            @PathVariable( required = false) String layerName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(defaultValue = "false", name = "default") boolean makeDefault) {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);
        
        if (layerName != null) {
            StyleInfo existing = catalog.getStyleByName(style.getName());
            if (existing == null) {
                throw new ResourceNotFoundException();
            }

            LayerInfo l = catalog.getLayerByName(layerName);
            l.getStyles().add(existing);

            //check for default
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

    @PostMapping(value = {"/styles", "/workspaces/{workspaceName}/styles"}, consumes = {
            SLDHandler.MIMETYPE_11,
            SLDHandler.MIMETYPE_10})
    public ResponseEntity<String> styleSLDPost(
            @RequestBody Style style,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(required = false) String name,
            @RequestHeader("Content-Type") String contentType, UriComponentsBuilder builder) {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);
        
        StyleHandler handler = org.geoserver.catalog.Styles.handler(contentType);
        if (name == null) {
            name = findNameFromObject(style);
        }

        //ensure that the style does not already exist
        if (catalog.getStyleByName(workspaceName, name) != null) {
            throw new RestException("Style " + name + " already exists.",
                    HttpStatus.FORBIDDEN);
        }

        StyleInfo sinfo = catalog.getFactory().createStyle();
        sinfo.setName(name);
        sinfo.setFilename(name + "." + handler.getFileExtension());
        sinfo.setFormat(handler.getFormat());
        sinfo.setFormatVersion(handler.versionForMimeType(contentType));

        if (workspaceName != null) {
            sinfo.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }

        // ensure that a existing resource does not already exist, because we may not want to overwrite it
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        if (dataDir.style(sinfo).getType() != Resource.Type.UNDEFINED) {
            String msg = "Style resource " + sinfo.getFilename() + " already exists.";
            throw new RestException(msg, HttpStatus.FORBIDDEN);
        }


        ResourcePool resourcePool = catalog.getResourcePool();
        try {
            if (style instanceof Style) {
                resourcePool.writeStyle(sinfo, (Style) style);
            } else {
                resourcePool.writeStyle(sinfo, (InputStream) style);
            }
        } catch (IOException e) {
            throw new RestException("Error writing style", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }

        catalog.add(sinfo);
        LOGGER.info("POST Style " + name);
        //build the new path
        UriComponents uriComponents = getUriComponents(name, workspaceName, builder);
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

    @GetMapping(path = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"}, produces = {MediaType.ALL_VALUE})
    protected RestWrapper<StyleInfo> styleGet(
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        return wrapObject(getStyleInternal(styleName, workspaceName), StyleInfo.class);
    }

    @GetMapping(path = {"/styles/{styleName}","/workspaces/{workspaceName}/styles/{styleName}"}, produces = {
            SLDHandler.MIMETYPE_10,
            SLDHandler.MIMETYPE_11})
    protected StyleInfo styleSLDGet(
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        return getStyleInternal(styleName, workspaceName);
    }

    protected StyleInfo getStyleInternal(String styleName, String workspace) {
        LOGGER.fine("GET style " + styleName);
        StyleInfo sinfo = catalog.getStyleByName(workspace, styleName);

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

    @DeleteMapping(path = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"})
    protected void styleDelete(
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(required = false, defaultValue = "false") boolean recurse,
            @RequestParam(required = false, defaultValue = "false") boolean purge) throws IOException {
        
        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }

        StyleInfo style = workspaceName != null ? catalog.getStyleByName(workspaceName, styleName) :
                catalog.getStyleByName(styleName);
        
        if(style == null) {
            throw new ResourceNotFoundException("Style " + styleName + " not found");
        }

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

    @PostMapping(value = {"/styles", "/workspaces/{workspaceName}/styles"}, consumes = {
            MediaTypeExtensions.APPLICATION_ZIP_VALUE})
    public ResponseEntity<String> stylePost(
            InputStream stream,
            @RequestParam(required = false) String name,
            @PathVariable(required = false) String workspaceName,
            UriComponentsBuilder builder) throws IOException {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);
        
        File directory = unzipSldPackage(stream);
        File uploadedFile = retrieveSldFile(directory);

        Style styleSld = parseSld(uploadedFile);

        if (name == null) {
            name = findNameFromObject(styleSld);
        }

        //ensure that the style does not already exist
        if (catalog.getStyleByName(workspaceName, name) != null) {
            throw new RestException("Style " + name + " already exists.", HttpStatus.FORBIDDEN);
        }

        // save image resources
        saveImageResources(directory, workspaceName);

        //create a style info object
        StyleInfo styleInfo = catalog.getFactory().createStyle();
        styleInfo.setName(name);
        styleInfo.setFilename(name + ".sld");

        if (workspaceName != null) {
            styleInfo.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }

        Resource style = dataDir.style(styleInfo);
        // ensure that a existing resource does not already exist, because we may not want to overwrite it
        if (dataDir.style(styleInfo).getType() != Resource.Type.UNDEFINED) {
            String msg = "Style resource " + styleInfo.getFilename() + " already exists.";
            throw new RestException(msg, HttpStatus.FORBIDDEN);
        }

        serializeSldFileInCatalog(style, uploadedFile);

        catalog.add(styleInfo);

        LOGGER.info("POST Style Package: " + name + ", workspace: " + workspaceName);
        UriComponents uriComponents = getUriComponents(name, workspaceName, builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<>(name, headers, HttpStatus.CREATED);
    }

    @PutMapping( value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"}, consumes = {
            MediaTypeExtensions.APPLICATION_ZIP_VALUE})
    public void styleZipPut(
            InputStream is,
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        putZipInternal(is, workspaceName, styleName);
    }

    /**
     * Workaround to support regular response content type when extension is in path
     */
    @Configuration
    static class StyleControllerConfiguration {
        @Bean
        PutIgnoringExtensionContentNegotiationStrategy stylePutContentNegotiationStrategy() {
            return new PutIgnoringExtensionContentNegotiationStrategy(
                    new PatternsRequestCondition("/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"),
                    Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML));
        }
    }

    @PutMapping(value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"}, consumes = {
            MediaType.ALL_VALUE})
    public void stylePut(
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName,
            @RequestParam(name = "raw", required = false, defaultValue = "false") boolean raw,
            HttpServletRequest request) throws IOException {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);
        StyleInfo s = catalog.getStyleByName( workspaceName, styleName );
        
        String contentType = request.getContentType();
        // String extentsion = "sld"; // TODO: determine this from path
        
        ResourcePool resourcePool = catalog.getResourcePool();
        if( raw ){
            writeRaw( s, request.getInputStream() );
        }
        else {
            String content = IOUtils.toString( request.getInputStream());
            EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
            for (StyleHandler format : Styles.handlers()) {
                for (Version version : format.getVersions()) {
                    String mimeType = format.mimeType(version);
                    if( !mimeType.equals(contentType)){
                        continue; // skip this format
                    }
                    try {
                        StyledLayerDescriptor sld = format.parse( content, version, null, entityResolver);
                        //If there are more than one layers, assume this is a style group and validate accordingly.
                        if (sld.getStyledLayers().length > 1) {
                            List<Exception> validationErrors = SLDNamedLayerValidator.validate(catalog, sld);
                            if (validationErrors.size() > 0) {
                                throw validationErrors.get(0);
                            }
                        }

                        Style style = Styles.style(sld);
                        if( format instanceof SLDHandler && sld.getStyledLayers().length <= 1){
                            s.setFormat(format.getFormat());
                            resourcePool.writeStyle(s, style, true);
                            catalog.save(s);
                        }
                        else {
                            s.setFormat(format.getFormat());
                            writeRaw(s, request.getInputStream());
                        }
                        return;
                    }
                    catch(Exception invalid){
                        throw new RestException("Invalid style:"+invalid.getMessage(), HttpStatus.BAD_REQUEST, invalid);
                    }
                }
            }
            throw new RestException("Unknown style format '"+contentType+"'", HttpStatus.BAD_REQUEST);
        }
    }
    
    private void writeRaw( StyleInfo info, InputStream input) throws IOException{
        ResourcePool resourcePool = catalog.getResourcePool();
        
        resourcePool.writeStyle(info, input);
        catalog.save( info);
    }
    
    @PutMapping(value = {"/styles/{styleName}", "/workspaces/{workspaceName}/styles/{styleName}"}, consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE})
    public void stylePut(
            @RequestBody StyleInfo info,
            @PathVariable String styleName,
            @PathVariable(required = false) String workspaceName) {

        if(workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);
        
        StyleInfo original = catalog.getStyleByName(workspaceName, styleName);

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
        File[] matchingFiles = directory.listFiles((dir, name) -> name.endsWith("sld"));

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
        return directory.listFiles((dir, name) ->
                validImageFileExtensions.contains(FilenameUtils.getExtension(name).toLowerCase()));
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
    
    private void putZipInternal(InputStream is, String workspace, String style) {
        if(workspace != null && catalog.getWorkspaceByName(workspace) == null) {
            throw new ResourceNotFoundException("Workspace " + workspace + " not found");
        }
        checkFullAdminRequired(workspace);

        
        File directory = null;
        try {
            directory = unzipSldPackage(is);
            File uploadedFile = retrieveSldFile(directory);

            Style styleSld = parseSld(uploadedFile);

            if (style == null) {
                style = findNameFromObject(styleSld);
            }

            if (style == null) {
                throw new RestException("Style must have a name.", HttpStatus.BAD_REQUEST);
            }

            //ensure that the style already exists
            if (!existsStyleInCatalog(workspace, style)) {
                throw new RestException("Style " + style + " doesn't exist.", HttpStatus.FORBIDDEN);
            }

            // save image resources
            saveImageResources(directory, workspace);

            // Save the style: serialize the style out into the data directory
            StyleInfo styleInfo = catalog.getStyleByName(workspace, style);
            serializeSldFileInCatalog(dataDir.style(styleInfo), uploadedFile);

            LOGGER.info("PUT Style Package: " + style + ", workspace: " + workspace);

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
