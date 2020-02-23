/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.CloseableIterator;
import org.geotools.data.FileGroupProvider.FileGroup;
import org.geotools.data.FileResourceInfo;
import org.geotools.data.ResourceInfo;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;

/** Class delegated to setup direct download links for a {@link CatalogInfo} instance. */
public class DownloadLinkHandler {

    private static Set<String> STANDARD_DOMAINS;
    public static final String RESOURCE_ID_PARAMETER = "resourceId";
    public static final String FILE_PARAMETER = "file";
    public static final String FILE_TEMPLATE = "${" + FILE_PARAMETER + "}";

    static final Logger LOGGER = Logging.getLogger(DownloadLinkHandler.class);

    static {
        STANDARD_DOMAINS = new HashSet<String>();
        STANDARD_DOMAINS.add(Utils.TIME_DOMAIN);
        STANDARD_DOMAINS.add(Utils.ELEVATION_DOMAIN);
        STANDARD_DOMAINS.add(Utils.BBOX);
    }

    /** An implementation of {@link CloseableIterator} for links creation */
    static class CloseableLinksIterator<T> implements CloseableIterator<String> {

        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

        private SimpleDateFormat dateFormat = null;

        public CloseableLinksIterator(String baseLink, CloseableIterator<FileGroup> dataIterator) {
            this.dataIterator = dataIterator;
            this.baseLink = baseLink;
        }

        private String baseLink;

        /** The underlying iterator providing files */
        private CloseableIterator<FileGroup> dataIterator;

        @Override
        public boolean hasNext() {
            return dataIterator.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove operation isn't supported");
        }

        @Override
        public String next() {
            // Get the file from the underlying iterator
            FileGroup element = dataIterator.next();
            File mainFile = element.getMainFile();
            String canonicalPath = null;
            try {
                canonicalPath = mainFile.getCanonicalPath();

                // Hash the file and setup the download link
                String hashFile = hashFile(mainFile);
                StringBuilder builder =
                        new StringBuilder(baseLink.replace(FILE_TEMPLATE, hashFile));
                Map<String, Object> metadata = element.getMetadata();
                if (metadata != null && !metadata.isEmpty()) {

                    Set<String> keys = metadata.keySet();

                    // Set bbox in the link
                    if (keys.contains(Utils.BBOX)) {
                        Object bbox = metadata.get(Utils.BBOX);
                        appendBBOXToLink((ReferencedEnvelope) bbox, builder);
                    }
                    // Set time and elevation as first domain elements in the link
                    if (keys.contains(Utils.TIME_DOMAIN)) {
                        Object time = metadata.get(Utils.TIME_DOMAIN);
                        appendRangeToLink(Utils.TIME_DOMAIN, time, builder);
                    }
                    if (keys.contains(Utils.ELEVATION_DOMAIN)) {
                        Object elevation = metadata.get(Utils.ELEVATION_DOMAIN);
                        appendRangeToLink(Utils.ELEVATION_DOMAIN, elevation, builder);
                    }
                    for (String key : keys) {
                        if (!STANDARD_DOMAINS.contains(key)) {
                            Object additional = metadata.get(key);
                            appendRangeToLink(key, additional, builder);
                        }
                    }
                }
                return builder.toString();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to encode the specified file:" + canonicalPath, e.getCause());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(
                        "Unable to encode the specified file:" + canonicalPath, e.getCause());
            }
        }

        /** Append the BBOX parameter to the directDownload link */
        private void appendBBOXToLink(ReferencedEnvelope envelope, StringBuilder builder) {
            if (envelope == null) {
                throw new IllegalArgumentException("Envelope can't be null");
            }
            builder.append("&")
                    .append(Utils.BBOX)
                    .append("=")
                    .append(envelope.getMinX())
                    .append(",")
                    .append(envelope.getMinY())
                    .append(",")
                    .append(envelope.getMaxX())
                    .append(",")
                    .append(envelope.getMaxY());
        }

