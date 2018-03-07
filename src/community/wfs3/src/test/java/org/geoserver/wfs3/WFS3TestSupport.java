package org.geoserver.wfs3;

import org.geoserver.test.GeoServerSystemTestSupport;

import javax.servlet.Filter;
import java.util.Collections;
import java.util.List;

public class WFS3TestSupport extends GeoServerSystemTestSupport {

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList(new WFS3Filter());
    }
}
