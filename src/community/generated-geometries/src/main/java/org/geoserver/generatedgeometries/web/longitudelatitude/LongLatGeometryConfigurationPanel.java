/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.web.longitudelatitude;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
import org.geoserver.generatedgeometries.core.GeneratedGeometryConfigurationException;
import org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LongLatConfiguration;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.SRSToCRSModel;

public class LongLatGeometryConfigurationPanel extends Panel {

    private String geometryAttributeName;
    private AttributeTypeInfo selectedLonAttribute;
    private AttributeTypeInfo selectedLatAttribute;

    private final transient Supplier<GeoServerApplication> geoServerApplicationSupplier;

    private ChoiceRenderer<AttributeTypeInfo> choiceRenderer =
            new ChoiceRenderer<AttributeTypeInfo>() {
                @Override
                public Object getDisplayValue(AttributeTypeInfo attributeTypeInfo) {
                    return attributeTypeInfo.getName();
                }
            };
    private TextField<String> geometryAttributeNameTextField;
    private DropDownChoice<AttributeTypeInfo> lonAttributeDropDown;
    private DropDownChoice<AttributeTypeInfo> latAttributeDropDown;
    private CRSPanel declaredCRS;

    public LongLatGeometryConfigurationPanel(String panelId, IModel model) {
        this(panelId, model, GeoServerApplication::get);
    }

    LongLatGeometryConfigurationPanel(
            String id,
            final IModel model,
            Supplier<GeoServerApplication> geoServerApplicationSupplier) {
        super(id, model);
        this.geoServerApplicationSupplier = geoServerApplicationSupplier;
        initComponents(model);
    }

    private void initComponents(IModel model) {
        add(new Label("attrLabel", new ResourceModel("geometryAttributeNameLabel")));
        geometryAttributeNameTextField =
                new TextField<>("geometryAttributeName", forExpression("geometryAttributeName"));
        add(geometryAttributeNameTextField);

        List<AttributeTypeInfo> attributes = getAttributes((FeatureTypeInfo) model.getObject());
        lonAttributeDropDown =
                new DropDownChoice<>(
                        "lonAttributesDropDown",
                        forExpression("selectedLonAttribute"),
                        attributes,
                        choiceRenderer);
        add(lonAttributeDropDown);
        latAttributeDropDown =
                new DropDownChoice<>(
                        "latAttributesDropDown",
                        forExpression("selectedLatAttribute"),
                        attributes,
                        choiceRenderer);
        add(latAttributeDropDown);
        declaredCRS =
                new CRSPanel("srsPicker", new SRSToCRSModel(new PropertyModel<>(model, "sRS")));
        add(declaredCRS);
        addAjaxTrigger(
                geometryAttributeNameTextField,
                lonAttributeDropDown,
                latAttributeDropDown,
                declaredCRS);
    }

    private <T> PropertyModel<T> forExpression(String expression) {
        return new PropertyModel<>(this, expression);
    }

    /**
     * Empty behavior factory for triggering inputs' updates via Ajax calls.
     *
     * @return a behavior with empty body
     */
    private Behavior onChangeAjaxTrigger() {
        return new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // intentionally empty
            }
        };
    }

    private void addAjaxTrigger(Component... components) {
        Stream.of(components).forEach(c -> c.add(onChangeAjaxTrigger()));
    }

    private List<AttributeTypeInfo> getAttributes(FeatureTypeInfo fti) {
        Catalog catalog = geoServerApplicationSupplier.get().getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        try {
            return resourcePool.loadAttributes(fti);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValid() {
        return isNotEmpty(geometryAttributeName)
                && selectedLonAttribute != null
                && selectedLatAttribute != null
                && declaredCRS.getCRS() != null;
    }

    LongLatConfiguration getLongLatConfiguration() {
        if (!isValid()) {
            throw new GeneratedGeometryConfigurationException("invalid configuration");
        }
        return new LongLatConfiguration(
                geometryAttributeName,
                selectedLonAttribute.getName(),
                selectedLatAttribute.getName(),
                declaredCRS.getCRS());
    }
}
