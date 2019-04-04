/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.web.GeoServerApplication;

/** Model for collection of available style formats. */
public class StyleFormatsModel extends LoadableDetachableModel<List<String>> {

    private static final long serialVersionUID = -5591450369784953326L;

    @Override
    protected List<String> load() {
        List<StyleHandler> handlers = GeoServerApplication.get().getBeansOfType(StyleHandler.class);
        return Lists.transform(
                handlers,
                new Function<StyleHandler, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable StyleHandler styleHandler) {
                        return styleHandler.getFormat();
                    }
                });
    }
}
