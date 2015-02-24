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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.geoserver.rest.RestletException;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;

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
     * @deprecated use {@link #handleBinUpload(String, File, Request)}.
     */
    public static File handleBinUpload(String datasetName, String extension,
            Request request) throws IOException, ConfigurationException {
    
        final File dir = GeoserverDataDirectory.findCreateConfigDir("data");
        return handleBinUpload( datasetName + "." + extension, dir, request );
    }

    /**
     * Reads content from the body of a request and writes it to a file in the given directory.
     * 
     * If the file already exists, the directory content will be deleted recursively 
     * before creating the new file.
     * 
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to.
     * @param request The request.
     * 
     * @return The file object representing the newly written file.
     * 
     * @throws IOException Any I/O errors that occur.
     * 
     * @deprecated use {@link #handleBinUpload(String, File, boolean, Request)}.
     */
    public static File handleBinUpload(String fileName, File directory, Request request)
            throws IOException {
        return handleBinUpload(fileName, directory, true, request);
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
    public static File handleBinUpload(String fileName, File directory, boolean deleteDirectoryContent, Request request) 
        throws IOException {
        
        final File newFile = new File(directory, fileName);
        
        if(newFile.exists()) {
            if (deleteDirectoryContent) {
                FileUtils.cleanDirectory(directory);
            } else {
                // delete the file, otherwise replacing it with a smaller one will leave bytes at the end
                newFile.delete();
            }
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
    public static File handleURLUpload(String datasetName, String extension, Request request) throws IOException, ConfigurationException {
        ////
        //
        // Get the dir where to write and create a file there
        //
        ////
        File dir = GeoserverDataDirectory.findCreateConfigDir("data");
        return handleURLUpload(datasetName + "." + extension, dir, request);
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
    public static File handleURLUpload(String fileName, File directory, Request request ) throws IOException {
      //this may exists already, but we don't fail here since 
        //it might be old and unused, if needed we fail later while copying
        File newFile  = new File(directory,fileName);
        
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
    public static void unzipFile( File zipFile, File outputDirectory, Request request, List<File> files) throws IOException {
        if ( outputDirectory == null ) {
            outputDirectory = zipFile.getParentFile();
        }
        if ( outputDirectory != null && !outputDirectory.exists() ) {
            outputDirectory.mkdir();
        }
        ZipFile archive = new ZipFile(zipFile);
        IOUtils.inflate(archive, outputDirectory, null, request, files);
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
        File outputDirectory = new File(GeoserverDataDirectory.findCreateConfigDir("data"), storeName);
        unzipFile(zipFile, outputDirectory, null, null);
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
}
