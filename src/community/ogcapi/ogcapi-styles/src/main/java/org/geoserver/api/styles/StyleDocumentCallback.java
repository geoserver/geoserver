/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractCollectionDocument;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.DocumentCallback;
import org.geoserver.api.Link;
import org.geoserver.api.StyleDocument;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.util.Version;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class StyleDocumentCallback implements DocumentCallback {

    Catalog catalog;

    public StyleDocumentCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void apply(Request dr, AbstractDocument document) {
        if (document instanceof AbstractCollectionDocument) {
            AbstractCollectionDocument collection = (AbstractCollectionDocument) document;
            Object subject = collection.getSubject();
            if (subject instanceof ResourceInfo) {
                addStyles(collection);
            } else if (!collection.getStyles().isEmpty()) {
                addLinks(collection.getStyles());
            }
        } else if (document instanceof StyleDocument) {
            addStyleLinks((StyleDocument) document);
        }
    }

    private void addStyles(AbstractCollectionDocument collection) {
        ResourceInfo ri = (ResourceInfo) collection.getSubject();
        for (LayerInfo layer : catalog.getLayers(ri)) {
            Set<StyleInfo> styles = new LinkedHashSet<>();
            styles.add(layer.getDefaultStyle());
            styles.addAll(layer.getStyles());

            for (StyleInfo style : styles) {
                StyleDocument styleDocument = new StyleDocument(style);
                addStyleLinks(styleDocument);
                collection.getStyles().add(styleDocument);
            }
        }
    }

    private void addLinks(List<StyleDocument> styleDocuments) {
        APIRequestInfo info = APIRequestInfo.get();
        for (StyleDocument styleDocument : styleDocuments) {
            if (styleDocument.getStyle() != null) {
                addStyleLinks(styleDocument);
            }
        }
    }

    static void addStyleLinks(StyleDocument styleDocument) {
        APIRequestInfo info = APIRequestInfo.get();
        StyleInfo style = styleDocument.getStyle();
        // adding the links to various formats
        String styleIdPathElement = ResponseUtils.urlEncode(styleDocument.getId());
        for (StyleHandler sh : Styles.handlers()) {
            // different versions have different mime types, create one link for each
            for (Version ver : sh.getVersions()) {
                // can we encode the style in this format?
                if ((style.getFormat() != null
                                && sh.getFormat().equals(style.getFormat())
                                && (ver.equals(style.getFormatVersion())
                                        || style.getFormatVersion() == null))
                        || sh.supportsEncoding(ver)) {
                    String styleURL =
                            buildURL(
                                    info.getBaseURL(),
                                    "ogc/styles/styles/" + styleIdPathElement,
                                    Collections.singletonMap("f", sh.mimeType(ver)),
                                    URLMangler.URLType.SERVICE);

                    Link link = new Link(styleURL, "stylesheet", sh.mimeType(ver), null);
                    styleDocument.addLink(link);
                }
            }
        }

        // adding the metadata link
        Collection<MediaType> metadataFormats =
                APIRequestInfo.get().getProducibleMediaTypes(StyleMetadataDocument.class, true);
        for (MediaType format : metadataFormats) {
            String metadataURL =
                    buildURL(
                            info.getBaseURL(),
                            "ogc/styles/styles/" + styleIdPathElement + "/metadata",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            Link link =
                    new Link(metadataURL, "describedBy", format.toString(), "The style metadata");
            styleDocument.addLink(link);
        }
    }
}
