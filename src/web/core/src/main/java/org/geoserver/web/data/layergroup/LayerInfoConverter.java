/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Locale;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;

public class LayerInfoConverter implements IConverter<LayerInfo> {

    private static final long serialVersionUID = -3540868744266790608L;

    @Override
    public LayerInfo convertToObject(String name, Locale locale) {
        return GeoServerApplication.get().getCatalog().getLayerByName(name);
    }

    @Override
    public String convertToString(LayerInfo obj, Locale locale) {
        if (obj == null) return "";
        else return ((LayerInfo) obj).prefixedName();
    }
}
