/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.StringResourceModel;

public class ToolPage extends GeoServerSecuredPage {
    @SuppressWarnings("serial")
    public ToolPage() {
        List links = getGeoServerApplication().getBeansOfType(ToolLinkInfo.class);
        for (ToolLinkExternalInfo link :
                getGeoServerApplication().getBeansOfType(ToolLinkExternalInfo.class)) {
            if (link.getDescriptionKey() == null) {
                continue;
            }
            links.add(link);
        }
        links = filterByAuth(links);

        add(
                new ListView("toolList", links) {
                    public void populateItem(ListItem item) {
                        final ComponentInfo info = (ComponentInfo) item.getModelObject();

                        AbstractLink link = null;
                        if (info instanceof ToolLinkInfo) {
                            final ToolLinkInfo tool = (ToolLinkInfo) info;
                            link = new BookmarkablePageLink("theLink", tool.getComponentClass());
                        } else {
                            final ToolLinkExternalInfo tool = (ToolLinkExternalInfo) info;
                            link = new ExternalLink("theLink", tool.getHref());
                        }

                        link.add(
                                new Label(
                                        "theTitle",
                                        new StringResourceModel(
                                                info.getTitleKey(), (Component) null, null)));
                        item.add(link);
                        item.add(
                                new Label(
                                        "theDescription",
                                        new StringResourceModel(
                                                info.getDescriptionKey(), (Component) null, null)));
                    }
                });
    }
}
