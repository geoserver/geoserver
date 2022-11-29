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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
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
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;
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
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AdminRequest;
import org.geoserver.util.InternationalStringUtils;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.store.NewDataPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geoserver.web.spring.security.GeoServerSession;
import org.geotools.feature.NameImpl;
import org.opengis.filter.Filter;
import org.opengis.util.InternationalString;

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

    /**
     * System property used to externally define {@link #SelectionMode}.
     *
     * <p>If provided this setting will override any configuration option.
     */
    public static String SELECTION_MODE = "GeoServerHomePage.selectionMode";

    enum SelectionMode {
        /** Workspace and layer auto-complete suitable for small catalogues. */
        AUTOCOMPLETE,
        /**
         * Layer autocomplete is only available when workspace prefix provided. Suitable for large
         * catalogues with many workspaces
         */
        PREFIX,
        /**
         * Workspace autocomplete only, disabling layer autocomplete.
         *
         * <p>Suitable for large/slow catalogues.
         */
        WORKSPACE,
        /** Disable autocomplete, use plain text-field for workspace and layer selection */
        DISABLE;
    }

    SelectionMode selectionMode = SelectionMode.WORKSPACE;

    {
        try {
            String mode = GeoServerExtensions.getProperty(SELECTION_MODE);
            if (!Strings.isEmpty(mode)) {
                selectionMode = SelectionMode.valueOf(mode.toUpperCase());
            }
        } catch (IllegalArgumentException ignore) {
            LOGGER.fine("GeoServer home page selection mode not set:" + ignore);
        }
    }

    /**
     * Optional workspace context for displayed web services, or {@code null} for global services.
     *
     * <p>This field matches the page parameter and is used to populate the raw text input of the
     * Select2DropDownChoice widget. This is used to lookup {@link #workspaceInfo} from the catalog.
     */
    private String workspace = null;

    /** Selected WorkspaceInfo used to filter page contents. */
    private WorkspaceInfo workspaceInfo;

    /**
     * Control used to display/define {@link #workspace} (and by extension {@link #workspaceInfo}.
     */
    private TextField<String> workspaceField;

    /**
     * Optional layer / layergroup context for displayed web services, or {@code null}.
     *
     * <p>Field initially populated by page parameter and matches the raw text input of the
     * Select2DropDownChoice widget. Used to look up {@link #layerInfo} in the catalog.
     */
    private String layer = null;

    /** Selected PublishedInfo (i.e. LayerInfo or LayerGroupInfo) used to filter page contents. */
    private PublishedInfo layerInfo = null;

    /** Control used to display/define {@link #layer} (and by extension {@link #layerInfo}. */
    private TextField<String> layerField;

    public GeoServerHomePage() {
        homeInit();
    }

    public GeoServerHomePage(PageParameters parameters) {
        super(parameters);
        homeInit();
    }

    private void homeInit() {

        GeoServer gs = getGeoServer();
        clearAdminRequestIfNotAdmin();

        boolean admin = getSession().isAdmin();
        initFromPageParameters(getPageParameters());

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

        String welcomeText = contactInfo.getWelcome();
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
            serviceDescriptions.addAll(provider.getServices(workspaceInfo, layerInfo));
            serviceLinks.addAll(provider.getServiceLinks(workspaceInfo, layerInfo));
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
        if (workspaceInfo == null && layerInfo == null) {
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
                            clearAdminRequestIfNotAdmin();
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
    private void selectHomePage(String workspaceName, String layerName) {
        String workspaceSelection = toWorkspace(workspaceName, layerName);
        String layerSelection = toLayer(workspaceName, layerName);

        PageParameters pageParams = new PageParameters();
        if (workspaceSelection != null) {
            pageParams.add("workspace", workspaceSelection, 0, INamedParameters.Type.QUERY_STRING);
        }
        if (layerSelection != null) {
            pageParams.add("layer", layerSelection, 1, INamedParameters.Type.QUERY_STRING);
        }
        setResponsePage(GeoServerHomePage.class, pageParams);
    }

    /**
     * Check {@link #workspaceField} selection and input to determine workspaceName.
     *
     * @return workspaceName, may be {@code null} if undefined.
     */
    private String getWorkspaceFieldText() {
        if (workspaceField.getModelObject() != null) {
            return workspaceField.getModelObject();
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
     * Check {@link #layerField} selection and input to determine layerName, or search for matching
     * layers.
     *
     * @return layerName, may include prefix, may be {@code null} if undefined.
     */
    private String getLayerFieldText() {
        if (layerField.getModelObject() != null) {
            return layerField.getModelObject();
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

        this.workspaceField =
                new AutoCompleteTextField<String>(
                        "workspace", new PropertyModel<>(this, "workspace")) {
                    @Override
                    protected Iterator<String> getChoices(String input) {
                        if (Strings.isEmpty(input) || selectionMode == SelectionMode.DISABLE) {
                            return Collections.emptyIterator();
                        }
                        Filter filter = Predicates.contains("name", input);
                        List<String> suggestions = new ArrayList<>(10);

                        clearAdminRequestIfNotAdmin();
                        long TIME_LIMIT = System.currentTimeMillis() + 3000;
                        try (CloseableIterator<WorkspaceInfo> search =
                                getCatalog().list(WorkspaceInfo.class, filter, 0, 10, null)) {
                            while (System.currentTimeMillis() < TIME_LIMIT && search.hasNext()) {
                                suggestions.add(search.next().getName());
                            }
                        }
                        return suggestions.iterator();
                    }
                };
        workspaceField.setConvertEmptyInputStringToNull(true);
        workspaceField.setRequired(false);
        workspaceField.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 891156513194272712L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String workspaceName = getWorkspaceFieldText();
                        selectHomePage(workspaceName, null);
                    }
                });
        form.add(workspaceField);
        this.layerField =
                new AutoCompleteTextField<String>("layer", new PropertyModel<>(this, "layer")) {
                    @Override
                    protected Iterator<String> getChoices(String input) {
                        if (Strings.isEmpty(input)
                                || (selectionMode == SelectionMode.DISABLE)
                                || (selectionMode == SelectionMode.PREFIX
                                        && workspaceInfo == null)) {
                            return Collections.emptyIterator();
                        }
                        Filter filter = getLayerFilter(workspace, input);
                        List<String> suggestions = new ArrayList<>(10);

                        clearAdminRequestIfNotAdmin();
                        long TWO_SECONDS = System.currentTimeMillis() + 1000;
                        try (CloseableIterator<PublishedInfo> search =
                                getCatalog().list(PublishedInfo.class, filter, 0, 10, null)) {
                            while (System.currentTimeMillis() < TWO_SECONDS && search.hasNext()) {
                                if (workspaceInfo == null) {
                                    suggestions.add(search.next().prefixedName());
                                } else {
                                    suggestions.add(search.next().getName());
                                }
                            }
                        }
                        return suggestions.iterator();
                    }
                };
        layerField.setRequired(false);
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
        form.add(layerField);
        return form;
    }

    private void initFromPageParameters(PageParameters pageParameters) {
        if (pageParameters == null || pageParameters.isEmpty()) {
            this.workspace = null;
            this.workspaceInfo = null;
            this.layer = null;
            this.layerInfo = null;
            return;
        }
        GeoServer gs = getGeoServer();

        // Step 1: Update fields from both page parameters (as workspace may have been defined by a
        // layer prefix)
        StringValue workspaceParam = getPageParameters().get("workspace");
        if (workspaceParam == null
                || workspaceParam.isEmpty()
                || Strings.isEmpty(workspaceParam.toString())) {
            this.workspace = null;
        } else {
            this.workspace = workspaceParam.toString();
        }

        StringValue layerParam = getPageParameters().get("layer");
        if (layerParam == null || layerParam.isEmpty() || Strings.isEmpty(layerParam.toString())) {
            this.layer = null; // for all services
        } else {
            this.layer = toLayer(this.workspace, layerParam.toString());
            this.workspace = toWorkspace(this.workspace, layerParam.toString());
        }

        // Step 2: Look up workspaceInfo and layerInfo in catalog
        if (this.workspace != null) {
            if (this.workspaceInfo != null && this.workspaceInfo.getName().equals(this.workspace)) {
                // no need to look up a second time, unless refresh?
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Parameter workspace='"
                                    + this.workspace
                                    + "' home page previously configured for this workspace");
                }
            } else {
                this.workspaceInfo = gs.getCatalog().getWorkspaceByName(this.workspace);
                if (this.workspaceInfo == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "Parameter workspace='"
                                        + this.workspace
                                        + "' unable to locate a workspace of this name");
                    }
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "Parameter workspace='"
                                        + this.workspace
                                        + "' located workspaceInfo used to filter page contents");
                    }
                }
            }
        } else {
            this.workspaceInfo = null; // list global services
            LOGGER.fine("Parameter workspace not supplied, list global services");
        }

        if (this.layer != null) {
            if (this.layerInfo != null && this.layerInfo.getName().equals(this.layer)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Parameter layer='"
                                    + this.layer
                                    + "' home page previously configured for this layer");
                }
            } else {
                this.layerInfo = layerInfo(workspaceInfo, this.layer);
                if (layerInfo != null) {
                    // Step 3: Double check workspace matches layer
                    String prefixedName = this.layerInfo.prefixedName();
                    if (prefixedName != null && prefixedName.contains(":")) {
                        String prefix = prefixedName.substring(0, prefixedName.indexOf(":"));
                        if (this.workspace == null || !this.workspace.equals(prefix)) {
                            this.workspace = prefix;
                            LOGGER.fine(
                                    "Parameter workspace='"
                                            + this.workspace
                                            + "' updated from found layer '"
                                            + prefixedName
                                            + "'");
                            if (this.layerInfo instanceof LayerInfo) {
                                this.workspaceInfo =
                                        ((LayerInfo) layerInfo)
                                                .getResource()
                                                .getStore()
                                                .getWorkspace();
                            } else if (this.layerInfo instanceof LayerGroupInfo) {
                                this.workspaceInfo = ((LayerGroupInfo) layerInfo).getWorkspace();
                            }
                            // workspaceInfo =
                            // getGeoServer().getCatalog().getWorkspaceByName(prefix);
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("Updated workspaceInfo used to filter page contents");
                            }
                        }
                    }
                } else {
                    LOGGER.fine(
                            "Parameter layer='"
                                    + this.layer
                                    + "' unable to locate a layer or layer group of this name");
                }
            }
        } else {
            this.layerInfo = null; // list global or workspace services
            LOGGER.fine("Parameter layer not supplied, list global or workspace services");
        }
    }

    /**
     * GeoServerHomePage works with an {@link AdminRequest} if the current user has that role.
     *
     * <p>This method checks if the current user is an admin leaving AdminRequest intact, or if the
     * user is not an admin AdminRequest is cleared.
     *
     * <p>This is required for methods like {@link #homeInit()} and any methods called via
     * LoadableDetachableModel that wish to interact with the catalog. The catalog is setup to denny
     * access to content if called from an AdminRequest if the current user is not an admin.
     */
    protected void clearAdminRequestIfNotAdmin() {
        boolean admin = getSession().isAdmin();
        if (!admin) {
            // clear admin request so it does not interfere with
            // catalogue access of workspaces and layers
            AdminRequest.abort();
        }
    }

    @Override
    protected String getDescription() {
        Locale locale = getLocale();
        Catalog catalog = getCatalog();

        // GeoServerBasePage calls getDescription from a LoadedDetachable model
        // So this method may be running concurrently with homeInit().
        //
        // To take some care to ensure lack of admin access
        // does not trip up count queries against the catalog
        // (usually only an issue on page refresh)
        clearAdminRequestIfNotAdmin();

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        numberFormat.setGroupingUsed(true);

        Filter allWorkspaces = acceptAll();
        int layerCount = countLayerNames();
        int workspaceCount = catalog.count(WorkspaceInfo.class, allWorkspaces);

        String userName = GeoServerSession.get().getUsername();

        HashMap<String, String> params = new HashMap<>();
        params.put("workspaceCount", numberFormat.format(workspaceCount));
        params.put("layerCount", numberFormat.format(layerCount));
        params.put("user", escapeMarkup(userName));

        boolean isGlobal = getGeoServer().getGlobal().isGlobalServices();

        StringBuilder builder = new StringBuilder();

        if (layerInfo != null && layerInfo instanceof LayerInfo) {
            params.put("layerName", escapeMarkup(layerInfo.prefixedName()));
            builder.append(
                    new StringResourceModel(
                                    "GeoServerHomePage.descriptionLayer",
                                    this,
                                    new Model<HashMap<String, String>>(params))
                            .getString());
        } else if (layerInfo != null && layerInfo instanceof LayerGroupInfo) {
            params.put("layerName", escapeMarkup(layerInfo.prefixedName()));
            builder.append(
                    new StringResourceModel(
                                    "GeoServerHomePage.descriptionLayerGroup",
                                    this,
                                    new Model<HashMap<String, String>>(params))
                            .getString());
        } else if (workspaceInfo != null) {
            params.put("workspaceName", escapeMarkup(workspaceInfo.getName()));

            builder.append(
                    new StringResourceModel(
                                    "GeoServerHomePage.descriptionWorkspace",
                                    this,
                                    new Model<HashMap<String, String>>(params))
                            .getString());
        } else if (isGlobal) {
            builder.append(
                    new StringResourceModel(
                                    "GeoServerHomePage.descriptionGlobal",
                                    this,
                                    new Model<HashMap<String, String>>(params))
                            .getString());
        } else {
            builder.append(
                    new StringResourceModel(
                                    "GeoServerHomePage.descriptionGlobalOff",
                                    this,
                                    new Model<HashMap<String, String>>(params))
                            .getString());
        }

        return builder.toString();
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

    /**
     * Escape text before being used in formatting (to prevent any raw html being displayed).
     *
     * @param text Text to escape
     * @return escaped text
     */
    private String escapeMarkup(String text) {
        return new StringBuilder(Strings.escapeMarkup(text)).toString();
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
            if (layerInfo != null) {
                if (layerInfo instanceof LayerInfo) {
                    layerCount = 1;
                    groupCount = 0;
                    storesCount = 1;
                    wsCount = 1;
                } else {
                    layerCount = 0;
                    groupCount = 1;
                    storesCount = 0;
                    wsCount = layerInfo.prefixedName().contains(":") ? 1 : 0;
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

    /**
     * Count of PublishedInfo (ie layer or layergroup) taking the current workspace and global
     * services into account.
     *
     * @return Count of addressable layers
     */
    private int countLayerNames() {
        Catalog catalog = getCatalog();
        String workspaceName = workspaceInfo != null ? workspaceInfo.getName() : null;

        return catalog.count(PublishedInfo.class, getLayerFilter(workspaceName, null));
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
     * Predicate construct to efficiently query catalog for PublishedInfo suitable for interaction.
     *
     * @param workspaceName Optional workspace name to limit search to a single workspace
     * @paran search Optional search string used to filter layers returned
     * @return Filter for use with catalog, or {@code null} to require manual entry.
     */
    protected Filter getLayerFilter(String workspaceName, String search) {

        // need to get only advertised and enabled layers
        Filter isLayerInfo = Predicates.isInstanceOf(LayerInfo.class);
        Filter isLayerGroupInfo = Predicates.isInstanceOf(LayerGroupInfo.class);

        Filter enabledFilter = Predicates.equal("resource.enabled", true);
        Filter storeEnabledFilter = Predicates.equal("resource.store.enabled", true);
        Filter advertisedFilter = Predicates.equal("resource.advertised", true);
        Filter enabledLayerGroup = Predicates.equal("enabled", true);
        Filter advertisedLayerGroup = Predicates.equal("advertised", true);

        // Filter for the Layers
        List<Filter> layerFilters = new ArrayList<>();
        layerFilters.add(isLayerInfo);
        if (workspaceName != null) {
            layerFilters.add(Predicates.equal("resource.namespace.prefix", workspaceName));
        }
        if (search != null) {
            layerFilters.add(Predicates.contains("name", search));
        }
        layerFilters.add(enabledFilter);
        layerFilters.add(storeEnabledFilter);
        layerFilters.add(advertisedFilter);

        Filter layerFilter = Predicates.and(layerFilters);

        // Filter for the LayerGroups
        List<Filter> groupFilters = new ArrayList<>();
        groupFilters.add(isLayerGroupInfo);
        if (workspaceName != null) {
            groupFilters.add(Predicates.equal("workspace.name", workspaceName));
        }
        if (search != null) {
            groupFilters.add(Predicates.contains("name", search));
        }
        if (!getGeoServer().getGlobal().isGlobalServices()) {
            // skip global layer groups if global services are disabled
            groupFilters.add(Predicates.not(Predicates.isNull("workspace.name")));
        }
        groupFilters.add(enabledLayerGroup);
        groupFilters.add(advertisedLayerGroup);

        Filter layerGroupFilter = Predicates.and(groupFilters);

        // Or filter for merging them
        return Predicates.or(layerFilter, layerGroupFilter);
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
            } else {
                return null;
            }
        }
        return workspaceName;
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
            return null;
        }
    }
}
