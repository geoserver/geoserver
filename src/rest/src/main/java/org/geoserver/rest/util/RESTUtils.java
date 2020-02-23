/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import javax.servlet.http.HttpServletRequest;
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
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestException;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Utility class for Restlets.
 *
 * @author David Winslow, OpenGeo
 * @author Simone Giannecchini, GeoSolutions
 * @author Justin Deoliveira, OpenGeo
 */
public class RESTUtils {

    static Logger LOGGER = Logging.getLogger("org.geoserver.rest.util");

    public static final String ROOT_KEY = "root";

    public static final String QUIET_ON_NOT_FOUND_KEY = "quietOnNotFound";

    /**
     * Reads content from the body of a request and writes it to a file.
     *
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to.
     * @param deleteDirectoryContent Delete directory content if the file already exists.
     * @param request The request.
     * @return The file object representing the newly written file.
     * @throws IOException Any I/O errors that occur.
     *     <p>TODO: move this to IOUtils.
     */
    public static org.geoserver.platform.resource.Resource handleBinUpload(
            String fileName,
            org.geoserver.platform.resource.Resource directory,
            boolean deleteDirectoryContent,
            HttpServletRequest request)
            throws IOException {
        return handleBinUpload(fileName, directory, deleteDirectoryContent, request, null);
    }

    /**
     * Reads content from the body of a request and writes it to a file.
     *
     * @param fileName The name of the file to write out.
     * @param directory The directory to write the file to.
     * @param deleteDirectoryContent Delete directory content if the file already exists.
     * @param request The request.
     * @return The file object representing the newly written file.
     * @throws IOException Any I/O errors that occur.
     *     <p>TODO: move this to IOUtils.
     */
    public static org.geoserver.platform.resource.Resource handleBinUpload(
            String fileName,
            org.geoserver.platform.resource.Resource directory,
            boolean deleteDirectoryContent,
            HttpServletRequest request,
            String workSpace)
            throws IOException {
        // Creation of a StringBuilder for the selected file
        StringBuilder itemPath = new StringBuilder(fileName);
        // Mediatype associated to the input file
        MediaType mediaType =
                request.getContentType() == null
                        ? null
                        : MediaType.valueOf(request.getContentType());
        // Only zip files are not remapped
        if (mediaType == null || !isZipMediaType(mediaType)) {
            String baseName = FilenameUtils.getBaseName(fileName);
            String itemName = FilenameUtils.getName(fileName);
            // Store parameters used for mapping the file path
            Map<String, String> storeParams = new HashMap<>();
            // Mapping item path
            remapping(workSpace, baseName, itemPath, itemName, storeParams);
        }

        final org.geoserver.platform.resource.Resource newFile = directory.get(itemPath.toString());

        if (Resources.exists(newFile)) {
            if (deleteDirectoryContent) {
                for (Resource file : directory.list()) {
                    file.delete();
                }
            } else {
                // delete the file, otherwise replacing it with a smaller one will leave bytes at
                // the end
                newFile.delete();
            }
        }

        try (OutputStream os = newFile.out()) {
            IOUtils.copy(request.getInputStream(), os);
        }
        return newFile;
    }

    /**
     * Reads a url from the body of a request, reads the contents of the url and writes it to a
     * file.
     *
     * @param fileName The name of the file to write.
     * @param directory The directory to write the new file to.
     * @param request The request.
     * @return The file object representing the newly written file.
     * @throws IOException Any I/O errors that occur.
     *     <p>TODO: move this to IOUtils
     */
    public static org.geoserver.platform.resource.Resource handleURLUpload(
            String fileName,
            String workSpace,
            org.geoserver.platform.resource.Resource directory,
            HttpServletRequest request)
            throws IOException {
        // Initial remapping of the input file
        StringBuilder itemPath = new StringBuilder(fileName);
        // Mediatype associated to the input file
        MediaType mediaType =
                request.getContentType() != null
                        ? MediaType.valueOf(request.getContentType())
                        : null;
        // Only zip files are not remapped
        if (mediaType == null || !isZipMediaType(mediaType)) {
            String baseName = FilenameUtils.getBaseName(fileName);
            // Store parameters used for mapping the file path
            Map<String, String> storeParams = new HashMap<>();
            String itemName = FilenameUtils.getName(fileName);
            // Mapping item path
            remapping(workSpace, baseName, itemPath, itemName, storeParams);
        }

        // this may exists already, but we don't fail here since
        // it might be old and unused, if needed we fail later while copying
        org.geoserver.platform.resource.Resource newFile = directory.get(itemPath.toString());

        // get the URL for this file to upload
        final InputStream inStream = request.getInputStream();
        final String stringURL = IOUtils.getStringFromStream(inStream);
        final URL fileURL = new URL(stringURL);

        ////
        //
        // Now do the real upload
        //
        ////
        final InputStream inputStream = fileURL.openStream();
        final OutputStream outStream = newFile.out();
        IOUtils.copyStream(inputStream, outStream, true, true);

        return newFile;
    }

    /** Handles an upload using the EXTERNAL method. */
    public static org.geoserver.platform.resource.Resource handleEXTERNALUpload(
            HttpServletRequest request) throws IOException {
        // get the URL for this file to upload
        final String stringURL;
        File inputFile;
        try (InputStream inStream = request.getInputStream()) {
            stringURL = IOUtils.getStringFromStream(inStream);
            inputFile = new File(stringURL);
            if (!inputFile.exists()) {
                URL fileURL = new URL(stringURL);
                inputFile = IOUtils.URLToFile(fileURL);
            }
        }

        if (inputFile == null || !inputFile.exists()) {
            throw new RestException(
                    "Failed to locate the input file " + stringURL, HttpStatus.BAD_REQUEST);
        } else if (!inputFile.canRead()) {
            throw new RestException(
                    "Input file is not readable, check filesystem permissions: " + stringURL,
                    HttpStatus.BAD_REQUEST);
        }

        return Files.asResource(inputFile);
    }

