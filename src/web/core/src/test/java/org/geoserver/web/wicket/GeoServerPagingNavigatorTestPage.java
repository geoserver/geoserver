/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Arrays;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;

public class GeoServerPagingNavigatorTestPage extends WebPage {
    public GeoServerPagingNavigatorTestPage() {
        PageableListView list =
                new PageableListView(
                        "list",
                        Arrays.asList(
                                new String[] {
                                    "aardvark",
                                    "bluebird",
                                    "crocodile",
                                    "dromedary camel",
                                    "elephant",
                                    "firefox",
                                    "gorilla",
                                    "hippo",
                                    "ibex",
                                    "jay"
                                }),
                        2) {
                    protected void populateItem(ListItem item) {
                        item.add(new Label("label", item.getModel()));
                    }
                };

        add(list);
        add(new GeoServerPagingNavigator("pager", list));
    }
}
