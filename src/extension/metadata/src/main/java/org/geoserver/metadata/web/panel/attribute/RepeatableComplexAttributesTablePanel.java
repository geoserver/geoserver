/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexAttributeGenerator;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.web.layer.MetadataTabPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Generate the gui as a list of Complex Objects(an object contains multiple simple fields or objects). Add ui
 * components to manage the list.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
// TODO WICKET8 - Verify this page works OK
public class RepeatableComplexAttributesTablePanel extends Panel {
    @Serial
    private static final long serialVersionUID = 1297739738862860160L;

    private GeoServerTablePanel<ComplexMetadataMap> tablePanel;

    private Label noData;

    private RepeatableComplexAttributeDataProvider dataProvider;

    private Map<String, List<Integer>> derivedAtts;

    private ComplexAttributeGenerator generator;

    private AttributeConfiguration attributeConfiguration;

    private ResourceInfo rInfo;

    public RepeatableComplexAttributesTablePanel(
            String id,
            RepeatableComplexAttributeDataProvider dataProvider,
            IModel<ComplexMetadataMap> metadataModel,
            AttributeConfiguration attributeConfiguration,
            ComplexAttributeGenerator generator,
            Map<String, List<Integer>> derivedAtts,
            ResourceInfo rInfo) {
        super(id, metadataModel);

        this.dataProvider = dataProvider;
        this.derivedAtts = derivedAtts;
        this.generator = generator;
        this.attributeConfiguration = attributeConfiguration;
        this.rInfo = rInfo;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(tablePanel = createAttributesTablePanel(dataProvider, derivedAtts));

        // the no data links label
        add(noData = new Label("noData", new ResourceModel("noData")));
        noData.setOutputMarkupId(true);
        noData.setOutputMarkupPlaceholderTag(true);
        updateTable();

        add(
                new AjaxSubmitLink("addNew") {

                    @Serial
                    private static final long serialVersionUID = 6840006565079316081L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        dataProvider.addField();
                        updateTable();
                        ((MarkupContainer) tablePanel.get("listContainer").get("items")).removeAll();
                        target.add(tablePanel);
                        target.add(noData);
                    }
                }.setVisible(isEnabledInHierarchy()));

        GeoServerDialog dialog = new GeoServerDialog("dialog");
        add(dialog);

        MetadataTabPanel tabPanel = findParent(MetadataTabPanel.class);

