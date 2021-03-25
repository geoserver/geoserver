/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;

public class GeoServerPagingNavigatorTestPage extends WebPage {
    public GeoServerPagingNavigatorTestPage() {
        List<String> animals =
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
                        });
        PageableListView<String> list =
                new PageableListView<String>("list", animals, 2) {
                    @Override
                    protected void populateItem(ListItem item) {
                        item.add(new Label("label", item.getModel()));
                    }
                };

        add(list);
        add(new GeoServerPagingNavigator("pager", list));
    }
}
