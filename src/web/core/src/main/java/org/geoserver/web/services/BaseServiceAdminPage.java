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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
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
 * <ul>
 *   <li>{@link #getServiceName()}}: The service being configured by {@code build} single page, or {@code buildPanel}
 *       for tabbed presentation.
 *   <li>{@link #getServiceClass()}: The {@link ServiceInfo} used to store configuration.
 * </ul>
 *
 * There are two presentation options:
 *
 * <p>Recommended: Subclasses of this page can use {@link #buildPanel(String, IModel, Form)} contribute an
 * {@link AdminPagePanel}. This panel is used as a starting point for a tabbed display of services associated with the
 * {@link #getServiceClass()} configuration. If {@code buildPanel} return {@code null} the subclass is assumed to
 * {@code wicket:extend} BaseServiceAdmin page by contribute form components using {@link #build(IModel, Form)} method.
 * Each component that is added to the form should have a corresponding markup entry of the following form:
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
    /** Application property allowing workspace admins access to the workspace service configuration too. */
    public static final String WORKSPACE_ADMIN_SERVICE_ACCESS = "WORKSPACE_ADMIN_SERVICE_ACCESS";

    /** Shared dialog used for feedback and confirmation. */
    protected GeoServerDialog dialog;

    /** Form on submit callbacks.. */
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

        Map<String, List<AdminPagePanelInfo>> panels = extensionPanels();
        List<ITab> tabs = new ArrayList<>();

        // initial panel used for tabbed presentation
        final AdminPagePanel initialAdminPanel = buildPanel("initial", infoModel, form);

        if (initialAdminPanel != null) {
            // TABBED PRESENTATION
            // general tab for common service configuration options
            tabs.add(new AbstractTab(new org.apache.wicket.model.ResourceModel("BaseServiceAdminPage.service")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new GeneralTabAdminPagePanel(panelId, infoModel, null);
                }
            });
            // service tab
            final List<AdminPagePanelInfo> servicePanels = panels.remove(getServiceType());
            tabs.add(new AbstractTab(this::getServiceType) {
                @Override
                public Panel getPanel(String panelId) {
                    return new ServiceAdminTabPanel(
                            panelId, infoModel, initialAdminPanel, servicePanels, onSubmitHooks);
                }
            });

            // remaining content on their own tabs
            if (!panels.isEmpty()) {
                for (String specificServiceType : panels.keySet()) {
                    List<AdminPagePanelInfo> tabPanels = panels.get(specificServiceType);
                    if (tabPanels != null && !tabPanels.isEmpty()) {
                        tabs.add(new AbstractTab(() -> specificServiceType) {
                            @Override
                            public Panel getPanel(String panelId) {
                                return new ServiceAdminTabPanel(panelId, infoModel, null, tabPanels, onSubmitHooks);
                            }
                        });
                    }
                }
            }
            TabbedPanel<ITab> tabbedPanel = new TabbedPanel<>("tabs", tabs) {
                @Override
                protected WebMarkupContainer newLink(String linkId, int index) {
                    return new SubmitLink(linkId) {
                        @Serial
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onSubmit() {
                            setSelectedTab(index);
                        }
                    };
                }
            };
            form.add(tabbedPanel);
            form.add(createPlaceholder("general"));
            form.add(createPlaceholder("extensions"));
        } else {
            // SINGLE PAGE PRESENTATION: for specific service type
            form.add(createPlaceholder("tabs"));
            form.add(new GeneralTabAdminPagePanel("general", infoModel, getServiceType()));

            // Subclass build adds content to bottom of page, rather than individual tabs
            build(infoModel, form);

            List<AdminPagePanelInfo> extensionPanels = panels.get(getServiceType());
            ListView extensionPanelView =
                    new AdminPagePanelInfoListView("extensions", extensionPanels, infoModel, onSubmitHooks);
            extensionPanelView.setReuseItems(true);
            form.add(extensionPanelView);
        }
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

    /** Create an invisible placeholder */
    Label createPlaceholder(String id) {
        Label placeholder = new Label(id);
        placeholder.setVisible(false);

        return placeholder;
    }

    /**
     * Look up AdminPagePanels for {@link #getServiceClass()}.
     *
     * @return AdminPagePanelInfo, listed by specific service type.
     */
    protected Map<String, List<AdminPagePanelInfo>> extensionPanels() {
        List<AdminPagePanelInfo> panels = getGeoServerApplication().getBeansOfType(AdminPagePanelInfo.class);
        for (Iterator<AdminPagePanelInfo> it = panels.iterator(); it.hasNext(); ) {
            AdminPagePanelInfo panel = it.next();
            if (!getServiceClass().equals(panel.getServiceClass())) {
                it.remove();
            }
        }
        Map<String, List<AdminPagePanelInfo>> panelsByTab = new HashMap<>();
        for (AdminPagePanelInfo panel : panels) {
            String type = panel.getSpecificServiceType();
            if (type == null) {
                type = getServiceType();
            }
            panelsByTab.computeIfAbsent(type, k -> new ArrayList<>()).add(panel);
        }
        return panelsByTab;
    }

    /**
     * The {@link ServiceInfo} storing configuration for this admin page.
     *
     * <p>This value is used to obtain a reference to the service info object via {@link GeoServer#getService(Class)}.
     */
    protected abstract Class<T> getServiceClass();

    /**
     * Callback for building an initial AdminPanel for tabbed ServiceAdminPage presentation.
     *
     * @param id Wicket id for created panel
     * @return Initial AdminPagePanel, or {@code null} for single page presentation.
     */
    protected AdminPagePanel buildPanel(String id, IModel<T> info, Form form) {
        return null;
    }

    /**
     * Extend the BaseServiceAdminPage by building adding additional components to the form for the page. This method is
     * only called if {@link #buildPanel(String, IModel, Form)} returns {@code null}.
     *
     * <p>The form uses a {@link CompoundPropertyModel} so in the normal case components do not need a model as it's
     * inherited from the parent. This means that component id's should match the info bean property they correspond to.
     *
     * @param info The service info object.
     * @param form The page form.
     * @deprecated use {@link #buildPanel(String, IModel, Form)} instead to build the main tab panel for the page.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    protected void build(IModel<T> info, Form form) {}

    /**
     * Callback for submit.
     *
     * <p>This implementation persists the service configuration. If the service has already been persisted (i.e.
     * {@code info.getId() != null}), it is saved (updated). Otherwise, it is added as a new service configuration —
     * this happens when a workspace-specific service is configured for the first time. Subclasses may extend / override
     * if need be.
     */
    protected void handleSubmit(T info) {
        if (info.getId() != null) {
            getGeoServer().save(info);
        } else {
            // ServiceInfo override being created for the first time
            getGeoServer().add(info);
        }
    }

    /** The string to use when representing this service to users, subclasses must override. */
    protected abstract String getServiceName();

    /**
     * The specific service type identifier (e.g., "WMS", "WFS", "WCS", "Features") Used to retrieve available versions
     * from the dispatcher service registry.
     *
     * <p>Services can share a common {@link #getServiceClass()} for configuration.
     *
     * <p>Subclasses must override.
     */
    protected abstract String getServiceType();

    /**
     * Model used to establish the context (global or workspace) for this service admin page.
     *
     * <p>Detached model that looks up ServiceInfo from GeoServer catalogue using optional workspaceName.
     *
     * @param <T>
     */
    class ServiceModel<T extends ServiceInfo> extends LoadableDetachableModel<T> {

        /* id reference */
        String id;

        /* reference via local workspace */
        Class<T> serviceClass;
        String workspaceName;

        /* direct reference */
        T service;

        /**
         * Create a ServiceModel using the provided ServiceInfo.
         *
         * @param service serivce info
         */
        ServiceModel(T service) {
            this.id = service.getId();
            if (this.id == null) {
                this.service = service;
            }
        }

        /**
         * Detached model looking up
         *
         * @param serviceClass ServiceInfo class
         * @param workspaceName Wworkspace name
         */
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
                    // workspace service configuration override
                    WorkspaceInfo ws = getCatalog().getWorkspaceByName(workspaceName);
                    return (T) getGeoServer().getService(ws, getServiceClass());
                }
                // global service
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

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        // This page is used in two context, for global services and workspace services
        // the authorizer is set to workspace admin to allow access to workspace services
        // but a check for full admin is performed in the constructor to verify access to global services
        // Rationale: this method is called when the page is constructed, and the workspace is not yet known
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    /** AdminPagePanel for general configuration options. */
    public class GeneralTabAdminPagePanel extends AdminPagePanel {
        @Serial
        private static final long serialVersionUID = -1;

        private static final boolean isCssEmpty =
                IsWicketCssFileEmpty(BaseServiceAdminPage.GeneralTabAdminPagePanel.class);

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

        public GeneralTabAdminPagePanel(String panelId, IModel<T> infoModel, String specificServiceType) {
            super(panelId, infoModel);

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

            // service control
            add(new ServiceControlAdminPanel<>("serviceControl", infoModel, specificServiceType));
        }
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
}
