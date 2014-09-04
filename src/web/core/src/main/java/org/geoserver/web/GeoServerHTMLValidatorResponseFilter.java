/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.util.string.AppendingStringBuffer;
import org.wicketstuff.htmlvalidator.HtmlValidationResponseFilter;

/**
 * Extends the standard XHTML validator from WicketStuff to make it toggable by the GUI
 * @author Andrea Aime
 */
public class GeoServerHTMLValidatorResponseFilter extends HtmlValidationResponseFilter {
    boolean enabled = false; 

    @Override
    public AppendingStringBuffer filter(AppendingStringBuffer responseBuffer) {
        if(enabled) {
            return super.filter(responseBuffer);
        } else {
            return responseBuffer;
        }
    }
    
}
