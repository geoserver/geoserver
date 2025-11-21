/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.layerdetails;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.acl.domain.rules.LayerAttribute.AccessType;
import org.geoserver.acl.plugin.web.accessrules.event.PublishedInfoChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.model.LayerAttributeDataProvider;
import org.geoserver.acl.plugin.web.accessrules.model.LayerAttributesEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerAttribute;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerDetails;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Editor panel for {@link MutableLayerDetails#getAttributes()}
 *
 * @see LayerDetailsEditPanel
 */
@SuppressWarnings("serial")
class LayerAttributesEditPanel extends FormComponentPanel<List<MutableLayerAttribute>> {

    private WebMarkupContainer attributesContainer;
    private LayerAttribtuesTable table;

    private LayerAttributesEditModel panelModel;

    public LayerAttributesEditPanel(String id, LayerAttributesEditModel panelModel) {
        super(id, panelModel.getModel());
        this.panelModel = panelModel;

        add(layerAttributesCheck());
        add(attributesContainer = attributesContainer());
        setVisible(panelModel.isShowPanel());
    }

    private FormComponent<Boolean> layerAttributesCheck() {
        IModel<Boolean> setAttributesModel = panelModel.getSetAttributesModel();
        CheckBox check = new CheckBox("layerAttributesCheck", setAttributesModel);
        check.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                onLayerAttributesCheckChanged(target);
            }
        });
        return check;
    }

    private WebMarkupContainer attributesContainer() {
        WebMarkupContainer container = new WebMarkupContainer("attributesContainer");
        container.setOutputMarkupPlaceholderTag(true);

        table = new LayerAttribtuesTable("table", panelModel.getDataProvider());
        container.add(table);

        return container;
    }

    private void onLayerAttributesCheckChanged(AjaxRequestTarget target) {
        // model updated, then...
        handleVisibility(target);
    }

    private void handleVisibility(AjaxRequestTarget target) {
        panelModel.computeAttributes();
        table.modelChanged();
        boolean showPanel = panelModel.isShowPanel();
        if (this.isVisible() != showPanel) {
            setVisible(showPanel);
            target.add(this);
        }
        boolean showTable = panelModel.isShowTable();
        if (attributesContainer.isVisible() != showTable) {
            attributesContainer.setVisible(showTable);
            target.add(attributesContainer);
        }
    }

    public @Override void onEvent(IEvent<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof PublishedInfoChangeEvent changeEvent) {
            AjaxRequestTarget target = changeEvent.getTarget();
            handleVisibility(target);
            //            table.modelChanged();
            //            target.add(this);
        }
    }

    @Override
    public void convertInput() {
        if (panelModel.getSetAttributesModel().getObject()) {
            IModel<List<MutableLayerAttribute>> model = panelModel.getModel();
            List<MutableLayerAttribute> convertedInput = model.getObject();
            setConvertedInput(convertedInput);
        } else {
            setConvertedInput(new ArrayList<>());
        }
    }

    class LayerAttribtuesTable extends GeoServerTablePanel<MutableLayerAttribute> {

        public LayerAttribtuesTable(String id, LayerAttributeDataProvider dataProvider) {
            super(id, dataProvider, false);
            setFilterVisible(false);
            setSortable(true);
            setPageable(false);
            setSelectable(false);
            setOutputMarkupId(true);
            Component table = get("listContainer");
            table.add(new AttributeModifier("class", "latts"));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Component getComponentForProperty(
                String id, IModel<MutableLayerAttribute> itemModel, Property<MutableLayerAttribute> property) {

            if (LayerAttributeDataProvider.ACCESS == property) {
                IModel<AccessType> model = (IModel<AccessType>) property.getModel(itemModel);
                return new AccessTypeComponent(id, model);
            } else if (LayerAttributeDataProvider.DATATYPE == property) {
                IModel<String> model = (IModel<String>) property.getModel(itemModel);
                return new DataTypeLabel(id, model);
            }
            return null;
        }
    }

    private static class DataTypeLabel extends Label {
        public DataTypeLabel(String id, IModel<String> model) {
            super(id, model);
        }

        @Override
        public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
            replaceComponentTagBody(markupStream, openTag, getDisplayValue());
        }

        private CharSequence getDisplayValue() {
            String value = getDefaultModelObjectAsString();
            int index = value.lastIndexOf('.');
            return index > 0 ? value.substring(1 + index) : value;
        }
    }

    private class AccessTypeComponent extends Fragment {

        public AccessTypeComponent(String id, IModel<AccessType> model) {
            super(id, "attributeAccessFragment", LayerAttributesEditPanel.this);
            add(new AttributeAccessRadioGroup("access", model));
        }
    }

    private static class AttributeAccessRadioGroup extends RadioGroup<AccessType> {

        public AttributeAccessRadioGroup(String id, IModel<AccessType> itemModel) {
            super(id, itemModel);
            Radio<AccessType> none = new Radio<>("NONE", Model.of(AccessType.NONE), this);
            Radio<AccessType> ro = new Radio<>("READONLY", Model.of(AccessType.READONLY), this);
            Radio<AccessType> rw = new Radio<>("READWRITE", Model.of(AccessType.READWRITE), this);
            add(none, ro, rw);
        }
    }
}
