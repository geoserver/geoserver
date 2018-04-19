/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.versioning.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.nsg.versioning.TimeVersioning;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WfsVersioningConfig extends PublishedConfigurationPanel<LayerInfo> {

    public WfsVersioningConfig(String id, IModel<LayerInfo> model) {
        super(id, model);
        // get the needed information from the model
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(model);
        boolean isVersioningActivated = TimeVersioning.isEnabled(featureTypeInfo);
        String idAttributeName = isVersioningActivated ? TimeVersioning.getNamePropertyName(featureTypeInfo) : null;
        String timeAttributeName = isVersioningActivated ? TimeVersioning.getTimePropertyName(featureTypeInfo) : null;
        List<String> attributesNames = getAttributesNames(featureTypeInfo);
        List<String> timeAttributesNames = getTimeAttributesNames(featureTypeInfo);
        // create dropdown choice for the id attribute name
        PropertyModel metadata = new PropertyModel(model, "resource.metadata");
        DropDownChoice<String> idAttributeChoice = new DropDownChoice<>("idAttributeChoice",
                new MapModel<>(metadata, TimeVersioning.NAME_PROPERTY_KEY), attributesNames);
        idAttributeChoice.setOutputMarkupId(true);
        idAttributeChoice.setOutputMarkupPlaceholderTag(true);
        idAttributeChoice.setRequired(true);
        idAttributeChoice.setVisible(isVersioningActivated);
        add(idAttributeChoice);
        // add label for id attribute name dropdown choice
        Label idAttributeChoiceLabel = new Label("idAttributeChoiceLabel",
                new StringResourceModel("WfsVersioningConfig.idAttributeChoiceLabel"));
        idAttributeChoiceLabel.setOutputMarkupId(true);
        idAttributeChoiceLabel.setOutputMarkupPlaceholderTag(true);
        idAttributeChoiceLabel.setVisible(isVersioningActivated);
        add(idAttributeChoiceLabel);
        // create dropdown choice for the time attribute name
        
        DropDownChoice<String> timeAttributeChoice = new DropDownChoice<>("timeAttributeChoice",
                new MapModel<>(metadata, TimeVersioning.TIME_PROPERTY_KEY), timeAttributesNames);
        timeAttributeChoice.setOutputMarkupId(true);
        timeAttributeChoice.setOutputMarkupPlaceholderTag(true);
        timeAttributeChoice.setRequired(true);
        timeAttributeChoice.setVisible(isVersioningActivated);
        add(timeAttributeChoice);
        // add label for id attribute name dropdown choice
        Label timeAttributeChoiceLabel = new Label("timeAttributeChoiceLabel",
                new StringResourceModel("WfsVersioningConfig.timeAttributeChoiceLabel"));
        timeAttributeChoiceLabel.setOutputMarkupId(true);
        timeAttributeChoiceLabel.setOutputMarkupPlaceholderTag(true);
        timeAttributeChoiceLabel.setVisible(isVersioningActivated);
        add(timeAttributeChoiceLabel);
        // checkbox for activating versioning
        CheckBox versioningActivateCheckBox = new AjaxCheckBox("versioningActivateCheckBox",
                new MapModel<>(metadata, TimeVersioning.ENABLED_KEY)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean checked = getModelObject();
                if (checked) {
                    // activate versioning attributes selection
                    idAttributeChoice.setVisible(true);
                    idAttributeChoiceLabel.setVisible(true);
                    timeAttributeChoice.setVisible(true);
                    timeAttributeChoiceLabel.setVisible(true);
                } else {
                    // deactivate versioning attributes selection
                    idAttributeChoice.setVisible(false);
                    idAttributeChoiceLabel.setVisible(false);
                    timeAttributeChoice.setVisible(false);
                    timeAttributeChoiceLabel.setVisible(false);
                }
                // update the dropdown choices and labels
                target.add(idAttributeChoice);
                target.add(idAttributeChoiceLabel);
                target.add(timeAttributeChoice);
                target.add(timeAttributeChoiceLabel);
            }
        };
        if (isVersioningActivated) {
            versioningActivateCheckBox.setModelObject(true);
        }
        versioningActivateCheckBox.setEnabled(!timeAttributesNames.isEmpty());
        add(versioningActivateCheckBox);
        // add versioning activating checkbox label
        Label versioningActivateCheckBoxLabel = new Label("versioningActivateCheckBoxLabel",
                new StringResourceModel("WfsVersioningConfig.versioningActivateCheckBoxLabel"));
        add(versioningActivateCheckBoxLabel);
    }

    private List<String> getAttributesNames(FeatureTypeInfo featureTypeInfo) {
        try {
            return featureTypeInfo.getFeatureType().getDescriptors().stream()
                    .map(attribute -> attribute.getName().getLocalPart())
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error processing attributes of feature type '%s'.",
                    featureTypeInfo.getName()), exception);
        }
    }

    private List<String> getTimeAttributesNames(FeatureTypeInfo featureTypeInfo) {
        try {
            return featureTypeInfo.getFeatureType().getDescriptors().stream().filter(attribute -> {
                Class binding = attribute.getType().getBinding();
                return Long.class.isAssignableFrom(binding)
                        || Date.class.isAssignableFrom(binding)
                        || Timestamp.class.isAssignableFrom(binding);
            }).map(attribute -> attribute.getName().getLocalPart()).collect(Collectors.toList());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error processing attributes of feature type '%s'.",
                    featureTypeInfo.getName()), exception);
        }
    }

    private FeatureTypeInfo getFeatureTypeInfo(IModel<LayerInfo> model) {
        return (FeatureTypeInfo) model.getObject().getResource();
    }
}
