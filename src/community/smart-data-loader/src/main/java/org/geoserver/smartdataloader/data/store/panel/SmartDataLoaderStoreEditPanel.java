/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.smartdataloader.data.JDBCDataStoreFactoryFinder;
import org.geoserver.smartdataloader.data.SmartDataLoaderDataAccessFactory;
import org.geoserver.smartdataloader.data.store.NestedTreeDomainModelVisitor;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataFactory;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadataConfig;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;

/** Implementation od StoreEditPanel for PostgisSmartAppSchemaDataAccessFactory. */
@SuppressWarnings("serial")
public class SmartDataLoaderStoreEditPanel extends StoreEditPanel {

    // resources
    private Model<DataStoreSummmary> datastoreModel;
    private ParamResourceModel rootentitiesResource =
            new ParamResourceModel("PostGisSmartAppSchemaStoreEditPanel.rootentities", this);
    private ParamResourceModel domainmodelResource =
            new ParamResourceModel("PostGisSmartAppSchemaStoreEditPanel.domainmodel", this);
    private ParamResourceModel exclusionsResource =
            new ParamResourceModel("PostGisSmartAppSchemaStoreEditPanel.exclusions", this);
    private ParamResourceModel datastorenameResource =
            new ParamResourceModel("PostGisSmartAppSchemaStoreEditPanel.datastorename", this);

    // view components
    private NestedTreePanel domainModelTree;
    private DropDownChoice<DataStoreSummmary> datastores;
    private SimpleDropDownChoiceParamPanel availableRootEntities;
    private WorkspacePanel workspacePanel;

    @SuppressWarnings("rawtypes")
    private TextParamPanel datastoreNamePanel;

    @SuppressWarnings("rawtypes")
    private TextParamPanel exclusions;

    @SuppressWarnings("rawtypes")
    private TextParamPanel datastorename;

    // models
    private DataStoreInfo smartAppSchemaDataStoreInfo;

    @SuppressWarnings("rawtypes")
    private final IModel model;

    // internal use
    private String selectedPostgisDataStoreId = "";
    private String selectedRootEntityName = "";
    private String excludedObjectCodesList = "";

    @SuppressWarnings("unused")
    private String selectedWorkspaceName = "";

