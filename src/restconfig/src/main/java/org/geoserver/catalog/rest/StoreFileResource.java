/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUploadPathMapper;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public abstract class StoreFileResource extends Resource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");
    
    protected Catalog catalog;
    
    public StoreFileResource( Request request, Response response, Catalog catalog) {
        super( null, request, response );
        this.catalog = catalog;
    }
    
    @Override
    public boolean allowPut() {
        return true;
    }
 
     /**
      * Convenience method for subclasses to look up the (URL-decoded)value of
      * an attribute from the request, ie {@link Request#getAttributes()}.
      * 
      * @param attribute The name of the attribute to lookup.
      * 
      * @return The value as a string, or null if the attribute does not exist
      *     or cannot be url-decoded.
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
      * @param method The method, one of 'file.' (inline), 'url.' (via url), or 'external.' (already on server)
      * @param storeName The name of the store being added
      * @param format The store format.
      */
     protected List<File> doFileUpload(String method, String workspaceName, String storeName, String format) {
         File directory = null;
         
         // Prepare the directory only in case this is not an external upload
         if (isInlineUpload(method)){ 
             try {                 
                 // Mapping of the input directory
                 if(method.startsWith("url.")){
                     // For URL upload method, workspace and StoreName are not considered
                     directory = createFinalRoot(null, null);
                 }else{
                     directory = createFinalRoot(workspaceName, storeName);
                 }
             } 
             catch (IOException e) {
                 throw new RestletException( e.getMessage(), Status.SERVER_ERROR_INTERNAL, e );
             }
         }
         return handleFileUpload(storeName, workspaceName, format, directory);
     }

    private File createFinalRoot(String workspaceName, String storeName)
            throws IOException {
        File directory;
        directory = catalog.getResourceLoader().findOrCreateDirectory("data", workspaceName,
                storeName);
        // Selection of the original ROOT directory path
        StringBuilder root = new StringBuilder(directory.getAbsolutePath());
        // StoreParams to use for the mapping.
        Map<String, String> storeParams = new HashMap<String, String>();
        // Listing of the available pathMappers
        List<RESTUploadPathMapper> mappers = GeoServerExtensions.extensions(RESTUploadPathMapper.class);
        // Mapping of the root directory
        for(RESTUploadPathMapper mapper : mappers){
            mapper.mapStorePath(root, workspaceName, storeName, storeParams);
        }
        // Creation of a new File pointing to the new root
        directory = new File(root.toString());
        // Creation of the new directory
        directory.mkdirs();
        return directory;
    }

    /**
     * 
     * @param store
     * @param format
     * @param directory
     * @return
     */
    protected List<File> handleFileUpload(String store, String workspace, String format, File directory) {
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);

        MediaType mediaType = getRequest().getEntity().getMediaType();
        if(LOGGER.isLoggable(Level.INFO))
            LOGGER.info("PUT file, mimetype: " + mediaType );

        List<File> files = new ArrayList<File>();
        
        File uploadedFile = null;
        boolean external = false;
        try {
            String method = (String) getRequest().getResourceRef().getLastSegment();
            if (method != null && method.toLowerCase().startsWith("file.")) {
                uploadedFile = RESTUtils.handleBinUpload(store + "." + format, workspace, directory, getRequest());
            }
            else if (method != null && method.toLowerCase().startsWith("url.")) {
                uploadedFile = RESTUtils.handleURLUpload(store + "." + format, workspace, directory, getRequest());
            }    
            else if (method != null && method.toLowerCase().startsWith("external.")) {
                uploadedFile = RESTUtils.handleEXTERNALUpload(getRequest());
                external = true;
            }
            else{
                final StringBuilder builder = 
                    new StringBuilder("Unrecognized file upload method: ").append(method);
                throw new RestletException( builder.toString(), Status.CLIENT_ERROR_BAD_REQUEST);
            }
        } 
        catch (Throwable t) {
            if(t instanceof RestletException) {
                throw (RestletException) t;
            } else {
                throw new RestletException( "Error while storing uploaded file:", Status.SERVER_ERROR_INTERNAL, t );
            }
        }
        
        //handle the case that the uploaded file was a zip file, if so unzip it
        if (mediaType!=null && RESTUtils.isZipMediaType( mediaType ) ) {
            //rename to .zip if need be
            if ( !uploadedFile.getName().endsWith( ".zip") ) {
                File newUploadedFile = new File( uploadedFile.getParentFile(), FilenameUtils.getBaseName(uploadedFile.getAbsolutePath()) + ".zip" );
                String oldFileName = uploadedFile.getName();
                if (!uploadedFile.renameTo( newUploadedFile )) {
                    String errorMessage = "Error renaming zip file from " + oldFileName
                            + " -> " + newUploadedFile.getName();
                    throw new RestletException(errorMessage, Status.SERVER_ERROR_INTERNAL);
                }
                uploadedFile = newUploadedFile;
            }
            //unzip the file 
            try {
                // Unzipping of the file and, if it is a POST request, filling of the File List
                RESTUtils.unzipFile(uploadedFile, directory, workspace , store, getRequest(), files, external);

                
                //look for the "primary" file
                //TODO: do a better check
                File primaryFile = findPrimaryFile( directory, format );
                if ( primaryFile != null ) {
                    uploadedFile = primaryFile;
                }
                else {
                    throw new RestletException( "Could not find appropriate " + format + " file in archive", Status.CLIENT_ERROR_BAD_REQUEST );
                }
            }
            catch( RestletException e ) {
                throw e;
            }
            catch( Exception e ) {
                throw new RestletException( "Error occured unzipping file", Status.SERVER_ERROR_INTERNAL, e );
            }
        }
        // If the File List is empty then the uploaded file must be added    
        if(files.isEmpty() && uploadedFile != null){
            files.add(uploadedFile);
        }
        
        return files;
    }

    /**
     * 
     * @param directory
     * @param format
     * @return
     */
    protected File findPrimaryFile(File directory, String format) {
        for (File f : FileUtils.listFiles(directory, new String[] { format }, true)) {
            // assume the first
            return f;
        }
        return null;
    }

}
