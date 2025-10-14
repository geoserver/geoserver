/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.io.Serial;
import java.util.Arrays;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.wicket.EnumChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.FileBlobStoreInfo;

/**
 * Panel for FileBlobStore
 *
 * @author Niels Charlier
 */
public class FileBlobStorePanel extends Panel {

    @Serial
    private static final long serialVersionUID = -8237328668463257329L;

    public FileBlobStorePanel(String id, final IModel<FileBlobStoreInfo> configModel) {
        super(id, configModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onInitialize() {
        super.onInitialize();

        DirectoryParamPanel paramPanel;
        add(
                paramPanel = new DirectoryParamPanel(
                        "baseDirectory",
                        new PropertyModel<>(getDefaultModel().getObject(), "baseDirectory"),
                        new ParamResourceModel("baseDirectory", this),
                        true));
        paramPanel.add(new AttributeModifier("title", new ResourceModel("baseDirectory.title")));
        paramPanel.getFormComponent().setModel((IModel<String>) paramPanel.getDefaultModel()); // disable filemodel
        paramPanel.setFileFilter(new Model<>((DirectoryFileFilter) DirectoryFileFilter.INSTANCE));
        add(new TextField<Integer>("fileSystemBlockSize")
                .setRequired(true)
                .add(new AttributeModifier("title", new ResourceModel("fileSystemBlockSize.title"))));
        DropDownChoice<FileBlobStoreInfo.PathGeneratorType> layouts = new DropDownChoice<>(
                "fileSystemLayout",
                new PropertyModel<>(getDefaultModel(), "pathGeneratorType"),
                Arrays.asList(FileBlobStoreInfo.PathGeneratorType.values()));
        layouts.setChoiceRenderer(new EnumChoiceRenderer(layouts));
        add(layouts);
    }
}
