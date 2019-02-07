/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.generatedgeometries.longitudelatitude.LongLatGeometryGenerationMethodology.LongLatConfiguration;
import org.geoserver.web.GeoServerApplication;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class LongLatGeometryConfigurationPanel extends Panel {

    private String geometryAttributeName;
    private AttributeTypeInfo selectedLonAttribute;
    private AttributeTypeInfo selectedLatAttribute;

    private ChoiceRenderer<AttributeTypeInfo> choiceRenderer = new ChoiceRenderer<AttributeTypeInfo>() {
        @Override
        public Object getDisplayValue(AttributeTypeInfo attributeTypeInfo) {
            return attributeTypeInfo.getName();
        }
    };
    private TextField<String> geometryAttributeNameTextField;
    private DropDownChoice<AttributeTypeInfo> lonAttributeDropDown;
    private DropDownChoice<AttributeTypeInfo> latAttributeDropDown;

    public LongLatGeometryConfigurationPanel(String panelId, IModel model) {
        super(panelId, model);
        initComponents(model);
    }

    private void initComponents(IModel model) {
        add(new Label("attrLabel", new ResourceModel("geometryAttributeNameLabel")));
        geometryAttributeNameTextField = new TextField<>("geometryAttributeName", forExpression("geometryAttributeName"));
        add(geometryAttributeNameTextField);

        List<AttributeTypeInfo> attributes = getAttributes((FeatureTypeInfo) model.getObject());
        lonAttributeDropDown = new DropDownChoice<>(
                "lonAttributesDropDown",
                forExpression("selectedLonAttribute"),
                attributes,
                choiceRenderer);
        add(lonAttributeDropDown);
        latAttributeDropDown = new DropDownChoice<>(
                "latAttributesDropDown",
                forExpression("selectedLatAttribute"),
                attributes,
                choiceRenderer);
        add(latAttributeDropDown);

        addAjaxTrigger(geometryAttributeNameTextField, lonAttributeDropDown, latAttributeDropDown);
    }

    private <T> PropertyModel<T> forExpression(String expression) {
        return new PropertyModel<>(this, expression);
    }

    private Behavior onChangeAjaxTrigger() {
        return new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        };
    }

    private void addAjaxTrigger(Component...components) {
        Stream.of(components).forEach(c -> c.add(onChangeAjaxTrigger()));
    }

    private List<AttributeTypeInfo> getAttributes(FeatureTypeInfo fti) {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        try {
            return resourcePool.loadAttributes(fti);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // TODO: should we filter to return Doubles/numbers only??
        //        FeatureType featureType = resourcePool.getFeatureType(fti);
        //        PropertyDescriptor pd = featureType.getDescriptor(attribute.getName());
        //        String typeName = pd.getType().getBinding().getSimpleName();
    }

    private boolean isValid() {
        return isNotEmpty(geometryAttributeName)
                && selectedLonAttribute != null
                && selectedLatAttribute != null;
    }

    LongLatConfiguration getLongLatConfiguration() throws ConfigurationException {
        if (!isValid()) {
            throw new ConfigurationException("invalid configuration");
        }
        return new LongLatConfiguration(geometryAttributeName, selectedLonAttribute.getName(), selectedLatAttribute.getName());
    }
}
