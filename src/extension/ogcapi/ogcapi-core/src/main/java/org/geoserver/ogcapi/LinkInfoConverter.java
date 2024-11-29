/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.LinkInfo.LINKS_METADATA_KEY;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.SettingsInfo;

/** Helper class to convert {@link LinkInfo} to {@link Link} and add them to a document */
public class LinkInfoConverter {

    /**
     * Adds links found under the {@link LinkInfo#LINKS_METADATA_KEY} metadata key to the document,
     * eventually filtering them by service class
     */
    public static void addLinksToDocument(
            AbstractDocument document, PublishedInfo publishedInfo, Class serviceClass) {
        if (publishedInfo instanceof LayerInfo) {
            addLinksToDocument(document, ((LayerInfo) publishedInfo).getResource(), serviceClass);
        }
    }

    /**
     * Adds links found under the {@link LinkInfo#LINKS_METADATA_KEY} metadata key to the document,
     * eventually filtering them by service class
     */
    @SuppressWarnings("unchecked")
    public static void addLinksToDocument(
            AbstractDocument document, LayerGroupInfo group, Class serviceClass) {
        List<LinkInfo> links = group.getMetadata().get(LINKS_METADATA_KEY, List.class);
        addLinksToDocument(document, serviceClass, links);
    }

    /**
     * Adds links found under the {@link LinkInfo#LINKS_METADATA_KEY} metadata key to the document,
     * eventually filtering them by service class
     */
    @SuppressWarnings("unchecked")
    public static void addLinksToDocument(
            AbstractDocument document, ResourceInfo resource, Class serviceClass) {
        List<LinkInfo> links = resource.getMetadata().get(LINKS_METADATA_KEY, List.class);
        addLinksToDocument(document, serviceClass, links);
    }

    @SuppressWarnings("unchecked")
    public static void addLinksToDocument(
            AbstractDocument document, SettingsInfo settings, Class<?> serviceClass) {
        List<LinkInfo> links = settings.getMetadata().get(LINKS_METADATA_KEY, List.class);
        addLinksToDocument(document, serviceClass, links);
    }

    private static void addLinksToDocument(
            AbstractDocument document, Class serviceClass, List<LinkInfo> links) {
        if (links != null) {
            APIService annotation = APIDispatcher.getApiServiceAnnotation(serviceClass);
            String service = Optional.ofNullable(annotation).map(s -> s.service()).orElse(null);
            links.stream()
                    .filter(l -> serviceMatch(l, service))
                    .forEach(l -> addLinkToDocument(l, document));
        }
    }

    private static void addLinkToDocument(LinkInfo l, AbstractDocument document) {
        document.addLink(toLink(l));
    }

    /** Converts a LinkInfo to a Link */
    public static Link toLink(LinkInfo l) {
        return new Link(l.getHref(), l.getRel(), l.getType(), l.getTitle());
    }

    private static boolean serviceMatch(LinkInfo l, String service) {
        return l.getService() == null || Objects.equals(l.getService(), service);
    }
}
