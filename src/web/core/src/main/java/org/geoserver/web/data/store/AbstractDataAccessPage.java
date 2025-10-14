/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.NamespacePanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;

/**
 * Abstract base class for adding/editing a {@link DataStoreInfo}, provides the UI components and a template method
 * {@link #onSaveDataStore(Form)} for the subclasses to perform the insertion or update of the object.
 *
 * @author Gabriel Roldan
 * @see DataAccessNewPage
 * @see DataAccessEditPage
 */
// TODO WICKET8 - Verify this page works OK
abstract class AbstractDataAccessPage extends GeoServerSecuredPage {

    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store");

    /**
     * Needed as an instance variable so if the DataAccess has a namespace parameter, it is automatically updated to
     * match the workspace's namespace as per GEOS-3149 until the resource/publish split is finalized
     */
    protected WorkspacePanel workspacePanel;

    protected StoreEditPanel storeEditPanel;

    public AbstractDataAccessPage() {}

    /** */
    protected void initUI(final DataStoreInfo storeInfo) throws IllegalArgumentException {

        if (storeInfo.getWorkspace() == null) {
            throw new IllegalArgumentException("Workspace not provided");
        }

        final Catalog catalog = getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        DataAccessFactory dsFactory;
        try {
            dsFactory = resourcePool.getDataStoreFactory(storeInfo);
        } catch (IOException e) {
            String msg = new ResourceModel("AbstractDataAccessPage.cantGetDataStoreFactory").getObject();
            msg += ": " + e.getMessage();
            throw new IllegalArgumentException(msg);
        }
        if (dsFactory == null) {
            String msg = new ResourceModel("AbstractDataAccessPage.cantGetDataStoreFactory").getObject();
            throw new IllegalArgumentException(msg);
        }

        final IModel<DataStoreInfo> model = new CompoundPropertyModel<>(storeInfo);

        final Form<DataStoreInfo> paramsForm = new Form<>("dataStoreForm", model);
        add(paramsForm);

        paramsForm.add(new Label("storeType", dsFactory.getDisplayName()));
        paramsForm.add(new Label("storeTypeDescription", dsFactory.getDescription()));

        workspacePanel = new WorkspacePanel(
                "workspacePanel",
                new PropertyModel<>(model, "workspace"),
                new ResourceModel("workspace", "Workspace"),
                true);
        paramsForm.add(workspacePanel);

        final TextParamPanel<String> dataStoreNamePanel = new TextParamPanel<>(
                "dataStoreNamePanel",
                new PropertyModel<>(model, "name"),
                new ResourceModel("AbstractDataAccessPage.dataSrcName", "Data Source Name"),
                true);
        paramsForm.add(dataStoreNamePanel);

        paramsForm.add(new TextParamPanel<>(
                "dataStoreDescriptionPanel",
                new PropertyModel<>(model, "description"),
                new ResourceModel("AbstractDataAccessPage.description", "Description"),
                false));

        paramsForm.add(new CheckBoxParamPanel(
                "dataStoreEnabledPanel",
                new PropertyModel<>(model, "enabled"),
                new ResourceModel("enabled", "Enabled")));

        paramsForm.add(new CheckBoxParamPanel(
                "disableOnConnFailurePanel",
                new PropertyModel<>(model, "disableOnConnFailure"),
                new ResourceModel("AbstractDataAccessPage.disableOnConnFailure", "Autodisable on connection failure")));

        {
            /*
             * Here's where the extension point is applied in order to give extensions a chance to
             * provide custom behavior/components for the coverage form other than the default
             * single "url" input field
             */
            GeoServerApplication app = getGeoServerApplication();
            storeEditPanel = StoreExtensionPoints.getStoreEditPanel("parametersPanel", paramsForm, storeInfo, app);
        }
        paramsForm.add(storeEditPanel);

        paramsForm.add(new FeedbackPanel("feedback"));

        // validate the selected workspace does not already contain a store with the same name
        final String dataStoreInfoId = storeInfo.getId();
        StoreNameValidator storeNameValidator = new StoreNameValidator(
                workspacePanel.getFormComponent(), dataStoreNamePanel.getFormComponent(), dataStoreInfoId);
        paramsForm.add(storeNameValidator);

        paramsForm.add(new BookmarkablePageLink<>("cancel", StorePage.class));

        paramsForm.add(new AjaxSubmitLink("save", paramsForm) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                try {
                    DataStoreInfo dataStore = (DataStoreInfo) getForm().getModelObject();
                    onSaveDataStore(dataStore, target, true);
                } catch (IllegalArgumentException e) {
                    paramsForm.error(e.getMessage());
                    target.add(paramsForm);
                }
            }
        });

        paramsForm.add(applyLink(paramsForm));

        // save the namespace panel as an instance variable. Needed as per GEOS-3149
        makeNamespaceSyncUpWithWorkspace(paramsForm);
    }

    private GeoserverAjaxSubmitLink applyLink(Form paramsForm) {
        return new GeoserverAjaxSubmitLink("apply", paramsForm, this) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(paramsForm);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target) {
                try {
                    DataStoreInfo info = (DataStoreInfo) getForm().getModelObject();
                    onSaveDataStore(info, target, false);
                } catch (IllegalArgumentException e) {
                    paramsForm.error(e.getMessage());
                    target.add(paramsForm);
                }
            }
        };
    }

    /**
     * Call back method called when the save button is hit. Subclasses shall override in order to perform the action
     * over the catalog, whether it is adding a new {@link DataStoreInfo} or saving the edits to an existing onefinal
     * StoreEditPanel
     *
     * @param info the object to save
     * @throws IllegalArgumentException with an appropriate message for the user if the operation failed
     */
    protected abstract void onSaveDataStore(final DataStoreInfo info, AjaxRequestTarget requestTarget, boolean doReturn)
            throws IllegalArgumentException;

    /**
     * Make the {@link #namespacePanel} model to synch up with the workspace whenever the {@link #workspacePanel} option
     * changes.
     *
     * <p>This is so to maintain namespaces in synch with workspace while the resource/publish split is not finalized,
     * as per GEOS-3149.
     *
     * <p>Removing this method and the call to it on {@link #getInputComponent(String, IModel, ParamInfo)} is all that's
     * needed to let the namespace be selectable independently of the workspace once the resource/publish split is done.
     */
    private void makeNamespaceSyncUpWithWorkspace(final Form paramsForm) {

        // do not allow the namespace choice to be manually changed
        final DropDownChoice wsDropDown = (DropDownChoice) workspacePanel.getFormComponent();
        // add an ajax onchange behaviour that keeps ws and ns in synch
        wsDropDown.add(new OnChangeAjaxBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            private NamespaceParamModel namespaceModel;
            private NamespacePanel namespacePanel;
            private boolean namespaceLookupOccurred;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // see if the namespace param is tied to a NamespacePanel and save it
                if (!namespaceLookupOccurred) {
                    // search for the panel
                    Component paramsPanel = AbstractDataAccessPage.this.get("dataStoreForm:parametersPanel");
                    namespacePanel = findNamespacePanel((MarkupContainer) paramsPanel);

                    // if the panel is not there search for the parameter and build a model
                    // around it
                    if (namespacePanel == null) {
                        final IModel model = paramsForm.getModel();
                        final DataStoreInfo info = (DataStoreInfo) model.getObject();
                        final Catalog catalog = getCatalog();
                        final ResourcePool resourcePool = catalog.getResourcePool();
                        DataAccessFactory dsFactory;
                        try {
                            dsFactory = resourcePool.getDataStoreFactory(info);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        final Param[] dsParams = dsFactory.getParametersInfo();
                        for (Param p : dsParams) {
                            if ("namespace".equals(p.getName())) {
                                final IModel<Map<String, Serializable>> paramsModel =
                                        new PropertyModel<>(model, "connectionParameters");
                                namespaceModel = new NamespaceParamModel(paramsModel, "namespace");
                                break;
                            }
                        }
                    }
                    namespaceLookupOccurred = true;
                }

                // get the namespace
                WorkspaceInfo ws = (WorkspaceInfo) wsDropDown.getModelObject();
                String prefix = ws.getName();
                NamespaceInfo namespaceInfo = getCatalog().getNamespaceByPrefix(prefix);
                if (namespacePanel != null) {
                    // update the GUI
                    namespacePanel.setDefaultModelObject(namespaceInfo);
                    target.add(namespacePanel.getFormComponent());
                } else if (namespaceModel != null) {
                    // update the model directly
                    namespaceModel.setObject(namespaceInfo);
                    // target.add(AbstractDataAccessPage.this);
                }
            }
        });
    }

    private NamespacePanel findNamespacePanel(MarkupContainer c) {
        Component child;
        for (Component component : c) {
            child = component;
            if (child instanceof NamespacePanel panel1) {
                return panel1;
            } else if (child instanceof MarkupContainer container) {
                NamespacePanel panel = findNamespacePanel(container);
                if (panel != null) {
                    return panel;
                }
            }
        }
        return null;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
