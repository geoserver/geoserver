/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.helloweb;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.web.GeoServerBasePage;

public class HelloPage extends GeoServerBasePage {

    public HelloPage() {
        add( new Label( "label", "Hello World!" ) );
    }
}