        add(
                new AjaxSubmitLink("generate") {

                    @Serial
                    private static final long serialVersionUID = 6840006565079316081L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        dialog.setInitialHeight(generator.getDialogContentHeight());
                        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                            @Serial
                            private static final long serialVersionUID = -8716380894588651422L;

                            @Override
                            protected Component getContents(String id) {
                                return generator.getDialogContent(id, (LayerInfo) tabPanel.getDefaultModelObject());
                            }

                            @Override
                            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                                generator.generate(
                                        attributeConfiguration,
                                        getMetadataModel().getObject(),
                                        (LayerInfo) tabPanel.getDefaultModelObject(),
                                        contents.getDefaultModelObject());
                                fixAll();
                                dataProvider.reset();
                                GeoServerTablePanel<ComplexMetadataMap> panel =
                                        createAttributesTablePanel(dataProvider, derivedAtts);
                                target.add(RepeatableComplexAttributesTablePanel.this
                                        .get("attributesTablePanel")
                                        .replaceWith(panel));
                                updateTable();
                                target.add(noData);
                                return true;
                            }
                        });
                    }
                }.setVisible(isEnabledInHierarchy()
                        && generator != null
                        && tabPanel != null
                        && generator.supports(
                                getMetadataModel().getObject(), (LayerInfo) tabPanel.getDefaultModelObject())));
    }

    private void fixAll() {
        ComplexMetadataService service =
                GeoServerApplication.get().getApplicationContext().getBean(ComplexMetadataService.class);
        for (int i = 0; i < getMetadataModel().getObject().size(attributeConfiguration.getKey()); i++) {
            service.init(
                    getMetadataModel().getObject().subMap(attributeConfiguration.getKey(), i),
                    attributeConfiguration.getTypename());
        }
    }

    private GeoServerTablePanel<ComplexMetadataMap> createAttributesTablePanel(
            RepeatableComplexAttributeDataProvider dataProvider, Map<String, List<Integer>> derivedAtts) {

        tablePanel = new GeoServerTablePanel<>("attributesTablePanel", dataProvider) {
            @Serial
            private static final long serialVersionUID = 4333335931795175790L;

            private IModel<ComplexMetadataMap> disabledValue = null;

            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(
                    String id,
                    IModel<ComplexMetadataMap> itemModel,
                    GeoServerDataProvider.Property<ComplexMetadataMap> property) {

                AttributeConfiguration attributeConfiguration = dataProvider.getConfiguration();

                // disable input values from template
                boolean enableInput = true;

                if (derivedAtts != null && derivedAtts.containsKey(attributeConfiguration.getKey())) {
                    List<Integer> indexes = derivedAtts.get(attributeConfiguration.getKey());
                    if (indexes.contains(itemModel.getObject().getIndex())) {
                        enableInput = false;
                        disabledValue = itemModel;
                    }
                }

                if (property.getName().equals(RepeatableComplexAttributeDataProvider.KEY_VALUE)) {

                    Component component = new AttributesTablePanel(
                            id,
                            new AttributeDataProvider(attributeConfiguration.getTypename(), rInfo),
                            itemModel,
                            null,
                            rInfo);
                    component.setEnabled(enableInput);

                    return component;

                } else if (property.getName().equals(RepeatableComplexAttributeDataProvider.KEY_REMOVE_ROW)) {
                    if (itemModel.equals(disabledValue)) {
                        // If the object is for a row that is not editable don't show the
                        // remove button
                        disabledValue = null;
                        return new Label(id, "");
                    } else {
                        MarkupContainer container = (MarkupContainer)
                                tablePanel.get("listContainer").get("items");
                        AjaxSubmitLink deleteAction = new AjaxSubmitLink(id) {

                            @Serial
                            private static final long serialVersionUID = -8829474855848647384L;

                            @Override
                            public void onSubmit(AjaxRequestTarget target) {
                                removeFields(target, itemModel);
                                updateTable();
                                container.removeAll();
                                target.add(tablePanel);
                                target.add(noData);
                            }
                        };
                        deleteAction.add(new AttributeAppender("class", "remove-link"));
                        deleteAction.setVisible(isEnabledInHierarchy());
                        return deleteAction;
                    }
                } else if (property.getName().equals(RepeatableComplexAttributeDataProvider.KEY_UPDOWN_ROW)) {
                    IModel<ComplexMetadataMap> model =
                            (IModel<ComplexMetadataMap>) RepeatableComplexAttributesTablePanel.this.getDefaultModel();
                    return new AttributePositionPanel(
                                    id,
                                    model,
                                    dataProvider.getConfiguration(),
                                    itemModel.getObject().getIndex(),
                                    getDerivedAtts(
                                            dataProvider.getConfiguration().getKey()),
                                    this)
                            .setVisible(isEnabledInHierarchy());
                }
                return null;
            }

            private void removeFields(AjaxRequestTarget target, IModel<ComplexMetadataMap> itemModel) {
                ComplexMetadataMap object = itemModel.getObject();
                dataProvider.removeField(object);
                target.add(this);
            }
        };

        tablePanel.setFilterVisible(false);
        tablePanel.setFilterable(false);
        tablePanel.getTopPager().setVisible(false);
        tablePanel.getBottomPager().setVisible(false);
        tablePanel.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        tablePanel.setSelectable(true);
        tablePanel.setSortable(false);
        tablePanel.setOutputMarkupId(true);
        tablePanel.setOutputMarkupPlaceholderTag(true);

        return tablePanel;
    }

    private List<Integer> getDerivedAtts(String key) {
        return derivedAtts == null ? null : derivedAtts.get(key);
    }

    @SuppressWarnings("unchecked")
    public IModel<ComplexMetadataMap> getMetadataModel() {
        return (IModel<ComplexMetadataMap>) getDefaultModel();
    }

    private void updateTable() {
        boolean isEmpty = dataProvider.getItems().isEmpty();
        tablePanel.setVisible(!isEmpty);
        noData.setVisible(isEmpty);
    }
}
