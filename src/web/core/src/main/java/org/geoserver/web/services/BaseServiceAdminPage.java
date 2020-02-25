/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.geoserver.web.GeoServerSecuredPage;
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
 * <p>Subclasses of this page should contribute form components in the {@link #build(ServiceInfo,
 * Form)} method. Each component that is added to the form should have a corresponding markup entry
 * of the following form:
 *
 * <pre>
 * <wicket:extend>
 *   &lt;li>
 *       &lt;span>
 *         &lt;label><wicket:message key="maxFeatures.title">Maximum Features</wicket:message></label>
 *         &lt;input wicket:id="maxFeatures" class="field text" type="text">
 *       &lt;/span>
 *       &lt;p class="instruct">
 *       &lt;/p>
 *     &lt;/li>
 *
 * </wicket:extend>
 *   </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class BaseServiceAdminPage<T extends ServiceInfo> extends GeoServerSecuredPage {

    protected GeoServerDialog dialog;
    protected List<SerializableConsumer<Void>> onSubmitHooks = new ArrayList<>();

    public BaseServiceAdminPage() {
        this(new PageParameters());
    }

    public BaseServiceAdminPage(PageParameters pageParams) {
        String wsName = pageParams.get("workspace").toString();
        init(new ServiceModel(getServiceClass(), wsName));
    }

    public BaseServiceAdminPage(T service) {
        init(new ServiceModel(service));
    }

    void init(final IModel<T> infoModel) {
        T service = infoModel.getObject();

        dialog = new GeoServerDialog("dialog");
        add(dialog);

        Form form = new Form("form", new CompoundPropertyModel(infoModel));
        add(form);

        if (service.getWorkspace() == null) {
            // create the panel that has the drop down list to switch between workspace
            form.add(new GlobalWorkspacePanel("workspace"));
        } else {
            // create just a panel with a label that signifies the workspace
            form.add(new LocalWorkspacePanel("workspace", service));
        }

        form.add(new HelpLink("workspaceHelp").setDialog(dialog));

        form.add(
                new Label(
                        "service.enabled",
                        new StringResourceModel("service.enabled", this)
                                .setParameters(getServiceName())));
        form.add(new TextField("maintainer"));
        TextField onlineResource = new TextField("onlineResource");

        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);

        // AF: Disable Binding if GeoServer Env Parametrization is enabled!
        if (gsEnvironment == null || !GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
            onlineResource.add(new UrlValidator());
        }

        form.add(onlineResource);
        form.add(new CheckBox("enabled"));
        form.add(new CheckBox("citeCompliant"));
        form.add(new TextField("title"));
        form.add(new TextArea("abstract"));
        form.add(
                new KeywordsEditor(
                        "keywords",
                        LiveCollectionModel.list(new PropertyModel(infoModel, "keywords"))));
        form.add(new TextField("fees"));
        form.add(new TextField("accessConstraints"));

        build(infoModel, form);

        // add the extension panels
        ListView extensionPanels = createExtensionPanelList("extensions", infoModel);
        extensionPanels.setReuseItems(true);
        form.add(extensionPanels);

        SubmitLink submit =
                new SubmitLink("submit", new StringResourceModel("save", (Component) null, null)) {
                    @Override
                    public void onSubmit() {
                        try {
                            handleSubmit((T) infoModel.getObject());
                            // execute all submit hooks
                            onSubmitHooks.forEach(
                                    x -> {
                                        x.accept(null);
                                    });
                            doReturn();
                        } catch (IllegalArgumentException ex) {
                            error(ex.getMessage());
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                };
        form.add(submit);

        Button cancel =
                new Button("cancel") {
                    public void onSubmit() {
                        doReturn();
                    }
                };
        form.add(cancel);
        cancel.setDefaultFormProcessing(false);
    }

    protected ListView createExtensionPanelList(String id, final IModel infoModel) {
        List<AdminPagePanelInfo> panels =
                getGeoServerApplication().getBeansOfType(AdminPagePanelInfo.class);
        for (Iterator<AdminPagePanelInfo> it = panels.iterator(); it.hasNext(); ) {
            AdminPagePanelInfo panel = it.next();
            if (!getServiceClass().equals(panel.getServiceClass())) {
                it.remove();
            }
        }

        return new ListView<AdminPagePanelInfo>(id, panels) {

            @Override
            protected void populateItem(ListItem<AdminPagePanelInfo> item) {
                AdminPagePanelInfo info = item.getModelObject();
                try {
                    AdminPagePanel panel =
                            info.getComponentClass()
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
        };
    }

    /**
     * The class of the service.
     *
     * <p>This value is used to obtain a reference to the service info object via {@link
     * GeoServer#getService(Class)}.
     */
    protected abstract Class<T> getServiceClass();

    /**
     * Builds the form for the page.
     *
     * <p>The form uses a {@link CompoundPropertyModel} so in the normal case components do not need
     * a model as its inherited from the parent. This means that component id's should match the
     * info bean property they correspond to.
     *
     * @param info The service info object.
     * @param form The page form.
     */
    protected abstract void build(IModel info, Form form); // {

    // }

    /**
     * Callback for submit.
     *
     * <p>This implementation simply saves the service. Subclasses may extend / override if need be.
     */
    protected void handleSubmit(T info) {
        if (info.getId() != null) {
            getGeoServer().save(info);
        }
        // else means a non attached instance was passed to us, do nothing, up to caller to add it
        // to configuration
    }

    /** The string to use when representing this service to users. Subclasses must override. */
    protected abstract String getServiceName();

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

    class ServiceFilteredWorkspacesModel extends LoadableDetachableModel {

        WorkspacesModel wsModel;

        ServiceFilteredWorkspacesModel(WorkspacesModel wsModel) {
            this.wsModel = wsModel;
        }

        @Override
        protected Object load() {
            Collection<WorkspaceInfo> workspaces = (Collection<WorkspaceInfo>) wsModel.getObject();

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

        public GlobalWorkspacePanel(String id) {
            super(id);

            final DropDownChoice<WorkspaceInfo> wsChoice =
                    new DropDownChoice<WorkspaceInfo>(
                            "workspace",
                            new ServiceFilteredWorkspacesModel(new WorkspacesModel()),
                            new WorkspaceChoiceRenderer());
            wsChoice.setNullValid(true);
            wsChoice.add(
                    new AjaxFormComponentUpdatingBehavior("change") {
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

        public LocalWorkspacePanel(String id, T service) {
            super(id);

            add(new Label("workspace", new PropertyModel(service, "workspace.name")));
        }
    }
}
