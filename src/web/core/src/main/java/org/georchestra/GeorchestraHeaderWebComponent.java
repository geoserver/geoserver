package org.georchestra;

import java.util.logging.Logger;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

public class GeorchestraHeaderWebComponent extends WebComponent {

    private String headerConfigFile;
    private String georchestraStylesheet;
    private String headerHeight;

    private static Logger LOGGER = Logging.getLogger(GeorchestraHeaderWebComponent.class);

    private void init() {
        headerHeight = getGeoServerApplication().getBean("georchestraHeaderHeight").toString();
        headerConfigFile =
                getGeoServerApplication().getBean("georchestraHeaderConfigFile").toString();
        georchestraStylesheet =
                getGeoServerApplication().getBean("georchestraStylesheet").toString();
    }

    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }

    public GeorchestraHeaderWebComponent(String id) {
        super(id);
        init();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.put("active-app", "geoserver");
        tag.put("config-file", this.headerConfigFile);
        tag.put("stylesheet", this.georchestraStylesheet);
        super.onComponentTag(tag);
    }
}
