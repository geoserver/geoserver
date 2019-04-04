/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.List;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.LimitedAreaRequestConstraints;
import org.geoserver.qos.xml.OwsRange;

public class LimitedConstraintsPanel
        extends BaseLimitedConstraintsPanel<LimitedAreaRequestConstraints> {

    public LimitedConstraintsPanel(String id, IModel<LimitedAreaRequestConstraints> model) {
        super(id, model, new LayersListModalBuilder());
        initImageSizeComponents();
    }

    @Override
    protected List<String> getSelectedLayers() {
        return getMainModel().getObject().getLayerNames();
    }

    protected void initImageSizeComponents() {
        // default empty object init
        if (mainModel.getObject().getImageWidth() == null)
            mainModel.getObject().setImageWidth(new OwsRange());
        if (mainModel.getObject().getImageHeight() == null)
            mainModel.getObject().setImageHeight(new OwsRange());
        // components:
        final TextField<String> imageMinWidthInput =
                new TextField<>(
                        "imageMinWidthInput",
                        new PropertyModel<>(mainModel, "imageWidth.minimunValue"));
        innerConstraintsDiv.add(imageMinWidthInput);

        final TextField<String> imageMaxWidthInput =
                new TextField<>(
                        "imageMaxWidthInput",
                        new PropertyModel<>(mainModel, "imageWidth.maximunValue"));
        innerConstraintsDiv.add(imageMaxWidthInput);

        final TextField<String> imageMinHeightInput =
                new TextField<>(
                        "imageMinHeightInput",
                        new PropertyModel<>(mainModel, "imageHeight.minimunValue"));
        innerConstraintsDiv.add(imageMinHeightInput);

        final TextField<String> imageMaxHeightInput =
                new TextField<>(
                        "imageMaxHeightInput",
                        new PropertyModel<>(mainModel, "imageHeight.maximunValue"));
        innerConstraintsDiv.add(imageMaxHeightInput);
    }
}
