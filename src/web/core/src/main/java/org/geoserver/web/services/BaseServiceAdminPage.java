/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.data.resource.TitleAndAbstractPanel;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.util.SerializableConsumer;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.web.wicket.LiveCollectionModel;

/**
 * Base page for service administration pages.
 *
 * <p>Subclasses of this page should contribute form components in the {@link #build(IModel, Form)} method. Each
 * component that is added to the form should have a corresponding markup entry of the following form:
 *
 * <pre>{@code
 * <wicket:extend>
 *   <div class="gs-form-group">
 *     <label for="maxFeatures">
 *       <wicket:message key="maxFeatures.title">Maximum Features</wicket:message>
 *     </label>
 *     <input class="field"  id="maxFeatures" wicket:id="maxFeatures" type="text">
 *   </div>
 * </wicket:extend>
 * }</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class BaseServiceAdminPage<T extends ServiceInfo> extends GeoServerSecuredPage {
    /** Allows workspace admins access to the workspace service configuration too * */
    public static final String WORKSPACE_ADMIN_SERVICE_ACCESS = "WORKSPACE_ADMIN_SERVICE_ACCESS";

    protected GeoServerDialog dialog;
    protected List<SerializableConsumer<Void>> onSubmitHooks = new ArrayList<>();

    /** create a page */
    public BaseServiceAdminPage() {
        this(new PageParameters());
    }

    public BaseServiceAdminPage(PageParameters pageParams) {
        String wsName = pageParams.get("workspace").toString();
        init(new ServiceModel<>(getServiceClass(), wsName));
    }

    public BaseServiceAdminPage(T service) {
        init(new ServiceModel<>(service));
    }

    void init(final IModel<T> infoModel) {
        T service = infoModel.getObject();

        dialog = new GeoServerDialog("dialog");
        add(dialog);

        Form<T> form = new Form<>("form", new CompoundPropertyModel<>(infoModel));
        add(form);

        boolean allowAccess = Boolean.parseBoolean(GeoServerExtensions.getProperty(WORKSPACE_ADMIN_SERVICE_ACCESS));
        if (service.getWorkspace() == null || !allowAccess) {
            // check it's really a full admin (to make sure the page cannot be accessed by workspace admins using
            // a direct link to the global edit page)
            if (!isAuthenticatedAsAdmin()) {
                throw new RestartResponseException(UnauthorizedPage.class);
            }

            // create the panel that has the dropdown list to switch between workspace
            form.add(new GlobalWorkspacePanel("workspace"));
        } else {
            // create just a panel with a label that signifies the workspace
            form.add(new LocalWorkspacePanel("workspace", service));
        }

        form.add(new HelpLink("workspaceHelp").setDialog(dialog));

        // add the extension panels
        Map<String,List<AdminPagePanelInfo>> panels = createExtensionPanelList("extensions", infoModel);
        List<AdminPagePanelInfo> servicePanels = panels.get(getServiceType());
        List<ITab> tabs = new ArrayList<>();

        // general tab
        tabs.add(new AbstractTab(new org.apache.wicket.model.ResourceModel("BaseServiceAdminPage.service")) {
            @Override
            public Panel getPanel(String panelId) {
                return new GeneralTabPanel(panelId, infoModel);
            }
        });
        // service tab
        tabs.add(new AbstractTab(this::getServiceType) {
            @Override
            public Panel getPanel(String panelId) {
                return new ServiceTabPanel(panelId, infoModel, form, panels.get(getServiceType()));
            }
        });
        panels.remove(getServiceType());

        // remaining content on their own tabs
        /*
        if(!panels.isEmpty()){
            for (String specificServiceType : panels.keySet()) {
                IModel<String> tabTile = new IModel<String>() {
                    @Override
                    public String getObject() {
                        return specificServiceType;
                    }
                };
                List<AdminPagePanelInfo> tabPanels = panels.get(specificServiceType);

                tabs.add(new AbstractTab(tabTile) {
                    @Override
                    public Panel getPanel(String panelId) {
                        Panel panel = new Panel(panelId,infoModel);

                        return new ServiceTabPanel(panelId, infoModel, form, panels.get(type));
                    }
                });
            }
            AbstractTab extensionsTab = new AbstractTab() {
                @Override
                public Panel getPanel(String panelId) {
                    return new ServiceTabPanel(panelId, infoModel, form, panels.get(getServiceType()));
                }
            };
        }
        */

        TabbedPanel<ITab> tabbedPanel = new TabbedPanel<>("tabs", tabs);
        form.add(tabbedPanel);

        // deprecated page build adds content to bottom of page, rather than tabs
        build(infoModel, form);

        SubmitLink submit = new SubmitLink("submit", new StringResourceModel("save", null, null)) {
            @Override
            public void onSubmit() {
                try {
                    onSave(infoModel, true);
                } catch (IllegalArgumentException ex) {
                    error(ex.getMessage());
                } catch (Exception e) {
                    error(e);
                }
            }
        };
        form.add(submit);

        form.add(applyLink(infoModel, form));

        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
        cancel.setDefaultFormProcessing(false);
    }

    protected void onSave(IModel<T> infoModel, boolean doReturn) {
        handleSubmit(infoModel.getObject());
        // execute all submit hooks
        onSubmitHooks.forEach(x -> {
            x.accept(null);
        });
        if (doReturn) {
            doReturn();
        }
    }

    private GeoserverAjaxSubmitLink applyLink(IModel<T> infoModel, Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target) {
                try {
                    onSave(infoModel, false);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(getForm());
                }
            }
        };
    }

    protected Map<String,List<AdminPagePanelInfo>> createExtensionPanelList(String id, final IModel infoModel) {
        List<AdminPagePanelInfo> panels = getGeoServerApplication().getBeansOfType(AdminPagePanelInfo.class);
        for (Iterator<AdminPagePanelInfo> it = panels.iterator(); it.hasNext(); ) {
            AdminPagePanelInfo panel = it.next();
            if (!getServiceClass().equals(panel.getServiceClass())) {
                it.remove();
            }
        }
        Map<String,List<AdminPagePanelInfo>> panelsByTab = new HashMap<>();
        for (AdminPagePanelInfo panel : panels) {
            String type = panel.getSpecificServiceType();
            if (type == null ) {
                type = getServiceType();
            }
            panelsByTab.computeIfAbsent(type, k -> new ArrayList<>()).add(panel);
        }
        return panelsByTab;
    }

    /**
     * The class of the service.
     *
     * <p>This value is used to obtain a reference to the service info object via {@link GeoServer#getService(Class)}.
     */
    protected abstract Class<T> getServiceClass();

    /**
     * Builds the form for the page.
     *
     * <p>The form uses a {@link CompoundPropertyModel} so in the normal case components do not need a model as it's
     * inherited from the parent. This means that component id's should match the info bean property they correspond to.
     *
     * @param info The service info object.
     * @param form The page form.
     *
     * @deprecated use {@link #buildPanel(IModel, Form)} instead to build the main tab panel for the page.
     */
    protected  void build(IModel info, Form form) {
    }

    /**
     * Callback for building the main AdminPanel for the page.
     *
     */
    protected AdminPagePanel buildPanel( IModel info, Form form) {
        return null;
    }

    /**
     * Callback for submit.
     *
     * <p>This implementation simply saves the service. Subclasses may extend / override if need be.
     */
    protected void handleSubmit(T info) {
        if (info.getId() != null) {
            getGeoServer().save(info);
        }
        // else means a non-attached instance was passed to us, do nothing, up to caller to add it
        // to configuration
    }

    /** The string to use when representing this service to users. Subclasses must override. */
    protected abstract String getServiceName();

    /**
     * The service type identifier (e.g., "WMS", "WFS", "WCS"). Used to retrieve available versions from the service
     * registry. Subclasses must override.
     */
    protected abstract String getServiceType();

    class ServiceModel<T extends ServiceInfo> extends LoadableDetachableModel<T> {

        /* id reference */
        String id;

        /* reference via local workspace */
        Class<T> serviceClass;
        String workspaceName;

        /* direct reference */
        T service;

        ServiceModel(T service) {
            this.id = service.getId();
            if (this.id == null) {
                this.service = service;
            }
        }

        ServiceModel(Class<T> serviceClass, String workspaceName) {
            this.serviceClass = serviceClass;
            this.workspaceName = workspaceName;
        }

        @Override
        @SuppressWarnings("unchecked") // casts to T
        protected T load() {
            if (id != null) {
                return (T) getGeoServer().getService(id, getServiceClass());
            }
            if (serviceClass != null) {
                if (workspaceName != null) {
                    WorkspaceInfo ws = getCatalog().getWorkspaceByName(workspaceName);
                    return (T) getGeoServer().getService(ws, getServiceClass());
                }

                return (T) getGeoServer().getService(getServiceClass());
            }
            return service;
        }

        @Override
        public void detach() {
            if (id == null && serviceClass == null) {
                // keep reference serialize service object
                service = getObject();
            } else {
                service = null;
            }
        }
    }

    class ServiceFilteredWorkspacesModel extends LoadableDetachableModel<List<WorkspaceInfo>> {

        WorkspacesModel wsModel;

        ServiceFilteredWorkspacesModel(WorkspacesModel wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        protected List<WorkspaceInfo> load() {
            List<WorkspaceInfo> workspaces = wsModel.getObject();

            GeoServer gs = getGeoServer();
            for (Iterator<WorkspaceInfo> it = workspaces.iterator(); it.hasNext(); ) {
                if (gs.getService(it.next(), getServiceClass()) == null) {
                    it.remove();
                }
            }
            return workspaces;
        }
    }

    class GlobalWorkspacePanel extends Panel {

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(BaseServiceAdminPage.GlobalWorkspacePanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), getClass().getSimpleName() + ".css")));
            }
        }

        public GlobalWorkspacePanel(String id) {
            super(id);

            final DropDownChoice<WorkspaceInfo> wsChoice = new DropDownChoice<>(
                    "workspace",
                    new ServiceFilteredWorkspacesModel(new WorkspacesModel()),
                    new WorkspaceChoiceRenderer());
            wsChoice.setNullValid(true);
            wsChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    WorkspaceInfo ws = wsChoice.getModelObject();
                    PageParameters pp = new PageParameters();

                    if (ws != null) {
                        pp.add("workspace", ws.getName());
                    }

                    setResponsePage(BaseServiceAdminPage.this.getClass(), pp);
                }
            });
            add(wsChoice);
        }
    }

    class LocalWorkspacePanel extends Panel {

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(BaseServiceAdminPage.LocalWorkspacePanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), getClass().getSimpleName() + ".css")));
            }
        }

        public LocalWorkspacePanel(String id, T service) {
            super(id);

            add(new Label("workspace", new PropertyModel<>(service, "workspace.name")));
        }
    }

    /**
     * Override this method to return true if the implementation support international content
     *
     * @return true if support international content false otherwise.
     */
    protected boolean supportInternationalContent() {
        return false;
    }

    private Fragment getInternationalContentFragment(IModel<T> infoModel, String id) {
        Fragment fragment;
        if (supportInternationalContent()) {
            fragment = new Fragment(id, "internationalStringFragment", this);
            fragment.add(new TitleAndAbstractPanel("titleAndAbstract", infoModel, "titleMsg", "abstract", this));
        } else {
            fragment = new Fragment(id, "stringFragment", this);
            fragment.add(new TextField<>("title"));
            fragment.add(new TextArea<>("abstract"));
        }
        return fragment;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        // This page is used in two context, for global services and workspace services
        // the authorizer is set to workspace admin to allow access to workspace services
        // but a check for full admin is performed in the constructor to verify access to global services
        // Rationale: this method is called when the page is constructed, and the workspace is not yet known
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    protected class GeneralTabPanel extends AdminPagePanel {
        @Serial
        private static final long serialVersionUID = -1;

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(BaseServiceAdminPage.GeneralTabPanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), getClass().getSimpleName() + ".css")));
            }
        }

        public GeneralTabPanel(String panelId, IModel<T> infoModel) {
            super(panelId, infoModel);

            T service = infoModel.getObject();

            // service control
            add(new Label(
                    "service.enabled", new StringResourceModel("service.enabled", this).setParameters(getServiceName())));
            CheckBox enabled = new CheckBox("enabled");
            enabled.setOutputMarkupId(true);
            enabled.setMarkupId("enabled");
            add(enabled);

            CheckBox citeCompliant = new CheckBox("citeCompliant");
            citeCompliant.setOutputMarkupId(true);
            citeCompliant.setMarkupId("citeCompliant");
            add(citeCompliant);

            // metadata
            add(getInternationalContentFragment(infoModel, "serviceTitleAndAbstract"));

            add(new TextField<>("maintainer"));
            TextField<String> onlineResource = new TextField<>("onlineResource");

            final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

            // AF: Disable Binding if GeoServer Env Parametrization is enabled!
            if (gsEnvironment == null || !GeoServerEnvironment.allowEnvParametrization()) {
                onlineResource.add(new UrlValidator());
            }
            add(onlineResource);
            add(new KeywordsEditor("keywords", LiveCollectionModel.list(new PropertyModel<>(infoModel, "keywords"))));
            add(new TextField<>("fees"));
            add(new TextField<>("accessConstraints"));
        }
    }

    protected class ServiceTabPanel extends AdminPagePanel {
        @Serial
        private static final long serialVersionUID = -1;

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(BaseServiceAdminPage.ServiceTabPanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), getClass().getSimpleName() + ".css")));
            }
        }

        public ServiceTabPanel(String panelId, IModel<T> infoModel, Form form, List<AdminPagePanelInfo> adminPagePanelInfos) {
            super(panelId, infoModel);
            T service = infoModel.getObject();

            add(new DisabledVersionsPanel(
                    "disabledVersions", new PropertyModel<>(infoModel, "disabledVersions"), getServiceType()));

            ListView extensionPanels = new AdminPagePanelInfoListView("extensions", adminPagePanelInfos, infoModel);
            extensionPanels.setReuseItems(true);

            add(extensionPanels);
        }
    }

    private class AdminPagePanelInfoListView extends ListView<AdminPagePanelInfo> {

        private final IModel infoModel;

        public AdminPagePanelInfoListView(String id, List<AdminPagePanelInfo> panels, IModel infoModel) {
            super(id, panels);
            this.infoModel = infoModel;
        }

        @Override
        protected void populateItem(ListItem<AdminPagePanelInfo> item) {
            AdminPagePanelInfo info = item.getModelObject();
            try {
                AdminPagePanel panel = info.getComponentClass()
                        .getConstructor(String.class, IModel.class)
                        .newInstance("content", infoModel);
                item.add(panel);
                // add onMainFormSubmit to hooks
                onSubmitHooks.add(x -> panel.onMainFormSubmit());
            } catch (Exception e) {
                throw new WicketRuntimeException(
                        "Failed to create admin extension panel of "
                                + "type "
                                + info.getComponentClass().getSimpleName(),
                        e);
            }
        }
    }
}
