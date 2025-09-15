/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.metadata.data.service.impl.DomainGenerator;
import org.geoserver.metadata.web.layer.MetadataTabPanel;
import org.geotools.api.data.DataAccess;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.util.logging.Logging;

public class GenerateDomainPanel extends Panel {

    @Serial
    private static final long serialVersionUID = -4252512711183089841L;

    private static final Logger LOGGER = Logging.getLogger(MetadataTabPanel.class);

    private FeatureTypeInfo fti;

    public GenerateDomainPanel(String id, FeatureTypeInfo fti) {
        super(id, new Model<>(new HashMap<>()));
        this.fti = fti;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = DomainGenerator.getDataAccess(fti);

        DropDownChoice<Boolean> methodChoice = new DropDownChoice<>(
                "method",
                new PropertyModel<>(getDefaultModel(), "method"),
                Lists.newArrayList(false, true),
                new IChoiceRenderer<>() {
                    @Serial
                    private static final long serialVersionUID = 1966992066973104491L;

                    @Override
                    public Object getDisplayValue(Boolean object) {
                        return new StringResourceModel("RepeatableComplexAttributesTablePanel.method." + object)
                                .getString();
                    }

                    @Override
                    public String getIdValue(Boolean object, int index) {
                        return object.toString();
                    }

                    @Override
                    public Boolean getObject(String id, IModel<? extends List<? extends Boolean>> choices) {
                        return Boolean.valueOf(id);
                    }
                });
        methodChoice.setDefaultModelObject(false);
        add(methodChoice);

        List<Name> names = new ArrayList<>();
        if (dataAccess != null) {
            try {
                names.addAll(dataAccess.getNames());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve table names", e);
            }
        }
        methodChoice.setEnabled(!names.isEmpty());

        DropDownChoice<Name> tableNameChoice =
                new DropDownChoice<>("tableName", new PropertyModel<>(getDefaultModel(), "tableName"), names);
        add(tableNameChoice.setNullValid(false).setEnabled(false).setOutputMarkupId(true));

        DropDownChoice<Name> valueAttributeNameChoice = new DropDownChoice<>("valueAttributeName");
        valueAttributeNameChoice.setModel(new PropertyModel<>(getDefaultModel(), "valueAttributeName"));
        add(valueAttributeNameChoice.setNullValid(false).setEnabled(false).setOutputMarkupId(true));

        DropDownChoice<Name> defAttributeNameChoice = new DropDownChoice<>("defAttributeName");
        defAttributeNameChoice.setModel(new PropertyModel<>(getDefaultModel(), "defAttributeName"));
        add(defAttributeNameChoice.setNullValid(false).setEnabled(false).setOutputMarkupId(true));

        methodChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial
            private static final long serialVersionUID = 6321014584689914438L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                tableNameChoice.setEnabled(methodChoice.getModelObject());
                valueAttributeNameChoice.setEnabled(methodChoice.getModelObject());
                defAttributeNameChoice.setEnabled(methodChoice.getModelObject());
                tableNameChoice.setRequired(methodChoice.getModelObject());
                valueAttributeNameChoice.setRequired(methodChoice.getModelObject());
                defAttributeNameChoice.setRequired(methodChoice.getModelObject());
                target.add(tableNameChoice);
                target.add(valueAttributeNameChoice);
                target.add(defAttributeNameChoice);
            }
        });

        tableNameChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial
            private static final long serialVersionUID = 6321014584689914438L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                List<Name> attributes = getAttributeNames(tableNameChoice.getModelObject());
                defAttributeNameChoice.setChoices(attributes);
                valueAttributeNameChoice.setChoices(attributes);
                target.add(valueAttributeNameChoice);
                target.add(defAttributeNameChoice);
            }
        });

        add(new FeedbackPanel("generateFeedback", new ContainerFeedbackMessageFilter(this)).setOutputMarkupId(true));
    }

    private List<Name> getAttributeNames(Name tableName) {
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = DomainGenerator.getDataAccess(fti);
        List<Name> attributeNames = new ArrayList<>();
        if (dataAccess != null) {
            try {
                for (PropertyDescriptor descriptor :
                        dataAccess.getSchema(tableName).getDescriptors()) {
                    attributeNames.add(descriptor.getName());
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve attributenames", e);
            }
        }
        return attributeNames;
    }
}
