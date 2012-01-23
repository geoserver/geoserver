package org.geoserver.web.data.store.panel;

import org.apache.wicket.markup.html.form.FormComponent;

public interface ParamPanel {
    /**
     * Returns the wrapped form component, if there is a single one, or null otherwise
     * 
     * @return
     */
    abstract public FormComponent getFormComponent();
}
