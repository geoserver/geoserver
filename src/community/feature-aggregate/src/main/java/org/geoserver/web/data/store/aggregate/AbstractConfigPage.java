/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.data.aggregate.AggregateTypeConfiguration;
import org.geotools.data.aggregate.SourceType;

/** Handles layer group */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public abstract class AbstractConfigPage extends GeoServerSecuredPage {

    AggregateStoreEditPanel master;

    private DropDownChoice<StoreInfo> stores;

    private DropDownChoice<String> types;

    private GeoServerTablePanel<SourceType> sourceTypes;

    private Form<AggregateTypeConfiguration> form;

    private Component addLink;

    Model<AggregateTypeConfiguration> configModel;

    private WebMarkupContainer sources;

    private TextField name;

    private AggregateTypeConfiguration originalConfig;

    public AbstractConfigPage(AggregateStoreEditPanel master) {
        this.master = master;
    }

    /**
     * Subclasses must call this method to initialize the UI for this page
     *
     * @param layerGroup
     */
    protected void initUI(AggregateTypeConfiguration config) {
        // we need to clone the config since the table will modify it directly, it's not like
        // a normal form component
        originalConfig = config;
        configModel = new Model<AggregateTypeConfiguration>(new AggregateTypeConfiguration(config));
        form = new Form<AggregateTypeConfiguration>("form", new CompoundPropertyModel(configModel));
        add(form);
        name = new TextField("name");
        name.setOutputMarkupId(true);
        name.setRequired(true);
        form.add(name);

        sources = new WebMarkupContainer("sources");
        sources.setOutputMarkupId(true);
        form.add(sources);

        stores =
                new DropDownChoice<StoreInfo>(
                        "stores",
                        new Model(null),
                        new StoreListModel(),
                        new StoreListChoiceRenderer());
        stores.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        types.setChoices(new TypeListModel(stores.getModel()));
                        addLink.setEnabled(false);
                        target.add(types);
                        target.add(addLink);
                    }
                });
        sources.add(stores);
        types =
                new DropDownChoice<String>(
                        "types", new Model(null), new TypeListModel(stores.getModel()));
        types.setOutputMarkupId(true);
        sources.add(types);
        types.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        addLink.setEnabled(
                                stores.getModelObject() != null && types.getModelObject() != null);
                        target.add(addLink);
                    }
                });
        addLink = addConfigLink();
        addLink.setOutputMarkupId(true);
        addLink.setEnabled(false);
        sources.add(addLink);

        sourceTypes =
                new GeoServerTablePanel<SourceType>(
                        "sourceTypes", new SourceTypeProvider(configModel)) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<SourceType> itemModel,
                            Property<SourceType> property) {
                        if (property.getName().equals("default")) {
                            return new Label(id, property.getModel(itemModel));
                        } else if (property.getName().equals("makeDefault")) {
                            return makeDefaultLink(id, itemModel);
                        } else if (property.getName().equals("remove")) {
                            return removeLink(id, itemModel);
                        } else {
                            return null;
                        }
                    }
                };
        sourceTypes.setPageable(false);
        sourceTypes.setFilterable(false);
        sources.add(sourceTypes);

        form.add(saveLink());
        form.add(cancelLink());
    }

    protected Component removeLink(String id, IModel itemModel) {
        SimpleAjaxLink link =
                new SimpleAjaxLink(id, itemModel, new ParamResourceModel("remove", this)) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        // remove the link
                        AggregateTypeConfiguration config = form.getModelObject();
                        config.getSourceTypes().remove(getModelObject());
                        // refresh the whole form (lazy, we could add a container around the table)
                        target.add(sources);
                    }
                };
        return link;
    }

    protected Component makeDefaultLink(String id, IModel itemModel) {
        return new SimpleAjaxLink(id, itemModel, new ParamResourceModel("makeDefault", this)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                // remove the link
                AggregateTypeConfiguration config = form.getModelObject();
                config.setPrimarySourceType((SourceType) getModelObject());
                // refresh the whole form (lazy, we could add a container around the table)
                target.add(sources);
            }
        };
    }

    private Component addConfigLink() {
        return new AjaxLink<Void>("addType") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AggregateTypeConfiguration config = form.getModelObject();
                String storeName = stores.getModelObject().getName();
                String typeName = types.getModelObject();
                stores.setModelObject(null);
                types.setModelObject(null);
                config.addSourceType(storeName, typeName);
                if (name.getModelObject() == null || "".equals(name.getModelObject())) {
                    name.setModelObject(typeName);
                    target.add(name);
                }
                // refresh the whole table
                target.add(sources);
            }
        };
    }

    private Link cancelLink() {
        return new Link("cancel") {

            @Override
            public void onClick() {
                setResponsePage(master.getPage());
            }
        };
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                AggregateTypeConfiguration config = form.getModelObject();
                List<SourceType> stypes = config.getSourceTypes();
                if (stypes == null || stypes.size() == 0) {
                    error(
                            new ParamResourceModel("atLeastOneSource", AbstractConfigPage.this)
                                    .getString());
                } else if (AbstractConfigPage.this.onSubmit()) {
                    originalConfig.copyFrom(config);
                    setResponsePage(master.getPage());
                }
            }
        };
    }

    /** Subclasses */
    protected abstract boolean onSubmit();
}
