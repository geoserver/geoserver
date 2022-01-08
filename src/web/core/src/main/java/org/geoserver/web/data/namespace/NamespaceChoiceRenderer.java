/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.namespace;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.NamespaceInfo;

/**
 * Simple choice renderer for {@link NamespaceInfo}
 *
 * @author Gabriel Roldan
 */
public class NamespaceChoiceRenderer extends ChoiceRenderer<NamespaceInfo> {

    private static final long serialVersionUID = 1L;

    @Override
    /** @see ChoiceRenderer#getDisplayValue(Object) */
    public Object getDisplayValue(NamespaceInfo nsInfo) {
        String displayValue = nsInfo.getPrefix() + ": <" + nsInfo.getURI() + ">";
        return displayValue;
    }

    /** @see ChoiceRenderer#getIdValue(Object, int) */
    @Override
    public String getIdValue(NamespaceInfo object, int index) {
        return object.getId();
    }
}
