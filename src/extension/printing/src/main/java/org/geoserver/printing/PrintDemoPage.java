/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.geoserver.web.GeoServerBasePage;

public class PrintDemoPage extends GeoServerBasePage implements IHeaderContributor {
    private static final long serialVersionUID = 2904825847695306563L;

    public void renderHead(IHeaderResponse response) {
        response.render(
                CssHeaderItem.forUrl(
                        "http://extjs.cachefly.net/ext-2.2.1/resources/css/ext-all.css", null));
        response.render(
                CssHeaderItem.forUrl(
                        "http://extjs.cachefly.net/ext-2.2.1/examples/shared/examples.css", null));
        response.render(
                JavaScriptHeaderItem.forUrl("http://extjs.cachefly.net/builds/ext-cdn-771.js"));
        response.render(JavaScriptHeaderItem.forUrl("http://openlayers.org/api/2.8/OpenLayers.js"));
        response.render(
                JavaScriptHeaderItem.forReference(
                        new JavaScriptResourceReference(PrintDemoPage.class, "GeoExt.js")));
        response.render(
                JavaScriptHeaderItem.forReference(
                        new JavaScriptResourceReference(PrintDemoPage.class, "GeoExtPrinting.js")));
        response.render(
                JavaScriptHeaderItem.forReference(
                        new JavaScriptResourceReference(PrintDemoPage.class, "Printing.js")));
    }
}
