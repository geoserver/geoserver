/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.updateschecker.UpdatesCheckerNotification;

/**
 * The updates-checker banner: the active notifications, most important first, each starting with a text label so
 * severity never depends on colour alone. Only the first is visible; the others are hidden and reachable through an "N
 * more notices" link to the settings page.
 *
 * <p>Dismissal is browser-side: {@code UpdatesCheckerDismissals.js} hides what the browser's storage says was dismissed
 * or snoozed, and writes the entry when an action link is clicked.
 */
public class UpdatesCheckerWarningPanel extends Panel {

    private final WebMarkupContainer root;

    /** @param notifications at least one notification, most important first */
    public UpdatesCheckerWarningPanel(String id, List<UpdatesCheckerNotification> notifications, String username) {
        super(id);

        root = new WebMarkupContainer("root");
        root.setOutputMarkupId(true);
        root.add(AttributeModifier.replace("data-user", username));
        add(root);

        final List<UpdatesCheckerNotification> shown = new ArrayList<>(notifications);
        root.add(new ListView<>("notification", shown) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<UpdatesCheckerNotification> item) {
                UpdatesCheckerNotificationItems.populate(item, UpdatesCheckerWarningPanel.this);
                if (item.getIndex() > 0) {
                    item.add(AttributeModifier.replace("hidden", "hidden"));
                }
            }
        });

        WebMarkupContainer more = new WebMarkupContainer("more");
        more.setVisible(shown.size() > 1);
        root.add(more);
        BookmarkablePageLink<Void> moreLink = new BookmarkablePageLink<>("moreLink", UpdatesCheckerPage.class);
        more.add(moreLink);
        moreLink.add(new Label("moreCount", String.valueOf(shown.size() - 1)));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        UpdatesCheckerNotificationItems.renderAssets(response);
        response.render(OnDomReadyHeaderItem.forScript(
                "GsUpdatesChecker.init(document.getElementById('" + root.getMarkupId() + "'));"));
    }
}
