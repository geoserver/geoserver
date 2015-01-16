package org.geoserver.catalog.rest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.restlet.data.*;
import org.restlet.resource.Resource;
//import org.geoserver.catalog.rest.SLDFormat;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.Exception;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to manage a style package upload in Geosever.
 *
 * A style package is a zip file that contains the sld and images used in the sld.
 *
 * @author Jose García (josegar74@gmail.com)
 *
 */
public class StylePackageResource extends Resource {

    private final static Logger LOGGER = Logger.getLogger(StylePackageResource.class.toString());

    protected Catalog catalog;

    public StylePackageResource(Request request, Response response, Catalog catalog) {
        super(null, request, response);
        this.catalog = catalog;
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePost() {
        String workspaceName = getAttribute("workspace");
        String styleName = getAttribute("style");

        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
        String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");

        File directory = null;

        try {
            directory = doFileUpload(styleName, workspaceName);
            File uploadedFile = retrieveSldFile(directory);

            Style style = parseSld(uploadedFile);

            if (name == null) {
                //infer name from sld
                name = style.getName();
            }

            if (name == null) {
                throw new RestletException("Style must have a name.", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            //ensure that the style does not already exist
            if (existsStyleInCatalog(workspaceName, name)) {
                throw new RestletException( "Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN  );
            }

            // serialize the style out into the data directory
            GeoServerResourceLoader loader = catalog.getResourceLoader();

            String path = "styles/" + name + ".sld";
            String pathStyleFolder = "styles/";
            if (workspaceName != null) {
                path = "workspaces/" + workspaceName + "/" + path;
                pathStyleFolder = "workspaces/" + workspaceName + "/" + pathStyleFolder;
            }

            File f;
            try {
                f = loader.find(path);
            }
            catch (IOException e) {
                throw new RestletException( "Error looking up file", Status.SERVER_ERROR_INTERNAL, e );
            }

            if ( f != null ) {
                String msg = "SLD file " + path + ".sld already exists.";
                throw new RestletException( msg, Status.CLIENT_ERROR_FORBIDDEN);
            }

            File stylesDir = new File(loader.getBaseDirectory(), pathStyleFolder);

            // save image resources
            saveImageResources(directory, stylesDir);

            // serialize the style out into the data directory
            try {
                f = loader.createFile(path);
                serializeSldFileInCatalog(f, uploadedFile);
            }
            catch (IOException e) {
                throw new RestletException( "Error saving the style", Status.SERVER_ERROR_INTERNAL, e );
            }

            //create a style info object
            StyleInfo sinfo = catalog.getFactory().createStyle();
            sinfo.setName(name);
            sinfo.setFilename(f.getName());
            if (workspaceName != null) {
                sinfo.setWorkspace(catalog.getWorkspaceByName(workspaceName));
            }
            catalog.add(sinfo);

            LOGGER.info("POST Style Package: " + name + ", workspace: " + workspaceName);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Error processing the style package (POST): " + e.getMessage());
            throw new RestletException( "Error processing the style", Status.SERVER_ERROR_INTERNAL, e );
        } finally {
            FileUtils.deleteQuietly(directory);
        }

    }

    @Override
    public void handlePut() {
        String workspaceName = getAttribute("workspace");
        String styleName = getAttribute("style");

        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
        String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");

        File directory = null;
        try {
            directory = doFileUpload(styleName, workspaceName);
            File uploadedFile = retrieveSldFile(directory);

            Style style = parseSld(uploadedFile);

            if (name == null) {
                //infer name from sld
                name = style.getName();
            }

            if (name == null) {
                throw new RestletException("Style must have a name.", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            //ensure that the style does already exist
            if (!existsStyleInCatalog(workspaceName, name)) {
                throw new RestletException( "Style " + name + " doesn't exists.", Status.CLIENT_ERROR_FORBIDDEN  );
            }


            //serialize the style out into the data directory
            String path = "styles/" + name + ".sld";
            String pathStyleFolder = "styles/";
            if (workspaceName != null) {
                path = "workspaces/" + workspaceName + "/" + path;
                pathStyleFolder = "workspaces/" + workspaceName + "/" + pathStyleFolder;
            }


            GeoServerResourceLoader loader = catalog.getResourceLoader();
            File f;
            try {
                f = loader.find(path);
            }
            catch (IOException e) {
                throw new RestletException( "Error looking up file", Status.SERVER_ERROR_INTERNAL, e );
            }

            if (f == null) {
                String msg = "SLD file " + name + ".sld doesn't exists.";
                throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
            }

            File stylesDir = new File(loader.getBaseDirectory(), pathStyleFolder);

            // save image resources
            saveImageResources(directory, stylesDir);

            // serialize the style out into the data directory
            serializeSldFileInCatalog(f, uploadedFile);

            // update a style info object
            StyleInfo sinfo = catalog.getStyleByName(workspaceName, name);
            sinfo.setFilename(f.getName());
            catalog.save(sinfo);

            LOGGER.info("PUT Style Package: " + name + ", workspace: " + workspaceName);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Error processing the style package (PUT): " + e.getMessage());
            throw new RestletException( "Error processing the style", Status.SERVER_ERROR_INTERNAL, e );
        } finally {
            FileUtils.deleteQuietly(directory);
        }
    }


    /**
     * Convenience method for subclasses to look up the (URL-decoded)value of
     * an attribute from the request, ie {@link Request#getAttributes()}.
     *
     * @param attribute The name of the attribute to lookup.
     * @return The value as a string, or null if the attribute does not exist
     *         or cannot be url-decoded.
     */
    protected String getAttribute(String attribute) {
        return RESTUtils.getAttribute(getRequest(), attribute);
    }

    /**
     * Determines the upload method from a request.
     */
    protected String getUploadMethod(Request request) {
        return ((String) request.getResourceRef().getLastSegment()).toLowerCase();
    }

    /**
     * Determines if the upload method is inline, ie the content is specified directly in the
     * request payload, or referenced by a url.
     *
     * @param method One of 'file.' (inline), 'url.' (via url), or 'external.' (already on server)
     */
    protected boolean isInlineUpload(String method) {
        return method != null && (method.startsWith("file.") || method.startsWith("url."));
    }

    /**
     * Does the file upload based on the specified method.
     *
     * @param style  The name of the style being added
     */
    protected File doFileUpload(String style, String workspaceName) {
        File directory = null;

        try {
            directory = catalog.getResourceLoader()
                    .findOrCreateDirectory("tmp" + "/" + style);
        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }

        handleFileUpload(style, workspaceName, directory);

        return directory;
    }

    /**
     * @param style
     * @param directory
     * @return List of uploaded files in zip
     */
    protected void handleFileUpload(String style, String workspaceName, File directory) {
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);

        MediaType mediaType = getRequest().getEntity().getMediaType();

        if (mediaType == null || !(RESTUtils.isZipMediaType(mediaType))) {
            throw new RestletException("SLD package file must be a zip file", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("PUT file, mimetype: " + mediaType);

        File uploadedFile = null;
        try {
            uploadedFile = RESTUtils.handleBinUpload(style, directory, true, getRequest(), workspaceName);
            LOGGER.fine("handleFileUpload: " + uploadedFile.getAbsolutePath());

        } catch (Throwable t) {
            throw new RestletException("Error while storing uploaded file:", Status.SERVER_ERROR_INTERNAL, t);
        }

        //handle the case that the uploaded file was a zip file, if so unzip it
        if (mediaType != null && RESTUtils.isZipMediaType(mediaType)) {
            //rename to .zip if need be
            if (!uploadedFile.getName().endsWith(".zip")) {
                File newUploadedFile = new File(uploadedFile.getParentFile(), FilenameUtils.getBaseName(uploadedFile.getAbsolutePath()) + ".zip");
                uploadedFile.renameTo(newUploadedFile);
                uploadedFile = newUploadedFile;
            }
            //unzip the file
            try {
                RESTUtils.unzipFile(uploadedFile, directory);

                File[] matchingFiles = directory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return (name.endsWith("sld") || name.endsWith("svg") || name.endsWith("png") || name.endsWith("jpg"));
                    }
                });

            } catch (RestletException e) {
                throw e;
            } catch (Exception e) {
                throw new RestletException("Error occured unzipping file", Status.SERVER_ERROR_INTERNAL, e);
            }
        } else {

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
    private void serializeSldFileInCatalog(File sldFile, File uploadedSldFile) {
        BufferedOutputStream out = null;
        try {
            //serialize the file to the styles directory
            out = new BufferedOutputStream(new FileOutputStream(sldFile));

            byte[] sldContent = FileUtils.readFileToByteArray(uploadedSldFile);

            out.write(sldContent);

            // This produces the error described in http://jira.codehaus.org/browse/GEOS-3840
            //InputStream is = new FileInputStream(uploadedSldFile);
            //SLDFormat sldFormat = new SLDFormat();
            //Style style = (Style) sldFormat.read(is);

            //SLDFormat format2 = new SLDFormat(true);
            //format2.toRepresentation(style).write(out);

            out.flush();

        } catch (IOException e) {
            throw new RestletException("Error creating file", Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
    /**
     * Save the image resources in the styles folder
     *
     * @param directory
     * @throws java.io.IOException
     */
    private void saveImageResources(File directory, File stylesDir) throws IOException {
        File[] imageFiles = retrieveImageFiles(directory);

        for (int i = 0; i < imageFiles.length; i++) {
            FileUtils.copyFileToDirectory(imageFiles[i],
                    stylesDir);
                    //catalog.getResourceLoader().findOrCreateDirectory("styles"));
        }
    }

    /**
     * Returns a list of image files in the given directory
     *
     * @param directory
     * @return
     */
    private File[] retrieveImageFiles(File directory) {
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith("svg") || name.endsWith("png") || name.endsWith("jpg"));
            }
        });

        return matchingFiles;
    }

    /**
     * Returns the sld file in the given directory. If no sld file, throws an exception
     *
     * @param directory
     * @return
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

        LOGGER.fine("handleFileUpload (sldFile): " + matchingFiles[0].getAbsolutePath());

        return matchingFiles[0];
    }


    /**
     * Parses the sld file.
     *
     * @param sldFile
     * @return
     */
    private Style parseSld(File sldFile) {
        Style style = null;
        InputStream is = null;

        try {
            is = new FileInputStream(sldFile);

            SLDParser parser
                    = new SLDParser(CommonFactoryFinder.getStyleFactory(null), is);

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
            throw new RestletException("Style error.", Status.CLIENT_ERROR_BAD_REQUEST);

        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
