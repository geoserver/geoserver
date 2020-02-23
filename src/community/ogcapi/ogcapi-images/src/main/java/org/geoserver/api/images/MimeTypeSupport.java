/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import java.net.URLConnection;
import org.apache.commons.io.FilenameUtils;

/**
 * Basic support for guessing mime types and files extensions (we should try to do better than
 * this...)
 */
class MimeTypeSupport {

    /**
     * Performs an educated guess on the file mimeType based on the file name (fast lookup, not very
     * precise)
     */
    public static String guessMimeType(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        if (extension.equals("tif") || extension.equals(".tiff")) {
            return "image/tiff";
        } else if (extension.equals("jp2")) {
            return "image/jp2";
        } else if (extension.equals("nc")) {
            return "application/x-netcdf"; // don't really know if it's a NetCDF4...
        } else {
            String mimeType = URLConnection.guessContentTypeFromName(fileName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            return mimeType;
        }
    }

    /** Tries to guess a flie */
    public static String guessFileExtension(String mimeType) {
        if (mimeType != null) {
            if (mimeType.startsWith("image")) {
                if (mimeType.endsWith("tiff")) {
                    return "tif";
                } else {
                    int idx = Math.max(mimeType.lastIndexOf("/"), mimeType.lastIndexOf("+"));
                    if (idx > 0 && idx < mimeType.length() - 1) {
                        return mimeType.substring(idx + 1);
                    }
                }
            } else if (mimeType.contains("netcdf")) {
                return "nc";
            }
        }

        return "bin";
    }
}
