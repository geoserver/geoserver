/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class AttributePositionPanel extends Panel {
    private static final long serialVersionUID = -4645368967597125299L;

    public AttributePositionPanel(
            String id,
            IModel<ComplexMetadataMap> mapModel,
            AttributeConfiguration attConfig,
            int index,
            List<Integer> derivedAtts,
            GeoServerTablePanel<?> tablePanel) {
        super(id, mapModel);
        AjaxSubmitLink upLink =
                new AjaxSubmitLink("up") {
                    private static final long serialVersionUID = -4165434301439054175L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        moveUpOrDown(mapModel, attConfig, index, -1, tablePanel);
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (index == 0
                                || derivedAtts != null
                                        && (derivedAtts.contains(index)
                                                || derivedAtts.contains(index - 1))) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        upLink.add(
                new Image(
                                "upImage",
                                new PackageResourceReference(
                                        GeoServerBasePage.class, "img/icons/silk/arrow_up.png"))
                        .add(
                                new AttributeModifier(
                                        "alt",
                                        new ParamResourceModel(
                                                "up", AttributePositionPanel.this))));
        add(upLink);

        AjaxSubmitLink downLink =
                new AjaxSubmitLink("down") {
                    private static final long serialVersionUID = -8005026702401617344L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        moveUpOrDown(mapModel, attConfig, index, 1, tablePanel);

                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        tablePanel.clearSelection();
                        target.add(tablePanel);
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (index == mapModel.getObject().size(attConfig.getKey()) - 1
                                || derivedAtts != null
                                        && (derivedAtts.contains(index)
                                                || derivedAtts.contains(index + 1))) {
                            tag.put("style", "visibility:hidden");
                        } else {
                            tag.put("style", "visibility:visible");
                        }
                    }
                };
        downLink.add(
                new Image(
                                "downImage",
                                new PackageResourceReference(
                                        GeoServerBasePage.class, "img/icons/silk/arrow_down.png"))
                        .add(
                                new AttributeModifier(
                                        "alt",
                                        new ParamResourceModel(
                                                "down", AttributePositionPanel.this))));
        add(downLink);
    }

    public void moveUpOrDown(
            IModel<ComplexMetadataMap> mapModel,
            AttributeConfiguration attConfig,
            int index,
            int diff,
            GeoServerTablePanel<?> tablePanel) {

        if (attConfig.getFieldType() == FieldTypeEnum.COMPLEX) {
            ComplexMetadataService service =
                    GeoServerApplication.get()
                            .getApplicationContext()
                            .getBean(ComplexMetadataService.class);

            ComplexMetadataMap other =
                    mapModel.getObject().subMap(attConfig.getKey(), index + diff);
            ComplexMetadataMap current = mapModel.getObject().subMap(attConfig.getKey(), index);

            ComplexMetadataMap old = current.clone();
            service.copy(other, current, attConfig.getTypename());
            service.copy(old, other, attConfig.getTypename());

        } else {
            ComplexMetadataAttribute<Serializable> other =
                    mapModel.getObject().get(Serializable.class, attConfig.getKey(), index + diff);
            ComplexMetadataAttribute<Serializable> current =
                    mapModel.getObject().get(Serializable.class, attConfig.getKey(), index);

            Serializable old = current.getValue();
            current.setValue(other.getValue());
            other.setValue(old);
        }
    }
}
