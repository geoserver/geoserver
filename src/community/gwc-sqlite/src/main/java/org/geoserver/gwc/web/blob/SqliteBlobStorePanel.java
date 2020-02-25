/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.sqlite.SqliteInfo;

/** Properties that will be common to all SQLite based blob stores. */
abstract class SqliteBlobStorePanel<T extends SqliteInfo> extends Panel {

    public SqliteBlobStorePanel(String id, final IModel<T> configurationModel) {
        super(id, configurationModel);
    }

    @Override
    protected void onInitialize() {

        super.onInitialize();

        // the root directory of this blob store
        DirectoryParamPanel directoryPanel =
                new DirectoryParamPanel(
                        "rootDirectory",
                        new PropertyModel<>(getDefaultModel().getObject(), "rootDirectory"),
                        new ParamResourceModel("rootDirectory", this),
                        true);
        add(directoryPanel);
        directoryPanel
                .getFormComponent()
                .setModel(new PropertyModel<>(getDefaultModel().getObject(), "rootDirectory"));
        directoryPanel.setFileFilter(
                new Model<>((DirectoryFileFilter) DirectoryFileFilter.INSTANCE));

        // properties that will be used to build a database file path
        add(
                new TextField<String>("templatePath")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "templatePath", new ResourceModel("templatePath"))));
        add(
                new TextField<Long>("rowRangeCount")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "rowRangeCount", new ResourceModel("rowRangeCount"))));
        add(
                new TextField<Long>("columnRangeCount")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "columnRangeCount",
                                        new ResourceModel("columnRangeCount"))));

        // connection pool related properties
        add(
                new TextField<Long>("poolSize")
                        .setRequired(true)
                        .add(new AttributeModifier("poolSize", new ResourceModel("poolSize"))));
        add(
                new TextField<Long>("poolReaperIntervalMs")
                        .setRequired(true)
                        .add(
                                new AttributeModifier(
                                        "poolReaperIntervalMs",
                                        new ResourceModel("poolReaperIntervalMs"))));

        // should database files be deleted or should we delete tiles ranges
        add(
                new CheckBox("eagerDelete")
                        .add(
                                new AttributeModifier(
                                        "eagerDelete", new ResourceModel("eagerDelete"))));

        // controls if the blob store will set and use the tile creation time
        add(
                new CheckBox("useCreateTime")
                        .add(
                                new AttributeModifier(
                                        "useCreateTime", new ResourceModel("useCreateTime"))));
    }
}
