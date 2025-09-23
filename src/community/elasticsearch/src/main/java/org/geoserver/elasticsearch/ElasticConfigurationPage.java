/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.elasticsearch;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.feature.type.Name;
import org.geotools.data.elasticsearch.ElasticAttribute;
import org.geotools.data.elasticsearch.ElasticDataStore;
import org.geotools.data.elasticsearch.ElasticLayerConfiguration;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

// TODO WICKET8 - Verify this page works OK
/**
 * Class to render and manage the Elasticsearch modal dialog This dialog allow the user to choice which Elasticsearch
 * attributes include in layers, selects attribute to use as GEOMETRY.
 */
abstract class ElasticConfigurationPage extends Panel {

    @Serial
    private static final long serialVersionUID = 5615867383881988931L;

    private static final Logger LOGGER = Logging.getLogger(ElasticConfigurationPage.class);

    private final String useAllMarkupId;

    private static final List<Class<? extends Geometry>> GEOMETRY_TYPES = Arrays.asList(
            Geometry.class,
            GeometryCollection.class,
            Point.class,
            MultiPoint.class,
            LineString.class,
            MultiLineString.class,
            Polygon.class,
            MultiPolygon.class);

    /**
     * Constructs the dialog to set Elasticsearch attributes and configuration options.
     *
     * @see ElasticAttributeProvider
     * @see ElasticAttribute
     */
    public ElasticConfigurationPage(String panelId, final IModel<?> model, boolean isRefresh) {
        super(panelId, model);

        ResourceInfo ri = (ResourceInfo) model.getObject();

        @SuppressWarnings("unchecked")
        final Form<?> elastic_form = new Form("es_form", new CompoundPropertyModel(this));
        add(elastic_form);

        List<ElasticAttribute> attributes;
        attributes = fillElasticAttributes(ri, isRefresh).getAttributes();
        final ElasticAttributeProvider attProvider = new ElasticAttributeProvider(attributes);

        final GeoServerTablePanel<ElasticAttribute> elasticAttributePanel;
        elasticAttributePanel = getElasticAttributePanel(attProvider);
        elastic_form.add(elasticAttributePanel);

        // select all check box
        boolean selectAll = true;
        for (final ElasticAttribute attribute : attributes) {
            if (attribute.isUse() == null || !attribute.isUse()) {
                selectAll = false;
            }
        }
        AjaxCheckBox useAllCheckBox = new AjaxCheckBox("useAll", Model.of(selectAll)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final boolean use = (Boolean) this.getDefaultModelObject();
                for (final ElasticAttribute attribute : attProvider.getItems()) {
                    attribute.setUse(use);
                }
                target.add(elasticAttributePanel);
            }
        };
        useAllCheckBox.setOutputMarkupId(true);
        elastic_form.add(useAllCheckBox);
        useAllMarkupId = useAllCheckBox.getMarkupId();