    public SmartDataLoaderStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
        model = storeEditForm.getModel();
        setDefaultModel(model);
        smartAppSchemaDataStoreInfo = ((DataStoreInfo) storeEditForm.getModel().getObject());
        // set helper variables
        selectedPostgisDataStoreId =
                getDataStoreInfoParam(SmartDataLoaderDataAccessFactory.DATASTORE_METADATA.key);
        selectedRootEntityName =
                getDataStoreInfoParam(SmartDataLoaderDataAccessFactory.ROOT_ENTITY.key);
        excludedObjectCodesList =
                getDataStoreInfoParam(SmartDataLoaderDataAccessFactory.DOMAIN_MODEL_EXCLUSIONS.key);
        // build connection parameters panel
        buildPostgisDropDownPanel();
        // build rootentity selector panel
        buildRootEntitySelectionPanel(model);
        // build entities, attributes and relations selector panel
        buildDomainModelTreePanel(model);
        // build exclusions panel (it's hidden)
        buildHiddenParametersPanel(model);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        // search for the workspace panel
        workspacePanel = (WorkspacePanel) getPage().get("dataStoreForm:workspacePanel");
        // attach behavior on form component selector, so we can filter list of available postgis
        // related datastores
        workspacePanel
                .getFormComponent()
                .add(
                        new AjaxFormComponentUpdatingBehavior("change") {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                WorkspaceInfo wi =
                                        ((WorkspaceInfo)
                                                workspacePanel.getFormComponent().getModelObject());
                                selectedWorkspaceName =
                                        ((WorkspaceInfo)
                                                        workspacePanel
                                                                .getFormComponent()
                                                                .getModelObject())
                                                .getName();
                                List<DataStoreSummmary> list = getPostgisDataStores(wi);
                                datastores.setChoices(list);
                                availableRootEntities
                                        .getFormComponent()
                                        .setChoices(Collections.EMPTY_LIST);
                                selectedRootEntityName = "";
                                // clear list of exclusions
                                excludedObjectCodesList = "";
                                exclusions.modelChanging();
                                smartAppSchemaDataStoreInfo
                                        .getConnectionParameters()
                                        .put(
                                                SmartDataLoaderDataAccessFactory
                                                        .DOMAIN_MODEL_EXCLUSIONS
                                                        .key,
                                                excludedObjectCodesList);
                                exclusions.modelChanged();
                                // rebuild tree
                                IModel iModel = new PropertyModel(model, "connectionParameters");
                                buildDomainModelTreePanel(iModel);
                                target.add(domainModelTree);
                                target.add(availableRootEntities);
                                target.add(datastores);
                            }
                        });

        // search for the datastorename panel
        datastoreNamePanel = (TextParamPanel) getPage().get("dataStoreForm:dataStoreNamePanel");
        // attach datastore name to hidden text component used to share datastorename with the
        // dataaccessfactory
        datastoreNamePanel
                .getFormComponent()
                .add(
                        new AjaxFormComponentUpdatingBehavior("change") {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                String name =
                                        ((String)
                                                datastoreNamePanel
                                                        .getFormComponent()
                                                        .getModelObject());
                                datastorename.modelChanging();
                                smartAppSchemaDataStoreInfo
                                        .getConnectionParameters()
                                        .put(
                                                SmartDataLoaderDataAccessFactory.DATASTORE_NAME.key,
                                                name);
                                datastorename.modelChanged();
                                target.add(datastorename);
                            }
                        });
    }

    /** Helper method that creates dropdown for postgis datastore selection. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void buildPostgisDropDownPanel() {
        List<DataStoreSummmary> postgisDSs = getPostgisDataStores(getWorkspaceInfo());

        if (datastoreModel == null
                && selectedPostgisDataStoreId != null
                && !selectedPostgisDataStoreId.isEmpty()) {
            this.datastoreModel =
                    new Model<>(
                            new DataStoreSummmary(
                                    getCatalog().getDataStore(selectedPostgisDataStoreId)));
        } else if (datastoreModel == null) {
            this.datastoreModel = new Model<>(new DataStoreSummmary());
        }
        datastores =
                new DropDownChoice<>(
                        "postgisdatastore", datastoreModel, postgisDSs, new StoreRenderer());
        datastores.setRequired(true);
        datastores.add(
                new AjaxFormComponentUpdatingBehavior("click") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        selectedPostgisDataStoreId =
                                datastores.getModelObject() != null
                                        ? datastores.getModelObject().getId()
                                        : null;
                        DataStoreInfo postgisDS = null;
                        if (selectedPostgisDataStoreId != null
                                && !selectedPostgisDataStoreId.isEmpty()) {
                            postgisDS = getCatalog().getDataStore(selectedPostgisDataStoreId);
                        }
                        smartAppSchemaDataStoreInfo
                                .getConnectionParameters()
                                .put(
                                        SmartDataLoaderDataAccessFactory.DATASTORE_METADATA.key,
                                        selectedPostgisDataStoreId);
                        List<String> list = new ArrayList<>();
                        if (postgisDS != null) {
                            list = getAvailableRootEntities(postgisDS);
                        }
                        availableRootEntities.getFormComponent().setChoices(list);
                        target.add(availableRootEntities);
                    }
                });
        datastores.setOutputMarkupId(true);
        Label dataStoreLabel = new Label("dataStoreName", "Data store name *");
        add(dataStoreLabel);
        add(datastores);
    }

    private List<DataStoreSummmary> getPostgisDataStores(WorkspaceInfo wi) {
        List<DataStoreSummmary> postgisDSs = new ArrayList<>();
        List<DataStoreInfo> dsList = getCatalog().getDataStoresByWorkspace(wi);
        // need to keep only those related to postgis
        for (DataStoreInfo ds : dsList) {
            Serializable type = ds.getConnectionParameters().get("dbtype");
            String dbType = type != null ? type.toString().toUpperCase() : null;
            List<JDBCDataStoreFactoryFinder.SupportedStoreType> supportedStoreTypes =
                    Arrays.asList(JDBCDataStoreFactoryFinder.SupportedStoreType.values());
            boolean isSupported =
                    dbType != null
                            && supportedStoreTypes.stream()
                                    .anyMatch(st -> dbType.toUpperCase().contains(st.name()));
            if (isSupported) {
                postgisDSs.add(new DataStoreSummmary(ds));
            }
        }
        return postgisDSs;
    }

    /** Helper method that creates the rootentity selector dropdown. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void buildRootEntitySelectionPanel(final IModel model) {
        IModel iModel = new PropertyModel(model, "connectionParameters");
        List<String> list = new ArrayList<>();
        DataStoreSummmary postgisSummary = datastores.getModel().getObject();
        if (postgisSummary != null && postgisSummary.getId() != null) {
            DataStoreInfo postgisDS = getCatalog().getDataStore(postgisSummary.getId());
            list = getAvailableRootEntities(postgisDS);
        }
        availableRootEntities =
                new SimpleDropDownChoiceParamPanel(
                        "rootentities",
                        new MapModel(iModel, SmartDataLoaderDataAccessFactory.ROOT_ENTITY.key),
                        rootentitiesResource,
                        list,
                        true);
        availableRootEntities
                .getFormComponent()
                .add(
                        new AjaxFormComponentUpdatingBehavior("change") {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                selectedRootEntityName =
                                        (String)
                                                availableRootEntities
                                                        .getFormComponent()
                                                        .getModelObject();
                                // clear list of exclusions
                                excludedObjectCodesList = "";
                                exclusions.modelChanging();
                                smartAppSchemaDataStoreInfo
                                        .getConnectionParameters()
                                        .put(
                                                SmartDataLoaderDataAccessFactory
                                                        .DOMAIN_MODEL_EXCLUSIONS
                                                        .key,
                                                excludedObjectCodesList);
                                exclusions.modelChanged();
                                // rebuild tree
                                buildDomainModelTreePanel(iModel);
                                target.add(domainModelTree);
                            }
                        });
        availableRootEntities.setOutputMarkupId(true);
        add(availableRootEntities);
    }

    /** Helper method that creates the DomainModel tree panel */
    protected void buildDomainModelTreePanel(final IModel model) {
        domainModelTree =
                new NestedTreePanel("domainmodel", null, domainmodelResource, null, false);
        domainModelTree.setOutputMarkupId(true);
        addOrReplace(domainModelTree);
        // avoid loading tree if rootentity was not selected
        if (selectedRootEntityName != null && !selectedRootEntityName.isEmpty()) {
            DataStoreInfo postgisDS = getCatalog().getDataStore(selectedPostgisDataStoreId);
            if (postgisDS != null) {
                // build DomainModel based on parameters
                DataStoreMetadata dsm = getDataStoreMetadata(postgisDS);
                DomainModelConfig dmc = new DomainModelConfig();
                dmc.setRootEntityName(selectedRootEntityName);
                DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
                DomainModel dm = dmb.buildDomainModel();
                // visit DomainModel with NestedTree
                NestedTreeDomainModelVisitor dmv = new NestedTreeDomainModelVisitor();
                dm.accept(dmv);
                // get the NestedTree representation
                DefaultTreeModel dtm = dmv.getTreeModel();
                // build treePanel with DomainModel and list of checkedNodes
                Set<DefaultMutableTreeNode> nodes = getNodes(dtm);
                Set<DefaultMutableTreeNode> checkedNodes = getCheckedNodes(dtm);
                domainModelTree.buildTree(dtm, checkedNodes);
                domainModelTree.add(
                        new AjaxEventBehavior(("click")) {
                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                // build list of exclusions based on tree selection
                                StringBuilder stringBuilder = new StringBuilder();
                                for (DefaultMutableTreeNode node : nodes) {
                                    if (!checkedNodes.contains(node)) {
                                        if (node.getParent() != null) {
                                            stringBuilder.append(
                                                    node.getParent().toString()
                                                            + "."
                                                            + node.toString());
                                        } else {
                                            stringBuilder.append(node.toString());
                                        }
                                        stringBuilder.append(",");
                                    }
                                }
                                String exclusionList = stringBuilder.toString();
                                int size = exclusionList.length();
                                String fullExclusionList = "";
                                if (size > 0) {
                                    fullExclusionList = exclusionList.substring(0, size - 1);
                                }
                                // set exclusionList value to exclusionsPanel (model)
                                exclusions.getFormComponent().modelChanging();
                                smartAppSchemaDataStoreInfo
                                        .getConnectionParameters()
                                        .put(
                                                SmartDataLoaderDataAccessFactory
                                                        .DOMAIN_MODEL_EXCLUSIONS
                                                        .key,
                                                fullExclusionList);
                                exclusions.getFormComponent().modelChanged();
                                target.add(exclusions);
                            }
                        });
            }
        }
    }

    /**
     * Helper method that creates a hidden parameters panel, that allows to set the exclusion
     * DomainModel objects (used internally in the form to keep list of exclusions based on tree
     * selection) and the datastore name that will be shared to the factory.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void buildHiddenParametersPanel(final IModel model) {
        IModel iModel = new PropertyModel(model, "connectionParameters");
        exclusions =
                new TextParamPanel(
                        "exclusions",
                        new MapModel(
                                iModel,
                                SmartDataLoaderDataAccessFactory.DOMAIN_MODEL_EXCLUSIONS.key),
                        exclusionsResource,
                        false);
        exclusions.setOutputMarkupId(true);
        exclusions.getFormComponent().setEnabled(false);
        exclusions.setVisible(false);
        add(exclusions);

        iModel = new PropertyModel(model, "connectionParameters");
        datastorename =
                new TextParamPanel(
                        "datastorename",
                        new MapModel(iModel, SmartDataLoaderDataAccessFactory.DATASTORE_NAME.key),
                        datastorenameResource,
                        false);
        datastorename.setOutputMarkupId(true);
        datastorename.getFormComponent().setEnabled(false);
        datastorename.setVisible(false);
        add(datastorename);
    }

    /**
     * Helper that includes all the detected nodes in the DomainModel and uncheck those listed in
     * the exclusion list
     */
    private Set<DefaultMutableTreeNode> getCheckedNodes(DefaultTreeModel dtm) {
        // get all nodes
        final Set<DefaultMutableTreeNode> allNodes = getNodes(dtm);
        // create empty set and add those that are in checked
        final Set<DefaultMutableTreeNode> checkedNodes = new HashSet<>();
        String[] elements = {};
        if (excludedObjectCodesList != null) {
            elements = excludedObjectCodesList.split(",");
        }
        List<String> excludedObjectsList = Arrays.asList(elements);
        for (DefaultMutableTreeNode node : allNodes) {
            StringBuilder name = new StringBuilder();
            if (node.getParent() != null) {
                name.append(node.getParent().toString() + "." + node.toString());
            } else {
                name.append(node.toString());
            }
            if (!excludedObjectsList.contains(name.toString())) {
                checkedNodes.add(node);
            }
        }
        return checkedNodes;
    }

    /**
     * Helper method to get the list of all the available entities that can be defined as root
     * entity for a DomainModel.
     */
    private List<String> getAvailableRootEntities(DataStoreInfo ds) {
        DataStoreMetadata dsm = this.getDataStoreMetadata(ds);
        @SuppressWarnings("unchecked")
        List<String> choiceList = new ArrayList<>();
        List<EntityMetadata> entities = dsm.getDataStoreEntities();
        for (EntityMetadata e : entities) {
            String name = e.getName();
            choiceList.add(name);
        }
        return choiceList;
    }

    /** Helper method to get Postgis-related DataStoreMetadata. */
    private DataStoreMetadata getDataStoreMetadata(DataStoreInfo ds) {
        JDBCDataStoreFactory factory =
                new JDBCDataStoreFactoryFinder().getFactoryFromType(ds.getType());
        JDBCDataStore jdbcDataStore = null;
        DataStoreMetadata dsm = null;
        try {
            jdbcDataStore = factory.createDataStore(ds.getConnectionParameters());
            DataStoreMetadataConfig config =
                    new JdbcDataStoreMetadataConfig(
                            jdbcDataStore, ds.getConnectionParameters().get("passwd").toString());
            dsm = (new DataStoreMetadataFactory()).getDataStoreMetadata(config);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving metadata from DB.");
        }
        jdbcDataStore.dispose();
        return dsm;
    }

    /** Helper that includes all the detected nodes in the DomainModel */
    private Set<DefaultMutableTreeNode> getNodes(DefaultTreeModel dtm) {
        final Set<DefaultMutableTreeNode> nodes = new HashSet<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) dtm.getRoot();
        convertTreeToSet(root, nodes);
        return nodes;
    }

    /** Helper method that allows to get the related workspace based on the form datastore model. */
    private WorkspaceInfo getWorkspaceInfo() {
        DataStoreInfo dsi = ((DataStoreInfo) storeEditForm.getModel().getObject());
        WorkspaceInfo wi = dsi.getWorkspace();
        return wi;
    }

    /** Helper method that returns a form datastore model parameter value based on the param key. */
    private String getDataStoreInfoParam(String key) {
        String param = (String) smartAppSchemaDataStoreInfo.getConnectionParameters().get(key);
        return param;
    }

    /**
     * Helped method that return list of nodes based on a TreeNode (recursively get full list of
     * nodes)
     */
    private static void convertTreeToSet(
            DefaultMutableTreeNode aNode, Set<DefaultMutableTreeNode> nodes) {
        for (int i = 0; i < aNode.getChildCount(); i++) {
            convertTreeToSet((DefaultMutableTreeNode) aNode.getChildAt(i), nodes);
        }
        nodes.add(aNode);
    }

    public static class DataStoreSummmary implements Serializable {

        private String name;
        private String id;

        public DataStoreSummmary() {}

        public DataStoreSummmary(DataStoreInfo ds) {
            this.name = ds.getName();
            this.id = ds.getId();
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }
    }

    public static class StoreRenderer extends ChoiceRenderer<DataStoreSummmary> {
        @Override
        public Object getDisplayValue(DataStoreSummmary object) {
            return object.getName();
        }

        @Override
        public String getIdValue(DataStoreSummmary object, int index) {
            return object.getId();
        }
    }
}
