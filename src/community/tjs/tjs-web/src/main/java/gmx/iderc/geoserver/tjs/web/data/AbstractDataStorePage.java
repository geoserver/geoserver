/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract base class for adding/editing a {@link DataStoreInfo}, provides the UI components and a
 * template method {@link #onSaveDataStore(Form)} for the subclasses to perform the insertion or
 * update of the object.
 *
 * @author Gabriel Roldan
 * @see DataAccessNewPage
 * @see DataAccessEditPage
 */
abstract class AbstractDataStorePage extends TJSBasePage {

    protected static final Logger LOGGER = Logging.getLogger("gmx.iderc.geoserver.tjs.web.data.datastore");

    /**
     * Needed as an instance variable so if the DataAccess has a namespace parameter, it is
     * automatically updated to match the workspace's namespace as per GEOS-3149 until the
     * resource/publish split is finalized
     */
//    protected WorkspacePanel workspacePanel;
//    private NamespacePanel namespacePanel;
    public AbstractDataStorePage() {

    }

    /**
     * @param storeInfo
     * @throws IllegalArgumentException
     */
    protected void initUI(final DataStoreInfo storeInfo) throws IllegalArgumentException {

//        if (storeInfo.getWorkspace() == null) {
//            throw new IllegalArgumentException("Workspace not provided");
//        }

        final TJSCatalog catalog = getTJSCatalog();
//        final ResourcePool resourcePool = catalog.getResourcePool();
        TJSDataAccessFactory dsFactory;
        try {
            dsFactory = catalog.getDataStoreFactory(storeInfo.getType());
        } catch (Exception e) {
            String msg = (String) new ResourceModel(
                                                           "AbstractDataStorePage.cantGetDataStoreFactory").getObject();
            msg += ": " + e.getMessage();
            throw new IllegalArgumentException(msg);
        }
        if (dsFactory == null) {
            String msg = (String) new ResourceModel(
                                                           "AbstractDataStorePage.cantGetDataStoreFactory").getObject();
            throw new IllegalArgumentException(msg);
        }

        final IModel model = new CompoundPropertyModel(storeInfo);

        final Form paramsForm = new Form("dataStoreForm", model);
        add(paramsForm);

        paramsForm.add(new Label("storeType", dsFactory.getDisplayName()));
        paramsForm.add(new Label("storeTypeDescription", dsFactory.getDescription()));

//        {
//            final IModel wsModel = new PropertyModel(model, "workspace");
//            final IModel wsLabelModel = new ResourceModel("workspace", "Workspace");
//            workspacePanel = new WorkspacePanel("workspacePanel", wsModel, wsLabelModel, true);
//        }
//        paramsForm.add(workspacePanel);

        final TextParamPanel dataStoreNamePanel;

        dataStoreNamePanel = new TextParamPanel("dataStoreNamePanel", new PropertyModel(model,
                                                                                               "name"),
                                                       new ResourceModel("AbstractDataAccessPage.dataSrcName", "Data Source Name"), true);
        paramsForm.add(dataStoreNamePanel);

        paramsForm.add(new TextParamPanel("dataStoreDescriptionPanel", new PropertyModel(model,
                                                                                                "description"), new ResourceModel("AbstractDataAccessPage.description", "Description"), false,
                                                 (IValidator[]) null));

        paramsForm.add(new CheckBoxParamPanel("dataStoreEnabledPanel", new PropertyModel(model,
                                                                                                "enabled"), new ResourceModel("enabled", "Enabled")));

        final DefaultDataStoreEditPanel storeEditPanel = new DefaultDataStoreEditPanel("parametersPanel", paramsForm);
        paramsForm.add(storeEditPanel);

        paramsForm.add(new FeedbackPanel("feedback"));

        // validate the selected workspace does not already contain a store with the same name
        final String dataStoreInfoId = storeInfo.getId();

//        StoreNameValidator storeNameValidator = new StoreNameValidator(workspacePanel
//                .getFormComponent(), dataStoreNamePanel.getFormComponent(), dataStoreInfoId);
//        paramsForm.add(storeNameValidator);

        paramsForm.add(new BookmarkablePageLink("cancel", StorePage.class));

        paramsForm.add(new AjaxSubmitLink("save", paramsForm) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    DataStoreInfo dataStore = (DataStoreInfo) form.getModelObject();
                    onSaveDataStore(dataStore, target);
                } catch (IllegalArgumentException e) {
                    paramsForm.error(e.getMessage());
                    target.addComponent(paramsForm);
                }
            }
        });
    }

    /**
     * Call back method called when the save button is hit. Subclasses shall override in order to
     * perform the action over the catalog, whether it is adding a new {@link DataStoreInfo} or
     * saving the edits to an existing one
     *
     * @param info          the object to save
     * @param requestTarget
     * @throws IllegalArgumentException with an appropriate message for the user if the operation failed
     */
    protected abstract void onSaveDataStore(final DataStoreInfo info,
                                            AjaxRequestTarget requestTarget) throws IllegalArgumentException;

    /**
     * Make the {@link #namespacePanel} model to synch up with the workspace whenever the
     * {@link #workspacePanel} option changes.
     * <p>
     * This is so to maintain namespaces in synch with workspace while the resource/publish split is
     * not finalized, as per GEOS-3149.
     * </p>
     * <p>
     * Removing this method and the call to it on
     * {@link #getInputComponent(String, IModel, ParamInfo)} is all that's needed to let the
     * namespace be selectable independently of the workspace once the resource/publish split is
     * done.
     * </p>
     */


    protected void clone(final DataStoreInfo source, DataStoreInfo target) {
        TJSDataStore store = source.getTJSDataStore(new NullProgressListener());
        target.setDescription(source.getDescription());
        target.setEnabled(source.getEnabled());
        target.setName(source.getName());
//        target.setWorkspace(source.getWorkspace());
        target.setType(source.getType());
        target.getConnectionParameters().clear();
        Map newparams = store.getDataStoreFactory().filterParamsForSave(source.getConnectionParameters());
        target.getConnectionParameters().putAll(newparams);
    }
}