        elastic_form.add(new AjaxButton("es_save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onSave(target);
            }
        });

        elastic_form.add(new AjaxButton("es_refresh") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                onRefresh(target);
            }
        });

        FeedbackPanel feedbackPanel = new FeedbackPanel("es_feedback");
        feedbackPanel.setOutputMarkupId(true);
        elastic_form.add(feedbackPanel);
    }

    /** Do nothing */
    @SuppressWarnings("unused")
    protected void onCancel(AjaxRequestTarget target) {
        done(target, null, null);
    }

    protected void onRefresh(AjaxRequestTarget target) {
        refresh(target);
    }

    /**
     * Validates Elasticsearch attributes configuration and stores the Elasticsearch layer configuration into feature
     * type metadata as {@link ElasticLayerConfiguration#KEY} <br>
     * Validation include the follow rules
     * <li>One attribute must be a GEOMETRY.
     *
     * @see ElasticLayerConfiguration
     * @see FeatureTypeInfo#getMetadata
     */
    private void onSave(AjaxRequestTarget target) {
        try {
            ResourceInfo ri = (ResourceInfo) getDefaultModel().getObject();
            ElasticLayerConfiguration layerConfig = fillElasticAttributes(ri, false);
            boolean geomSet = false;
            // Validate configuration
            for (ElasticAttribute att : layerConfig.getAttributes()) {
                if (Geometry.class.isAssignableFrom(att.getType()) && att.isUse()) {
                    geomSet = true;
                }
            }
            if (!geomSet) {
                error(new ParamResourceModel("geomEmptyFailure", this).getString());
            }

            Catalog catalog = ((GeoServerApplication) this.getPage().getApplication()).getCatalog();
            FeatureTypeInfo typeInfo;
            DataStoreInfo dsInfo = catalog.getStore(ri.getStore().getId(), DataStoreInfo.class);
            ElasticDataStore ds = (ElasticDataStore) dsInfo.getDataStore(null);
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(dsInfo);
            typeInfo = builder.buildFeatureType(ds.getFeatureSource(ri.getQualifiedName()));
            typeInfo.setName(ri.getName());
            typeInfo.setNativeName(ri.getName());
            typeInfo.getMetadata().put(ElasticLayerConfiguration.KEY, layerConfig);
            LayerInfo layerInfo = builder.buildLayer(typeInfo);
            layerInfo.setName(ri.getName());

            done(target, layerInfo, layerConfig);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            error(new ParamResourceModel("creationFailure", this, e).getString());
        }
    }

    /*
     * Load ElasticLayerConfiguration configuration before shows on table Reloads
     * Elasticsearch attributes from datastore and merge it with user attributes
     * configurations
     */
    private ElasticLayerConfiguration fillElasticAttributes(ResourceInfo ri, boolean refresh) {

        ElasticLayerConfiguration layerConfig =
                (ElasticLayerConfiguration) ri.getMetadata().get(ElasticLayerConfiguration.KEY);

        if (layerConfig == null) {
            layerConfig = new ElasticLayerConfiguration(ri.getName());
            ri.getMetadata().put(ElasticLayerConfiguration.KEY, layerConfig);
        }

        try {
            ElasticDataStore dataStore =
                    (ElasticDataStore) ((DataStoreInfo) ri.getStore()).getDataStore(new NullProgressListener());

            ArrayList<ElasticAttribute> result = new ArrayList<>();
            Map<String, ElasticAttribute> tempMap = new HashMap<>();
            final List<ElasticAttribute> attributes = layerConfig.getAttributes();
            for (ElasticAttribute att : attributes) {
                tempMap.put(att.getName(), att);
            }

            final String docType = layerConfig.getDocType();
            final Name layerName = new NameImpl(layerConfig.getLayerName());
            dataStore.getDocTypes().put(layerName, docType);
            if (refresh) {
                // The datastore caches the layer configuration, so we need to clear it by adding an
                // empty one if we are refreshing
                ElasticLayerConfiguration emptyLayerConfiguration =
                        new ElasticLayerConfiguration(layerConfig.getLayerName());
                dataStore.getLayerConfigurations().put(layerConfig.getLayerName(), emptyLayerConfiguration);
            }

            for (ElasticAttribute at : dataStore.getElasticAttributes(layerName)) {
                if (tempMap.containsKey(at.getName())) {
                    at = tempMap.get(at.getName());
                }
                result.add(at);
            }
            layerConfig.getAttributes().clear();
            layerConfig.getAttributes().addAll(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        Collections.sort(layerConfig.getAttributes());
        return layerConfig;
    }

    /*
     * Builds attribute table
     */
    private GeoServerTablePanel<ElasticAttribute> getElasticAttributePanel(ElasticAttributeProvider attProvider) {
        GeoServerTablePanel<ElasticAttribute> atts =
                new GeoServerTablePanel<ElasticAttribute>("esAttributes", attProvider) {
                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<ElasticAttribute> itemModel, Property<ElasticAttribute> property) {
                        ElasticAttribute att = itemModel.getObject();
                        boolean isGeometry = att.getType() != null && Geometry.class.isAssignableFrom(att.getType());
                        if (property == ElasticAttributeProvider.NAME && isGeometry) {
                            Fragment f = new Fragment(id, "label", ElasticConfigurationPage.this);
                            f.add(new Label("label", att.getDisplayName() + "*"));
                            return f;
                        } else if (property == ElasticAttributeProvider.TYPE && isGeometry) {
                            Fragment f = new Fragment(id, "geometry", ElasticConfigurationPage.this);
                            //noinspection unchecked
                            f.add(new DropDownChoice<>(
                                    "geometry",
                                    new PropertyModel<>(itemModel, "type"),
                                    GEOMETRY_TYPES,
                                    new GeometryTypeRenderer()));
                            return f;
                        } else if (property == ElasticAttributeProvider.USE) {
                            CheckBox checkBox = new CheckBox("use", new PropertyModel<>(itemModel, "use"));
                            checkBox.setOutputMarkupId(true);
                            String script = "document.getElementById('"
                                    + checkBox.getMarkupId()
                                    + "').addEventListener('click', function(event) {\n"
                                    + "    document.getElementById('"
                                    + useAllMarkupId
                                    + "').checked = false;\n"
                                    + "});";
                            checkBox.add(new Behavior() {

                                @Override
                                public void renderHead(Component component, IHeaderResponse response) {
                                    super.renderHead(component, response);
                                    response.render(OnLoadHeaderItem.forScript(script));
                                }
                            });
                            Fragment f = new Fragment(id, "checkboxUse", ElasticConfigurationPage.this);
                            f.add(checkBox);
                            return f;
                        } else if (property == ElasticAttributeProvider.DEFAULT_GEOMETRY) {
                            if (isGeometry) {
                                Fragment f = new Fragment(id, "checkboxDefaultGeometry", ElasticConfigurationPage.this);
                                f.add(new CheckBox(
                                        "defaultGeometry", new PropertyModel<>(itemModel, "defaultGeometry")));
                                return f;
                            } else {
                                return new Fragment(id, "empty", ElasticConfigurationPage.this);
                            }
                        } else if (property == ElasticAttributeProvider.SRID) {
                            if (isGeometry) {
                                Fragment f = new Fragment(id, "label", ElasticConfigurationPage.this);
                                f.add(new Label("label", String.valueOf(att.getSrid())));
                                return f;
                            } else {
                                return new Fragment(id, "empty", ElasticConfigurationPage.this);
                            }
                        } else if (property == ElasticAttributeProvider.DATE_FORMAT) {
                            List<String> validFormats = null;
                            if (att.getValidDateFormats() == null) {
                                if (att.getDateFormat() != null) {
                                    validFormats = new ArrayList<>();
                                    validFormats.add(att.getDateFormat());
                                    att.setValidDateFormats(validFormats);
                                }
                            } else {
                                validFormats = att.getValidDateFormats();
                            }
                            if (validFormats != null) {
                                Fragment f = new Fragment(id, "label", ElasticConfigurationPage.this);
                                String format = "";
                                for (int i = 0; i < validFormats.size(); i++) {
                                    if (i != validFormats.size()) {
                                        format = format + validFormats.get(i) + " || ";
                                    } else {
                                        format = format + validFormats.get(i);
                                    }
                                }
                                f.add(new Label("label", format));
                                return f;
                            } else {
                                return new Fragment(id, "empty", ElasticConfigurationPage.this);
                            }
                        } else if (property == ElasticAttributeProvider.ANALYZED) {
                            if (att.getAnalyzed() != null && att.getAnalyzed()) {
                                Fragment f = new Fragment(id, "label", ElasticConfigurationPage.this);
                                f.add(new Label("label", "x"));
                                return f;
                            } else {
                                return new Fragment(id, "empty", ElasticConfigurationPage.this);
                            }
                        } else if (property == ElasticAttributeProvider.STORED) {
                            if (att.isStored()) {
                                Fragment f = new Fragment(id, "label", ElasticConfigurationPage.this);
                                f.add(new Label("label", "x"));
                                return f;
                            } else {
                                return new Fragment(id, "empty", ElasticConfigurationPage.this);
                            }
                        } else if (property == ElasticAttributeProvider.ORDER) {
                            TextField<Integer> order =
                                    new TextField<>("order", new PropertyModel<>(itemModel, "order"));
                            Fragment f = new Fragment(id, "textOrderValue", ElasticConfigurationPage.this);
                            f.add(order);
                            return f;
                        } else if (property == ElasticAttributeProvider.CUSTOM_NAME) {
                            TextField<String> customName =
                                    new TextField<>("customName", new PropertyModel<>(itemModel, "customName"));
                            Fragment f = new Fragment(id, "textCustomNameValue", ElasticConfigurationPage.this);
                            f.add(customName);
                            return f;
                        }
                        return null;
                    }

                    @Override
                    protected void onPopulateItem(
                            Property<ElasticAttribute> property, ListItem<Property<ElasticAttribute>> item) {
                        if (property == ElasticAttributeProvider.STORED) {
                            item.add(AttributeModifier.replace("class", "textAlignCenter"));
                        } else if (property == ElasticAttributeProvider.ANALYZED) {
                            item.add(AttributeModifier.replace("class", "textAlignCenter"));
                        }
                    }
                };
        atts.setOutputMarkupId(true);
        atts.setFilterVisible(false);
        atts.setSortable(false);
        atts.setPageable(false);
        atts.setOutputMarkupId(true);
        return atts;
    }

    /*
     * Render geometry type select
     */
    private static class GeometryTypeRenderer implements IChoiceRenderer<Class<? extends Geometry>> {

        @Override
        public Object getDisplayValue(Class<? extends Geometry> object) {
            return object.getSimpleName();
        }

        @Override
        public String getIdValue(Class<? extends Geometry> object, int index) {
            return (String) getDisplayValue(object);
        }

        @Override
        public Class<? extends Geometry> getObject(
                String id, IModel<? extends List<? extends Class<? extends Geometry>>> choices) {
            for (Class<? extends Geometry> c : GEOMETRY_TYPES) {
                if (id.equals(getDisplayValue(c))) {
                    return c;
                }
            }
            return null;
        }
    }

    /**
     * Abstract method to implements in panel that opens the dialog to close the dialog itself <br>
     * This method is called after modal executes its operation
     *
     * @param target ajax response target
     * @param layerInfo GeoServer layer configuration
     * @param layerConfig Elasticsearch layer configuration
     * @see #onSave
     * @see #onCancel
     */
    abstract void done(AjaxRequestTarget target, LayerInfo layerInfo, ElasticLayerConfiguration layerConfig);

    abstract void refresh(AjaxRequestTarget target);
}
