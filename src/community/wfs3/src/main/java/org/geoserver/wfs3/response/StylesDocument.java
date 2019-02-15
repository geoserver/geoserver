/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import java.util.List;

public class StylesDocument {

    List<StyleDocument> styles;

    public StylesDocument(List<StyleDocument> styles) {
        this.styles = styles;
    }

    public List<StyleDocument> getStyles() {
        return styles;
    }

    public void setStyles(List<StyleDocument> styles) {
        this.styles = styles;
    }
}
