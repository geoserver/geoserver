/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.web;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SitemapPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public SitemapPanel(String id) {
        super(id);
        IModel<String> href = new Model<String>("../geosearch/sitemap.xml");
        ExternalLink link = new ExternalLink("sitemapLink", href);
        add(link);
    }

}
