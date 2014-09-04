/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.namespace;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.NamespaceInfo;

/**
 * Simple choice renderer for {@link NamespaceInfo}
 * 
 * @author Gabriel Roldan
 */
public class NamespaceChoiceRenderer implements IChoiceRenderer {

    private static final long serialVersionUID = 1L;

    /**
     * @see IChoiceRenderer#getDisplayValue(Object)
     */
    public Object getDisplayValue(Object object) {
        NamespaceInfo nsInfo = (NamespaceInfo) object;
        String displayValue = nsInfo.getPrefix() + ": <" + nsInfo.getURI() + ">";
        return displayValue;
    }

    /**
     * @see IChoiceRenderer#getIdValue(Object, int)
     */
    public String getIdValue(Object object, int index) {
        return ((NamespaceInfo) object).getId();
    }

}
