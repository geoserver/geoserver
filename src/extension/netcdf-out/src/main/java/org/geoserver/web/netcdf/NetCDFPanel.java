/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
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
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.GlobalAttribute;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class NetCDFPanel<T extends NetCDFSettingsContainer> extends FormComponentPanel<T>{

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    protected final ListView<GlobalAttribute> globalAttributes;
    protected final CheckBox shuffle;

    protected final TextField<Integer> compressionLevel;

    public static final ResourceReference ADD_ICON = new ResourceReference(GeoServerBasePage.class,
            "img/icons/silk/add.png");

    public static final ResourceReference DELETE_ICON = new ResourceReference(
            GeoServerBasePage.class, "img/icons/silk/delete.png");

    protected final DropDownChoice<DataPacking> dataPacking;

    protected final WebMarkupContainer container;

    public NetCDFPanel(String id, IModel<T> netcdfModel) {
        super(id, netcdfModel);

        // New Container
        // container for ajax updates
        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);


        // CheckBox associated to the shuffle parameter
        shuffle = new CheckBox("shuffle", new PropertyModel(netcdfModel, "shuffle"));
        container.add(shuffle);

        // TextBox associated to the compression parameter
        compressionLevel = new TextField<Integer>("compressionLevel",
                new PropertyModel(netcdfModel, "compressionLevel"));

        List<DataPacking> dataPackings = Arrays.asList(DataPacking.values());
        dataPacking = new DropDownChoice<DataPacking>("dataPacking", new PropertyModel(netcdfModel,
                "dataPacking"), dataPackings);
        dataPacking.setOutputMarkupId(true);
        container.add(dataPacking);        

        compressionLevel.add(new RangeValidator(0, 9));
        container.add(compressionLevel);

        IModel<List<GlobalAttribute>> attributeModel = new PropertyModel(netcdfModel,
                "globalAttributes");
        // Global Attributes definition
        globalAttributes = new ListView<GlobalAttribute>("globalAttributes", attributeModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<GlobalAttribute> item) {
                // Create form
                final Label keyField;
                keyField = new Label("key", new PropertyModel<String>(item.getModel(), "key"));
                item.add(keyField);

                // Create form
                final Label valueField;
                valueField = new Label("value", new PropertyModel<String>(item.getModel(), "value"));
                item.add(valueField);

                final Component removeLink;

                removeLink = new ImageAjaxLink("remove", DELETE_ICON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        List<GlobalAttribute> list;
                        list = new ArrayList<GlobalAttribute>(globalAttributes.getModelObject());
                        final GlobalAttribute attribute = (GlobalAttribute) getDefaultModelObject();

                        list.remove(attribute);
                        globalAttributes.setModelObject(list);
                        item.remove();

                        target.addComponent(container);
                    }
                };
                removeLink.setDefaultModel(item.getModel());
                item.add(removeLink);
            }
        };
        globalAttributes.setOutputMarkupId(true);
        container.add(globalAttributes);

        // TextField for a new Value
        final TextField<String> newValue = new TextField<String>("newValue", Model.of(""));
        newValue.setOutputMarkupId(true);
        container.add(newValue);

        // TextField for a new Key
        final TextField<String> newKey = new TextField<String>("newKey", Model.of(""));
        newKey.setOutputMarkupId(true);
        container.add(newKey);

        GeoServerAjaxFormLink addLink = new GeoServerAjaxFormLink("add") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                newValue.processInput();
                newKey.processInput();
                String key = newKey.getModelObject();
                if (key == null || key.isEmpty()) {
                    ParamResourceModel rm = new ParamResourceModel(
                            "NetCDFOutSettingsPanel.nonEmptyKey", null, "");
                    error(rm.getString());
                } else {
                    String value = newValue.getModelObject();
                    GlobalAttribute attribute = new GlobalAttribute(key, value);
                    if (!globalAttributes.getModelObject().contains(attribute)) {
                        globalAttributes.getModelObject().add(attribute);
                    }
                    newKey.setModel(Model.of("")); // Reset the key field
                    newValue.setModel(Model.of("")); // Reset the Value field

                    target.addComponent(container);
                }
            }
        };
        addLink.add(new Icon("addIcon", ADD_ICON));
        container.add(addLink);
        NetCDFSettingsContainer object = netcdfModel.getObject();
        if (object == null) {
            netcdfModel.setObject((T)new NetCDFSettingsContainer());
        }
    }

    @Override
    public void convertInput() {
        globalAttributes.visitChildren((component, visit) -> {
            if (component instanceof FormComponent) {
                FormComponent<?> formComponent = (FormComponent<?>) component;
                formComponent.processInput();
            }
        });
        compressionLevel.processInput();
        dataPacking.processInput();
        shuffle.processInput();
        List<GlobalAttribute> info = globalAttributes.getModelObject();
        NetCDFSettingsContainer convertedInput = new NetCDFSettingsContainer();
        convertedInput.setCompressionLevel(compressionLevel.getModelObject());
        convertedInput.setGlobalAttributes(info);
        convertedInput.setDataPacking(dataPacking.getModelObject());
        convertedInput.setShuffle(shuffle.getModelObject());
        setConvertedInput((T) convertedInput);
    }
}
