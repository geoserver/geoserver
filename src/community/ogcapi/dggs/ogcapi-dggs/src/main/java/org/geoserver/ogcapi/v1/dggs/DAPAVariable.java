/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.geoserver.ogcapi.AbstractDocument;
import org.opengis.feature.type.AttributeDescriptor;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DAPAVariable extends AbstractDocument {

    String id;
    String title;
    String description;
    String uom;

    public DAPAVariable() {}

    public DAPAVariable(AttributeDescriptor ad) {
        this.id = ad.getLocalName();
        this.description = "Field of type " + ad.getType().getBinding().getSimpleName();
    }

    public DAPAVariable(String id, String title, String uom) {
        this.id = id;
        this.title = title;
        this.uom = uom;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
