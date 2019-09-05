/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import java.io.Serializable;
import org.apache.wicket.model.IModel;

public class ComplexMetadataAttributeModel<T extends Serializable> implements IModel<T> {
    private static final long serialVersionUID = 2943279172304236560L;

    private ComplexMetadataAttribute<T> att;

    public ComplexMetadataAttributeModel(ComplexMetadataAttribute<T> att) {
        this.att = att;
    }

    @Override
    public T getObject() {
        return att.getValue();
    }

    @Override
    public void setObject(T object) {
        att.setValue(object);
    }

    @Override
    public void detach() {}
}
