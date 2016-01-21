/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
//import org.apache.wicket.markup.html.resources.JavascriptPackageResourceReference;

import org.geoserver.web.GeoServerBasePage;

public class PrintDemoPage extends GeoServerBasePage implements IHeaderContributor {
        private static final long serialVersionUID = 2904825847695306563L;

        public void renderHead(IHeaderResponse response) {
	    response.render(CssHeaderItem.forCSS("http://extjs.cachefly.net/ext-2.2.1/resources/css/ext-all.css", null));
	    response.render(CssHeaderItem.forCSS("http://extjs.cachefly.net/ext-2.2.1/examples/shared/examples.css", null));
	    response.render(OnLoadHeaderItem.forScript("http://extjs.cachefly.net/builds/ext-cdn-771.js"));
	    response.render(OnLoadHeaderItem.forScript("http://openlayers.org/api/2.8/OpenLayers.js"));
	    response.render(OnLoadHeaderItem.forScript(PrintDemoPage.class.getResource("GeoExt.js").toString()));
	    response.render(OnLoadHeaderItem.forScript(PrintDemoPage.class.getResource("GeoExtPrinting.js").toString()));
	    response.render(OnLoadHeaderItem.forScript(PrintDemoPage.class.getResource("GeoExtPrinting.js").toString()));
	  }
}
