/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;
import static org.geoserver.web.util.WebUtils.toResourceName;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geoserver.web.HomePagePreviewSectionProvider;
import org.geoserver.web.PreviewLink;

/** Contributes preview section to GeoServer Home page. */
public class PreviewHomePageContentProvider implements GeoServerHomePageContentProvider {

    @Override
    public boolean checkContext(boolean isAdmin, WorkspaceInfo workspaceInfo, PublishedInfo layerInfo) {
        return !isAdmin && layerInfo != null;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Component getPageBodyComponent(String id) {
        return new PreviewPanel(id);
    }

    /**
     * Preview panel for {@link GeoServerHomePage}.
     *
     * <p>Sections are contributed by {@link HomePagePreviewSectionProvider} extensions.
     */
    static class PreviewPanel extends Panel {
        private static final boolean IS_CSS_EMPTY = IsWicketCssFileEmpty(PreviewPanel.class);
        private static final JavaScriptResourceReference JS =
                new JavaScriptResourceReference(PreviewPanel.class, "PreviewHomePageContentProvider.js");

        PreviewPanel(String id) {
            super(id);
            add(sections("sections"));
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            if (!IS_CSS_EMPTY) {
                response.render(CssHeaderItem.forReference(
                        new PackageResourceReference(getClass(), toResourceName(getClass(), "css"))));
            }
            response.render(JavaScriptReferenceHeaderItem.forReference(JS));
        }

        private Component sections(String id) {
            WebMarkupContainer sections = new WebMarkupContainer(id);
            sections.setOutputMarkupPlaceholderTag(true);

            LoadableDetachableModel<List<Section>> model = new LoadableDetachableModel<>() {
                @Override
                protected List<Section> load() {
                    PublishedInfo published = ((GeoServerHomePage) PreviewPanel.this.getPage()).getPublishedInfo();
                    if (published == null) return List.of();

                    List<HomePagePreviewSectionProvider> providers =
                            GeoServerExtensions.extensions(HomePagePreviewSectionProvider.class);

                    List<Section> result = new ArrayList<>();
                    for (HomePagePreviewSectionProvider provider : providers) {
                        if (!provider.supports(published)) continue;
                        List<PreviewLink> links = provider.getLinks(published);
                        if (links != null && !links.isEmpty()) {
                            result.add(new Section(provider.getTitleKey(), links));
                        }
                    }
                    sections.setVisible(!result.isEmpty());
                    return result;
                }
            };

            sections.add(new ListView<>("section", model) {
                @Override
                protected void populateItem(ListItem<Section> item) {
                    Section section = item.getModelObject();
                    String sectionTitle =
                            getLocalizer().getString(section.titleKey(), PreviewPanel.this, section.titleKey());
                    item.add(AttributeModifier.replace("data-section-title", Model.of(sectionTitle)));
                    item.add(new Label("sectionTitle", sectionTitle));
                    item.add(new ListView<>("previewLink", section.links()) {
                        @Override
                        protected void populateItem(ListItem<PreviewLink> item) {
                            PreviewLink link = item.getModelObject();
                            item.add(AttributeModifier.replace("data-filter-label", Model.of(link.label())));
                            if (link.catalogLinkType() != null) {
                                item.add(AttributeModifier.replace(
                                        "data-catalog-link", Model.of(link.catalogLinkType())));
                            }
                            ExternalLink externalLink = new ExternalLink("theLink", link.href(), link.label());
                            if (link.title() != null) {
                                externalLink.add(AttributeModifier.append("title", link.title()));
                            }
                            item.add(externalLink);
                        }
                    });
                }
            });
            return sections;
        }
    }

    /** Section title key and links to render. */
    record Section(String titleKey, List<PreviewLink> links) {}
}
