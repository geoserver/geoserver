/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

/** A loadable model for the registered style list that does sort the styles too */
@SuppressWarnings("serial")
public class StylesModel extends LoadableDetachableModel<List<StyleInfo>> {

    @Override
    protected List<StyleInfo> load() {
        List<StyleInfo> styles =
                new ArrayList<StyleInfo>(GeoServerApplication.get().getCatalog().getStyles());
        Collections.sort(styles, new StyleNameComparator());
        return styles;
    }
}
