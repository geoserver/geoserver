package org.georchestra;

import java.util.logging.Logger;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.util.tester.DummyHomePage;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

public class GeorchestraHeaderIframe extends InlineFrame {

    private String headerUrl;
    private String headerHeight;

    private static Logger LOGGER = Logging.getLogger(GeorchestraHeaderIframe.class);

    private void init() {
        headerHeight = getGeoServerApplication().getBean("georchestraHeaderHeight").toString();
        headerUrl = getGeoServerApplication().getBean("georchestraHeaderUrl").toString();
    }

    protected GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }

    public GeorchestraHeaderIframe(String id) {
        super(id, new DummyHomePage());
        init();
    }

    @Override
    protected CharSequence getURL() {
        return this.headerUrl + "?active=geoserver";
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.put(
                "style",
                "width:100%;height:" + this.headerHeight + "px;border:none;overflow:hidden;");
        super.onComponentTag(tag);
    }
}
