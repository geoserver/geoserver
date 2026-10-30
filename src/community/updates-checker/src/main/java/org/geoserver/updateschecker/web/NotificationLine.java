/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.updateschecker.Notification;

/**
 * One notification, as the banner and the Status section show it: the severity label, the text, the details link and
 * the action links. Styled with GeoServer's {@code feedbackPanel*} classes, which follow the light and dark themes.
 */
class NotificationLine extends Panel {

    private static final long serialVersionUID = 1L;

    private static final PackageResourceReference SCRIPT =
            new PackageResourceReference(NotificationLine.class, "notifications.js");

    private static final PackageResourceReference STYLES =
            new PackageResourceReference(NotificationLine.class, "notifications.css");

    /**
     * Fills a list item with the line, and gives the item what {@code notifications.js} reads: the storage key of a
     * dismissal, a second key a dismissal also sets, and whether it lasts for the browser session only.
     */
    static void populate(ListItem<Notification> item) {
        Notification n = item.getModelObject();
        item.add(AttributeModifier.append("class", "gs-updates-checker-item " + cssClass(n)));
        item.add(AttributeModifier.replace("data-type", n.getType().name()));
        item.add(AttributeModifier.replace("data-key", n.getDismissKey()));
        item.add(AttributeModifier.replace("data-also", n.getAlsoDismissKey()));
        if (n.getType().isSessionOnly()) {
            item.add(AttributeModifier.replace("data-session-only", "true"));
        }
        item.add(new NotificationLine("line", n));
    }

    static void renderAssets(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(STYLES));
        response.render(JavaScriptHeaderItem.forReference(SCRIPT));
    }

    private static String cssClass(Notification n) {
        switch (n.getSeverity()) {
            case CRITICAL:
            case HIGH:
                return "feedbackPanelERROR";
            case WARN:
                return "feedbackPanelWARNING";
            default:
                return "feedbackPanelINFO";
        }
    }

    private NotificationLine(String id, Notification n) {
        super(id);
        setRenderBodyOnly(true);
        add(new Label("label", n.getSeverity().label(getLocale())));
        add(new Label("message", n.getMessageHtml(getLocale())).setEscapeModelStrings(false));

        WebMarkupContainer details = new WebMarkupContainer("details");
        details.setVisible(n.getLink() != null);
        add(details);
        details.add(new ExternalLink("link", n.getLink() == null ? "#" : n.getLink()));

        add(new ListView<>("action", n.getActions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Notification.Action> item) {
                Notification.Action action = item.getModelObject();
                WebMarkupContainer link = new WebMarkupContainer("actionLink");
                link.add(AttributeModifier.replace("data-action", action.getId()));
                link.add(AttributeModifier.replace(
                        "data-snooze", action.getSnoozeMillis() > 0 ? String.valueOf(action.getSnoozeMillis()) : null));
                item.add(link);
                String text = getString(action.getMessageKey());
                if (n.getTargetVersion() != null) {
                    text = text.replace("{0}", n.getTargetVersion());
                }
                link.add(new Label("actionText", text));
            }
        });
    }
}
