package org.geoserver.helloweb;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.web.GeoServerBasePage;

public class HelloPage extends GeoServerBasePage {

    public HelloPage() {
        add( new Label( "label", "Hello World!" ) );
    }
}
