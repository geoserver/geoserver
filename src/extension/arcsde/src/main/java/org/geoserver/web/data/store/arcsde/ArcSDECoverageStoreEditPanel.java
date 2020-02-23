/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.arcsde;

import static org.geoserver.web.data.store.arcsde.RasterTableSelectionPanel.TABLE_NAME;
import static org.geotools.arcsde.data.ArcSDEDataStoreFactory.INSTANCE_PARAM;
import static org.geotools.arcsde.data.ArcSDEDataStoreFactory.PASSWORD_PARAM;
import static org.geotools.arcsde.data.ArcSDEDataStoreFactory.PORT_PARAM;
import static org.geotools.arcsde.data.ArcSDEDataStoreFactory.SERVER_PARAM;
import static org.geotools.arcsde.data.ArcSDEDataStoreFactory.USER_PARAM;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.INSTANCE_NAME_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.PASSWORD_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.PORT_NUMBER_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.SERVER_NAME_PARAM_NAME;
import static org.geotools.arcsde.session.ArcSDEConnectionConfig.USER_NAME_PARAM_NAME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.arcsde.data.ArcSDEDataStoreFactory;
import org.geotools.arcsde.raster.gce.ArcSDERasterFormat;
import org.geotools.arcsde.session.ArcSDEConnectionConfig;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;

/**
 * Provides the form components for the arcsde coverage edit page
 *
 * @author Gabriel Roldan
 */
