/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.visit.IVisitor;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.ExtraVariable;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.GlobalAttribute;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.VariableAttribute;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class NetCDFPanel<T extends NetCDFSettingsContainer> extends FormComponentPanel<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    protected final ListView<GlobalAttribute> globalAttributes;
    protected final ListView<VariableAttribute> variableAttributes;
    protected final ListView<ExtraVariable> extraVariables;
    protected final CheckBox shuffle;
    protected final CheckBox copyAttributes;
    protected final CheckBox copyGlobalAttributes;

    protected final TextField<Integer> compressionLevel;

    public static final PackageResourceReference ADD_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/add.png");

    public static final PackageResourceReference DELETE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    protected final DropDownChoice<DataPacking> dataPacking;

    protected final WebMarkupContainer container;
    protected final ListView<NetCDFExtensionPanelInfo> extensionPanels;

    @SuppressWarnings({"serial", "unchecked"})
    public NetCDFPanel(String id, IModel<T> netcdfModel) {
        super(id, netcdfModel);
        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);
        shuffle = new CheckBox("shuffle", new PropertyModel(netcdfModel, "shuffle"));
        container.add(shuffle);
        copyAttributes =
                new CheckBox("copyAttributes", new PropertyModel<>(netcdfModel, "copyAttributes"));
        container.add(copyAttributes);
        copyGlobalAttributes =
                new CheckBox(
                        "copyGlobalAttributes",
                        new PropertyModel(netcdfModel, "copyGlobalAttributes"));
        container.add(copyGlobalAttributes);
        compressionLevel =
                new TextField<>(
                        "compressionLevel", new PropertyModel<>(netcdfModel, "compressionLevel"));
        List<DataPacking> dataPackings = Arrays.asList(DataPacking.values());
        dataPacking =
                new DropDownChoice<>(
                        "dataPacking",
                        new PropertyModel<>(netcdfModel, "dataPacking"),
                        dataPackings);
        dataPacking.setOutputMarkupId(true);
        container.add(dataPacking);
        compressionLevel.add(new RangeValidator(0, 9));
        container.add(compressionLevel);

        ///////////////////////////////
        // Global Attributes definition
        ///////////////////////////////

        {
            IModel<List<GlobalAttribute>> model =
                    new PropertyModel(netcdfModel, "globalAttributes");
            globalAttributes =
                    new ListView<GlobalAttribute>("globalAttributes", model) {

                        @Override
                        protected void populateItem(final ListItem<GlobalAttribute> item) {
                            Label keyField =
                                    new Label(
                                            "globalAttributeKey",
                                            new PropertyModel<String>(item.getModel(), "key"));
                            item.add(keyField);
                            Label valueField =
                                    new Label(
                                            "globalAttributeValue",
                                            new PropertyModel<String>(item.getModel(), "value"));
                            item.add(valueField);
                            Component removeLink =
                                    new ImageAjaxLink("removeGlobalAttributeIcon", DELETE_ICON) {

                                        @Override
                                        protected void onClick(AjaxRequestTarget target) {
                                            List<GlobalAttribute> list =
                                                    new ArrayList<>(
                                                            globalAttributes.getModelObject());
                                            final GlobalAttribute attribute =
                                                    (GlobalAttribute) getDefaultModelObject();
                                            list.remove(attribute);
                                            globalAttributes.setModelObject(list);
                                            item.remove();
                                            target.add(container);
                                        }
                                    };
                            removeLink.setDefaultModel(item.getModel());
                            item.add(removeLink);
                        }
                    };
            globalAttributes.setOutputMarkupId(true);
            container.add(globalAttributes);
            TextField<String> newValue = new TextField<>("newGlobalAttributeValue", Model.of(""));
            newValue.setOutputMarkupId(true);
            container.add(newValue);
            TextField<String> newKey = new TextField<>("newGlobalAttributeKey", Model.of(""));
            newKey.setOutputMarkupId(true);
            container.add(newKey);
            GeoServerAjaxFormLink addLink =
                    new GeoServerAjaxFormLink("addGlobalAttribute") {

                        @Override
                        protected void onClick(AjaxRequestTarget ajaxTarget, Form form) {
                            newKey.processInput();
                            newValue.processInput();
                            String key = newKey.getModelObject();
                            String value = newValue.getModelObject();
                            if (key == null || key.trim().isEmpty()) {
                                ParamResourceModel rm =
                                        new ParamResourceModel("NetCDFOut.emptyKey", null, "");
                                error(rm.getString());
                            } else {
                                GlobalAttribute attribute = new GlobalAttribute(key, value);
                                if (!globalAttributes.getModelObject().contains(attribute)) {
                                    globalAttributes.getModelObject().add(attribute);
                                }
                                newKey.setModel(Model.of(""));
                                newValue.setModel(Model.of(""));
                                ajaxTarget.add(container);
                            }
                        }
                    };
            addLink.add(new Icon("addGlobalAttributeIcon", ADD_ICON));
            container.add(addLink);
        }

        /////////////////////////////////
        // Variable Attributes definition
        /////////////////////////////////

        IModel<List<VariableAttribute>> varAttributesModel =
                new PropertyModel(netcdfModel, "variableAttributes");
        variableAttributes = new VariableAttributeListView(varAttributesModel);
        variableAttributes.setOutputMarkupId(true);
        container.add(variableAttributes);
        final TextField<String> newValue =
                new TextField<>("newVariableAttributeValue", Model.of(""));
        newValue.setOutputMarkupId(true);
        container.add(newValue);
        final TextField<String> newKey = new TextField<>("newVariableAttributeKey", Model.of(""));
        newKey.setOutputMarkupId(true);
        container.add(newKey);
        GeoServerAjaxFormLink addVariableLink = new AddVariableLink(newKey, newValue);
        addVariableLink.add(new Icon("addVariableAttributeIcon", ADD_ICON));
        container.add(addVariableLink);

        /////////////////////////////
        // Extra Variables definition
        /////////////////////////////

        IModel<List<ExtraVariable>> extraVarModel =
                new PropertyModel(netcdfModel, "extraVariables");
        extraVariables = new ExtraVariableListView(extraVarModel);
        extraVariables.setOutputMarkupId(true);
        container.add(extraVariables);
        TextField<String> newSource = new TextField<>("newExtraVariableSource", Model.of(""));
        newSource.setOutputMarkupId(true);
        container.add(newSource);
        TextField<String> newOutput = new TextField<>("newExtraVariableOutput", Model.of(""));
        newOutput.setOutputMarkupId(true);
        container.add(newOutput);
        TextField<String> newDimensions =
                new TextField<>("newExtraVariableDimensions", Model.of(""));
        newDimensions.setOutputMarkupId(true);
        container.add(newDimensions);
        GeoServerAjaxFormLink addExtraVariableLink =
                new AddExtraVariableLink(newSource, newOutput, newDimensions);
        addExtraVariableLink.add(new Icon("addExtraVariableIcon", ADD_ICON));
        container.add(addExtraVariableLink);

        ///////////////////////////
        // End of definition blocks
        ///////////////////////////

        NetCDFSettingsContainer object = netcdfModel.getObject();
        if (object == null) {
            netcdfModel.setObject((T) new NetCDFSettingsContainer());
        }

        // extension panels
        extensionPanels = createExtensionPanelList("extensions", netcdfModel);
        extensionPanels.setReuseItems(true);
        add(extensionPanels);
    }

    @Override
    public void convertInput() {
        IVisitor<Component, Object> formComponentVisitor =
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                };
        globalAttributes.visitChildren(formComponentVisitor);
        variableAttributes.visitChildren(formComponentVisitor);
        extraVariables.visitChildren(formComponentVisitor);
        compressionLevel.processInput();
        dataPacking.processInput();
        shuffle.processInput();
        copyAttributes.processInput();
        copyGlobalAttributes.processInput();
        NetCDFSettingsContainer convertedInput = new NetCDFSettingsContainer();
        convertedInput.setCompressionLevel(compressionLevel.getModelObject());
        convertedInput.setGlobalAttributes(globalAttributes.getModelObject());
        convertedInput.setVariableAttributes(variableAttributes.getModelObject());
        convertedInput.setExtraVariables(extraVariables.getModelObject());
        convertedInput.setDataPacking(dataPacking.getModelObject());
        convertedInput.setShuffle(shuffle.getModelObject());
        convertedInput.setCopyAttributes(copyAttributes.getModelObject());
        convertedInput.setCopyGlobalAttributes(copyGlobalAttributes.getModelObject());

        extensionPanels.visitChildren(
                (component, visit) -> {
                    if (component instanceof NetCDFExtensionPanel) {
                        NetCDFExtensionPanel extension = (NetCDFExtensionPanel) component;
                        extension.convertInput(convertedInput);
                    }
                });

        setConvertedInputGenerics(convertedInput);
    }

    @SuppressWarnings("unchecked")
    private void setConvertedInputGenerics(NetCDFSettingsContainer convertedInput) {
        setConvertedInput((T) convertedInput);
    }

    protected ListView<NetCDFExtensionPanelInfo> createExtensionPanelList(
            String id, final IModel infoModel) {
        List<NetCDFExtensionPanelInfo> panels =
                GeoServerApplication.get().getBeansOfType(NetCDFExtensionPanelInfo.class);
        return new ListView<NetCDFExtensionPanelInfo>(id, panels) {

            @Override
            protected void populateItem(ListItem<NetCDFExtensionPanelInfo> item) {
                NetCDFExtensionPanelInfo info = item.getModelObject();
                try {
                    NetCDFExtensionPanel panel =
                            info.getComponentClass()
                                    .getConstructor(String.class, IModel.class, NetCDFPanel.class)
                                    .newInstance("content", infoModel, NetCDFPanel.this);
                    item.add(panel);
                } catch (Exception e) {
                    throw new WicketRuntimeException(
                            "Failed to create NetCDF extension panel of "
                                    + "type "
                                    + info.getComponentClass().getSimpleName(),
                            e);
                }
            }
        };
    }

    private class VariableAttributeListView extends ListView<VariableAttribute> {

        public VariableAttributeListView(IModel<List<VariableAttribute>> varAttributesModel) {
            super("variableAttributes", varAttributesModel);
        }

        @Override
        protected void populateItem(final ListItem<VariableAttribute> item) {
            Label keyField =
                    new Label("variableAttributeKey", new PropertyModel<>(item.getModel(), "key"));
            item.add(keyField);
            Label valueField =
                    new Label(
                            "variableAttributeValue",
                            new PropertyModel<>(item.getModel(), "value"));
            item.add(valueField);
            Component removeLink =
                    new ImageAjaxLink("removeVariableAttributeIcon", DELETE_ICON) {

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            List<VariableAttribute> list =
                                    new ArrayList<>(variableAttributes.getModelObject());
                            VariableAttribute attribute =
                                    (VariableAttribute) getDefaultModelObject();
                            list.remove(attribute);
                            variableAttributes.setModelObject(list);
                            item.remove();
                            target.add(container);
                        }
                    };
            removeLink.setDefaultModel(item.getModel());
            item.add(removeLink);
        }
    }

    private class ExtraVariableListView extends ListView<ExtraVariable> {

        public ExtraVariableListView(IModel<List<ExtraVariable>> extraVarModel) {
            super("extraVariables", extraVarModel);
        }

        @Override
        protected void populateItem(final ListItem<ExtraVariable> item) {
            item.add(
                    new Label(
                            "extraVariableSource", new PropertyModel<>(item.getModel(), "source")));
            item.add(
                    new Label(
                            "extraVariableOutput", new PropertyModel<>(item.getModel(), "output")));
            item.add(
                    new Label(
                            "extraVariableDimensions",
                            new PropertyModel<>(item.getModel(), "dimensions")));
            Component removeLink =
                    new ImageAjaxLink("removeExtraVariableIcon", DELETE_ICON) {

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            List<ExtraVariable> list =
                                    new ArrayList<>(extraVariables.getModelObject());
                            final ExtraVariable attribute = (ExtraVariable) getDefaultModelObject();
                            list.remove(attribute);
                            extraVariables.setModelObject(list);
                            item.remove();
                            target.add(container);
                        }
                    };
            removeLink.setDefaultModel(item.getModel());
            item.add(removeLink);
        }
    }

    private class AddVariableLink extends GeoServerAjaxFormLink {

        private final TextField<String> newKey;
        private final TextField<String> newValue;

        public AddVariableLink(TextField<String> newKey, TextField<String> newValue) {
            super("addVariableAttribute");
            this.newKey = newKey;
            this.newValue = newValue;
        }

        @Override
        protected void onClick(AjaxRequestTarget ajaxTarget, Form form) {
            newKey.processInput();
            newValue.processInput();
            String key = newKey.getModelObject();
            String value = newValue.getModelObject();
            if (key == null || key.trim().isEmpty()) {
                ParamResourceModel rm = new ParamResourceModel("NetCDFOut.emptyKey", null, "");
                error(rm.getString());
            } else {
                VariableAttribute attribute = new VariableAttribute(key, value);
                if (!variableAttributes.getModelObject().contains(attribute)) {
                    variableAttributes.getModelObject().add(attribute);
                }
                newKey.setModel(Model.of(""));
                newValue.setModel(Model.of(""));
                ajaxTarget.add(container);
            }
        }
    }

    private class AddExtraVariableLink extends GeoServerAjaxFormLink {

        private final TextField<String> newSource;
        private final TextField<String> newOutput;
        private final TextField<String> newDimensions;

        public AddExtraVariableLink(
                TextField<String> newSource,
                TextField<String> newOutput,
                TextField<String> newDimensions) {
            super("addExtraVariable");
            this.newSource = newSource;
            this.newOutput = newOutput;
            this.newDimensions = newDimensions;
        }

        @Override
        protected void onClick(AjaxRequestTarget ajaxTarget, Form form) {
            newSource.processInput();
            newOutput.processInput();
            newDimensions.processInput();
            String source = newSource.getModelObject();
            String output = newOutput.getModelObject();
            String dimensions = newDimensions.getModelObject();
            if ((source == null || source.trim().isEmpty())
                    && (output == null || output.trim().isEmpty())) {
                ParamResourceModel rm =
                        new ParamResourceModel("NetCDFOut.emptySourceOutput", null, "");
                error(rm.getString());
            } else if (dimensions != null && dimensions.split("\\s").length > 1) {
                ParamResourceModel rm =
                        new ParamResourceModel("NetCDFOut.tooManyDimensions", null, "");
                error(rm.getString());
            } else {
                extraVariables.getModelObject().add(new ExtraVariable(source, output, dimensions));
                newOutput.setModel(Model.of(""));
                newSource.setModel(Model.of(""));
                newDimensions.setModel(Model.of(""));
                ajaxTarget.add(container);
            }
        }
    }
}
