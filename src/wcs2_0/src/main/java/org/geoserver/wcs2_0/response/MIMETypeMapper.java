/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Simple mapping utility to map native formats to Mime Types using ImageIO reader capabilities.
 *
 * <p>It does perform caching of the mappings. Tha cache should be very small, hence it uses hard
 * references.
 *
 * @author Simone Giannechini, GeoSolutions
 */
public class MIMETypeMapper implements ApplicationContextAware {

    private static final String NO_MIME_TYPE = "NoMimeType";

    public static final String DEFAULT_FORMAT =
            GeoTIFFCoverageResponseDelegate.GEOTIFF_CONTENT_TYPE;

    private Logger LOGGER = Logging.getLogger(MIMETypeMapper.class);

    private final SoftValueHashMap<String, String> mimeTypeCache =
            new SoftValueHashMap<String, String>(100);

    private final Set<String> outputMimeTypes = new HashSet<String>();

    private List<CoverageMimeTypeMapper> mappers;

    /** Constructor. */
    private MIMETypeMapper(CoverageResponseDelegateFinder finder, Catalog catalog) {
        // collect all of the output mime types
        for (String of : finder.getOutputFormats()) {
            CoverageResponseDelegate delegate = finder.encoderFor(of);
            String mime = delegate.getMimeType(of);
            outputMimeTypes.add(mime);
        }
        catalog.addListener(new MimeTypeCacheClearingListener());
    }

    /**
     * Returns a mime types for the provided {@link CoverageInfo} using the {@link
     * CoverageInfo#getNativeFormat()} as its key. In case none was found, the DEFAULT_FORMAT format
     * is returned.
     *
     * @param cInfo the {@link CoverageInfo} to find a mime type for
     * @return a mime types or null for the provided {@link CoverageInfo} using the {@link
     *     CoverageInfo#getNativeFormat()} as its key.
     * @throws IOException in case we don't manage to open the underlying file
     */
    public String mapNativeFormat(final CoverageInfo cInfo) throws IOException {
        // checks
        Utilities.ensureNonNull("cInfo", cInfo);

        String mime = mimeTypeCache.get(cInfo.getId());
        if (mime != null) {
            if (NO_MIME_TYPE.equals(mime)) {
                return DEFAULT_FORMAT;
            } else {
                return mime;
            }
        }

        for (CoverageMimeTypeMapper mapper : mappers) {
            mime = mapper.getMimeType(cInfo);
            if (mime != null) {
                break;
            }
        }

        // the native format must be encodable
        if (mime != null && outputMimeTypes.contains(mime)) {
            mimeTypeCache.put(cInfo.getId(), mime);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Added mapping for mime: " + mime);
            }
            return mime;
        } else {
            // we either don't have a clue about the mime, or we don't have an encoder,
            // save the response as null
            mimeTypeCache.put(cInfo.getId(), NO_MIME_TYPE);
            return DEFAULT_FORMAT;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        mappers = GeoServerExtensions.extensions(CoverageMimeTypeMapper.class, applicationContext);
    }

    /**
     * Cleans the mime type cache contents on reload
     *
     * @author Andrea Aime - GeoSolutions
     */
    public class MimeTypeCacheClearingListener extends CatalogVisitorAdapter
            implements CatalogListener {

        public void handleAddEvent(CatalogAddEvent event) {}

        public void handleModifyEvent(CatalogModifyEvent event) {}

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            event.getSource().accept(this);
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) {
            event.getSource().accept(this);
        }

        public void reloaded() {
            outputMimeTypes.clear();
        }

        @Override
        public void visit(CoverageInfo coverage) {
            outputMimeTypes.remove(coverage.getId());
        }
    }
}
