/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.wicket.model;

import org.apache.wicket.model.PropertyModel;

/**
 * Extension of property model. Additional support for: - read-only properties (particularly for
 * disabled fields, otherwise a null is sent despite content)
 *
 * @author Niels Charlier
 * @param <T>
 */
public class ExtPropertyModel<T> extends PropertyModel<T> {

    private static final long serialVersionUID = 8377548798715670872L;

    private boolean readOnly;

    public ExtPropertyModel(Object modelObject, String expression) {
        super(modelObject, expression);
    }

    @Override
    public void setObject(T object) {
        if (!readOnly) {
            super.setObject(object);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public ExtPropertyModel<T> setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }
}
