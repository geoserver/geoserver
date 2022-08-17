/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.catalog.Predicates.acceptAll;

import com.google.common.base.Stopwatch;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.INamedParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geotools.feature.NameImpl;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Home page, shows introduction for each kind of service along with any service links.
 *
 * <p>This page uses {@link ServiceDescriptionProvider} extension point allow other modules describe
 * web services, and the web service links.
 *
 * <p>The {@link CapabilitiesHomePageLinkProvider} extension point to enable other modules to
 * contribute components. The default {@link ServiceInfoCapabilitiesProvider} contributes the
 * capabilities links for all the available {@link ServiceInfo} implementations that were not
 * covered by ServiceDescriptionProvider. Other extension point implementations may contribute
 * service description document links not backed by ServiceInfo objects.
 *
 * <p>The {@link GeoServerHomePageContentProvider} is used by modules to contribute information,
 * status and warnings.
 *
 * <p>The page has built-in functionality providing administrators with a configuration summary.
 *
 * <p>This page can change between gloabl-service, workspace service and layer service.
 *
 * @author Andrea Aime - TOPP
 */
public class GeoServerHomePage extends GeoServerBasePage implements GeoServerUnlockablePage {

    /** Display contact name linking to contact URL. */
    private ExternalLink contactURL;

    /** Context workspace for displayed web services, or null for global services */
    private String workspace = null;

    /** Context layer / layergroup for displayed web services (optional) */
    private String layer = null;

    public GeoServerHomePage() {
        homeInit();
    }

    public GeoServerHomePage(PageParameters parameters) {
        super(parameters);
        homeInit();
    }

