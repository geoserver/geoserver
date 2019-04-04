/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleType;

/** A loadable model for the registered style list that does sort the styles too */
@SuppressWarnings("serial")
public class StyleTypeModel extends LoadableDetachableModel<List<StyleType>> {

    @Override
    protected List<StyleType> load() {
        List<StyleType> styleTypes = Arrays.asList(StyleType.values());
        Collections.sort(styleTypes);
        return styleTypes;
    }
}
