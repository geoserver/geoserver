/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class PublishedInfoDetachableModel extends LoadableDetachableModel<PublishedInfo> {

    private String id;
    private IModel<String> workspace = Model.of();
    private IModel<String> layer = Model.of();
    private Class<? extends PublishedInfo> type;

    public PublishedInfoDetachableModel() {}

    public PublishedInfoDetachableModel(PublishedInfo info) {
        setObject(info);
    }

    public PublishedInfoDetachableModel(String workspace, String layer) {
        setObject(workspace, layer);
    }

    public PublishedInfoDetachableModel(IModel<String> workspace, IModel<String> layer) {
        setObject(workspace, layer);
    }

    public String getWorkspace() {
        return workspace == null ? null : workspace.getObject();
    }

    public String getLayer() {
        return layer == null ? null : layer.getObject();
    }

    public @Nullable PublishedInfo setObject(String workspace, String layer) {
        setObject(Model.of(workspace), Model.of(layer));
        PublishedInfo info = loadByName();
        setObject(info);
        return info;
    }

    private void setObject(IModel<String> workspace, IModel<String> layer) {
        this.workspace = workspace;
        this.layer = layer;
        this.id = null;
        this.type = null;
    }

    public void setObject(final String layer) {
        this.layer.setObject(layer);
    }

    @Override
    public void setObject(final PublishedInfo info) {
        super.setObject(info);
        if (null != info) {
            WorkspaceInfo ws;
            if (info instanceof LayerGroupInfo groupInfo) {
                type = LayerGroupInfo.class;
                ws = groupInfo.getWorkspace();
            } else if (info instanceof LayerInfo layerInfo) {
                type = LayerInfo.class;
                ws = layerInfo.getResource().getStore().getWorkspace();
            } else {
                throw new IllegalArgumentException("unknown PublishedInfo type " + info);
            }
            id = info.getId();
            layer = Model.of(info.getName());
            workspace = ws == null ? Model.of() : Model.of(ws.getName());
        }
        // log.info("Selected layer changed [ws:{}, layer:{}, id:{}]", workspace, layer, id);
    }

    @Override
    protected PublishedInfo load() {
        PublishedInfo info = getObject();
        if (null != info) return info;
        if (null == id) return loadByName();
        return loadByIdAndType();
    }

    private PublishedInfo loadByIdAndType() {
        Catalog catalog = getRawCatalog();
        if (LayerGroupInfo.class.equals(type)) {
            return catalog.getLayerGroup(id);
        }
        return catalog.getLayer(id);
    }

    private @Nullable PublishedInfo loadByName() {
        PublishedInfo info = null;
        final String workspace = getWorkspace();
        final String layer = getLayer();
        if (null == workspace && null != layer) {
            Catalog catalog = getRawCatalog();
            info = catalog.getLayerGroupByName(CatalogFacade.NO_WORKSPACE, layer);
        } else if (null != workspace && null != layer) {
            Catalog catalog = getRawCatalog();
            info = catalog.getLayerByName(workspace + ":" + layer);
            if (null == info) info = catalog.getLayerGroupByName(workspace, layer);
        }
        return info;
    }

    private Catalog getRawCatalog() {
        ApplicationContext context = GeoServerApplication.get().getApplicationContext();
        Catalog catalog = context.getBean("rawCatalog", Catalog.class);
        return catalog;
    }
}
