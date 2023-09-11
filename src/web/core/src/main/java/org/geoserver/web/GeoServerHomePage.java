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
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.INamedParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geotools.api.util.InternationalString;
import org.geotools.feature.NameImpl;

/**
 * Home page, shows introduction for each kind of service along with any service links.
 *
 * <p>This page uses the {@link ServiceDescriptionProvider} extension point to allow other modules
 * to describe web services, and the web service links.
 *
 * <p>The {@link CapabilitiesHomePageLinkProvider} extension point enables other modules to
 * contribute components. The default {@link ServiceInfoCapabilitiesProvider} contributes the
 * capabilities links for all the available {@link ServiceInfo} implementations that were not
 * covered by the ServiceDescriptionProvider extensions. Other extension point implementations may
 * contribute service description document links not backed by ServiceInfo objects.
 *
 * <p>The {@link GeoServerHomePageContentProvider} is used by modules to contribute information,
 * status and warnings.
 *
 * <p>The page has built-in functionality providing administrators with a configuration summary.
 *
 * <p>This page can change between global service, workspace service and layer service.
 *
 * @author Andrea Aime - TOPP
 */
public class GeoServerHomePage extends GeoServerBasePage implements GeoServerUnlockablePage {

    // used only during page initialization, not persisted (needed temporarily as a field in order
    // to work across the GeoServerBasePage framework of description calculation and component
    // initialization)
    transient HomePageSelection selection;
    /**
     * Optional workspace context for displayed web services, or {@code null} for global services.
     *
     * <p>This field matches the page parameter and is used to populate the raw text input of the
     * Select2DropDownChoice widget. This is used to lookup {@link #workspaceInfo} from the catalog.
     */
    private WorkspaceInfo workspaceInfo;

    /** Control used to display/define {@link #workspaceInfo}. */
    private FormComponent<WorkspaceInfo> workspaceField;

    /**
     * Optional layer / layergroup context for displayed web services, or {@code null}.
     *
     * <p>Field initially populated by page parameter and matches the raw text input of the
     * Select2DropDownChoice widget. Used to look up {@link #publishedInfo} in the catalog.
     */
    private PublishedInfo publishedInfo;

    /** Control used to display/define {@link #publishedInfo}. */
    private FormComponent<PublishedInfo> layerField;

    private String description;

    public GeoServerHomePage() {
        homeInit();
    }

    public GeoServerHomePage(PageParameters parameters) {
        super(parameters);
        homeInit();
        // no longer needed at this point, clear it
        this.selection = null;
    }

    @Override
    protected void commonBaseInit() {
        // setup parameters, they will be needed to build the description
        initFromPageParameters(getPageParameters());
        this.selection = HomePageSelection.getHomePageSelection(this);

        // now let the common base init do its job
        super.commonBaseInit();
    }

