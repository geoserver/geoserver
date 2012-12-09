/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.istyle;

import org.geoserver.web.GeoServerBasePage;

public class IStylePage extends GeoServerBasePage {

    public IStylePage() {
        add(new IStylePanel("styler", null));
    }

}
