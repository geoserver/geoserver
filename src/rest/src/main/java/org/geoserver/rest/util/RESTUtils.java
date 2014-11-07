/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.rest.RestletException;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.vfny.geoserver.global.ConfigurationException;
import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;
import org.restlet.data.Method;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Utility class for Restlets.
 * 
 * @author David Winslow, OpenGeo
 * @author Simone Giannecchini, GeoSolutions
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RESTUtils {
    
    static Logger LOGGER = Logging.getLogger("org.geoserver.rest.util");

    public static final String ROOT_KEY = "root";
    
    public static final String QUIET_ON_NOT_FOUND_KEY = "quietOnNotFound";
    
    /**
     * Returns the underlying HttpServletRequest from a Restlet Request object.
     * <p>
     * Note that this only returns a value in the case where the Restlet 
     * request/call is originating from a servlet.
     * </p>
     * @return The HttpServletRequest, or null.
     */
    public static HttpServletRequest getServletRequest( Request request ) {
        if ( request instanceof HttpRequest ) {
            HttpRequest httpRequest = (HttpRequest) request;
            if ( httpRequest.getHttpCall() instanceof ServletCall ) {
                ServletCall call = (ServletCall) httpRequest.getHttpCall();
                return call.getRequest();
            }
        }
        
        return null;
    }
    
    /**
     * Returns the base url of a request.
     */
    public static String getBaseURL( Request request ) {
        Reference ref = request.getResourceRef();
        HttpServletRequest servletRequest = getServletRequest(request);
        if ( servletRequest != null ) {
            String baseURL = ref.getIdentifier();
            return baseURL.substring(0, baseURL.length()-servletRequest.getPathInfo().length());
        } else {
            return ref.getParentRef().getIdentifier();
        }
    }
    
    /**
     * This function gets the stream of the request to copy it into a file.
     * 
     * This method will create a "data" folder in GEOSERVER_DATA_DIRECTORY if needed.
     * 
     * @deprecated use {@link #handleBinUpload(String, File, Request)}.
     */
    public static File handleBinUpload(String datasetName, String extension,
            Request request) throws IOException, ConfigurationException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource data = loader.get("data");
        
        final File dir = data.dir(); // find or create
        return handleBinUpload( datasetName + "." + extension, null, dir, request );
    }

    /**
     * Reads content from the body of a request and writes it to a file in the given directory.
     * 
     * If the file already exists, the directory content will be deleted recursively 
     * before creating the new file.
     * 
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to
     * @param request The request.
     * 
     * @return The file object representing the newly written file.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * @deprecated use {@link #handleBinUpload(String, File, boolean, Request)}.
     */
    public static File handleBinUpload(String fileName, String workSpace, File directory, Request request)
            throws IOException {
        return handleBinUpload(fileName, directory, true, request, workSpace);
    }
    
    /**
     * Reads content from the body of a request and writes it to a file.
     * 
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to.
     * @param deleteDirectoryContent Delete directory content if the file already exists.
     * @param request The request.
     * 
     * @return The file object representing the newly written file.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * TODO: move this to IOUtils.
     */
    public static File handleBinUpload(String fileName, File directory, boolean deleteDirectoryContent, Request request) throws IOException {
        return handleBinUpload(fileName, directory, deleteDirectoryContent, request, null);
    }

    
    /**
     * Reads content from the body of a request and writes it to a file.
     * 
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to.
     * @param deleteDirectoryContent Delete directory content if the file already exists.
     * @param request The request.
     * 
     * @return The file object representing the newly written file.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * TODO: move this to IOUtils.
     */
    public static File handleBinUpload(String fileName, File directory, boolean deleteDirectoryContent,
            Request request, String workSpace) throws IOException {
        // Creation of a StringBuilder for the selected file
        StringBuilder itemPath = new StringBuilder(fileName);
        // Mediatype associated to the input file
        MediaType mediaType = request.getEntity().getMediaType();
        // Only zip files are not remapped
        if(mediaType == null || !isZipMediaType( mediaType )){
            String baseName = FilenameUtils.getBaseName(fileName);
            String itemName = FilenameUtils.getName(fileName);
            // Store parameters used for mapping the file path
            Map<String, String> storeParams = new HashMap<String, String>();
            // Mapping item path
            remapping(workSpace, baseName, itemPath, itemName, storeParams);
        }

        final File newFile = new File(directory, itemPath.toString());

        if(newFile.exists()) {
            if (deleteDirectoryContent) {
                FileUtils.cleanDirectory(directory);
            } else {
                // delete the file, otherwise replacing it with a smaller one will leave bytes at the end
                newFile.delete();
            }
        }else{
            // Create the directory tree associated to the input file
            newFile.getParentFile().mkdirs();
        }
        
        final ReadableByteChannel source = request.getEntity().getChannel();
        RandomAccessFile raf = null;
        FileChannel outputChannel = null;
        try {
            raf = new RandomAccessFile(newFile, "rw");
            outputChannel = raf.getChannel();
            IOUtils.copyChannel(1024 * 1024, source, outputChannel);
        } finally {
            try {
                if(raf != null) {
                    raf.close();
                }
            } finally {
                IOUtils.closeQuietly(source);
                IOUtils.closeQuietly(outputChannel);
            }
        }
        return newFile;
    }
    
    /**
     * Handles the upload of a dataset using the URL method.
     * 
     * @param datasetName the name of the uploaded dataset.
     * @param extension the extension of the uploaded dataset.
     * @param request the incoming request.
     * @return a {@link File} that points to the final uploaded dataset.
     * 
     * @throws IOException
     * @throws ConfigurationException
     * 
     * @deprecated use {@link #handleURLUpload(String, File, Request)}.
     */
    public static File handleURLUpload(String datasetName, String workSpace, String extension, Request request) throws IOException, ConfigurationException {
        // Get the dir where to write and create a file there
        
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource data = loader.get("data");
        final File dir = data.dir(); // find or create
        return handleURLUpload(datasetName + "." + extension, workSpace, dir, request);
    }
    
    /**
     * Reads a url from the body of a request, reads the contents of the url and writes it to a file.
     *   
     * @param fileName The name of the file to write.
     * @param directory The directory to write the new file to.
     * @param request The request.
     * 
     * @return The file object representing the newly written file.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * TODO: move this to IOUtils
     */
    public static File handleURLUpload(String fileName, String workSpace, File directory, Request request)
            throws IOException {
        //Initial remapping of the input file
        StringBuilder itemPath = new StringBuilder(fileName);
        // Mediatype associated to the input file
        MediaType mediaType = request.getEntity().getMediaType();
        // Only zip files are not remapped
        if(mediaType == null || !isZipMediaType( mediaType )){
            String baseName = FilenameUtils.getBaseName(fileName);
            // Store parameters used for mapping the file path
            Map<String, String> storeParams = new HashMap<String, String>();
            String itemName = FilenameUtils.getName(fileName);
            // Mapping item path
            remapping(workSpace, baseName, itemPath, itemName, storeParams);
        }

        //this may exists already, but we don't fail here since 
        //it might be old and unused, if needed we fail later while copying
        File newFile  = new File(directory,itemPath.toString());
        
        //get the URL for this file to upload
        final InputStream inStream=request.getEntity().getStream();
        final String stringURL=IOUtils.getStringFromStream(inStream);
        final URL fileURL=new URL(stringURL);
        
        ////
        //
        // Now do the real upload
        //
        ////
        //check if it is a file
        final File inputFile= IOUtils.URLToFile(fileURL);
        if(inputFile!=null && inputFile.exists() && inputFile.canRead()) {
            IOUtils.copyFile(inputFile, newFile);
        } else {
            final InputStream inputStream =  fileURL.openStream();
            final OutputStream outStream = new FileOutputStream(newFile);
            IOUtils.copyStream(inputStream, outStream, true, true);
        }
        
        return newFile;
    }
    
    /**
     * Handles an upload using the EXTERNAL method.
     * 
     * @param request
     * @throws IOException 
     */
    public static File handleEXTERNALUpload(Request request) throws IOException {
        //get the URL for this file to upload
        InputStream inStream = null;
        URL fileURL ;
        try {
            inStream = request.getEntity().getStream();
            final String stringURL = IOUtils.getStringFromStream(inStream);
            fileURL = new URL(stringURL);
        } finally {
            IOUtils.closeQuietly(inStream);
        }

        final File inputFile = IOUtils.URLToFile(fileURL);
        if(inputFile == null || !inputFile.exists()) {
            throw new RestletException("Failed to locate the input file " + fileURL, Status.CLIENT_ERROR_BAD_REQUEST);
        } else if(!inputFile.canRead()) {
            throw new RestletException("Input file is not readable, check filesystem permissions: " + fileURL, 
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return inputFile;
    }
    
    static Set<String> ZIP_MIME_TYPES = new HashSet();
    static {
        ZIP_MIME_TYPES.add( "application/zip" );
        ZIP_MIME_TYPES.add( "multipart/x-zip" );
        ZIP_MIME_TYPES.add( "application/x-zip-compressed" );
    }
    /**
     * Determines if the specified media type represents a zip stream.
     */
    public static boolean isZipMediaType( MediaType mediaType ) {
        return ZIP_MIME_TYPES.contains( mediaType.toString() );
    }
    
    /**
     * Unzips a zip a file to a specified directory, deleting the zip file after unpacking.
     * 
     * @param zipFile The zip file.
     * @param outputDirectory The directory to unpack the contents to.
     * @param request HTTP request sent.
     * @param files Empty List to be filled with the zip files.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * TODO: move this to IOUtils
     */
    public static void unzipFile( File zipFile, File outputDirectory ) throws IOException {
        unzipFile(zipFile, outputDirectory, null, null, null, null, false);
    }
    
    /**
     * Unzips a zip a file to a specified directory, deleting the zip file after unpacking.
     * 
     * @param zipFile The zip file.
     * @param outputDirectory The directory to unpack the contents to.
     * @param external 
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * TODO: move this to IOUtils
     */
    public static void unzipFile(File zipFile, File outputDirectory, String workspace,
            String store, Request request, List<File> files, 
            boolean external) throws IOException {

        if (outputDirectory == null) {
            outputDirectory = zipFile.getParentFile();
        }
        if (outputDirectory != null && !outputDirectory.exists()) {
            outputDirectory.mkdir();
        }
        ZipFile archive = new ZipFile(zipFile);

        IOUtils.inflate(archive, outputDirectory, null, workspace, store, request, files, external);
        IOUtils.deleteFile(zipFile);
    }
    
    /**
     * Unzip a zipped dataset.
     * 
     * @param storeName the name of the store to handle.
     * @param zipFile the zipped archive 
     * @return null if the zip file does not point to a valid zip file, the output directory otherwise.
     * 
     * @deprecated use {@link #unzipFile(File, File)}
     *  
     */
    public static File unpackZippedDataset(String storeName, File zipFile) throws IOException, ConfigurationException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        String outputPath = Paths.path("data",Paths.convert(storeName));
        Resource directory = loader.get(outputPath);
        File outputDirectory = directory.dir(); // find or create
        unzipFile(zipFile, outputDirectory, null, null, null, null, false);
        return outputDirectory;
    }

    /**
     * Fetch a request attribute as a String, accounting for URL-encoding.
     *
     * @param request the Restlet Request object that might contain the attribute
     * @param name the name of the attribute to retrieve
     *
     * @return the attribute, URL-decoded, if it exists and is a valid URL-encoded string, or null
     *     otherwise
     */
    public static String getAttribute(Request request, String name) {
        Object o = request.getAttributes().get(name);
        return decode(o);
    }
    
    public static String getQueryStringValue(Request request, String key) {
        String value = request.getResourceRef().getQueryAsForm().getFirstValue(key);
        return decode(value);
    }
    
    static String decode(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            return URLDecoder.decode(value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Method for searching an item inside the MetadataMap.
     * 
     * @param workspaceName
     * @param storeName
     * @param catalog
     * @return
     */
    public static String getItem(String workspaceName, String storeName, Catalog catalog, String key) {
        // Initialization of a null String containing the root directory to use for the input store config
        String item = null;

        // ////////////////////////////////////
        //
        // Check Store info if present
        //
        // ////////////////////////////////////
        item = extractMapItem(loadMapfromStore(storeName, catalog), key);

        // ////////////////////////////////////
        //
        // Check WorkSpace info if not found
        // inside the Store Info
        //
        // ////////////////////////////////////
        if (item == null) {
            item = extractMapItem(loadMapfromWorkSpace(workspaceName, catalog), key);
        }

        // ////////////////////////////////////
        //
        // Finally check Global info
        //
        // ////////////////////////////////////

        if (item == null) {
            item = extractMapItem(loadMapFromGlobal(), key);
        }

        return item;
    }

    /**
     * This method is used for extracting the metadata map from the selected store
     * 
     * @param storeName
     * @param catalog
     * @return
     */
    public static MetadataMap loadMapfromStore(String storeName, Catalog catalog) {
       StoreInfo storeInfo = catalog.getStoreByName(storeName, CoverageStoreInfo.class);
        if(storeInfo == null){
            storeInfo = catalog.getStoreByName(storeName, DataStoreInfo.class);
        }
        // If the Store is present, then the associated MetadataMap is selected
        if(storeInfo != null){
            MetadataMap map = storeInfo.getMetadata();
            return map;
        }
       return null;
    }

    /**
     * This method is used for extracting the metadata map from the selected workspace
     * 
     * @param workspaceName
     * @param catalog
     * @return
     */
    public static MetadataMap loadMapfromWorkSpace(String workspaceName, Catalog catalog) {
       WorkspaceInfo wsInfo = catalog.getWorkspaceByName(workspaceName);
       // If the WorkSpace is present, then the associated MetadataMap is selected
       if(wsInfo != null){
           GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
           SettingsInfo info = gs.getSettings(wsInfo);
           MetadataMap map = info != null ? info.getMetadata() : null;
           return map;
       }
       return null;
    }
   
    /**
     * This method is used for extracting the metadata map from the global settings
     * 
     * @return
     */
    public static MetadataMap loadMapFromGlobal() {
       GeoServerInfo gsInfo = GeoServerExtensions.bean(GeoServer.class).getGlobal();
       // Global info should be always not null
       if(gsInfo != null){
           SettingsInfo info = gsInfo.getSettings();
           MetadataMap map = info != null ? info.getMetadata() : null;
           return map;
       }
       return null;
    }

    /**
     * Extraction of the item from the metadata map
     * @param <T>
     * 
     * @param map
     * @return
     */
    public static String extractMapItem(MetadataMap map, String key) {
       if(map != null && !map.isEmpty()){
           String item = map.get(key, String.class);
           
           if (item != null && !item.isEmpty()){
               
               return item;
           } 
       }
       return null;
   }
    
    public static String getRootDirectory(String workspaceName, String storeName, Catalog catalog) {
        String rootDir = getItem(workspaceName, storeName, catalog, ROOT_KEY);
        if(rootDir != null){
            // Check if it already exists
            File root = new File(rootDir);
            if (!root.exists()) {
                if (!root.mkdirs()) {
                    root.delete();
                    return null;
                }
            } else {
                if (!root.isDirectory()) {
                    LOGGER.info(rootDir + " ROOT path is not a directory");
                    return null;
                }
            }
        }
        return rootDir;
    }

    public static void remapping(String workspace, String store, StringBuilder itemPath,
            String initialFileName, Map<String, String> storeParams) throws IOException {
        // Selection of the available PathMapper
        List<RESTUploadPathMapper> mappers = GeoServerExtensions
                .extensions(RESTUploadPathMapper.class);
        // Mapping the item path
        for (RESTUploadPathMapper mapper : mappers) {
            mapper.mapItemPath(workspace, store, storeParams, itemPath, initialFileName);
        }
    }
}
