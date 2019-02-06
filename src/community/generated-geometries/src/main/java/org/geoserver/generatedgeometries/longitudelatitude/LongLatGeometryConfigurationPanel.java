/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.generatedgeometries.longitudelatitude.LongLatGeometryGenerationMethodology.LongLatConfiguration;
import org.geoserver.web.GeoServerApplication;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.IOException;
import java.util.List;

public class LongLatGeometryConfigurationPanel extends Panel {

    private String geometryAttributeName;
    private AttributeTypeInfo selectedLonAttribute;
    private AttributeTypeInfo selectedLatAttribute;

    private ChoiceRenderer<AttributeTypeInfo> choiceRenderer =
            new ChoiceRenderer<AttributeTypeInfo>() {
                @Override
                public Object getDisplayValue(AttributeTypeInfo attributeTypeInfo) {
                    return attributeTypeInfo.getName();
                }
            };

    public LongLatGeometryConfigurationPanel(String panelId, IModel model) {
        super(panelId, model);
        initComponents(model);
    }

    private void initComponents(IModel model) {
        add(
                new Label(
                        "attrLabel",
                        new org.apache.wicket.model.ResourceModel("geometryAttributeNameLabel")));
        add(
                new TextField<String>(
                        "geometryAttributeName",
                        new PropertyModel<>(this, "geometryAttributeName")));

        List<AttributeTypeInfo> attributes = getAttributes((FeatureTypeInfo) model.getObject());
        add(
                new DropDownChoice<>(
                        "lonAttributesDropDown",
                        new PropertyModel<>(this, "selectedLonAttribute"),
                        attributes,
                        choiceRenderer));
        add(
                new DropDownChoice<>(
                        "latAttributesDropDown",
                        new PropertyModel<>(this, "selectedLatAttribute"),
                        attributes,
                        choiceRenderer));
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
        // TODO: implement
        return false;
    }

    LongLatConfiguration getLongLatConfiguration() throws ConfigurationException {
        if (isValid()) {
            try {
                return new LongLatConfiguration(
                        geometryAttributeName,
                        selectedLonAttribute.getAttribute(),
                        selectedLatAttribute.getAttribute());
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
        throw new ConfigurationException("invalid configuration");
    }
}
