package org.geoserver.web.istyle;

import org.geoserver.web.GeoServerBasePage;

public class IStylePage extends GeoServerBasePage {

    public IStylePage() {
        add(new IStylePanel("styler", null));
    }

}
