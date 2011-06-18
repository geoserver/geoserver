package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

/**
 * A choice renderer in which each displayed value is also the id
 */
@SuppressWarnings("serial")
public class SimpleChoiceRenderer implements IChoiceRenderer { 

    public Object getDisplayValue(Object object) {
        return object;
    }

    public String getIdValue(Object object, int index) {
        return object.toString();
    }

}
