/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;

/** Contributes metadata links section to GeoServer Home page. */
public class DataLinkHomePageContentProvider implements GeoServerHomePageContentProvider {

    protected static final Logger LOGGER = Logging.getLogger(GeoServerHomePage.class);

    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return !isAdmin && layerInfo != null && layerInfo instanceof LayerInfo;
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new DataPanel(id);
    }

    /**
     * Preview panel for {@link GeoServerHomePage}. Panel models will require {@code getPage()} lookup to determine
     * context.
     */
    static class DataPanel extends Panel {
        private static final boolean isCssEmpty = IsWicketCssFileEmpty(DataPanel.class);

        private final Component dataPanel;

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

        public DataPanel(String id) {
            super(id);
            dataPanel = dataPanel("dataPanel");

            add(dataPanel);
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
        private Component dataPanel(String id) {
            final WebMarkupContainer container = new WebMarkupContainer(id);
            container.setOutputMarkupId(true);

            LoadableDetachableModel<List<DataLinkInfo>> links = new LoadableDetachableModel<>() {
                @Override
                protected List<DataLinkInfo> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) DataPanel.this.getPage();
                    PublishedInfo publishedInfo = homePage.getPublishedInfo();
                    List<DataLinkInfo> links = new ArrayList<>();

                    if (publishedInfo != null && publishedInfo instanceof LayerInfo) {
                        LayerInfo layerInfo = (LayerInfo) publishedInfo;
                        links.addAll(layerInfo.getResource().getDataLinks());
                    }
                    // only show panel if we have something to share
                    dataPanel.setVisible(!links.isEmpty());
                    return links;
                }
            };
            ListView<DataLinkInfo> metadata = new ListView<>("data", links) {
                @Override
                public void populateItem(ListItem<DataLinkInfo> item) {
                    DataLinkInfo data = item.getModelObject();

                    String title = translateFormat("format.", data.getType());

                    ExternalLink link = new ExternalLink("theLink", data.getContent(), title);
                    link.add(AttributeModifier.append("title", data.getType()));
                    item.add(link);
                }
            };
            container.add(metadata);

            return container;
        }

        /**
         * Translate format (if translation available).
         *
         * @param prefix protocol
         * @param format output format
         * @return format translation (defaults to format if unavailable)
         */
        private String translateFormat(String prefix, String format) {
            try {
                return getLocalizer().getString(prefix + format, this);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return format;
            }
        }
    }
}
