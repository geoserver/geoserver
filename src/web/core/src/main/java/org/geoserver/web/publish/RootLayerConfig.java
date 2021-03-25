/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.util.MetadataMapModel;

public class RootLayerConfig extends PublishedConfigurationPanel<PublishedInfo> {
    public static final long serialVersionUID = -1l;

    public RootLayerConfig(String id, IModel<? extends PublishedInfo> model) {
        super(id, model);
        IModel<Boolean> rootLayerModel =
                new MetadataMapModel<>(
                        new PropertyModel<>(model, "metadata"),
                        PublishedInfo.ROOT_IN_CAPABILITIES,
                        Boolean.class);

        RadioGroup<Boolean> rootLayer = new RadioGroup<>("rootLayer", rootLayerModel);
        add(rootLayer);

        IModel<Boolean> rootLayerGlobalModel = new Model<>(null);
        IModel<Boolean> rootLayerYesModel = new Model<>(Boolean.TRUE);
        IModel<Boolean> rootLayerNoModel = new Model<>(Boolean.FALSE);

        Radio<Boolean> rootLayerGlobal = new Radio<>("rootLayerGlobal", rootLayerGlobalModel);
        Radio<Boolean> rootLayerYes = new Radio<>("rootLayerYes", rootLayerYesModel);
        Radio<Boolean> rootLayerNo = new Radio<>("rootLayerNo", rootLayerNoModel);

        rootLayer.add(rootLayerGlobal);
        rootLayer.add(rootLayerYes);
        rootLayer.add(rootLayerNo);
    }
}