    private void homeInit() {
        GeoServer gs = getGeoServer();

        boolean admin = getSession().isAdmin();

        ContactInfo contactInfo = gs.getSettings().getContact();
        if (workspaceInfo != null) {
            SettingsInfo settings = gs.getSettings(workspaceInfo);
            if (settings != null) {
                contactInfo = settings.getContact();
            }
        }

        Form<GeoServerHomePage> form = selectionForm(true);
        add(form);

        Locale locale = getLocale();

        InternationalString welcome =
                InternationalStringUtils.growable(
                        contactInfo.getInternationalWelcome(), contactInfo.getWelcome());
        String welcomeText = welcome.toString(locale);
        Label welcomeMessage = new Label("welcome", welcomeText);
        welcomeMessage.setVisible(StringUtils.isNotBlank(welcomeText));
        add(welcomeMessage);

        add(belongsTo(contactInfo, locale));

        add(footerMessage(contactInfo, locale));
        add(footerContact(contactInfo, locale));

        if (admin) {
            // show admin some additional details
            add(adminOverview());
        } else {
            // add catalogLinks placeholder (even when not admin) to identify this page location
            add(placeholderLabel("catalogLinks"));
        }

        // additional content provided by plugins across the geoserver codebase
        // for example security warnings to admin
        add(additionalHomePageContent());

        List<ServiceDescription> serviceDescriptions = new ArrayList<>();
        List<ServiceLinkDescription> serviceLinks = new ArrayList<>();
        for (ServiceDescriptionProvider provider :
                getGeoServerApplication().getBeansOfType(ServiceDescriptionProvider.class)) {
            serviceDescriptions.addAll(provider.getServices(workspaceInfo, publishedInfo));
            serviceLinks.addAll(provider.getServiceLinks(workspaceInfo, publishedInfo));
        }
        ServicesPanel serviceList =
                new ServicesPanel("serviceList", serviceDescriptions, serviceLinks, admin);
        add(serviceList);
        if (serviceDescriptions.isEmpty() && serviceLinks.isEmpty()) {
            serviceList.setVisible(false);
        }
        // service capabilities title only shown if needed
        Localizer localizer = GeoServerApplication.get().getResourceSettings().getLocalizer();

        final Label serviceCapabilitiesTitle =
                new Label(
                        "serviceCapabilities",
                        localizer.getString("GeoServerHomePage.serviceCapabilities", this));
        // Not displayed unless a CapabilitiesHomePageLinkProvider provides a non-null Component
        serviceCapabilitiesTitle.setVisible(false);
        add(serviceCapabilitiesTitle);

        // Only list generic service capabilities as global services
        IModel<List<CapabilitiesHomePageLinkProvider>> capsProviders;
        if (workspaceInfo == null && publishedInfo == null) {
            capsProviders = getContentProviders(CapabilitiesHomePageLinkProvider.class);
        } else {
            capsProviders = Model.ofList(new ArrayList<>());
        }
        ListView<CapabilitiesHomePageLinkProvider> capsView =
                new ListView<CapabilitiesHomePageLinkProvider>("providedCaps", capsProviders) {
                    private static final long serialVersionUID = -4859682164111586340L;

                    @Override
                    protected void populateItem(ListItem<CapabilitiesHomePageLinkProvider> item) {
                        CapabilitiesHomePageLinkProvider provider = item.getModelObject();
                        Component capsList = null;
                        if (!(provider instanceof ServiceDescriptionProvider)) {
                            capsList = provider.getCapabilitiesComponent("capsList");
                            if (capsList != null) {
                                // provider has component to show, title is required to be shown
                                serviceCapabilitiesTitle.setVisible(true);
                            }
                        }
                        if (capsList == null) {
                            capsList = placeholderLabel("capsList");
                        }
                        item.add(capsList);
                    }
                };
        add(capsView);

        // set the description now, not going to change during page lifecycle, so that we can
        // then release the selection (it holds onto non-serializable objects)
        this.description = Optional.ofNullable(selection.getDescription()).orElse("");
    }

    /**
     * Select of global web services, or virtual web service (defined for workspace, or layer).
     *
     * <p>This method is used by the {@link #selectionForm(boolean)} controls to update the page
     * parameters and refresh the page.
     *
     * @param workspaceName Workspace name typed in or selected by user, or {@code null} for global
     *     services.
     * @param layerName Layer name typed in or selected by user, or {@code null} for global or
     *     workspaces services.
     */
    void selectHomePage(String workspaceName, String layerName) {
        String workspaceSelection = toWorkspace(workspaceName, layerName);
        String layerSelection = toLayer(workspaceName, layerName);

        PageParameters pageParams = new PageParameters();
        if (!Strings.isEmpty(workspaceSelection)) {
            pageParams.add("workspace", workspaceSelection, 0, INamedParameters.Type.QUERY_STRING);
        }
        if (!Strings.isEmpty(layerSelection)) {
            pageParams.add("layer", layerSelection, 1, INamedParameters.Type.QUERY_STRING);
        }
        setResponsePage(GeoServerHomePage.class, pageParams);
    }