public final class ArcSDECoverageStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 4149587878405421797L;

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.store.arcsde");

    private static final String RESOURCE_KEY_PREFIX =
            ArcSDECoverageStoreEditPanel.class.getSimpleName();

    final FormComponent<String> server;

    final FormComponent<String> port;

    final FormComponent<String> instance;

    final FormComponent<String> user;

    final FormComponent<String> password;

    final FormComponent<String> table;

    public ArcSDECoverageStoreEditPanel(final String componentId, final Form<?> storeEditForm) {
        super(componentId, storeEditForm);

        final IModel<?> model = storeEditForm.getModel();
        setDefaultModel(model);
        final CoverageStoreInfo storeInfo = (CoverageStoreInfo) storeEditForm.getModelObject();
        {
            Map<String, String> existingParameters = parseConnectionParameters(storeInfo);
            Map<String, Serializable> connectionParameters = storeInfo.getConnectionParameters();
            connectionParameters.putAll(existingParameters);
        }

        final IModel<Map<String, Object>> paramsModel =
                new PropertyModel<Map<String, Object>>(model, "connectionParameters");

        addConnectionPrototypePanel(storeInfo);

        // server, port, instance, user, pwd
        server = addTextPanel(paramsModel, SERVER_PARAM);
        port = addTextPanel(paramsModel, PORT_PARAM);
        instance = addTextPanel(paramsModel, INSTANCE_PARAM);
        user = addTextPanel(paramsModel, USER_PARAM);
        password = addPasswordPanel(paramsModel);

        server.setOutputMarkupId(true);
        port.setOutputMarkupId(true);
        instance.setOutputMarkupId(true);
        user.setOutputMarkupId(true);
        password.setOutputMarkupId(true);

        table = addRasterTable(storeInfo, paramsModel);

        /*
         * Listen to form submission and update the model's URL
         */
        storeEditForm.add(
                new IFormValidator() {
                    private static final long serialVersionUID = 1L;

                    public FormComponent<?>[] getDependentFormComponents() {
                        return new FormComponent<?>[] {
                            server, port, instance, user, password, table
                        };
                    }

                    public void validate(final Form<?> form) {
                        CoverageStoreInfo storeInfo = (CoverageStoreInfo) form.getModelObject();
                        final String serverVal = server.getValue();
                        final String portVal = port.getValue();
                        final String instanceVal = instance.getValue();
                        final String userVal = user.getValue();
                        final String passwordVal = password.getValue();
                        final String tableVal = table.getValue();

                        StringBuilder urlb = new StringBuilder("sde://");

                        urlb.append(userVal).append(":").append(passwordVal).append("@");
                        urlb.append(serverVal).append(":").append(portVal).append("/");
                        if (instanceVal != null && instanceVal.trim().length() > 0) {
                            urlb.append(instanceVal);
                        }
                        urlb.append("#").append(tableVal);
                        String coverageUrl = urlb.toString();
                        LOGGER.info("Coverage URL: '" + coverageUrl + "'");
                        storeInfo.setURL(coverageUrl);
                    }
                });
    }

    private FormComponent<String> addRasterTable(
            final CoverageStoreInfo storeInfo, final IModel<Map<String, Object>> paramsModel) {

        // final String resourceKey = RESOURCE_KEY_PREFIX + "." + TABLE_NAME;

        boolean isNew = storeInfo.getId() == null;
        FormComponent<String> tableComponent = addTableNameComponent(paramsModel, isNew);
        return tableComponent;
    }

    /**
     * @return a combobox set up to display the list of available raster tables if the StoreInfo is
     *     new, or a non editable text box if we're editing an existing StoreInfo
     */
    private FormComponent<String> addTableNameComponent(
            final IModel<Map<String, Object>> paramsModel, final boolean isNew) {

        final FormComponent<String> tableNameComponent;
        final String panelId = "tableNamePanel";

        if (isNew) {
            RasterTableSelectionPanel selectionPanel;
            selectionPanel =
                    new RasterTableSelectionPanel(
                            panelId,
                            paramsModel,
                            storeEditForm,
                            server,
                            port,
                            instance,
                            user,
                            password);
            add(selectionPanel);

            DropDownChoice<String> tableDropDown = selectionPanel.getFormComponent();
            tableNameComponent = tableDropDown;
        } else {
            /*
             * We're editing an existing store. Don't allow to change the table name, it could be
             * catastrophic for the Catalog/ResourcePool as ability to get to the coverage is really
             * based on the Store's URL and the CoverageInfo is tied to it
             */
            final IModel<String> paramValue = new MapModel<String>(paramsModel, TABLE_NAME);
            final String resourceKey = RESOURCE_KEY_PREFIX + "." + TABLE_NAME;
            final IModel<String> paramLabelModel = new ResourceModel(resourceKey, TABLE_NAME);
            final boolean required = true;
            TextParamPanel<String> tableNamePanel;
            tableNamePanel =
                    new TextParamPanel<String>(panelId, paramValue, paramLabelModel, required);
            add(tableNamePanel);

            tableNameComponent = tableNamePanel.getFormComponent();
            tableNameComponent.setEnabled(false);

            final String titleKey = resourceKey + ".title";
            ResourceModel titleModel = new ResourceModel(titleKey);
            String title = String.valueOf(titleModel.getObject());

            tableNamePanel.add(AttributeModifier.replace("title", title));
        }

        return tableNameComponent;
    }

    private FormComponent<String> addPasswordPanel(final IModel<Map<String, Object>> paramsModel) {

        final String paramName = PASSWORD_PARAM.key;
        final String resourceKey = RESOURCE_KEY_PREFIX + "." + paramName;

        final PasswordParamPanel pwdPanel =
                new PasswordParamPanel(
                        paramName,
                        new MapModel<String>(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        true);
        add(pwdPanel);

        String defaultTitle = String.valueOf(PASSWORD_PARAM.title);

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        pwdPanel.add(AttributeModifier.replace("title", title));

        return pwdPanel.getFormComponent();
    }

    private FormComponent<String> addTextPanel(
            final IModel<Map<String, Object>> paramsModel, final Param param) {

        final String paramName = param.key;
        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final boolean required = param.required;

        final TextParamPanel<String> textParamPanel =
                new TextParamPanel<String>(
                        paramName,
                        new MapModel<String>(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        required);
        textParamPanel.getFormComponent().setType(param.type);

        String defaultTitle = String.valueOf(param.title);

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    private void addConnectionPrototypePanel(final CoverageStoreInfo storeInfo) {

        final String resourceKey = RESOURCE_KEY_PREFIX + ".prototype";
        Label label = new Label("prototypeLabel", new ResourceModel(resourceKey));
        final String title = String.valueOf(new ResourceModel(resourceKey + ".title").getObject());
        final AttributeModifier titleSetter = AttributeModifier.replace("title", title);
        label.add(titleSetter);
        add(label);

        final DropDownChoice<StoreInfo> existingArcSDECoverages;
        existingArcSDECoverages =
                new DropDownChoice<>(
                        "connectionPrototype",
                        new Model<StoreInfo>(),
                        new ArcSDEStoreListModel(),
                        new ArcSDEStoreListChoiceRenderer());

        existingArcSDECoverages.add(titleSetter);
        add(existingArcSDECoverages);

        existingArcSDECoverages.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final String storeId = existingArcSDECoverages.getValue();
                        final List<StoreInfo> choices =
                                (List<StoreInfo>) existingArcSDECoverages.getChoices();
                        for (StoreInfo store : choices) {
                            if (store.getId().equals(storeId)) {
                                Map<String, String> connParams = parseConnectionParameters(store);
                                server.setModelObject(connParams.get(SERVER_NAME_PARAM_NAME));
                                port.setModelObject(connParams.get(PORT_NUMBER_PARAM_NAME));
                                instance.setModelObject(connParams.get(INSTANCE_NAME_PARAM_NAME));
                                user.setModelObject(connParams.get(USER_NAME_PARAM_NAME));
                                password.setModelObject(connParams.get(PASSWORD_PARAM_NAME));

                                target.add(server);
                                target.add(port);
                                target.add(instance);
                                target.add(user);
                                target.add(password);
                                break;
                            }
                        }
                    }
                });
    }

    final class ArcSDEStoreListModel extends LoadableDetachableModel<List<StoreInfo>> {
        private static final long serialVersionUID = 1L;

        @Override
        protected List<StoreInfo> load() {
            final StoreInfo storeInfo = (StoreInfo) getDefaultModelObject();
            final Catalog catalog = storeInfo.getCatalog();

            List<StoreInfo> stores = catalog.getStores(StoreInfo.class);
            stores = new ArrayList<StoreInfo>(stores);

            {
                final String arcsdeCoverageType = ArcSDERasterFormat.getInstance().getName();
                final String arcsdeVectorType = new ArcSDEDataStoreFactory().getDisplayName();

                StoreInfo store;
                String type;
                for (Iterator<StoreInfo> it = stores.iterator(); it.hasNext(); ) {
                    store = it.next();
                    type = store.getType();
                    if (arcsdeCoverageType.equals(type) || arcsdeVectorType.equals(type)) {
                        continue;
                    } else {
                        it.remove();
                    }
                }
            }

            Collections.sort(
                    stores,
                    new Comparator<StoreInfo>() {
                        public int compare(StoreInfo o1, StoreInfo o2) {
                            if (o1.getWorkspace().equals(o2.getWorkspace())) {
                                return String.CASE_INSENSITIVE_ORDER.compare(
                                        o1.getName(), o2.getName());
                            }
                            return String.CASE_INSENSITIVE_ORDER.compare(
                                    o1.getWorkspace().getName(), o2.getWorkspace().getName());
                        }
                    });
            return stores;
        }
    }

    static final class ArcSDEStoreListChoiceRenderer extends ChoiceRenderer<Object> {
        private static final long serialVersionUID = 1L;

        public Object getDisplayValue(final Object store) {
            StoreInfo info = (StoreInfo) store;
            return new StringBuilder(info.getWorkspace().getName())
                    .append(':')
                    .append(info.getName());
        }

        public String getIdValue(final Object store, final int index) {
            return ((StoreInfo) store).getId();
        }
    }

    private Map<String, String> parseConnectionParameters(final StoreInfo storeInfo) {

        Map<String, String> params = new HashMap<String, String>();

        if (storeInfo instanceof CoverageStoreInfo) {
            String url = ((CoverageStoreInfo) storeInfo).getURL();

            if (null != url && url.startsWith("sde:")) {
                ArcSDEConnectionConfig connectionConfig;
                connectionConfig =
                        ArcSDERasterFormat.sdeURLToConnectionConfig(new StringBuffer(url));
                params.put(SERVER_NAME_PARAM_NAME, connectionConfig.getServerName());
                params.put(PORT_NUMBER_PARAM_NAME, connectionConfig.getPortNumber().toString());
                params.put(INSTANCE_NAME_PARAM_NAME, connectionConfig.getDatabaseName());
                params.put(USER_NAME_PARAM_NAME, connectionConfig.getUserName());
                params.put(PASSWORD_PARAM_NAME, connectionConfig.getPassword());

                // parse table name
                int idx = url.lastIndexOf('#');
                if (idx > 0) {
                    String tableName = url.substring(idx + 1);
                    params.put(RasterTableSelectionPanel.TABLE_NAME, tableName);
                }
            } else {
                params.put(PORT_NUMBER_PARAM_NAME, "5151");
            }
        } else {
            Map<String, Serializable> storeParams =
                    ((DataStoreInfo) storeInfo).getConnectionParameters();
            params.put(SERVER_NAME_PARAM_NAME, (String) storeParams.get(SERVER_NAME_PARAM_NAME));
            params.put(
                    PORT_NUMBER_PARAM_NAME,
                    String.valueOf(storeParams.get(PORT_NUMBER_PARAM_NAME)));
            params.put(
                    INSTANCE_NAME_PARAM_NAME, (String) storeParams.get(INSTANCE_NAME_PARAM_NAME));
            params.put(USER_NAME_PARAM_NAME, (String) storeParams.get(USER_NAME_PARAM_NAME));
            params.put(PASSWORD_PARAM_NAME, (String) storeParams.get(PASSWORD_PARAM_NAME));
        }
        return params;
    }
}