    private void homeInit() {
        GeoServer gs = getGeoServer();

        WorkspaceInfo workspaceInfo = null;
        PublishedInfo layerInfo = null;

        if (getPageParameters() != null && !getPageParameters().isEmpty()) {
            StringValue workspaceParam = getPageParameters().get("workspace");
            if (workspaceParam == null
                    || workspaceParam.isEmpty()
                    || Strings.isEmpty(workspaceParam.toString())) {
                this.workspace = null; // list global services
            } else {
                this.workspace = workspaceParam.toString();
            }

            StringValue layerParam = getPageParameters().get("layer");
            if (layerParam == null
                    || layerParam.isEmpty()
                    || Strings.isEmpty(layerParam.toString())) {
                this.layer = null; // for all services
            } else {
                this.layer = layerParam.toString();
            }
        }

        if (this.workspace != null) {
            workspaceInfo = gs.getCatalog().getWorkspaceByName(this.workspace);
        }
        if (this.layer != null) {
            layerInfo = layerInfo(workspaceInfo, this.layer);
        }

        ContactInfo contactInfo = gs.getSettings().getContact();
        if (workspaceInfo != null) {
            SettingsInfo settings = gs.getSettings(workspaceInfo);
            if (settings != null) {
                contactInfo = settings.getContact();
            }
        }

        Form<?> form = new Form("form");
        add(form);
        SubmitLink refresh =
                new SubmitLink("refresh") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(
                                GeoServerHomePage.class,
                                new PageParameters()
                                        .set(
                                                "workspace",
                                                workspace,
                                                0,
                                                INamedParameters.Type.QUERY_STRING)
                                        .set(
                                                "layer",
                                                layer,
                                                1,
                                                INamedParameters.Type.QUERY_STRING));
                    }
                };
        form.add(refresh);
        form.setDefaultButton(refresh);

        @SuppressWarnings("PMD.UseDiamondOperator") // java 8 compiler cannot infer type
        TextField<String> workspaceField =
                new TextField<String>("workspace", new PropertyModel<>(this, "workspace"));
        form.add(workspaceField);
        TextField<String> layerField =
                new TextField<String>("layer", new PropertyModel<>(this, "layer"));
        form.add(layerField);

        // add some contact info
        contactURL = new ExternalLink("contactURL", contactInfo.getOnlineResource());
        contactURL.add(new Label("contactName", contactInfo.getContactOrganization()));
        add(contactURL);

        {
            String version = String.valueOf(new ResourceModel("version").getObject());
            String contactEmail = contactInfo.getContactEmail();

            HashMap<String, String> params = new HashMap<>();
            params.put("version", version);
            params.put(
                    "contactEmail",
                    (contactEmail == null ? "geoserver@example.org" : contactEmail));

            Label footerMessage =
                    new Label(
                            "footerMessage",
                            new StringResourceModel(
                                    "GeoServerHomePage.footer", this, new Model(params)));
            footerMessage.setEscapeModelStrings(false);
            add(footerMessage);
        }

        Authentication auth = getSession().getAuthentication();
        if (isAdmin(auth)) {
            // show admin some additional details
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
            // add catalogLinks placeholder (even when not admin) to identify this page location
            Label placeHolder = new Label("catalogLinks");
            placeHolder.setVisible(false);
            add(placeHolder);
        }

        // additional content provided by plugins across the geoserver codebase
        // for example security warnings to admin
        final IModel<List<GeoServerHomePageContentProvider>> contentProviders =
                getContentProviders(GeoServerHomePageContentProvider.class);
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

        String workspace = workspaceInfo != null ? workspaceInfo.getName() : null;
        String layer = null;
        List<ServicesPanel.ServiceDescription> serviceDescriptions = new ArrayList<>();
        List<ServicesPanel.ServiceLinkDescription> serviceLinks = new ArrayList<>();
        for (ServiceDescriptionProvider provider :
                getGeoServerApplication().getBeansOfType(ServiceDescriptionProvider.class)) {
            serviceDescriptions.addAll(provider.getServices(workspaceInfo, layerInfo));
            serviceLinks.addAll(provider.getServiceLinks(workspaceInfo, layerInfo));
        }
        add(new ServicesPanel("serviceList", serviceDescriptions, serviceLinks));

        final IModel<List<CapabilitiesHomePageLinkProvider>> capsProviders =
                getContentProviders(CapabilitiesHomePageLinkProvider.class);

        ListView<CapabilitiesHomePageLinkProvider> capsView =
                new ListView<CapabilitiesHomePageLinkProvider>("providedCaps", capsProviders) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<CapabilitiesHomePageLinkProvider> item) {
                        CapabilitiesHomePageLinkProvider provider = item.getModelObject();
                        Component capsList;
                        if (provider instanceof ServiceDescriptionProvider) {
                            Label placeHolder = new Label("contentList");
                            placeHolder.setVisible(false);
                            capsList = placeHolder;
                        } else {
                            capsList = provider.getCapabilitiesComponent("capsList");
                            if (capsList == null) {
                                Label placeHolder = new Label("contentList");
                                placeHolder.setVisible(false);
                                capsList = placeHolder;
                            }
                        }
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

    /**
     * Look up published info using page workspace / layer context (see {@code
     * LocalWorkspaceCallback}).
     *
     * @param workspaceName Name of workspace
     * @param layerName Name of layer or layer group
     * @return PublishedInfo representing layer info or group info, or {@code null} if not found
     */
    protected PublishedInfo layerInfo(WorkspaceInfo workspaceInfo, String layerName) {
        if (layerName == null) {
            return null;
        }
        Catalog catalog = getGeoServer().getCatalog();
        if (workspaceInfo != null) {
            NamespaceInfo namespaceInfo = catalog.getNamespaceByPrefix(workspaceInfo.getName());
            LayerInfo layerInfo =
                    catalog.getLayerByName(new NameImpl(namespaceInfo.getURI(), layerName));
            if (layerInfo != null) {
                return layerInfo;
            }
            LayerGroupInfo groupInfo = catalog.getLayerGroupByName(workspaceInfo, layerName);
            if (groupInfo != null) {
                return groupInfo;
            }
        } else {
            LayerInfo layerInfo = catalog.getLayerByName(layerName);
            if (layerInfo != null) {
                return layerInfo;
            }
            LayerGroupInfo groupInfo = catalog.getLayerGroupByName((WorkspaceInfo) null, layerName);
            if (groupInfo != null) {
                return groupInfo;
            }
        }
        return null;
    }
}