    static Set<String> ZIP_MIME_TYPES = new HashSet();

    static {
        ZIP_MIME_TYPES.add("application/zip");
        ZIP_MIME_TYPES.add("multipart/x-zip");
        ZIP_MIME_TYPES.add("application/x-zip-compressed");
    }

    /** Determines if the specified request contains a zip stream. */
    public static boolean isZipMediaType(HttpServletRequest request) {
        return ZIP_MIME_TYPES.contains(request.getContentType());
    }

    /** Determines if the specified media type represents a zip stream. */
    public static boolean isZipMediaType(MediaType mediaType) {
        return ZIP_MIME_TYPES.contains(mediaType.toString());
    }

    /**
     * Unzips a zip a file to a specified directory, deleting the zip file after unpacking.
     *
     * @param zipFile The zip file.
     * @param outputDirectory The directory to unpack the contents to.
     * @throws IOException Any I/O errors that occur.
     *     <p>TODO: move this to IOUtils
     */
    public static void unzipFile(
            org.geoserver.platform.resource.Resource zipFile,
            org.geoserver.platform.resource.Resource outputDirectory)
            throws IOException {
        unzipFile(zipFile, outputDirectory, null, null, null, null, false);
    }

    /**
     * Unzips a zip a file to a specified directory, deleting the zip file after unpacking.
     *
     * @param zipFile The zip file.
     * @param outputDirectory The directory to unpack the contents to.
     * @throws IOException Any I/O errors that occur.
     *     <p>TODO: move this to IOUtils
     */
    public static void unzipFile(
            org.geoserver.platform.resource.Resource zipFile,
            org.geoserver.platform.resource.Resource outputDirectory,
            String workspace,
            String store,
            HttpServletRequest request,
            List<org.geoserver.platform.resource.Resource> files,
            boolean external)
            throws IOException {

        if (outputDirectory == null) {
            outputDirectory = zipFile.parent();
        }
        ZipFile archive = new ZipFile(zipFile.file());

        IOUtils.inflate(archive, outputDirectory, null, workspace, store, files, external, true);
        zipFile.delete();
    }

    /**
     * Fetch a request attribute as a String, accounting for URL-encoding.
     *
     * @param request the Restlet Request object that might contain the attribute
     * @param name the name of the attribute to retrieve
     * @return the attribute, URL-decoded, if it exists and is a valid URL-encoded string, or null
     *     otherwise
     */
    public static String getAttribute(HttpServletRequest request, String name) {
        Object o = request.getAttribute(name);
        return decode(o);
    }

    public static String getQueryStringValue(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
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

    /** Method for searching an item inside the MetadataMap. */
    public static String getItem(
            String workspaceName, String storeName, Catalog catalog, String key) {
        // Initialization of a null String containing the root directory to use for the input store
        // config
        String item;

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

    /** This method is used for extracting the metadata map from the selected store */
    public static MetadataMap loadMapfromStore(String storeName, Catalog catalog) {
        StoreInfo storeInfo = catalog.getStoreByName(storeName, CoverageStoreInfo.class);
        if (storeInfo == null) {
            storeInfo = catalog.getStoreByName(storeName, DataStoreInfo.class);
        }
        // If the Store is present, then the associated MetadataMap is selected
        if (storeInfo != null) {
            return storeInfo.getMetadata();
        }
        return null;
    }

    /** This method is used for extracting the metadata map from the selected workspace */
    public static MetadataMap loadMapfromWorkSpace(String workspaceName, Catalog catalog) {
        WorkspaceInfo wsInfo = catalog.getWorkspaceByName(workspaceName);
        // If the WorkSpace is present, then the associated MetadataMap is selected
        if (wsInfo != null) {
            GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
            SettingsInfo info = gs.getSettings(wsInfo);
            return info != null ? info.getMetadata() : null;
        }
        return null;
    }

    /** This method is used for extracting the metadata map from the global settings */
    public static MetadataMap loadMapFromGlobal() {
        GeoServerInfo gsInfo = GeoServerExtensions.bean(GeoServer.class).getGlobal();
        // Global info should be always not null
        if (gsInfo != null) {
            SettingsInfo info = gsInfo.getSettings();
            return info != null ? info.getMetadata() : null;
        }
        return null;
    }

    /** Extraction of the item from the metadata map */
    public static String extractMapItem(MetadataMap map, String key) {
        if (map != null && !map.isEmpty()) {
            String item = map.get(key, String.class);

            if (item != null && !item.isEmpty()) {

                return item;
            }
        }
        return null;
    }

    public static String getRootDirectory(String workspaceName, String storeName, Catalog catalog) {
        String rootDir = getItem(workspaceName, storeName, catalog, ROOT_KEY);
        if (rootDir != null) {
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

    public static void remapping(
            String workspace,
            String store,
            StringBuilder itemPath,
            String initialFileName,
            Map<String, String> storeParams)
            throws IOException {
        // Selection of the available PathMapper
        List<RESTUploadPathMapper> mappers =
                GeoServerExtensions.extensions(RESTUploadPathMapper.class);
        // Mapping the item path
        for (RESTUploadPathMapper mapper : mappers) {
            mapper.mapItemPath(workspace, store, storeParams, itemPath, initialFileName);
        }
    }

    /** Unzips a InputStream to a directory */
    public static void unzipInputStream(InputStream in, File outputDirectory) throws IOException {
        org.geoserver.util.IOUtils.decompress(in, outputDirectory);
    }
}
