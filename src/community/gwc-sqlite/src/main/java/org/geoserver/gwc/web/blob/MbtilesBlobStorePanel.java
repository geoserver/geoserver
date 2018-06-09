/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.sqlite.MbtilesInfo;

/** Panel that contains the properties required to configure a MBTiles blob store. */
public class MbtilesBlobStorePanel extends SqliteBlobStorePanel<MbtilesInfo> {

    public MbtilesBlobStorePanel(String id, IModel<MbtilesInfo> configurationModel) {
        super(id, configurationModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        // the directory that may contain user provided mbtiles metadata
        DirectoryParamPanel directoryPanel =
                new DirectoryParamPanel(
                        "mbtilesMetadataDirectory",
                        new PropertyModel<>(
                                getDefaultModel().getObject(), "mbtilesMetadataDirectory"),
                        new ParamResourceModel("mbtilesMetadataDirectory", this),
                        false);
        add(directoryPanel);
        directoryPanel
                .getFormComponent()
                .setModel(
                        new PropertyModel<>(
                                getDefaultModel().getObject(), "mbtilesMetadataDirectory"));
        directoryPanel.setFileFilter(
                new Model<>((DirectoryFileFilter) DirectoryFileFilter.INSTANCE));
        // controls the store executor concurrency (this is used to parallelize some operations)
        add(
                new TextField<Integer>("executorConcurrency")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "executorConcurrency",
                                        new ResourceModel("executorConcurrency"))));
    }
}
