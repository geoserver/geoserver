/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.form.ChoiceRenderer;

/** A choice renderer in which each displayed value is also the id */
@SuppressWarnings("serial")
public class SimpleChoiceRenderer<T> extends ChoiceRenderer<T> {

    public Object getDisplayValue(T object) {
        return object;
    }

    public String getIdValue(T object, int index) {
        return object.toString();
    }
}
