/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

public abstract class AbstractStoreUploadController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(AbstractStoreUploadController.class);

    /** The ways a file upload can be achieved */
    protected enum UploadMethod {
        file(true),
        external(false),
        url(true);

        boolean inline;

        UploadMethod(boolean inline) {
            this.inline = inline;
        }

        public boolean isInline() {
            return inline;
        }
    }

    public AbstractStoreUploadController(Catalog catalog) {
        super(catalog);
    }

    /** */
    protected List<Resource> handleFileUpload(
            String store,
            String workspace,
            String filename,
            UploadMethod method,
            String format,
            Resource directory,
            HttpServletRequest request) {

        List<Resource> files = new ArrayList<>();

        Resource uploadedFile;
        boolean external = false;
        try {
            if (method == UploadMethod.file) {
                // we want to delete the previous dir contents only in case of PUT, not
                // in case of POST (harvest, available only for raster data)
                boolean cleanPreviousContents = HttpMethod.PUT.name().equals(request.getMethod());
                if (filename == null) {
                    filename = buildUploadedFilename(store, format);
                }
                uploadedFile =
                        RESTUtils.handleBinUpload(
                                filename, directory, cleanPreviousContents, request, workspace);
            } else if (method == UploadMethod.url) {
                uploadedFile =
                        RESTUtils.handleURLUpload(
                                buildUploadedFilename(store, format),
                                workspace,
                                directory,
                                request);
            } else if (method == UploadMethod.external) {
                uploadedFile = RESTUtils.handleEXTERNALUpload(request);
                external = true;
            } else {
                throw new RestException(
                        "Unrecognized file upload method: " + method, HttpStatus.BAD_REQUEST);
            }
        } catch (Throwable t) {
            if (t instanceof RestException) {
                throw (RestException) t;
            } else {
                throw new RestException(
                        "Error while storing uploaded file:", HttpStatus.INTERNAL_SERVER_ERROR, t);
            }
        }

        // handle the case that the uploaded file was a zip file, if so unzip it
        if (RESTUtils.isZipMediaType(request)) {
            // rename to .zip if need be
            if (!uploadedFile.name().endsWith(".zip")) {
                Resource newUploadedFile =
                        uploadedFile
                                .parent()
                                .get(FilenameUtils.getBaseName(uploadedFile.path()) + ".zip");
                String oldFileName = uploadedFile.name();
                if (!uploadedFile.renameTo(newUploadedFile)) {
                    String errorMessage =
                            "Error renaming zip file from "
                                    + oldFileName
                                    + " -> "
                                    + newUploadedFile.name();
                    throw new RestException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                uploadedFile = newUploadedFile;
            }
            // unzip the file
            try {
                // Unzipping of the file and, if it is a POST request, filling of the File List
                RESTUtils.unzipFile(
                        uploadedFile, directory, workspace, store, request, files, external);

                // look for the "primary" file
                // TODO: do a better check
                Resource primaryFile = findPrimaryFile(directory, format);
                if (primaryFile != null) {
                    uploadedFile = primaryFile;
                } else {
                    throw new RestException(
                            "Could not find appropriate " + format + " file in archive",
                            HttpStatus.BAD_REQUEST);
                }
            } catch (RestException e) {
                throw e;
            } catch (Exception e) {
                throw new RestException(
                        "Error occured unzipping file", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
        // If the File List is empty then the uploaded file must be added
        if (files.isEmpty() && uploadedFile != null) {
            files.clear();
            files.add(uploadedFile);
        } else {
            files.add(0, uploadedFile);
        }

        return files;
    }

    /** Build name for an uploaded file. */
    private String buildUploadedFilename(String store, String format) {
        if ("h2".equalsIgnoreCase(format)) {
            return store + ".data.db";
        } else {
            return store + "." + format;
        }
    }

    /** */
    protected Resource findPrimaryFile(Resource directory, String format) {
        for (Resource f :
                Resources.list(
                        directory, new Resources.ExtensionFilter(format.toUpperCase()), true)) {
            // assume the first
            return f;
        }
        return null;
    }
}
