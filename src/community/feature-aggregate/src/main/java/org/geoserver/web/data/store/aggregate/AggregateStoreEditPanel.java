/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import static org.geotools.data.aggregate.AggregatingDataStoreFactory.CONFIGURATION;
import static org.geotools.data.aggregate.AggregatingDataStoreFactory.CONFIGURATION_XML;
import static org.geotools.data.aggregate.AggregatingDataStoreFactory.PARALLELISM;
import static org.geotools.data.aggregate.AggregatingDataStoreFactory.TOLERATE_CONNECTION_FAILURE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.data.aggregate.AggregateTypeConfiguration;
import org.geotools.data.aggregate.AggregatingDataStoreFactory;

/**
 * Provides the form components for the shapefile datastore
 *
 * @author Andrea Aime - GeoSolution
 */
@SuppressWarnings("serial")
public class AggregateStoreEditPanel extends StoreEditPanel {

    private List<AggregateTypeConfiguration> configs;
    private GeoServerTablePanel<AggregateTypeConfiguration> configTable;
    private ConfigModel configModel;

    public AggregateStoreEditPanel(final String componentId, final Form<?> storeEditForm) {
        super(componentId, storeEditForm);

        final IModel<?> model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Object>> paramsModel =
                new PropertyModel<Map<String, Object>>(model, "connectionParameters");
        new MapModel<String>(paramsModel, CONFIGURATION.key).setObject(null);

        add(
                new TextParamPanel<String>(
                        "parallelism",
                        new MapModel<String>(paramsModel, PARALLELISM.key),
                        new ParamResourceModel("parallelism", this),
                        true));

        add(
                new CheckBoxParamPanel(
                        "tolerateErrors",
                        new MapModel<String>(paramsModel, TOLERATE_CONNECTION_FAILURE.key),
                        new ParamResourceModel("tolerateErrors", this)));

        configModel = new ConfigModel(new MapModel<String>(paramsModel, CONFIGURATION_XML.key));
        configs = configModel.getObject();
        if (configs == null) {
            configs = new ArrayList<AggregateTypeConfiguration>();
            configModel.setObject(configs);
        }
        configTable =
                new GeoServerTablePanel<AggregateTypeConfiguration>(
                        "configTable", new ConfigurationListProvider(configs)) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<AggregateTypeConfiguration> itemModel,
                            Property<AggregateTypeConfiguration> property) {
                        if (property == ConfigurationListProvider.NAME) {
                            return editLink(id, itemModel);
                        } else if (property == ConfigurationListProvider.SOURCES) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property == ConfigurationListProvider.REMOVE) {
                            return removeLink(id, itemModel);
                        } else {
                            return null;
                        }
                    }
                };
        configTable.setPageable(false);
        configTable.setFilterable(false);
        configTable.setOutputMarkupId(true);
        add(configTable);

        add(addNewLink());
    }

    protected Component addNewLink() {
        AjaxLink<Void> link =
                new AjaxLink<Void>("addNew") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new ConfigNewPage(AggregateStoreEditPanel.this));
                    }
                };
        return link;
    }

    protected Component editLink(String id, IModel<AggregateTypeConfiguration> itemModel) {
        SimpleAjaxLink<?> link =
                new SimpleAjaxLink<AggregateTypeConfiguration>(
                        id, itemModel, new PropertyModel<String>(itemModel, "name")) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        setResponsePage(
                                new ConfigEditPage(
                                        AggregateStoreEditPanel.this,
                                        (AggregateTypeConfiguration) getModelObject()));
                    }
                };
        return link;
    }

    Component removeLink(String id, IModel<AggregateTypeConfiguration> itemModel) {
        final AggregateTypeConfiguration entry = (AggregateTypeConfiguration) itemModel.getObject();
        ImageAjaxLink<?> link =
                new ImageAjaxLink<Object>(
                        id,
                        new PackageResourceReference(
                                GeoServerApplication.class, "img/icons/silk/delete.png")) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {

                        configs.remove(entry);
                        target.add(configTable);
                    }

                    protected IAjaxCallListener getAjaxCallListener() {
                        return new AjaxCallListener() {

                            @Override
                            public CharSequence getBeforeHandler(Component component) {
                                String msg =
                                        new ParamResourceModel(
                                                        "confirmTypeRemoval",
                                                        AggregateStoreEditPanel.this,
                                                        entry.getName())
                                                .getString();
                                return "if(!confirm('"
                                        + msg.replaceAll("'", "\\\\'")
                                        + "')) return false;";
                            }
                        };
                    }
                };
        link.getImage()
                .add(
                        new AttributeModifier(
                                "alt",
                                new ParamResourceModel("AggregateStoreEditPanel.th.remove", link)));
        return link;
    }

    @Override
    public boolean onSave() {
        configModel.setObject(configs);
        return true;
    }

    class ConfigModel implements IModel<List<AggregateTypeConfiguration>> {
        IModel<String> model;

        public ConfigModel(IModel<String> model) {
            this.model = model;
        }

        @Override
        public void detach() {
            model.detach();
        }

        @Override
        public List<AggregateTypeConfiguration> getObject() {
            String xml = (String) model.getObject();
            try {
                return new AggregatingDataStoreFactory().parseConfiguration(xml);
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        @Override
        public void setObject(List<AggregateTypeConfiguration> configs) {
            try {
                String xml = new AggregatingDataStoreFactory().encodeConfiguration(configs);
                model.setObject(xml);
            } catch (IOException e) {
                throw new RuntimeException("Failed to encode the configurations back to xml");
            }
        }
    }

    public void addConfiguration(AggregateTypeConfiguration config) {
        configs.add(config);
    }
}
