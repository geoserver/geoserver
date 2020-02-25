/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.StringResourceModel;

public class DemoPage extends GeoServerBasePage {
    @SuppressWarnings("serial")
    public DemoPage() {
        List<DemoLinkInfo> links = getGeoServerApplication().getBeansOfType(DemoLinkInfo.class);
        add(
                new ListView("demoList", links) {
                    public void populateItem(ListItem item) {
                        final DemoLinkInfo info = (DemoLinkInfo) item.getModelObject();
                        item.add(
                                new BookmarkablePageLink("theLink", info.getComponentClass())
                                        .add(
                                                new Label(
                                                        "theTitle",
                                                        new StringResourceModel(
                                                                info.getTitleKey(),
                                                                (Component) null,
                                                                null))));
                        item.add(
                                new Label(
                                        "theDescription",
                                        new StringResourceModel(
                                                info.getDescriptionKey(), (Component) null, null)));
                    }
                });
    }
}
