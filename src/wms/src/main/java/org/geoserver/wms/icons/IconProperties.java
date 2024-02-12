/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.style.Style;
import org.geotools.util.URLs;

/**
 * Stores the values of dynamic style properties needed to generate an icon for a particular
 * feature.
 *
 * @author David Winslow, OpenGeo
 * @author Kevin Smith, OpenGeo
 */
public abstract class IconProperties {
    private IconProperties() {}

    public abstract Double getOpacity();

    public abstract Double getScale();

    public abstract Double getHeading();

    public abstract String href(String baseURL, String workspace, String styleName);

    public abstract Style inject(Style base);

    public abstract Map<String, String> getProperties();

    public abstract String getIconName(Style style);

    public abstract boolean isExternal();

    public static IconProperties generator(
            final Double opacity,
            final Double scale,
            final Double heading,
            final Map<String, String> styleProperties) {
        return new IconProperties() {
            @Override
            public Double getOpacity() {
                return opacity;
            }

            @Override
            public Double getScale() {
                return scale;
            }

            @Override
            public Double getHeading() {
                return heading;
            }

            @Override
            public boolean isExternal() {
                return false;
            }

            @Override
            public String href(String baseURL, String workspace, String styleName) {
                String stylePathFragment;
                if (workspace != null) {
                    stylePathFragment =
                            ResponseUtils.urlEncode(workspace)
                                    + "/"
                                    + ResponseUtils.urlEncode(styleName);
                } else {
                    stylePathFragment = ResponseUtils.urlEncode(styleName);
                }
                return ResponseUtils.buildURL(
                        baseURL,
                        "kml/icon/" + stylePathFragment,
                        styleProperties,
                        URLType.RESOURCE);
            }

            @Override
            public Style inject(Style base) {
                return IconPropertyInjector.injectProperties(base, styleProperties);
            }

            @Override
            public Map<String, String> getProperties() {
                return styleProperties;
            }

            @Override
            public String getIconName(Style style) {
                try {
                    final MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(style.getName().getBytes(StandardCharsets.UTF_8));
                    for (Map.Entry<String, String> property : styleProperties.entrySet()) {
                        digest.update(property.getKey().getBytes(StandardCharsets.UTF_8));
                        digest.update(property.getValue().getBytes(StandardCharsets.UTF_8));
                    }
                    final byte[] hash = digest.digest();
                    final StringBuilder builder = new StringBuilder();
                    for (byte b : hash) {
                        builder.append(String.format("%02x", b));
                    }
                    return builder.toString();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static IconProperties externalReference(
            final Double opacity, final Double scale, final Double heading, final String url) {
        return new IconProperties() {
            @Override
            public Double getOpacity() {
                return opacity;
            }

            @Override
            public Double getScale() {
                return scale;
            }

            @Override
            public Double getHeading() {
                return heading;
            }

            @Override
            public String href(String baseURL, String workspace, String styleName) {

                try {
                    URL target = new URL(url);
                    String graphicProtocol = target.getProtocol();

                    if ("file".equals(graphicProtocol)) {
                        File file = URLs.urlToFile(target);

                        if (file.isAbsolute()) {
                            GeoServerDataDirectory dataDir =
                                    (GeoServerDataDirectory)
                                            GeoServerExtensions.bean("dataDirectory");
                            // we grab the canonical path to make sure we can compare them, no
                            // relative parts in them and so on
                            File canonicalFile = file.getCanonicalFile();

                            SimpleEntry<WorkspaceInfo, File> stylesDirEntry =
                                    findStylesDir(canonicalFile, dataDir);
                            if (stylesDirEntry == null) {
                                // we don't know how to handle this then...
                                return null;
                            }
                            return rebuildIconHref(
                                    baseURL,
                                    canonicalFile,
                                    stylesDirEntry.getKey(),
                                    stylesDirEntry.getValue());
                        }
                        return ResponseUtils.buildURL(
                                baseURL,
                                "styles/" + target.getPath(),
                                Collections.emptyMap(),
                                URLType.RESOURCE);
                    } else if (!("http".equals(graphicProtocol)
                            || "https".equals(graphicProtocol))) {
                        return null;
                    }

                    // return ResponseUtils.buildURL(baseURL, "rest/render/kml/icon/" + styleName,
                    // styleProperties, URLType.RESOURCE);
                    return url;
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }

            private String rebuildIconHref(
                    String baseURL, File canonicalFile, WorkspaceInfo ws, File stylesDir) {
                String styles = "styles/" + (ws != null ? ws.getName() + "/" : "");
                String path = styles + stylesDir.toURI().relativize(canonicalFile.toURI());
                return ResponseUtils.buildURL(baseURL, path, null, URLType.RESOURCE);
            }

            private SimpleEntry<WorkspaceInfo, File> findStylesDir(
                    File canonicalFile, GeoServerDataDirectory dataDir) throws IOException {
                // try main dir
                File stylesDir = dataDir.getStyles().dir().getCanonicalFile();
                if (isPartOfStylesDir(canonicalFile, stylesDir)) {
                    return new SimpleEntry<>(null, stylesDir);
                }
                // try workspaces
                List<WorkspaceInfo> workspaces =
                        ((Catalog) GeoServerExtensions.bean("catalog")).getWorkspaces();
                for (WorkspaceInfo ws : workspaces) {
                    stylesDir = dataDir.getStyles(ws).dir().getCanonicalFile();
                    if (isPartOfStylesDir(canonicalFile, stylesDir)) {
                        return new SimpleEntry<>(ws, stylesDir);
                    }
                }
                return null;
            }

            private boolean isPartOfStylesDir(File canonicalFile, File stylesDir) {
                return canonicalFile.getAbsolutePath().startsWith(stylesDir.getAbsolutePath());
            }

            @Override
            public boolean isExternal() {
                return true;
            }

            @Override
            public Style inject(Style base) {
                return IconPropertyInjector.injectProperties(base, Collections.emptyMap());
            }

            @Override
            public Map<String, String> getProperties() {
                return Collections.emptyMap();
            }

            @Override
            public String getIconName(Style style) {
                throw new RuntimeException("An implementation is missing");
            }
        };
    }
}
