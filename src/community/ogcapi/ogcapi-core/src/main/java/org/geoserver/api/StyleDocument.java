/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.Description;
import org.geotools.util.logging.Logging;

/**
 * A document describing a style. Typically just id and title, if the style service is embeeded a
 * callback will add links to the formats and metadata
 */
@JsonPropertyOrder({"id", "title", "links"})
public class StyleDocument extends AbstractDocument {

    static final Logger LOGGER = Logging.getLogger(StyleDocument.class);

    String title;
    StyleInfo style;

    public StyleDocument(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public StyleDocument(StyleInfo style) {
        this.style = style;
        this.id = style.prefixedName();
        try {
            this.title =
                    Optional.ofNullable(style.getStyle().getDescription())
                            .map(Description::getTitle)
                            .map(Object::toString)
                            .orElse(null);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING, "Could not get description from style, setting it to null", e);
            this.title = null;
        }
    }

    public String getTitle() {
        return title;
    }

    @JsonIgnore
    public StyleInfo getStyle() {
        return style;
    }
}
