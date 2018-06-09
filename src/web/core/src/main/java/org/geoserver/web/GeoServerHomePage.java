/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.catalog.Predicates.acceptAll;

import com.google.common.base.Stopwatch;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Home page, shows just the introduction and the capabilities link
 *
 * <p>This page uses the {@link CapabilitiesHomePageLinkProvider} extension point to enable other
 * modules to contribute links for GetCapabilities documents. The default {@link
 * ServiceInfoCapabilitiesProvider} contributes the capabilities links for all the available {@link
 * ServiceInfo} implementations. Other extension point implementations may contribute service
 * description document links non backed by ServiceInfo objects.
 *
 * @author Andrea Aime - TOPP
 */
public class GeoServerHomePage extends GeoServerBasePage implements GeoServerUnlockablePage {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GeoServerHomePage() {
        GeoServer gs = getGeoServer();
        ContactInfo contact = gs.getGlobal().getSettings().getContact();

        // add some contact info
        add(
                new ExternalLink("contactURL", contact.getOnlineResource())
                        .add(new Label("contactName", contact.getContactOrganization())));
        {
            String version = String.valueOf(new ResourceModel("version").getObject());
            String contactEmail = contact.getContactEmail();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("version", version);
            params.put(
                    "contactEmail",
                    (contactEmail == null ? "geoserver@example.org" : contactEmail));
            Label label =
                    new Label(
                            "footerMessage",
                            new StringResourceModel(
                                    "GeoServerHomePage.footer", this, new Model(params)));
            label.setEscapeModelStrings(false);
            add(label);
        }

        Authentication auth = getSession().getAuthentication();
        if (isAdmin(auth)) {
            Stopwatch sw = Stopwatch.createStarted();
            Fragment f = new Fragment("catalogLinks", "catalogLinksFragment", this);
            Catalog catalog = getCatalog();

            NumberFormat numberFormat = NumberFormat.getIntegerInstance(getLocale());
            numberFormat.setGroupingUsed(true);

            final Filter allLayers = acceptAll();
            final Filter allStores = acceptAll();
            final Filter allWorkspaces = acceptAll();

            final int layerCount = catalog.count(LayerInfo.class, allLayers);
            final int storesCount = catalog.count(StoreInfo.class, allStores);
            final int wsCount = catalog.count(WorkspaceInfo.class, allWorkspaces);

            f.add(
                    new BookmarkablePageLink("layersLink", LayerPage.class)
                            .add(new Label("nlayers", numberFormat.format(layerCount))));
            f.add(new BookmarkablePageLink("addLayerLink", NewLayerPage.class));

            f.add(
                    new BookmarkablePageLink("storesLink", StorePage.class)
                            .add(new Label("nstores", numberFormat.format(storesCount))));
            f.add(new BookmarkablePageLink("addStoreLink", NewDataPage.class));

            f.add(
                    new BookmarkablePageLink("workspacesLink", WorkspacePage.class)
                            .add(new Label("nworkspaces", numberFormat.format(wsCount))));
            f.add(new BookmarkablePageLink("addWorkspaceLink", WorkspaceNewPage.class));
            add(f);

            sw.stop();
        } else {
            Label placeHolder = new Label("catalogLinks");
            placeHolder.setVisible(false);
            add(placeHolder);
        }

        final IModel<List<GeoServerHomePageContentProvider>> contentProviders;
        contentProviders = getContentProviders(GeoServerHomePageContentProvider.class);
        ListView<GeoServerHomePageContentProvider> contentView =
                new ListView<GeoServerHomePageContentProvider>(
                        "contributedContent", contentProviders) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<GeoServerHomePageContentProvider> item) {
                        GeoServerHomePageContentProvider provider = item.getModelObject();
                        Component extraContent = provider.getPageBodyComponent("contentList");
                        if (null == extraContent) {
                            Label placeHolder = new Label("contentList");
                            placeHolder.setVisible(false);
                            extraContent = placeHolder;
                        }
                        item.add(extraContent);
                    }
                };
        add(contentView);

        final IModel<List<CapabilitiesHomePageLinkProvider>> capsProviders;
        capsProviders = getContentProviders(CapabilitiesHomePageLinkProvider.class);

        ListView<CapabilitiesHomePageLinkProvider> capsView =
                new ListView<CapabilitiesHomePageLinkProvider>("providedCaps", capsProviders) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<CapabilitiesHomePageLinkProvider> item) {
                        CapabilitiesHomePageLinkProvider provider = item.getModelObject();
                        Component capsList = provider.getCapabilitiesComponent("capsList");
                        item.add(capsList);
                    }
                };
        add(capsView);
    }

    private <T> IModel<List<T>> getContentProviders(final Class<T> providerClass) {
        IModel<List<T>> providersModel =
                new LoadableDetachableModel<List<T>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<T> load() {
                        GeoServerApplication app = getGeoServerApplication();
                        List<T> providers = app.getBeansOfType(providerClass);
                        return providers;
                    }
                };
        return providersModel;
    }

    /** Checks if the current user is authenticated and is the administrator */
    private boolean isAdmin(Authentication authentication) {

        return GeoServerExtensions.bean(GeoServerSecurityManager.class)
                .checkAuthenticationForAdminRole(authentication);
    }
}
