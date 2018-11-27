/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.backuprestore.tasklet.AbstractCatalogBackupRestoreTasklet;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.opengis.filter.Filter;

@SuppressWarnings("serial")
public class ResourceFilePanel extends Panel {

    protected static Logger LOGGER = Logging.getLogger(ResourceFilePanel.class);

    private static final String[] FILE_EXTENSIONS =
            new String[] {".zip", ".gz", ".tar", ".tgz", ".bz"};

    String file;
    List<WorkspaceInfo> workspaces;
    Map<String, List<StoreInfo>> stores;
    Map<String, List<LayerInfo>> layers;
    Filter wsFilter;
    Filter siFilter;
    Filter liFilter;

    TextField fileField;
    GeoServerDialog dialog;

    private BackupRestoreDataPage backupRestoreDataPage;

    public ResourceFilePanel(String id, BackupRestoreDataPage container) {
        super(id);

        this.backupRestoreDataPage = container;

        add(dialog = new GeoServerDialog("dialog"));

        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);

        fileField = new TextField("file");
        fileField.setRequired(true);
        fileField.setOutputMarkupId(true);
        fileField.add(
                new OnChangeAjaxBehavior() {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        // Access the updated model value:
                        final String valueAsString =
                                ((TextField<String>) getComponent()).getModelObject();

                        doUpdate(target, valueAsString);
                    }
                });

        form.add(fileField);
        form.add(chooserButton(form));
    }

    /** @return the archive file resource */
    public Resource getResource() {
        return Files.asResource(new File(this.file));
    };

    /** @return the workspaces */
    public List<WorkspaceInfo> getWorkspaces() {
        return workspaces;
    }

    /** @return the stores */
    public Map<String, List<StoreInfo>> getStores() {
        return stores;
    }

    /** @return the layers */
    public Map<String, List<LayerInfo>> getLayers() {
        return layers;
    }

    Component chooserButton(Form form) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("chooser") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        dialog.setTitle(new ParamResourceModel("chooseFile", this));
                        dialog.showOkCancel(
                                target,
                                new GeoServerDialog.DialogDelegate() {

                                    @Override
                                    protected Component getContents(String id) {
                                        // use what the user currently typed
                                        File file = null;
                                        if (!fileField.getInput().trim().equals("")) {
                                            file = new File(fileField.getInput());
                                            if (!file.exists()) file = null;
                                        }

                                        GeoServerFileChooser chooser =
                                                new GeoServerFileChooser(id, new Model(file)) {
                                                    @Override
                                                    protected void fileClicked(
                                                            File file, AjaxRequestTarget target) {
                                                        ResourceFilePanel.this.file =
                                                                file.getAbsolutePath();

                                                        fileField.clearInput();
                                                        fileField.setModelObject(
                                                                file.getAbsolutePath());

                                                        target.add(fileField);
                                                        dialog.close(target);
                                                    }
                                                };

                                        initFileChooser(chooser);
                                        return chooser;
                                    }

                                    @Override
                                    protected boolean onSubmit(
                                            AjaxRequestTarget target, Component contents) {

                                        GeoServerFileChooser chooser =
                                                (GeoServerFileChooser) contents;
                                        file =
                                                ((File) chooser.getDefaultModelObject())
                                                        .getAbsolutePath();

                                        // clear the raw input of the field won't show the new model
                                        // value
                                        fileField.clearInput();
                                        // fileField.setModelObject(file);

                                        target.add(fileField);

                                        return true;
                                    }

                                    @Override
                                    public void onClose(AjaxRequestTarget target) {
                                        // update the field with the user chosen value
                                        target.add(fileField);

                                        doUpdate(target, file);
                                    }
                                });
                    }
                };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        return link;
    }

    SubmitLink submitLink() {
        return new SubmitLink("submit") {

            @Override
            public void onSubmit() {}
        };
    }

    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        fileChooser.setFilter(new Model(new ExtensionFileFilter(FILE_EXTENSIONS)));
    }

    /** @param target */
    private void doUpdate(final AjaxRequestTarget target, final String valueAsString) {
        Catalog catalog = GeoServerApplication.get().getCatalog();

        // use what the user currently typed
        File file = null;
        if (!valueAsString.trim().equals("")) {
            file = new File(valueAsString);
            if (!file.exists() || file.isDirectory()) {
                file = null;
                workspaces = null;
                stores = null;
                layers = null;
            }
        }

        List<WorkspaceInfo> workspaceInfos = new ArrayList<WorkspaceInfo>();
        Map<String, List<StoreInfo>> storeInfos = new HashMap<String, List<StoreInfo>>();
        Map<String, List<LayerInfo>> layerInfos = new HashMap<String, List<LayerInfo>>();
        try {
            if (file != null) {
                Resource archiveFile = Files.asResource(file);
                if (Resources.exists(archiveFile) && archiveFile.getType() != Type.DIRECTORY) {
                    Resource tmpDir =
                            BackupUtils.geoServerTmpDir(
                                    new GeoServerDataDirectory(
                                            GeoServerApplication.get().getResourceLoader()));

                    try {
                        BackupUtils.extractTo(archiveFile, tmpDir);
                        Resource brCatalogIndex =
                                tmpDir.get(AbstractCatalogBackupRestoreTasklet.BR_INDEX_XML);
                        if (Resources.exists(brCatalogIndex)) {
                            SAXBuilder saxBuilder = new SAXBuilder();
                            Document document = saxBuilder.build(brCatalogIndex.in());
                            Element classElement = document.getRootElement();
                            List<Element> workspaceList = classElement.getChildren("Workspace");

                            for (Element ws : workspaceList) {
                                String wsName = ws.getChild("Name").getText();
                                WorkspaceInfo workspace = catalog.getWorkspaceByName(wsName);
                                if (workspace == null) {
                                    workspace = catalog.getFactory().createWorkspace();
                                    workspace.setName(wsName);
                                }
                                workspaceInfos.add(workspace);

                                storeInfos.put(wsName, new ArrayList<StoreInfo>());
                                for (Element st : ws.getChildren("Store")) {
                                    String stName = st.getChild("Name").getText();
                                    String type = st.getAttribute("type").getValue();
                                    StoreInfo store = null;
                                    ResourceInfo resource = null;

                                    if ("DataStoreInfo".equals(type)) {
                                        store = catalog.getDataStoreByName(stName);

                                        if (store == null) {
                                            store = catalog.getFactory().createDataStore();
                                            store.setName(stName);
                                        }
                                    } else if ("CoverageStoreInfo".equals(type)) {
                                        store = catalog.getCoverageStoreByName(stName);

                                        if (store == null) {
                                            store = catalog.getFactory().createCoverageStore();
                                            store.setName(stName);
                                        }
                                    }

                                    if (store != null) {
                                        storeInfos.get(wsName).add(store);

                                        layerInfos.put(stName, new ArrayList<LayerInfo>());
                                        for (Element ly : st.getChildren("Layer")) {
                                            String lyName = ly.getChild("Name").getText();

                                            if ("DataStoreInfo".equals(type)) {
                                                resource = catalog.getFeatureTypeByName(lyName);

                                                if (resource == null) {
                                                    resource =
                                                            catalog.getFactory()
                                                                    .createFeatureType();
                                                    store.setWorkspace(workspace);
                                                    resource.setStore(store);
                                                    resource.setName(lyName);
                                                }
                                            } else if ("CoverageStoreInfo".equals(type)) {
                                                resource = catalog.getCoverageByName(lyName);

                                                if (resource == null) {
                                                    resource =
                                                            catalog.getFactory().createCoverage();
                                                    store.setWorkspace(workspace);
                                                    resource.setStore(store);
                                                    resource.setName(lyName);
                                                }
                                            }

                                            LayerInfo layer = catalog.getLayerByName(lyName);
                                            if (layer == null) {
                                                layer = catalog.getFactory().createLayer();
                                                layer.setResource(resource);
                                                layer.setName(lyName);
                                            }
                                            layerInfos.get(stName).add(layer);
                                        }
                                    }
                                }
                            }
                            workspaces = workspaceInfos;
                            stores = storeInfos;
                            layers = layerInfos;

                            Element filtersList = classElement.getChild("Filters");

                            for (Element filter : filtersList.getChildren("Filter")) {
                                if ("WorkspaceInfo"
                                        .equals(filter.getAttribute("type").getValue())) {
                                    wsFilter = ECQL.toFilter(filter.getChild("ECQL").getText());
                                }
                                if ("StoreInfo".equals(filter.getAttribute("type").getValue())) {
                                    siFilter = ECQL.toFilter(filter.getChild("ECQL").getText());
                                }
                                if ("LayerInfo".equals(filter.getAttribute("type").getValue())) {
                                    liFilter = ECQL.toFilter(filter.getChild("ECQL").getText());
                                }
                            }
                        }
                    } finally {
                        if (tmpDir != null) {
                            tmpDir.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occurred while parsing Backup/Restore Index.", e);
        }

        if (backupRestoreDataPage.get("workspace") != null) {
            backupRestoreDataPage.get("workspace").setDefaultModelObject(null);
            backupRestoreDataPage.get("store").setDefaultModelObject(null);
            backupRestoreDataPage.get("layer").setDefaultModelObject(null);
            target.add(backupRestoreDataPage.get("workspace"));
            target.add(backupRestoreDataPage.get("store"));
            target.add(backupRestoreDataPage.get("layer"));
        }
    }
}
