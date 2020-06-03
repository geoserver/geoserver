/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.treeview;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

public class ClassAppender extends AttributeAppender {

    private static final long serialVersionUID = 4717967910213677953L;

    public ClassAppender(IModel<?> appendModel) {
        super("class", appendModel, " ");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClassAppender
                && ((ClassAppender) other).getReplaceModel().equals(getReplaceModel());
    }

    @Override
    public int hashCode() {
        return this.getReplaceModel().hashCode();
    }
}
