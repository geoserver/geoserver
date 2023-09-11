/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Iterator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;

/** Contains the list of styles for the "/styles" endpoint */
@JsonPropertyOrder({"styles", "links"})
public class StylesDocument extends AbstractDocument {
    private final Catalog catalog;

    public StylesDocument(Catalog catalog) {
        this.catalog = catalog;

        addSelfLinks("ogc/styles/v1/styles");
    }

    @SuppressWarnings("PMD.CloseResource") // hopefully closed as it gets iterated
    public Iterator<StyleDocument> getStyles() {
        // full scan (we might add paging/filtering later)
        CloseableIterator<StyleInfo> styles = catalog.list(StyleInfo.class, Filter.INCLUDE);
        return new Iterator<StyleDocument>() {

            StyleDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                boolean hasNext = styles.hasNext();
                if (!hasNext) {
                    styles.close();
                    return false;
                } else {
                    try {
                        StyleInfo style = styles.next();
                        StyleDocument styleDocument = new StyleDocument(style);
                        StyleDocumentCallback.addStyleLinks(styleDocument);

                        next = styleDocument;
                        return true;
                    } catch (Exception e) {
                        styles.close();
                        throw new ServiceException(
                                "Failed to iterate over the feature types in the catalog", e);
                    }
                }
            }

            @Override
            public StyleDocument next() {
                StyleDocument result = next;
                this.next = null;
                return result;
            }
        };
    }
}
