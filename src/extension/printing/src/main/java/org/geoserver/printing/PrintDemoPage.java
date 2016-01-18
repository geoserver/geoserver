/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptPackageResourceReference;

import org.geoserver.web.GeoServerBasePage;

public class PrintDemoPage extends GeoServerBasePage implements IHeaderContributor {
  public void renderHead(IHeaderResponse response) {
    response.renderCSSReference(
        "http://extjs.cachefly.net/ext-2.2.1/resources/css/ext-all.css"
    );
    response.renderCSSReference(
        "http://extjs.cachefly.net/ext-2.2.1/examples/shared/examples.css"
    );
    response.renderJavascriptReference("http://extjs.cachefly.net/builds/ext-cdn-771.js");
    response.renderJavascriptReference("http://openlayers.org/api/2.8/OpenLayers.js");
    
    response.renderJavascriptReference(
    	new JavascriptPackageResourceReference(PrintDemoPage.class, "GeoExt.js")
    );
    response.renderJavascriptReference(
        new JavascriptPackageResourceReference(PrintDemoPage.class, "GeoExtPrinting.js")
    );
    response.renderJavascriptReference(
        new JavascriptPackageResourceReference(PrintDemoPage.class, "Printing.js")
    );
  }
}
