/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Model for collection of available style formats.
 */
public class StyleFormatsModel extends LoadableDetachableModel {
    @Override
    protected Object load() {
        List<StyleHandler> handlers = GeoServerApplication.get().getBeansOfType(StyleHandler.class);
        return Lists.transform(handlers, new Function<StyleHandler,String>() {
            @Nullable
            @Override
            public String apply(@Nullable StyleHandler styleHandler) {
                return styleHandler.getFormat();
            }
        });
    }
}
