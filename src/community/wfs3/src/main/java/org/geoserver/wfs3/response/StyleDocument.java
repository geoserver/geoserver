/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import java.util.Objects;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wfs3.NCNameResourceCodec;

public class StyleDocument extends AbstractDocument {

    String id;

    public StyleDocument(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static StyleDocument build(StyleInfo style) {
        if (style.getWorkspace() == null
                || Objects.equals(LocalWorkspace.get(), style.getWorkspace())) {
            return new StyleDocument(style.getName());
        } else {
            return new StyleDocument(
                    NCNameResourceCodec.encode(style.getWorkspace().getName(), style.getName()));
        }
    }
}
