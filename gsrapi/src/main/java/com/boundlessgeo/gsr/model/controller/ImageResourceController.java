/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 *
 * @author tkunicki
 */
public class ImageResourceController extends AbstractController {

    public static final String PROPERTY_IMAGE_RESOURCE_DIR = "GSR_IMAGE_RESOURCE_DIR";

    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String HTTP_HEADER_ETAG = "ETag";
    private static final String HTTP_HEADER_CACHE_CONTROL = "Cache-Control";

    private static final Map<String, String> defaultMimeTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        defaultMimeTypes.put(".gif", "image/gif");
        defaultMimeTypes.put(".jpeg", "image/jpeg");
        defaultMimeTypes.put(".jpg", "image/jpeg");
        defaultMimeTypes.put(".png", "image/png");
    }

    private final File imageBaseDirectory;

    public ImageResourceController(GeoServer geoserver) {
        this.imageBaseDirectory = findImageResourceDirectory(geoserver);
    }

    @Override
    public ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {

        final String path = request.getRequestURI();

        int index = path.lastIndexOf('/');
        String fileName = index < 0 ? path : path.substring(index + 1);

        dispatchImageResource(fileName, request, response);

        return null;
    }

    public boolean dispatchImageResource(final String fileName, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final boolean debug = logger.isDebugEnabled();
        if (debug) {
            logger.debug("Attemping to dispatch image controller: " + fileName);
        }

        boolean resolved = false;
        File imageFile = new File(imageBaseDirectory, fileName);
        if (imageFile.canRead()) {
            try {
                commitResponse(imageFile, response);
                resolved = true;
            } catch (IOException e) {
                logger.info(e.getMessage());
                if (debug) {
                    logger.debug("Error dispatching image controller response", e);
                }
            }
        }
        return resolved;
    }

    public void commitResponse(File imageFile, HttpServletResponse response)
            throws IOException {
        writeHeaders(imageFile, response);
        writeImageData(imageFile, response);

    }

    protected void writeHeaders(File imageFile, HttpServletResponse response) {

        // determine mimetype
        String imagePath = imageFile.getPath();
        String mimetype = getServletContext().getMimeType(imagePath);
        if (mimetype == null) {
            final int extIndex = imagePath.lastIndexOf('.');
            if (extIndex != -1) {
                String extension = imagePath.substring(extIndex);
                mimetype = defaultMimeTypes.get(extension.toLowerCase());
            }
        }

        long length = imageFile.length();
        long lastModified = imageFile.lastModified();

        response.setContentType(mimetype);
        response.setHeader(HTTP_HEADER_CONTENT_LENGTH, Long.toString(length));
        if (lastModified != 0) {
            response.setHeader(HTTP_HEADER_ETAG, '"' + Long.toString(lastModified) + '"');
            response.setDateHeader(HTTP_HEADER_LAST_MODIFIED, lastModified);
        }
        if (!response.containsHeader(HTTP_HEADER_CACHE_CONTROL)) {
            response.setHeader(HTTP_HEADER_CACHE_CONTROL, "max-age=86400");
        }
    }

    protected void writeImageData(File imageFile, HttpServletResponse response) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(imageFile);
            os = response.getOutputStream();
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    private File findImageResourceDirectory(GeoServer geoserver) {
        File candidate;
        String propertyPath = System.getProperty(PROPERTY_IMAGE_RESOURCE_DIR);
        if (propertyPath != null) {
            candidate = new File(propertyPath);
            if (candidate.isDirectory()) {
                logger.info("Using " + propertyPath + " for GeoServices REST API image controller directory");
                return candidate;
            } else {
                logger.warn("Property " + PROPERTY_IMAGE_RESOURCE_DIR + " is set to " + propertyPath +
                        " but it does not appear to be a directory");
            }
        }
        candidate = new File(geoserver.getCatalog().getResourceLoader().getBaseDirectory(), "images");
        logger.info("Using default location of  " + candidate.getPath() + " for GeoServices REST API image controller directory");
        return candidate;
    }
}
