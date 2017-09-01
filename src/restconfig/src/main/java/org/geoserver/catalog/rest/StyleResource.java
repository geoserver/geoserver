/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.util.IOUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.xml.sax.EntityResolver;

import com.google.common.io.Files;

public class StyleResource extends AbstractCatalogResource {

    private List<String> validImageFileExtensions = new ArrayList<String>();
    
    private GeoServerDataDirectory dataDir;

    public StyleResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, StyleInfo.class, catalog);

        validImageFileExtensions = Arrays.asList(new String[]{"svg", "png", "jpg"});
        
        dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats =  super.createSupportedFormats(request,response);
        boolean prettyPrint = isPrettyPrint(request);
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                formats.add(new StyleFormat(sh.mimeType(ver), ver, prettyPrint, sh, request, entityResolver));
            }
        }

        return formats;
    }
    
    boolean isPrettyPrint(Request request) {
        Form q = request.getResourceRef().getQueryAsForm();
        String pretty = q.getFirstValue("pretty");
        return pretty != null && Boolean.TRUE.equals(Converters.convert(pretty, Boolean.class));
    }
    
    @Override
    protected Object handleObjectGet() {
        String workspace = getAttribute("workspace");
        String style = getAttribute("style");
        
        LOGGER.fine( "GET style " + style );
        return catalog.getStyleByName(workspace,style);
    }

    @Override
    public boolean allowPost() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("style") == null;
    }

    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String layer = getAttribute( "layer" );
        
        if ( object instanceof StyleInfo ) {
            StyleInfo style = (StyleInfo) object;
            
            if ( layer != null ) {
                StyleInfo existing = catalog.getStyleByName( style.getName() );
                if ( existing == null ) {
                    //TODO: add a new style to catalog
                    throw new RestletException( "No such style: " + style.getName(), Status.CLIENT_ERROR_NOT_FOUND );
                }
                
                LayerInfo l = catalog.getLayerByName( layer );
                l.getStyles().add( existing );
                
                //check for default
                String def = getRequest().getResourceRef().getQueryAsForm().getFirstValue("default");
                if ( "true".equals( def ) ) {
                    l.setDefaultStyle( existing );
                }
                catalog.save(l);
                LOGGER.info( "POST style " + style.getName() + " to layer " + layer);
            }
            else {

                if (workspace != null) {
                    style.setWorkspace(catalog.getWorkspaceByName(workspace));
                }

                catalog.add( style  );
                LOGGER.info( "POST style " + style.getName() );
            }

            return style.getName();
        }
        else if (getRequest().getEntity().getMediaType().equals(MediaType.APPLICATION_ZIP) && object instanceof InputStream) {
            File directory = null;
            try {
                directory = unzipSldPackage((InputStream) object);
                File uploadedFile = retrieveSldFile(directory);

                Style styleSld = parseSld(uploadedFile);
                //figure out the name of the new style, first check if specified directly
                String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");

                if (name == null) {
                    name = findNameFromObject(styleSld);
                }

                //ensure that the style does not already exist
                if (catalog.getStyleByName(workspace, name) != null) {
                    throw new RestletException("Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN);
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
                    throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
                }

                serializeSldFileInCatalog(style, uploadedFile);

                catalog.add(styleInfo);

                LOGGER.info("POST Style Package: " + name + ", workspace: " + workspace);

                return name;
            } catch (RestletException e) {
                // Re-throw the exception
                throw e;

            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.severe("Error processing the style package (POST): " + e.getMessage());
                throw new RestletException( "Error processing the style", Status.SERVER_ERROR_INTERNAL, e );
            } finally {
                FileUtils.deleteQuietly(directory);
            }
        }
        else if (object instanceof Style || object instanceof InputStream) {

            //figure out the name of the new style, first check if specified directly
            String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");
            ;

            if (name == null) {
                name = findNameFromObject(object);
            }

            //ensure that the style does not already exist
            if (catalog.getStyleByName(workspace, name) != null) {
                throw new RestletException("Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN);
            }

            // style format/handler
            StyleHandler styleFormat = ((StyleFormat) getFormatPostOrPut()).getHandler();

            StyleInfo sinfo = catalog.getFactory().createStyle();
            sinfo.setName(name);
            sinfo.setFilename(name + "." + styleFormat.getFileExtension());
            sinfo.setFormat(styleFormat.getFormat());
            sinfo.setFormatVersion(styleFormat.versionForMimeType(getRequest().getEntity().getMediaType().getName()));

            if (workspace != null) {
                sinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            // ensure that a existing resource does not already exist, because we may not want to overwrite it
            GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
            if (dataDir.style(sinfo).getType() != Resource.Type.UNDEFINED) {
                String msg = "Style resource " + sinfo.getFilename() + " already exists.";
                throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
            }


            ResourcePool resourcePool = catalog.getResourcePool();
            try {
                if (object instanceof Style) {
                    resourcePool.writeStyle(sinfo, (Style) object);
                } else {
                    resourcePool.writeStyle(sinfo, (InputStream) object);
                }
            } catch (IOException e) {
                throw new RestletException("Error writing style", Status.SERVER_ERROR_INTERNAL, e);
            }

            catalog.add(sinfo);
            LOGGER.info("POST Style " + name);
            return name;
        }

        return null;
    }

    String findNameFromObject(Object object) {
        String name = null;
        if (object instanceof Style) {
            name = ((Style)object).getName();
        }

        if (name == null) {
            // generate a random one
            for (int i = 0; name == null && i < 100; i++) {
                String candidate = "style-"+UUID.randomUUID().toString().substring(0, 7);
                if (catalog.getStyleByName(candidate) == null) {
                    name = candidate;
                }
            }
        }

        if (name == null) {
            throw new RestletException("Unable to generate style name, specify one with 'name' parameter",
                Status.SERVER_ERROR_INTERNAL);
        }

        return name;
    }

    @Override
    public boolean allowPut() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("style") != null;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String style = getAttribute("style");
        String workspace = getAttribute("workspace");

        if ( object instanceof StyleInfo ) {
            StyleInfo s = (StyleInfo) object;
            StyleInfo original = catalog.getStyleByName( workspace, style );
     
            //ensure no workspace change
            if (s.getWorkspace() != null) {
                if (!s.getWorkspace().equals(original.getWorkspace())) {
                    throw new RestletException( "Can't change the workspace of a style, instead " +
                        "DELETE from existing workspace and POST to new workspace", Status.CLIENT_ERROR_FORBIDDEN );
                }
            }
            
            new CatalogBuilder( catalog ).updateStyle( original, s );
            catalog.save( original );
        }
        else if (getRequest().getEntity().getMediaType().equals(MediaType.APPLICATION_ZIP) && object instanceof InputStream) {
            File directory = null;
            try {
                directory = unzipSldPackage((InputStream) object);
                File uploadedFile = retrieveSldFile(directory);

                Style styleSld = parseSld(uploadedFile);

                //figure out the name of the style, first check if specified directly
                String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");

                if (name == null) {
                    name = findNameFromObject(styleSld);
                }

                if (name == null) {
                    throw new RestletException("Style must have a name.", Status.CLIENT_ERROR_BAD_REQUEST);
                }

                //ensure that the style does already exist
                if (!existsStyleInCatalog(workspace, name)) {
                    throw new RestletException( "Style " + name + " doesn't exists.", Status.CLIENT_ERROR_FORBIDDEN  );
                }

                // save image resources
                saveImageResources(directory, workspace);

                // Save the style: serialize the style out into the data directory
                StyleInfo styleInfo = catalog.getStyleByName( workspace, style );
                serializeSldFileInCatalog(dataDir.style(styleInfo), uploadedFile);

                LOGGER.info("PUT Style Package: " + name + ", workspace: " + workspace);

            } catch (RestletException e) {
                // Re-throw the exception
                throw e;

            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.severe("Error processing the style package (PUT): " + e.getMessage());
                throw new RestletException( "Error processing the style", Status.SERVER_ERROR_INTERNAL, e );
            } finally {
                FileUtils.deleteQuietly(directory);
            }
        }
        else if (object instanceof Style || object instanceof InputStream) {
            /*
             * Force the .sld file to be overriden and it's Style object cleared from the
             * ResourcePool cache
             */
            StyleInfo s = catalog.getStyleByName( workspace, style );

            ResourcePool resourcePool = catalog.getResourcePool();
            if (object instanceof Style) {
                resourcePool.writeStyle(s, (Style) object, true);
            }
            else {
                resourcePool.writeStyle(s, (InputStream)object);
            }

            /*
             * make sure to save the StyleInfo so that the Catalog issues the notification events
             */
            catalog.save(s);
        }
        else if (object instanceof InputStream) {
            LOGGER.info( "PUT style InputStream");

        }
        LOGGER.info( "PUT style " + style);
    }

    @Override
    public boolean allowDelete() {
        return getAttribute( "style" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String styleName = getAttribute("style");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);

        StyleInfo style = workspace != null ? catalog.getStyleByName(workspace, styleName) :
            catalog.getStyleByName(styleName);

        if (recurse) {
            new CascadeDeleteVisitor(catalog).visit(style);
        } else {
            // ensure that no layers reference the style
            List<LayerInfo> layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                throw new RestletException("Can't delete style referenced by existing layers.", Status.CLIENT_ERROR_FORBIDDEN);
            }
            catalog.remove(style);
        }

        // check purge parameter to determine if the underlying file
        // should be deleted
        boolean purge = getQueryStringValue("purge", Boolean.class, false);
        catalog.getResourcePool().deleteStyle(style, purge);

        LOGGER.info("DELETE style " + styleName);
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
            throw new RestletException("No sld file provided:", Status.CLIENT_ERROR_FORBIDDEN);
        }

        LOGGER.fine("retrieveSldFile (sldFile): " + matchingFiles[0].getAbsolutePath());

        return matchingFiles[0];
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
                throw new RestletException("Style error.", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            return style;

        } catch (Exception ex) {
            LOGGER.severe(ex.getMessage());
            throw new RestletException("Style error. " + ex.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST);

        } finally {
            IOUtils.closeQuietly(is);
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
            throw new RestletException("Error creating file", Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

}