        /**
         * Append a coverage domain (time, elevation, custom) to the direct download link.
         *
         * @param key the name of the parameter domain to be added
         * @param domain the value of the domain
         * @param builder the builder currently used for Link construction
         */
        private void appendRangeToLink(String key, Object domain, StringBuilder builder) {
            builder.append("&").append(key).append("=");
            if (domain instanceof DateRange) {
                // instantiate a new DateFormat instead of using a static one since
                // it's not thread safe
                if (dateFormat == null) {
                    dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                DateRange dateRange = (DateRange) domain;
                builder.append(dateFormat.format(dateRange.getMinValue()))
                        .append("/")
                        .append(dateFormat.format(dateRange.getMaxValue()));
            } else if (domain instanceof NumberRange) {
                NumberRange numberRange = (NumberRange) domain;
                builder.append(numberRange.getMinValue())
                        .append("/")
                        .append(numberRange.getMaxValue());
            } else if (domain instanceof Range) {
                // Generic range
                Range range = (Range) domain;
                builder.append(range.getMinValue()).append("/").append(range.getMaxValue());
            } else {
                throw new IllegalArgumentException("Domain " + domain + " isn't supported");
            }
        }

        @Override
        public void close() throws IOException {
            dataIterator.close();
        }
    }

    /** Template download link to be updated with actual values */
    protected static String LINK =
            "ows?service=CSW&version=${version}&request="
                    + "DirectDownload&"
                    + RESOURCE_ID_PARAMETER
                    + "=${nameSpace}:${layerName}&"
                    + FILE_PARAMETER
                    + "="
                    + FILE_TEMPLATE;

    /** Generate download links for the specified info object. */
    public CloseableIterator<String> generateDownloadLinks(CatalogInfo info) {
        Request request = Dispatcher.REQUEST.get();
        String baseURL = null;

        // Retrieve the baseURL (something like: http://host:port/geoserver/...)
        try {
            if (baseURL == null) {
                baseURL = ResponseUtils.baseURL(request.getHttpRequest());
            }

            baseURL = ResponseUtils.buildURL(baseURL, "/", null, URLType.SERVICE);
        } catch (Exception e) {
        }
        if (baseURL == null) {
            throw new IllegalArgumentException("baseURL is required to create download links");
        }
        baseURL += LINK;
        baseURL = baseURL.replace("${version}", request.getVersion());

        if (info instanceof CoverageInfo) {
            return linksFromCoverage(baseURL, (CoverageInfo) info);
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Download link for vectors isn't supported." + " Returning null");
            }
        }
        return null;
    }

    /**
     * Return an {@link Iterator} containing {@link String}s representing the downloadLinks
     * associated to the provided {@link CoverageInfo} object.
     */
    protected CloseableIterator<String> linksFromCoverage(
            String baseURL, CoverageInfo coverageInfo) {
        GridCoverage2DReader reader;
        try {
            reader =
                    (GridCoverage2DReader)
                            coverageInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
            String name = DirectDownload.extractName(coverageInfo);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader available for the specified coverage: " + name);
            }
            ResourceInfo resourceInfo = reader.getInfo(name);
            if (resourceInfo instanceof FileResourceInfo) {
                FileResourceInfo fileResourceInfo = (FileResourceInfo) resourceInfo;

                // Replace the template URL with proper values
                String baseLink =
                        baseURL.replace("${nameSpace}", coverageInfo.getNamespace().getName())
                                .replace("${layerName}", coverageInfo.getName());

                CloseableIterator<org.geotools.data.FileGroupProvider.FileGroup> dataIterator =
                        fileResourceInfo.getFiles(null);
                return new CloseableLinksIterator(baseLink, dataIterator);

            } else {
                throw new RuntimeException(
                        "Donwload links handler need to provide "
                                + "download links to files. The ResourceInfo associated with the store should be a FileResourceInfo instance");
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to generate download links", e.getCause());
        }
    }

    /**
     * Return a SHA-1 based hash for the specified file, by appending the file's base name to the
     * hashed full path. This allows to hide the underlying file system structure.
     */
    public static String hashFile(File mainFile) throws IOException, NoSuchAlgorithmException {
        String canonicalPath = mainFile.getCanonicalPath();
        String mainFilePath = FilenameUtils.getPath(canonicalPath);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(mainFilePath.getBytes());
        return Hex.encodeHexString(md.digest()) + "-" + mainFile.getName();
    }

    /**
     * Given a file download link, extract the link with no file references, used to request the
     * full layer download.
     */
    public String extractFullDownloadLink(String link) {
        int resourceIdIndex = link.indexOf(RESOURCE_ID_PARAMETER);
        int nextParamIndex = link.indexOf("&" + FILE_PARAMETER, resourceIdIndex);
        return link.substring(0, nextParamIndex);
    }
}