    /**
     * Check {@link #workspaceField} selection and input to determine workspaceName.
     *
     * @return workspaceName, may be {@code null} if undefined.
     */
    String getWorkspaceFieldText() {
        if (workspaceField.getModelObject() != null) {
            return workspaceField.getModelObject().getName();
        }
        if (workspaceField.hasRawInput()) {
            String rawInput = workspaceField.getRawInput();
            if (StringUtils.isNotBlank(rawInput)) {
                return rawInput;
            }
        }
        if (StringUtils.isNotBlank(workspaceField.getInput())) {
            return workspaceField.getInput();
        }
        return null;
    }

    /**
     * Check {@link #layerField} selection and input to determine layerName.
     *
     * @return layerName, may include prefix, may be {@code null} if undefined.
     */
    private String getLayerFieldText() {
        if (layerField.getModelObject() != null) {
            return layerField.getModelObject().prefixedName();
        }
        if (layerField.hasRawInput()) {
            String rawInput = layerField.getRawInput();
            if (StringUtils.isNotBlank(rawInput)) {
                return rawInput;
            }
        }
        if (StringUtils.isNotBlank(layerField.getInput())) {
            return layerField.getInput();
        }
        return null;
    }

    /**
     * Form for selection of global web services, or virtual web service (defined for workspace, or
     * layer).
     *
     * @param ajax Configure Select2DropDownChoice to use AjaxFormComponentUpdatingBehavior, false
     *     to round-trip selection changes back to component.
     * @return form
     */
    private Form<GeoServerHomePage> selectionForm(final boolean ajax) {

        Form<GeoServerHomePage> form = new Form<>("form");
        form.add(
                new Image(
                        "workspace.icon",
                        new PackageResourceReference(
                                GeoServerHomePage.class, "img/icons/silk/folder.png")));
        form.add(
                new Image(
                        "layer.icon",
                        new PackageResourceReference(
                                GeoServerHomePage.class, "img/icons/silk/picture_empty.png")));

        SubmitLink refresh =
                new SubmitLink("refresh") {
                    @Override
                    public void onSubmit() {
                        String workspaceName = getWorkspaceFieldText();
                        String layerName = getLayerFieldText();

                        selectHomePage(workspaceName, layerName);
                    }
                };
        refresh.setVisible(false);
        form.add(refresh);
        form.setDefaultButton(refresh);

        // Ask model to connect so values are present for property model
        this.workspaceField = selection.getWorkspaceField(form, "workspace");
        workspaceField.setRequired(false);

        if (ajax) {
            workspaceField.add(
                    new AjaxFormComponentUpdatingBehavior("change") {
                        private static final long serialVersionUID = 5871428962450362668L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            String workspaceName = getWorkspaceFieldText();

                            selectHomePage(workspaceName, null);
                        }
                    });
        }

