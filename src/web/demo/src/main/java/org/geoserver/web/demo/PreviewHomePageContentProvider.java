/* (c) 2026Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerHomePageContentProvider;

/** Contributes preview section to GeoServer Home page. */
public class PreviewHomePageContentProvider implements GeoServerHomePageContentProvider {

    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return !isAdmin && layerInfo != null;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new PreviewPanel(id);
    }

    /**
     * Preview panel for {@link GeoServerHomePage}. Panel models will require {@code getPage()} lookup to determine
     * context.
     */
    static class PreviewPanel extends Panel {
        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(PreviewHomePageContentProvider.PreviewPanel.class);

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

        public PreviewPanel(String id) {
            super(id);

            LoadableDetachableModel<List<ExternalLink>> previewLinks = new LoadableDetachableModel<>() {
                @Override
                protected List<ExternalLink> load() {
                    GeoServerHomePage homePage = (GeoServerHomePage) PreviewPanel.this.getPage();
                    PublishedInfo layerInfo = homePage.getPublishedInfo();

                    return commonFormatLinks(new PreviewLayer(layerInfo));
                }
            };
            ListView<ExternalLink> commonFormats = new ListView<>("commonFormats", previewLinks) {
                @Override
                public void populateItem(ListItem<ExternalLink> item) {
                    item.add(item.getModelObject());
                }
            };
            add(commonFormats);
        }

        private List<ExternalLink> commonFormatLinks(PreviewLayer layer) {
            List<ExternalLink> links = new ArrayList<>();
            List<CommonFormatLink> formats = getGeoServerApplication().getBeansOfType(CommonFormatLink.class);
            Collections.sort(formats);
            for (CommonFormatLink link : formats) {
                links.add(link.getFormatLink(layer));
            }
            return links;
        }

        protected GeoServerApplication getGeoServerApplication() {
            return (GeoServerApplication) getApplication();
        }
    }
}
