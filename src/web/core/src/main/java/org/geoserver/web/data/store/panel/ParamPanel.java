/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.form.FormComponent;

public interface ParamPanel {
    /** Returns the wrapped form component, if there is a single one, or null otherwise */
    public abstract FormComponent getFormComponent();
}