        this.layerField = selection.getPublishedField(form, "layer");
        layerField.setRequired(false);
        if (ajax) {
            layerField.add(
                    new AjaxFormComponentUpdatingBehavior("change") {
                        private static final long serialVersionUID = 5871428962450362669L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            String workspaceName = getWorkspaceFieldText();
                            String layerName = getLayerFieldText();

                            selectHomePage(workspaceName, layerName);
                        }
                    });
        }
        return form;
    }

    private void initFromPageParameters(PageParameters pageParameters) {
        if (pageParameters == null || pageParameters.isEmpty()) {
            this.workspaceInfo = null;
            this.publishedInfo = null;
            return;
        }
        GeoServer gs = getGeoServer();

        // Step 1: Update fields from both page parameters (as workspace may have been defined by a
        // layer prefix)
        String workspace =
                Optional.ofNullable(getPageParameters().get("workspace"))
                        .map(p -> p.toString())
                        .orElse(null);

        String layer =
                Optional.ofNullable(getPageParameters().get("layer"))
                        .map(p -> p.toString())
                        .orElse(null);
        if (layer != null) {
            workspace = toWorkspace(workspace, layer);
            layer = toLayer(workspace, layer);
        }

        // Step 2: Look up workspaceInfo and layerInfo in catalog
        if (workspace != null) {
            if (this.workspaceInfo != null && this.workspaceInfo.getName().equals(workspace)) {
                // no need to look up a second time, unless refresh?
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Parameter workspace='"
                                    + workspace
                                    + "' home page previously configured for this workspace");
                }
            } else {
                this.workspaceInfo = gs.getCatalog().getWorkspaceByName(workspace);
                if (this.workspaceInfo == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        String error =
                                "Parameter workspace='"
                                        + workspace
                                        + "' unable to locate a workspace of this name";
                        error(error);
                        LOGGER.warning(error);
                    }
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "Parameter workspace='"
                                        + workspace
                                        + "' located workspaceInfo used to filter page contents");
                    }
                }
            }
        } else {
            this.workspaceInfo = null; // list global services
            LOGGER.fine("Parameter workspace not supplied, list global services");
        }

        if (layer != null) {
            if (this.publishedInfo != null && this.publishedInfo.getName().equals(layer)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Parameter layer='"
                                    + layer
                                    + "' home page previously configured for this layer");
                }
            } else {
                this.publishedInfo = layerInfo(workspaceInfo, layer);
                if (publishedInfo != null) {
                    // Step 3: Double check workspace matches layer
                    String prefixedName = this.publishedInfo.prefixedName();
                    if (prefixedName != null && prefixedName.contains(":")) {
                        String prefix = prefixedName.substring(0, prefixedName.indexOf(":"));
                        if (workspace == null || !workspace.equals(prefix)) {
                            LOGGER.fine(
                                    "Parameter workspace='"
                                            + workspace
                                            + "' updated from found layer '"
                                            + prefixedName
                                            + "'");
                            if (this.publishedInfo instanceof LayerInfo) {
                                this.workspaceInfo =
                                        ((LayerInfo) publishedInfo)
                                                .getResource()
                                                .getStore()
                                                .getWorkspace();
                            } else if (this.publishedInfo instanceof LayerGroupInfo) {
                                this.workspaceInfo =
                                        ((LayerGroupInfo) publishedInfo).getWorkspace();
                            }
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("Updated workspaceInfo used to filter page contents");
                            }
                        }
                    }
                } else {
                    LOGGER.fine(
                            "Parameter layer='"
                                    + layer
                                    + "' unable to locate a layer or layer group of this name");
                }
            }
        } else {
            this.publishedInfo = null; // list global or workspace services
            LOGGER.fine("Parameter layer not supplied, list global or workspace services");
        }
    }

    @Override
    protected String getDescription() {
        return this.description;
    }

    /**
     * Additional content provided by plugins across the geoserver codebase for example security
     * warnings to admin
     *
     * @return ListView processing {@link GeoServerHomePageContentProvider} components
     */
    private ListView<GeoServerHomePageContentProvider> additionalHomePageContent() {
        final IModel<List<GeoServerHomePageContentProvider>> contentProviders =
                getContentProviders(GeoServerHomePageContentProvider.class);

        return new ListView<GeoServerHomePageContentProvider>(
                "contributedContent", contentProviders) {
            private static final long serialVersionUID = 3756653714268296207L;

            @Override
            protected void populateItem(ListItem<GeoServerHomePageContentProvider> item) {
                GeoServerHomePageContentProvider provider = item.getModelObject();
                Component extraContent = provider.getPageBodyComponent("contentList");
                if (null == extraContent) {
                    extraContent = placeholderLabel("contentList");
                }
                item.add(extraContent);
            }
        };
    }

    private Label placeholderLabel(String wicketId) {
        Label placeHolder = new Label(wicketId);
        placeHolder.setVisible(false);
        return placeHolder;
    }

    private Fragment adminOverview() {
        Stopwatch sw = Stopwatch.createStarted();
        try {
            Fragment catalogLinks = new Fragment("catalogLinks", "catalogLinksFragment", this);
            Catalog catalog = getCatalog();

            int layerCount, groupCount, storesCount, wsCount;
            if (publishedInfo != null) {
                if (publishedInfo instanceof LayerInfo) {
                    layerCount = 1;
                    groupCount = 0;
                    storesCount = 1;
                    wsCount = 1;
                } else {
                    layerCount = 0;
                    groupCount = 1;
                    storesCount = 0;
                    wsCount = publishedInfo.prefixedName().contains(":") ? 1 : 0;
                }
            } else if (workspaceInfo != null) {
                layerCount =
                        catalog.count(
                                LayerInfo.class,
                                Predicates.equal(
                                        "resource.namespace.prefix", workspaceInfo.getName()));
                groupCount =
                        catalog.count(
                                LayerGroupInfo.class,
                                Predicates.equal("workspace.name", workspaceInfo.getName()));
                storesCount =
                        catalog.count(
                                StoreInfo.class,
                                Predicates.equal("workspace.name", workspaceInfo.getName()));
                wsCount = 1;
            } else {
                layerCount = catalog.count(LayerInfo.class, acceptAll());
                groupCount = catalog.count(LayerGroupInfo.class, acceptAll());
                storesCount = catalog.count(StoreInfo.class, acceptAll());
                wsCount = catalog.count(WorkspaceInfo.class, acceptAll());
            }

            NumberFormat numberFormat = NumberFormat.getIntegerInstance(getLocale());
            numberFormat.setGroupingUsed(true);

            catalogLinks.add(
                    new BookmarkablePageLink<LayerPage>("layersLink", LayerPage.class)
                            .add(new Label("nlayers", numberFormat.format(layerCount))));
            catalogLinks.add(
                    new BookmarkablePageLink<NewLayerPage>("addLayerLink", NewLayerPage.class));

            catalogLinks.add(
                    new BookmarkablePageLink<LayerGroupPage>("groupsLink", LayerGroupPage.class)
                            .add(new Label("ngroups", numberFormat.format(groupCount))));
            catalogLinks.add(
                    new BookmarkablePageLink<LayerGroupEditPage>(
                            "addGroupLink", LayerGroupEditPage.class));

            catalogLinks.add(
                    new BookmarkablePageLink<StorePage>("storesLink", StorePage.class)
                            .add(new Label("nstores", numberFormat.format(storesCount))));
            catalogLinks.add(
                    new BookmarkablePageLink<NewDataPage>("addStoreLink", NewDataPage.class));

            catalogLinks.add(
                    new BookmarkablePageLink<WorkspacePage>("workspacesLink", WorkspacePage.class)
                            .add(new Label("nworkspaces", numberFormat.format(wsCount))));
            catalogLinks.add(
                    new BookmarkablePageLink<WorkspaceNewPage>(
                            "addWorkspaceLink", WorkspaceNewPage.class));
            return catalogLinks;
        } finally {
            sw.stop();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Admin summary of catalog links took " + sw.elapsed().toMillis() + " ms");
            }
        }
    }

    /**
     * Organization link to online resource, or placeholder if organization not provided.
     *
     * @param contactInfo
     * @param locale
     * @return organization link to online resource.
     */
    private Label belongsTo(ContactInfo contactInfo, Locale locale) {
        InternationalString onlineResource =
                InternationalStringUtils.growable(
                        contactInfo.getInternationalOnlineResource(),
                        InternationalStringUtils.firstNonBlank(
                                contactInfo.getOnlineResource(),
                                getGeoServer().getSettings().getOnlineResource()));

        InternationalString organization =
                InternationalStringUtils.growable(
                        contactInfo.getInternationalContactOrganization(),
                        contactInfo.getContactOrganization());

        if (organization == null || onlineResource == null) {
            return placeholderLabel("belongsTo");
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("organization", StringEscapeUtils.escapeHtml4(organization.toString(locale)));
        params.put(
                "onlineResource", StringEscapeUtils.escapeHtml4(onlineResource.toString(locale)));

        Label belongsToMessage =
                new Label(
                        "belongsTo",
                        new StringResourceModel(
                                "GeoServerHomePage.belongsTo",
                                this,
                                new Model<HashMap<String, String>>(params)));
        belongsToMessage.setEscapeModelStrings(false);
        return belongsToMessage;
    }

    private Label footerMessage(ContactInfo contactInfo, Locale locale) {
        String version = String.valueOf(new ResourceModel("version").getObject());

        HashMap<String, String> params = new HashMap<>();
        params.put("version", version);

        Label footerMessage =
                new Label(
                        "footerMessage",
                        new StringResourceModel(
                                "GeoServerHomePage.footer",
                                this,
                                new Model<HashMap<String, String>>(params)));

        footerMessage.setEscapeModelStrings(false);
        return footerMessage;
    }

    private Label footerContact(ContactInfo contactInfo, Locale locale) {
        InternationalString contactEmailText =
                InternationalStringUtils.growable(
                        contactInfo.getInternationalContactEmail(), contactInfo.getContactEmail());

        String contactEmail = contactEmailText.toString(locale);

        if (Strings.isEmpty(contactEmail)) {
            return placeholderLabel("footerContact");
        }
        HashMap<String, String> params = new HashMap<>();
        params.put("contactEmail", StringEscapeUtils.escapeHtml4(contactEmail));
        Label message =
                new Label(
                        "footerContact",
                        new StringResourceModel(
                                "GeoServerHomePage.footerContact",
                                this,
                                new Model<HashMap<String, String>>(params)));

        message.setEscapeModelStrings(false);
        return message;
    }

    public WorkspaceInfo getWorkspaceInfo() {
        return workspaceInfo;
    }

    public void setWorkspaceInfo(WorkspaceInfo workspaceInfo) {
        this.workspaceInfo = workspaceInfo;
    }

    public PublishedInfo getPublishedInfo() {
        return publishedInfo;
    }

    public void setPublishedInfo(PublishedInfo layerInfo) {
        this.publishedInfo = layerInfo;
    }

    private <T> IModel<List<T>> getContentProviders(final Class<T> providerClass) {
        IModel<List<T>> providersModel =
                new LoadableDetachableModel<List<T>>() {
                    private static final long serialVersionUID = 3042209889224234562L;

                    @Override
                    protected List<T> load() {
                        GeoServerApplication app = getGeoServerApplication();
                        List<T> providers = app.getBeansOfType(providerClass);
                        return providers;
                    }
                };
        return providersModel;
    }

    /**
     * Look up published info using page workspace / layer context (see {@code
     * LocalWorkspaceCallback}).
     *
     * @param workspaceInfo Name of workspace
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
            return groupInfo;
        } else {
            LayerInfo layerInfo = catalog.getLayerByName(layerName);
            if (layerInfo != null) {
                return layerInfo;
            }
            LayerGroupInfo groupInfo =
                    catalog.getLayerGroupByName(DefaultCatalogFacade.NO_WORKSPACE, layerName);
            if (groupInfo != null) {
                return groupInfo;
            }
            groupInfo = catalog.getLayerGroupByName(DefaultCatalogFacade.ANY_WORKSPACE, layerName);
            return groupInfo;
        }
    }

    /**
     * Determine workspace parameter from select input.
     *
     * @param workspaceName
     * @param layerName
     * @return workspace parameter value
     */
    String toWorkspace(String workspaceName, String layerName) {
        if (!Strings.isEmpty(layerName)) {
            if (layerName.contains(":")) {
                return layerName.substring(0, layerName.indexOf(":"));
            }
        }
        if (!Strings.isEmpty(workspaceName)) {
            return workspaceName;
        } else {
            return "";
        }
    }

    /**
     * Determine layer parameter from select input.
     *
     * @param workspaceName
     * @param layerName
     * @return layer parameter value
     */
    String toLayer(String workspaceName, String layerName) {
        if (!Strings.isEmpty(layerName)) {
            if (layerName.contains(":")) {
                return layerName.substring(layerName.indexOf(":") + 1);
            } else {
                return layerName;
            }
        } else {
            return "";
        }
    }
}
