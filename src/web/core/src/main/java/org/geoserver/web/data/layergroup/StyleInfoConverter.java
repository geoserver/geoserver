/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Locale;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

public class StyleInfoConverter implements IConverter<StyleInfo> {

    private static final long serialVersionUID = -1984255970892520909L;

    @Override
    public StyleInfo convertToObject(String name, Locale locale) {
        return GeoServerApplication.get().getCatalog().getStyleByName(name);
    }

    @Override
    public String convertToString(StyleInfo obj, Locale locale) {
        if (obj == null) return "";
        else return ((StyleInfo) obj).getName();
    }
}
