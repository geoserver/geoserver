/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * A mapper specifically thought for formats that do have a corresponding JAI image reader
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ImgMimeTypeMapper implements CoverageMimeTypeMapper {

    static final Logger LOGGER = Logging.getLogger(ImgMimeTypeMapper.class);

    @Override
    public String getMimeType(CoverageInfo cInfo) throws IOException {
        // no mapping let's go with the ImageIO reader code
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        final File sourceFile =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                cInfo.getStore().getURL()),
                        true);
        if (sourceFile == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Original source is null");
            }
            return null;
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Original source: " + sourceFile.getAbsolutePath());
            }
        }

        ImageInputStream inStream = null;
        ImageReader reader = null;
        try {
            inStream = ImageIO.createImageInputStream(sourceFile);
            if (inStream == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inStream);
            if (readers.hasNext()) {
                reader = readers.next();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Found reader for format: " + reader.getFormatName());
                }
                String mime = reader.getOriginatingProvider().getMIMETypes()[0];
                // the native format rules says "the range set values can be obtained unaltered",
                // so we cannot allow lossy compressions (which would alter the range set values)
                String lcMime = mime.toLowerCase();
                if (lcMime.contains("jpeg") || lcMime.contains("mrsid") || lcMime.contains("ecw")) {
                    return null;
                }
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Unable to map mime type for coverage: " + cInfo.toString());
            }
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }

            try {
                if (reader != null) {
                    reader.dispose();
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
        return null;
    }
}
