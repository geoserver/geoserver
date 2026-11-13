/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.updateschecker.Notification;
import org.geoserver.updateschecker.Status;
import org.geoserver.updateschecker.UpdatesCheckerService;

/**
 * The welcome page banner: the first notification that this browser has not dismissed, and a link to the settings page
 * for the others. The first view of a session starts empty and is filled once the check is done.
 */
public class BannerPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private final WebMarkupContainer root = new WebMarkupContainer("root");

    public BannerPanel(String id) {
        super(id);
        setOutputMarkupId(true);
        root.setOutputMarkupId(true);
        add(root);
        if (FirstViewBehavior.isChecked()) {
            fill(GeoServerExtensions.bean(UpdatesCheckerService.class).getStatus());
        } else {
            fill(null);
            add(new FirstViewBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onStatus(AjaxRequestTarget target, Status status) {
                    fill(status);
                    target.add(BannerPanel.this);
                }
            });
        }
    }

    private void fill(Status status) {
        List<Notification> notifications = status == null ? List.of() : status.getNotifications();
        root.setVisible(!notifications.isEmpty());
        root.addOrReplace(new ListView<>("notification", notifications) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Notification> item) {
                NotificationLine.populate(item);
                // notifications.js picks the one to show; hiding the rest now avoids a flash of all of them
                if (item.getIndex() > 0) {
                    item.add(AttributeModifier.replace("hidden", "hidden"));
                }
            }
        });
        // notifications.js shows the right count, or hides the link
        BookmarkablePageLink<Void> more = new BookmarkablePageLink<>("more", UpdatesCheckerPage.class);
        more.add(new Label("moreCount", String.valueOf(Math.max(notifications.size() - 1, 0))));
        root.addOrReplace(more);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        NotificationLine.renderAssets(response);
        response.render(OnDomReadyHeaderItem.forScript(
                "GsUpdatesChecker.init(document.getElementById('" + root.getMarkupId() + "'));"));
    }
}
