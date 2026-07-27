/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Provides feedback on layer enabled, visible status when home page displaying layer. */
public class PublishedInfoHomePageContentProvider implements GeoServerHomePageContentProvider {
    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return isAdmin && layerInfo != null && (!layerInfo.isAdvertised() || !layerInfo.isEnabled());
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new LayerFeedbackPanel(id);
    }

    static class LayerFeedbackPanel extends Panel {
        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(PublishedInfoHomePageContentProvider.LayerFeedbackPanel.class);

        public LayerFeedbackPanel(String id) {
            super(id);
            add(feedbackList("layerFeedback"));
        }

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), toResourceName(getClass(), "css"))));
            }
        }

        ListView<String> feedbackList(String id) {

            LoadableDetachableModel<List<String>> feedbackModel = new LoadableDetachableModel<>() {
                @Override
                protected List<String> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) LayerFeedbackPanel.this.getPage();

                    List<String> feedbackList = new ArrayList<>();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();
                    if (layerInfo != null) {
                        if (!layerInfo.isAdvertised()) {
                            feedbackList.add("feedback.unlisted");
                        }
                        if (!layerInfo.isEnabled()) {
                            feedbackList.add("feedback.disabled");
                        }
                    }
                    // only show this panel if there is content to share
                    LayerFeedbackPanel.this.setVisible(!feedbackList.isEmpty());
                    return feedbackList;
                }
            };
            return new ListView<>(id, feedbackModel) {
                @Override
                protected void populateItem(ListItem<String> item) {
                    String feedbackKey = item.getModelObject();

                    GeoServerHomePage homePage = (GeoServerHomePage) LayerFeedbackPanel.this.getPage();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();
                    StringResourceModel message = new StringResourceModel(feedbackKey, LayerFeedbackPanel.this)
                            .setParameters(layerInfo.getName());

                    item.add(new Label("message", message));
                }
            };
        }
    }
}
