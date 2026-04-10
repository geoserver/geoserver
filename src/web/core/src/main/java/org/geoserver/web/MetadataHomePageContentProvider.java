/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;

/** Contributes metadata links section to GeoServer Home page. */
public class MetadataHomePageContentProvider implements GeoServerHomePageContentProvider {

    protected static final Logger LOGGER = Logging.getLogger(GeoServerHomePage.class);

    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return !isAdmin && layerInfo != null;
    }

    @Override
    public int getOrder() {
        return 1100;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new MetadataPanel(id);
    }

    /**
     * Preview panel for {@link GeoServerHomePage}. Panel models will require {@code getPage()} lookup to determine
     * context.
     */
    static class MetadataPanel extends Panel {
        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(MetadataHomePageContentProvider.MetadataPanel.class);

        private final Component metadataPanel;

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

        public MetadataPanel(String id) {
            super(id);
            metadataPanel = metadataPanel("metadataPanel");
            add(metadataPanel);
        }

        /** Return the application instance. */
        protected GeoServerApplication getGeoServerApplication() {
            return (GeoServerApplication) getApplication();
        }

        /**
         * Setup metadataPanel div, which will only be visible when metadata links are available.
         *
         * @param id wicket id
         * @return common preview
         */
        private Component metadataPanel(String id) {
            final WebMarkupContainer container = new WebMarkupContainer(id);
            container.setOutputMarkupId(true);

            LoadableDetachableModel<List<MetadataLinkInfo>> links = new LoadableDetachableModel<>() {
                @Override
                protected List<MetadataLinkInfo> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) MetadataPanel.this.getPage();
                    PublishedInfo publishedInfo = homePage.getPublishedInfo();
                    List<MetadataLinkInfo> links = new ArrayList<>();

                    if (publishedInfo != null && publishedInfo instanceof LayerInfo) {
                        LayerInfo layerInfo = (LayerInfo) publishedInfo;
                        links.addAll(layerInfo.getResource().getMetadataLinks());
                    } else if (publishedInfo != null && publishedInfo instanceof LayerGroupInfo) {
                        LayerGroupInfo layerGroupInfo = (LayerGroupInfo) publishedInfo;
                        links.addAll(layerGroupInfo.getMetadataLinks());
                    }
                    // only show panel if we have something to share
                    metadataPanel.setVisible(!links.isEmpty());
                    return links;
                }
            };
            ListView<MetadataLinkInfo> metadata = new ListView<>("metadata", links) {
                @Override
                public void populateItem(ListItem<MetadataLinkInfo> item) {
                    MetadataLinkInfo info = item.getModelObject();

                    String type = info.getMetadataType();
                    String about = info.getAbout();
                    String url = info.getContent();
                    String format = info.getType();

                    String title = about;
                    if (Strings.isEmpty(title)) {
                        title = format;
                    }
                    if (!Strings.isEmpty(type) && !"other".equalsIgnoreCase(type)) {
                        title += " (" + type + ")";
                    }

                    ExternalLink link = new ExternalLink("theLink", url, title);
                    if (!Strings.isEmpty(format)) {
                        link.add(AttributeModifier.append("title", format));
                    }

                    item.add(link);
                }
            };
            container.add(metadata);

            return container;
        }
    }
}
