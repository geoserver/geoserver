/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
