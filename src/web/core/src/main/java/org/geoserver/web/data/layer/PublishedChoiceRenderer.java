/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.PublishedInfo;

/** Simple choice renderer for {@link PublishedInfo} */
public class PublishedChoiceRenderer extends ChoiceRenderer<PublishedInfo> {

    private static final long serialVersionUID = -5617204445401506143L;

    @Override
    public Object getDisplayValue(PublishedInfo layer) {
        return layer.getName();
    }

    @Override
    public String getIdValue(PublishedInfo layer, int index) {
        return layer.prefixedName();
    }
}
