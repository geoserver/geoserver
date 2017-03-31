/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geotools.util.logging.Logging;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Message;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.resource.Representation;
import org.vfny.geoserver.global.ConfigurationException;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.zip.ZipInputStream;

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
     * Unzip a zipped dataset.
     * 
     * @param storeName the name of the store to handle.
     * @param zipFile the zipped archive 
     * @return null if the zip file does not point to a valid zip file, the output directory otherwise.
     * 
     * @deprecated use {@link #unzipFile(File, File)}
     *  
     */
    public static org.geoserver.platform.resource.Resource unpackZippedDataset(String storeName, org.geoserver.platform.resource.Resource zipFile) throws IOException, ConfigurationException {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        String outputPath = Paths.path("data",Paths.convert(storeName));
        Resource directory = loader.get(outputPath);
        IOUtils.unzipFile(zipFile, directory, null, null, null, null, false);
        return directory;
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
     *
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
     *
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
     *
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
     *
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
     * 
     * @param map
     * @param key
     *
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
            File rootFile = new File(rootDir);
            if (rootFile.isAbsolute()) {
                if (!rootFile.exists()) {
                    if (!rootFile.mkdirs()) {
                        rootFile.delete();
                        return null;
                    }
                } else {
                    if (!rootFile.isDirectory()) {
                        LOGGER.info(rootDir + " ROOT path is not a directory");
                        return null;
                    }
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


    /**
     * Unzips a InputStream to a directory
     *
     * @param in
     * @param outputDirectory
     * @throws IOException
     */
    public static void unzipInputStream(InputStream in, File outputDirectory) throws IOException {
        ZipInputStream zin = null;

        try {
            zin = new ZipInputStream(in);

            ZipEntry entry;
            byte[] buffer = new byte[2048];

            while((entry = zin.getNextEntry())!=null) {
                String outpath = outputDirectory.getAbsolutePath() + "/" + entry.getName();
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(outpath);
                    int len = 0;
                    while ((len = zin.read(buffer)) > 0)
                    {
                        output.write(buffer, 0, len);
                    }
                } finally {
                    IOUtils.closeQuietly(output);
                }
            }
        } finally {
            IOUtils.closeQuietly(zin);
        }
    }

    /**
     *
     * Use this to read or manipulate custom headers in a request or response
     *
     * @return headers form
     */
    public static Form getHeaders(Message message) {
        Form headers = (Form) message.getAttributes().get("org.restlet.http.headers");
        if (headers == null) {
            headers = new Form();
            message.getAttributes().put("org.restlet.http.headers", headers);
        }
        return headers;
    }

    /**
     *
     * Create an empty response body for HEAD requests
     *
     * @return empty representation.
     */
    public static Representation emptyBody() {
        return new Representation() { //empty
            @Override public ReadableByteChannel getChannel() throws IOException { return null; }
            @Override public InputStream getStream() throws IOException { return null; }
            @Override public void write(OutputStream outputStream) throws IOException {}
            @Override public void write(WritableByteChannel writableChannel) throws IOException {}
        };
    }

}
