/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Localizer;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.util.logging.Logging;

/** Builds {@link PreviewLink}s from catalog metadata and data links for the home page preview panel. */
public final class PreviewCatalogLinkSupport {

    public static final String METADATA_LINKS = "PreviewHomePageContentProvider.metadataLinks";
    public static final String DATA_LINKS = "PreviewHomePageContentProvider.dataLinks";
    public static final String COMMON_FORMATS = "commonFormats";
    public static final String MAP_FORMATS = "mapFormats";
    public static final String VECTOR_FORMATS = "vectorFormats";
    public static final String TILED_FORMATS = "tiledFormats";

    private static final Logger LOGGER = Logging.getLogger(PreviewCatalogLinkSupport.class);

    private PreviewCatalogLinkSupport() {}

    /** Metadata links configured on the published layer or layer group. */
    public static List<PreviewLink> metadataLinks(PublishedInfo published) {
        published = resolvePublished(published);
        if (published == null) {
            return List.of();
        }
        List<MetadataLinkInfo> sources = new ArrayList<>();
        if (published instanceof LayerInfo layerInfo) {
            ResourceInfo resource = resource(layerInfo);
            if (resource != null) {
                sources.addAll(resource.getMetadataLinks());
            }
        } else if (published instanceof LayerGroupInfo groupInfo) {
            sources.addAll(groupInfo.getMetadataLinks());
        }
        List<PreviewLink> links = new ArrayList<>();
        for (MetadataLinkInfo info : sources) {
            PreviewLink link = toMetadataPreviewLink(info);
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    /** Data links configured on the published layer resource. */
    public static List<PreviewLink> dataLinks(PublishedInfo published) {
        published = resolvePublished(published);
        if (!(published instanceof LayerInfo layerInfo)) {
            return List.of();
        }
        ResourceInfo resource = resource(layerInfo);
        if (resource == null) {
            return List.of();
        }
        List<PreviewLink> links = new ArrayList<>();
        for (DataLinkInfo info : resource.getDataLinks()) {
            PreviewLink link = toDataPreviewLink(info);
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    /** Reload layer or layer group from the catalog so resource links are populated. */
    static PublishedInfo resolvePublished(PublishedInfo published) {
        if (published == null) {
            return null;
        }
        Catalog catalog = GeoServerApplication.get().getCatalog();
        if (published instanceof LayerInfo layer) {
            LayerInfo resolved = catalog.getLayerByName(layer.prefixedName());
            return resolved != null ? resolved : layer;
        }
        if (published instanceof LayerGroupInfo group) {
            LayerGroupInfo resolved;
            if (group.getWorkspace() != null) {
                resolved = catalog.getLayerGroupByName(group.getWorkspace(), group.getName());
            } else {
                resolved = catalog.getLayerGroupByName(group.getName());
            }
            return resolved != null ? resolved : group;
        }
        return published;
    }

    private static ResourceInfo resource(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        if (resource == null) {
            LOGGER.fine(() -> "Layer " + layer.prefixedName() + " has no resource for catalog links");
            return null;
        }
        Catalog catalog = GeoServerApplication.get().getCatalog();
        if (resource.getId() != null) {
            ResourceInfo resolved = catalog.getResource(resource.getId(), ResourceInfo.class);
            if (resolved != null) {
                return resolved;
            }
        }
        return resource;
    }

    private static PreviewLink toMetadataPreviewLink(MetadataLinkInfo info) {
        String url = info.getContent();
        if (Strings.isEmpty(url)) {
            return null;
        }
        String type = info.getMetadataType();
        String about = info.getAbout();
        String format = info.getType();

        String label = about;
        if (Strings.isEmpty(label)) {
            label = format;
        }
        if (Strings.isEmpty(label)) {
            label = url;
        }
        if (!Strings.isEmpty(type) && !"other".equalsIgnoreCase(type)) {
            label += " (" + type + ")";
        }
        String title = Strings.isEmpty(format) ? label : format;
        return new PreviewLink(label, url, title, PreviewLink.METADATA);
    }

    private static PreviewLink toDataPreviewLink(DataLinkInfo info) {
        String url = info.getContent();
        if (Strings.isEmpty(url)) {
            return null;
        }
        String type = info.getType();
        String label = translateFormat(type);
        if (Strings.isEmpty(label)) {
            label = url;
        }
        return new PreviewLink(label, url, type, PreviewLink.DATA);
    }

    private static String translateFormat(String format) {
        if (Strings.isEmpty(format)) {
            return format;
        }
        try {
            Localizer localizer =
                    GeoServerApplication.get().getResourceSettings().getLocalizer();
            return localizer.getString("format." + format, null, format);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.getMessage());
            return format;
        }
    }
}
