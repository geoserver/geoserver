package org.geoserver.web.wicket;

import java.util.Arrays;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;

public class GeoServerPagingNavigatorTestPage extends WebPage {
    public GeoServerPagingNavigatorTestPage() {
        PageableListView list = new PageableListView("list", Arrays.asList(new String[]{
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
        }), 2){
            protected void populateItem(ListItem item){
                item.add(new Label("label", item.getModel()));
            }
        };

        add(list);
        add(new GeoServerPagingNavigator("pager", list));
    }
}
