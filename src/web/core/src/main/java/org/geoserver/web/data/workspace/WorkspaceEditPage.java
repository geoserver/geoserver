/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.admin.ContactPanel;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.geoserver.web.data.namespace.NamespaceDetachableModel;
import org.geoserver.web.data.settings.SettingsPluginPanelInfo;
import org.geoserver.web.security.AccessDataRuleInfoManager;
import org.geoserver.web.security.AccessDataRulePanel;
import org.geoserver.web.security.DataAccessRuleInfo;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.services.ServiceMenuPageInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.logging.Logging;

/** Allows editing a specific workspace */
public class WorkspaceEditPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 4341324830412716976L;

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.workspace");

    IModel<WorkspaceInfo> wsModel;
    IModel<NamespaceInfo> nsModel;
    SettingsPanel settingsPanel;
    ServicesPanel servicesPanel;
    AccessDataRulePanel accessDataPanel;
    WsEditInfoPanel basicInfoPanel;
    GeoServerDialog dialog;
    TabbedPanel tabbedPanel;

    /** Uses a "name" parameter to locate the workspace */
    public WorkspaceEditPage(PageParameters parameters) {
        String wsName = parameters.get("name").toString();
        WorkspaceInfo wsi = getCatalog().getWorkspaceByName(wsName);

        if (wsi == null) {
            getSession()
                    .error(
                            new ParamResourceModel("WorkspaceEditPage.notFound", this, wsName)
                                    .getString());
            doReturn(WorkspacePage.class);
            return;
        }

        init(wsi);
    }

    public WorkspaceEditPage(WorkspaceInfo ws) {
        init(ws);
    }

    private void init(WorkspaceInfo ws) {
        boolean defaultWs = ws.getId().equals(getCatalog().getDefaultWorkspace().getId());

        wsModel = new WorkspaceDetachableModel(ws);

        NamespaceInfo ns = getCatalog().getNamespaceByPrefix(ws.getName());

        if (ns == null) {
            // unfortunately this may happen if the namespace associated to the workspace was
            // deleted or never created
            throw new RuntimeException(
                    String.format(
                            "Workspace '%s' associated namespace doesn't exists.", ws.getName()));
        }

        nsModel = new NamespaceDetachableModel(ns);

        Form form =
                new Form<NamespaceInfo>("form", new CompoundPropertyModel<NamespaceInfo>(nsModel));
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(
                new AbstractTab(new Model<String>("Basic Info")) {

                    private static final long serialVersionUID = 5216769765556937554L;

                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        try {
                            basicInfoPanel =
                                    new WsEditInfoPanel(panelId, wsModel, nsModel, defaultWs);
                            return basicInfoPanel;
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                    }
                });
        if (AccessDataRuleInfoManager.canAccess()) {
            tabs.add(
                    new AbstractTab(new Model<String>("Security")) {

                        private static final long serialVersionUID = 5216769765556937554L;

                        @Override
                        public WebMarkupContainer getPanel(String panelId) {
                            try {
                                AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
                                ListModel<DataAccessRuleInfo> ownModel =
                                        new ListModel<>(
                                                manager.getDataAccessRuleInfo(wsModel.getObject()));
                                accessDataPanel =
                                        new AccessDataRulePanel(panelId, wsModel, ownModel);
                                return accessDataPanel;
                            } catch (Exception e) {
                                throw new WicketRuntimeException(e);
                            }
                        }
                    });
        }

        tabbedPanel =
                new TabbedPanel("tabs", tabs) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer newLink(String linkId, final int index) {
                        return new SubmitLink(linkId) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onSubmit() {
                                setSelectedTab(index);
                            }
                        };
                    }
                };
        tabbedPanel.setOutputMarkupId(true);
        form.add(tabbedPanel);
        form.add(submitLink());
        form.add(new BookmarkablePageLink<WorkspacePage>("cancel", WorkspacePage.class));
        add(form);
    }

    private SubmitLink submitLink() {
        return new SubmitLink("save") {

            private static final long serialVersionUID = -3462848930497720229L;

            @Override
            public void onSubmit() {
                try {
                    saveWorkspace();
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING, "Failed to save workspace", e);
                    error(
                            e.getMessage() == null
                                    ? "Failed to save workspace, no error message available, see logs for details"
                                    : e.getMessage());
                }
            }
        };
    }

    private void saveWorkspace() {
        final Catalog catalog = getCatalog();

        NamespaceInfo namespaceInfo = (NamespaceInfo) nsModel.getObject();
        WorkspaceInfo workspaceInfo = (WorkspaceInfo) wsModel.getObject();

        namespaceInfo.setIsolated(workspaceInfo.isIsolated());

        // sync up workspace name with namespace prefix, temp measure until the two become separate
        namespaceInfo.setPrefix(workspaceInfo.getName());

        // validate workspace and namespace before updating them
        catalog.validate(workspaceInfo, false).throwIfInvalid();
        catalog.validate(namespaceInfo, false).throwIfInvalid();

        // this will ensure all datastore namespaces are updated when the workspace is modified
        catalog.save(workspaceInfo);
        catalog.save(namespaceInfo);
        if (basicInfoPanel.defaultWs) {
            catalog.setDefaultWorkspace(workspaceInfo);
        }

        GeoServer geoServer = getGeoServer();

        // persist/depersist any settings configured local to the workspace
        Settings set = settingsPanel.set;
        if (set.enabled) {
            if (set.model instanceof NewSettingsModel) {
                geoServer.add(set.model.getObject());
            } else {
                geoServer.save(set.model.getObject());
            }
        } else {
            // remove if necessary
            if (set.model instanceof ExistingSettingsModel) {
                geoServer.remove(set.model.getObject());
            }
        }

        // persist/depersist any services configured local to this workspace
        for (Service s : servicesPanel.services) {
            if (s.enabled) {
                if (s.model instanceof ExistingServiceModel) {
                    // nothing to do, service has already been added
                    continue;
                }
                geoServer.add(s.model.getObject());
            } else {
                // remove if necessary
                if (s.model instanceof ExistingServiceModel) {
                    // means they are removing an existing service, look it up and remove
                    geoServer.remove(s.model.getObject());
                }
            }
        }
        try {
            if (accessDataPanel != null) accessDataPanel.save();
            doReturn(WorkspacePage.class);
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error saving access rules associated to workspace " + workspaceInfo.getName(),
                    e);
            error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /*
     * Data object to hold onto transient settings, and maintain state of enabled for the workspace.
     */
    static class Settings implements Serializable {
        private static final long serialVersionUID = -5855608735160516252L;

        /** track selection */
        Boolean enabled;

        /** created settings, not yet added to configuration */
        IModel<SettingsInfo> model;
    }

    static class ExistingSettingsModel extends LoadableDetachableModel<SettingsInfo> {

        private static final long serialVersionUID = -8203239697623788188L;
        IModel<WorkspaceInfo> wsModel;

        ExistingSettingsModel(IModel<WorkspaceInfo> wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        protected SettingsInfo load() {
            GeoServer gs = GeoServerApplication.get().getGeoServer();
            return gs.getSettings(wsModel.getObject());
        }
    }

    static class NewSettingsModel extends Model<SettingsInfo> {

        private static final long serialVersionUID = -4365626821652771933L;
        IModel<WorkspaceInfo> wsModel;
        SettingsInfo info;

        NewSettingsModel(IModel<WorkspaceInfo> wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        public SettingsInfo getObject() {
            if (info == null) {
                GeoServer gs = GeoServerApplication.get().getGeoServer();
                info = gs.getFactory().createSettings();

                // initialize from global settings
                SettingsInfo global = gs.getGlobal().getSettings();

                // hack, we need to copy out composite objects separately to get around proxying
                // madness
                ContactInfo contact = gs.getFactory().createContact();
                OwsUtils.copy(global.getContact(), contact, ContactInfo.class);

                OwsUtils.copy(global, info, SettingsInfo.class);
                info.setContact(contact);

                info.setWorkspace(wsModel.getObject());
            }
            return info;
        }
    }

    class WsEditInfoPanel extends Panel {

        private static final long serialVersionUID = -8487041433764733692L;

        boolean defaultWs;

        public WsEditInfoPanel(
                String id,
                IModel<WorkspaceInfo> wsModel,
                IModel<NamespaceInfo> nsModel,
                boolean defaultWs) {
            super(id, wsModel);
            this.defaultWs = defaultWs;

            // check for full admin, we don't allow workspace admins to change all settings
            boolean isFullAdmin = isAuthenticatedAsAdmin();

            TextField<String> name =
                    new TextField<String>("name", new PropertyModel<String>(wsModel, "name"));
            name.setRequired(true);
            name.setEnabled(isFullAdmin);

            name.add(new XMLNameValidator());
            add(name);
            TextField<String> uri =
                    new TextField<String>(
                            "uri", new PropertyModel<String>(nsModel, "uRI"), String.class);
            uri.setRequired(true);
            uri.add(new URIValidator());
            add(uri);
            CheckBox defaultChk =
                    new CheckBox("default", new PropertyModel<Boolean>(this, "defaultWs"));
            add(defaultChk);
            defaultChk.setEnabled(isFullAdmin);

            CheckBox isolatedChk =
                    new CheckBox("isolated", new PropertyModel<>(wsModel, "isolated"));
            add(isolatedChk);
            defaultChk.setEnabled(isFullAdmin);

            add(dialog = new GeoServerDialog("dialog"));

            // local settings
            add(settingsPanel = new SettingsPanel("settings", wsModel));
            add(new HelpLink("settingsHelp").setDialog(dialog));

            // local services
            add(servicesPanel = new ServicesPanel("services", wsModel));
            add(new HelpLink("servicesHelp").setDialog(dialog));
        }
    }

    class SettingsPanel extends FormComponentPanel<Serializable> {

        private static final long serialVersionUID = -1580928887379954134L;

        WebMarkupContainer settingsContainer;
        ContactPanel contactPanel;
        WebMarkupContainer otherSettingsPanel;
        Settings set;

        public SettingsPanel(String id, IModel<WorkspaceInfo> model) {
            super(id, new Model<>());

            SettingsInfo settings = getGeoServer().getSettings(model.getObject());

            set = new Settings();
            set.enabled = settings != null;
            set.model =
                    settings != null
                            ? new ExistingSettingsModel(wsModel)
                            : new NewSettingsModel(wsModel);

            add(
                    new CheckBox("enabled", new PropertyModel<Boolean>(set, "enabled"))
                            .add(
                                    new AjaxFormComponentUpdatingBehavior("click") {
                                        private static final long serialVersionUID =
                                                -7851699665702753119L;

                                        @Override
                                        protected void onUpdate(AjaxRequestTarget target) {
                                            contactPanel.setVisible(set.enabled);
                                            otherSettingsPanel.setVisible(set.enabled);
                                            target.add(settingsContainer);
                                        }
                                    }));

            settingsContainer = new WebMarkupContainer("settingsContainer");
            settingsContainer.setOutputMarkupId(true);
            add(settingsContainer);

            contactPanel =
                    new ContactPanel(
                            "contact",
                            new CompoundPropertyModel<ContactInfo>(
                                    new PropertyModel<ContactInfo>(set.model, "contact")));
            contactPanel.setOutputMarkupId(true);
            contactPanel.setVisible(set.enabled);
            settingsContainer.add(contactPanel);

            otherSettingsPanel =
                    new WebMarkupContainer("otherSettings", new CompoundPropertyModel<>(set.model));
            otherSettingsPanel.setOutputMarkupId(true);
            otherSettingsPanel.setVisible(set.enabled);
            otherSettingsPanel.add(new CheckBox("verbose"));
            otherSettingsPanel.add(new CheckBox("verboseExceptions"));
            otherSettingsPanel.add(new CheckBox("localWorkspaceIncludesPrefix"));
            otherSettingsPanel.add(
                    new TextField<Integer>("numDecimals").add(RangeValidator.minimum(0)));
            otherSettingsPanel.add(
                    new DropDownChoice<String>("charset", GlobalSettingsPage.AVAILABLE_CHARSETS));
            otherSettingsPanel.add(new TextField<String>("proxyBaseUrl").add(new UrlValidator()));

            // Addition of pluggable extension points
            ListView<SettingsPluginPanelInfo> extensions =
                    SettingsPluginPanelInfo.createExtensions(
                            "extensions", set.model, getGeoServerApplication());
            otherSettingsPanel.add(extensions);

            settingsContainer.add(otherSettingsPanel);
        }
    }

    /*
     * Data object to hold onto transient services, and maintain state of selected services for
     * the workspace.
     */
    static class Service implements Serializable {
        private static final long serialVersionUID = 3283857206025172687L;

        /** track selection */
        Boolean enabled;

        /** the admin page for the service */
        ServiceMenuPageInfo<?> adminPage;

        /** created service, not yet added to configuration */
        IModel<ServiceInfo> model;
    }

    static class NewServiceModel extends Model<ServiceInfo> {

        private static final long serialVersionUID = -3467556623909292282L;
        IModel<WorkspaceInfo> wsModel;
        Class<ServiceInfo> serviceClass;
        ServiceInfo service;

        NewServiceModel(IModel<WorkspaceInfo> wsModel, Class<ServiceInfo> serviceClass) {
            this.wsModel = wsModel;
            this.serviceClass = serviceClass;
        }

        @Override
        public ServiceInfo getObject() {
            if (service == null) {
                service = create();
            }
            return service;
        }

        ServiceInfo create() {
            // create it
            GeoServer gs = GeoServerApplication.get().getGeoServer();

            ServiceInfo newService = gs.getFactory().create(serviceClass);

            // initialize from global service
            ServiceInfo global = gs.getService(serviceClass);
            OwsUtils.copy(global, newService, serviceClass);
            newService.setWorkspace(wsModel.getObject());

            // hack, but need id to be null so its considered unattached
            ((ServiceInfoImpl) newService).setId(null);

            return newService;
        }
    }

    static class ExistingServiceModel extends LoadableDetachableModel<ServiceInfo> {

        private static final long serialVersionUID = -2170117760214309321L;
        IModel<WorkspaceInfo> wsModel;
        Class<ServiceInfo> serviceClass;

        ExistingServiceModel(IModel<WorkspaceInfo> wsModel, Class<ServiceInfo> serviceClass) {
            this.wsModel = wsModel;
            this.serviceClass = serviceClass;
        }

        @Override
        protected ServiceInfo load() {
            return GeoServerApplication.get()
                    .getGeoServer()
                    .getService(wsModel.getObject(), serviceClass);
        }
    }

    class ServicesPanel extends FormComponentPanel<Serializable> {

        private static final long serialVersionUID = 7375904545106343626L;
        List<Service> services;

        public ServicesPanel(String id, final IModel<WorkspaceInfo> wsModel) {
            super(id, new Model<>());

            services = services(wsModel);
            ListView<Service> serviceList =
                    new ListView<Service>("services", services) {

                        private static final long serialVersionUID = -4142739871430618450L;

                        @Override
                        protected void populateItem(ListItem<Service> item) {
                            Service service = item.getModelObject();

                            final Link<Service> link =
                                    new Link<Service>("link", new Model<Service>(service)) {
                                        private static final long serialVersionUID =
                                                1111536301891090436L;

                                        @Override
                                        public void onClick() {
                                            Service s = getModelObject();
                                            Page page = null;

                                            if (s.model instanceof ExistingServiceModel) {
                                                // service that has already been added,
                                                PageParameters pp =
                                                        new PageParameters()
                                                                .add(
                                                                        "workspace",
                                                                        wsModel.getObject()
                                                                                .getName());
                                                try {
                                                    page =
                                                            s.adminPage
                                                                    .getComponentClass()
                                                                    .getConstructor(
                                                                            PageParameters.class)
                                                                    .newInstance(pp);
                                                } catch (Exception e) {
                                                    throw new WicketRuntimeException(e);
                                                }
                                            } else {
                                                // service that has yet to be added
                                                try {
                                                    page =
                                                            s.adminPage
                                                                    .getComponentClass()
                                                                    .getConstructor(
                                                                            s.adminPage
                                                                                    .getServiceClass())
                                                                    .newInstance(
                                                                            s.model.getObject());
                                                } catch (Exception e) {
                                                    throw new WicketRuntimeException(e);
                                                }
                                            }
                                            ((BaseServiceAdminPage<?>) page)
                                                    .setReturnPage(WorkspaceEditPage.this);
                                            setResponsePage(page);
                                        }
                                    };
                            link.setOutputMarkupId(true);
                            link.setEnabled(service.enabled);

                            AjaxCheckBox enabled =
                                    new AjaxCheckBox(
                                            "enabled",
                                            new PropertyModel<Boolean>(service, "enabled")) {
                                        private static final long serialVersionUID =
                                                6369730006169869310L;

                                        @Override
                                        protected void onUpdate(AjaxRequestTarget target) {
                                            link.setEnabled(getModelObject());
                                            target.add(link);
                                        }
                                    };
                            item.add(enabled);

                            ServiceMenuPageInfo info = service.adminPage;

                            link.add(
                                    new AttributeModifier(
                                            "title",
                                            new StringResourceModel(
                                                    info.getDescriptionKey(),
                                                    (Component) null,
                                                    null)));
                            link.add(
                                    new Label(
                                            "link.label",
                                            new StringResourceModel(
                                                    info.getTitleKey(), (Component) null, null)));

                            Image image;
                            if (info.getIcon() != null) {
                                image =
                                        new Image(
                                                "link.icon",
                                                new PackageResourceReference(
                                                        info.getComponentClass(), info.getIcon()));
                            } else {
                                image =
                                        new Image(
                                                "link.icon",
                                                new PackageResourceReference(
                                                        GeoServerBasePage.class,
                                                        "img/icons/silk/wrench.png"));
                            }
                            image.add(
                                    new AttributeModifier(
                                            "alt",
                                            new ParamResourceModel(info.getTitleKey(), null)));
                            link.add(image);
                            item.add(link);
                        }
                    };
            add(serviceList);
        }

        List<Service> services(IModel<WorkspaceInfo> wsModel) {
            List<Service> services = new ArrayList<Service>();

            for (ServiceMenuPageInfo page :
                    getGeoServerApplication().getBeansOfType(ServiceMenuPageInfo.class)) {
                Service service = new Service();
                service.adminPage = page;
                service.enabled =
                        getGeoServer().getService(wsModel.getObject(), page.getServiceClass())
                                != null;

                // if service is disabled, create a placeholder model to hold a newly created one,
                // otherwise create a live model to the existing service
                @SuppressWarnings("unchecked")
                Class<ServiceInfo> serviceClass = (Class<ServiceInfo>) page.getServiceClass();
                service.model =
                        !service.enabled
                                ? new NewServiceModel(wsModel, serviceClass)
                                : new ExistingServiceModel(wsModel, serviceClass);
                services.add(service);
            }

            return services;
        }
    }
}
