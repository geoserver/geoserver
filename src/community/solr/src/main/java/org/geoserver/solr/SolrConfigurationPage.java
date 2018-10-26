/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.solr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.solr.SolrAttribute;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrLayerConfiguration;
import org.geotools.data.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Class to render and manage the SOLR modal dialog This dialog allow the user to choice which SOLR
 * attributes include in layers, selects attributes to use as PK, as GEOMETRY, and set native SRID
 */
public abstract class SolrConfigurationPage extends Panel {

    private static final long serialVersionUID = 5615867383881988931L;

    private static final Logger LOGGER = Logging.getLogger(SolrConfigurationPage.class);

    private String solrurl;

    private FeedbackPanel feedbackPanel;

    private static final List<Class<?>> GEOMETRY_TYPES =
            Arrays.asList(
                    Geometry.class,
                    GeometryCollection.class,
                    Point.class,
                    MultiPoint.class,
                    LineString.class,
                    MultiLineString.class,
                    Polygon.class,
                    MultiPolygon.class);

    /**
     * Constructs the dialog to set SOLR attributes with the follow components:
     * <li>The checkbox to hide/show the empty attributes
     * <li>The table with SOLR attributes and configuration options
     *
     * @see {@link SolrAttributeProvider}
     * @see {@link SolrAttribute}
     */
    public SolrConfigurationPage(String panelId, final IModel<?> model) {
        super(panelId, model);

        ResourceInfo ri = (ResourceInfo) model.getObject();

        DataStoreInfo store = (DataStoreInfo) ri.getStore();

        Map<String, Serializable> connectionparameters = store.getConnectionParameters();

        solrurl = (String) connectionparameters.get("solr_url");

        final Form<SolrConfigurationPage> solr_form =
                new Form<SolrConfigurationPage>(
                        "solr_form", new CompoundPropertyModel<SolrConfigurationPage>(this));
        add(solr_form);

        List<SolrAttribute> attributes =
                fillSolrAttributes((ResourceInfo) model.getObject()).getAttributes();
        final SolrAttributeProvider attProvider = new SolrAttributeProvider(attributes);

        final GeoServerTablePanel<SolrAttribute> solrAttributePanel =
                getSolrAttributePanel(attProvider);
        solr_form.add(solrAttributePanel);

        AjaxCheckBox checkBox =
                new AjaxCheckBox("hideEmpty", Model.of(Boolean.TRUE)) {
                    /** */
                    private static final long serialVersionUID = 8715377219204904531L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        attProvider.reload((Boolean) this.getDefaultModelObject());
                        target.add(solrAttributePanel);
                    }
                };

        checkBox.setOutputMarkupId(true);
        solr_form.add(checkBox);

        solr_form.add(
                new AjaxButton("solr_save") {
                    /** */
                    private static final long serialVersionUID = 819555072210390051L;

                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        onSave(target);
                    }
                });

        feedbackPanel = new FeedbackPanel("solr_feedback");
        feedbackPanel.setOutputMarkupId(true);
        solr_form.add(feedbackPanel);
    }

    /** Do nothing */
    protected void onCancel(AjaxRequestTarget target) {
        done(target, null);
    }

    /**
     * Validates SOLR attributes configuration and stores the SOLR layer configuration into feature
     * type metadata as {@link SolrLayerConfiguration#KEY} <br>
     * Validation include the follow rules
     * <li>One attribute must be a PK
     * <li>One attribute must be a GEOMETRY
     * <li>GEOMETRY attribute must have a SRID
     *
     * @see {@link SolrLayerConfiguration}
     * @see {@link FeatureTypeInfo#getMetadata}
     */
    protected void onSave(AjaxRequestTarget target) {
        try {
            ResourceInfo ri = (ResourceInfo) getDefaultModel().getObject();
            SolrLayerConfiguration layerConfiguration = fillSolrAttributes(ri);

            Boolean pkSet = false;
            Boolean geomSet = false;
            Boolean sridSet = false;
            // Validate configuration
            for (SolrAttribute att : layerConfiguration.getAttributes()) {
                if (att.isPk() && att.isUse()) {
                    pkSet = true;
                }
                if (Geometry.class.isAssignableFrom(att.getType()) && att.isUse()) {
                    geomSet = true;
                    if (att.getSrid() != null) {
                        sridSet = true;
                    }
                }
            }
            if (!pkSet) {
                error(
                        new ParamResourceModel("pkEmptyFailure", SolrConfigurationPage.this)
                                .getString());
            }
            if (!geomSet) {
                error(
                        new ParamResourceModel("geomEmptyFailure", SolrConfigurationPage.this)
                                .getString());
            } else if (!sridSet) {
                error(
                        new ParamResourceModel("sridEmptyFailure", SolrConfigurationPage.this)
                                .getString());
            }
            if (!pkSet || !geomSet || !sridSet) {
                target.add(feedbackPanel);
                return;
            }
            ri.getMetadata().put(SolrLayerConfiguration.KEY, layerConfiguration);

            done(target, ri);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            error(new ParamResourceModel("creationFailure", this, e).getString());
        }
    }

    /*
     * Load SolrLayerConfiguration configuration before shows on table Reloads SOLR attributes from
     * datastore and merge it with user attributes configurations
     */
    private SolrLayerConfiguration fillSolrAttributes(ResourceInfo ri) {
        SolrLayerConfiguration solrLayerConfiguration =
                (SolrLayerConfiguration) ri.getMetadata().get(SolrLayerConfiguration.KEY);
        try {

            ArrayList<SolrAttribute> result = new ArrayList<SolrAttribute>();
            Map<String, SolrAttribute> tempMap = new HashMap<String, SolrAttribute>();
            if (solrLayerConfiguration != null) {
                for (SolrAttribute att : solrLayerConfiguration.getAttributes()) {
                    tempMap.put(att.getName(), att);
                }
            } else {
                tempMap.clear();
                solrLayerConfiguration = new SolrLayerConfiguration(new ArrayList<SolrAttribute>());
                solrLayerConfiguration.setLayerName(ri.getName());
                ri.getMetadata().put(SolrLayerConfiguration.KEY, solrLayerConfiguration);
            }
            SolrDataStore dataStore =
                    (SolrDataStore)
                            ((DataStoreInfo) ri.getStore())
                                    .getDataStore(new NullProgressListener());
            ArrayList<SolrAttribute> attributes =
                    dataStore.getSolrAttributes(solrLayerConfiguration.getLayerName());
            for (SolrAttribute at : attributes) {
                if (tempMap.containsKey(at.getName())) {
                    SolrAttribute prev = tempMap.get(at.getName());
                    prev.setEmpty(at.getEmpty());
                    at = prev;
                }
                result.add(at);
            }
            solrLayerConfiguration.getAttributes().clear();
            solrLayerConfiguration.getAttributes().addAll(result);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return solrLayerConfiguration;
    }

    /*
     * Builds attribute table
     */
    private GeoServerTablePanel<SolrAttribute> getSolrAttributePanel(
            SolrAttributeProvider attProvider) {
        GeoServerTablePanel<SolrAttribute> atts =
                new GeoServerTablePanel<SolrAttribute>("solrAttributes", attProvider) {
                    /** */
                    private static final long serialVersionUID = 7306412054935816724L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<SolrAttribute> itemModel,
                            Property<SolrAttribute> property) {
                        SolrAttribute att = (SolrAttribute) itemModel.getObject();
                        boolean isGeometry =
                                att.getType() != null
                                        && Geometry.class.isAssignableFrom(att.getType());
                        boolean isPK = att.isPk();
                        if (property == SolrAttributeProvider.PK) {
                            if (isPK) {
                                Fragment f =
                                        new Fragment(id, "checkboxPk", SolrConfigurationPage.this);
                                f.add(
                                        new CheckBox(
                                                "pk", new PropertyModel<Boolean>(itemModel, "pk")));
                                return f;
                            } else {
                                Fragment f = new Fragment(id, "empty", SolrConfigurationPage.this);
                                return f;
                            }
                        } else if (property == SolrAttributeProvider.NAME && (isGeometry || isPK)) {
                            Fragment f = new Fragment(id, "label", SolrConfigurationPage.this);
                            f.add(
                                    new Label(
                                            "label",
                                            ((SolrAttribute) itemModel.getObject()).getName()
                                                    + "*"));
                            return f;

                        } else if (property == SolrAttributeProvider.TYPE && isGeometry) {
                            Fragment f = new Fragment(id, "geometry", SolrConfigurationPage.this);
                            f.add(
                                    new DropDownChoice(
                                            "geometry",
                                            new PropertyModel(itemModel, "type"),
                                            GEOMETRY_TYPES,
                                            new GeometryTypeRenderer()));
                            return f;
                        } else if (property == SolrAttributeProvider.USE) {
                            Fragment f =
                                    new Fragment(id, "checkboxUse", SolrConfigurationPage.this);
                            f.add(
                                    new CheckBox(
                                            "use", new PropertyModel<Boolean>(itemModel, "use")));
                            return f;
                        } else if (property == SolrAttributeProvider.EMPTY) {
                            Fragment f =
                                    new Fragment(id, "checkboxEmpty", SolrConfigurationPage.this);
                            f.add(
                                    new CheckBox(
                                            "isEmpty",
                                            new PropertyModel<Boolean>(itemModel, "empty")));
                            return f;
                        } else if (property == SolrAttributeProvider.SRID) {
                            if (isGeometry) {
                                Fragment f = new Fragment(id, "text", SolrConfigurationPage.this);
                                f.add(
                                        new TextField<Integer>(
                                                "text",
                                                new PropertyModel<Integer>(itemModel, "srid")));
                                return f;
                            } else {
                                Fragment f = new Fragment(id, "empty", SolrConfigurationPage.this);
                                return f;
                            }
                        } else if (property == SolrAttributeProvider.DEFAULT_GEOMETRY) {
                            if (isGeometry) {
                                Fragment f =
                                        new Fragment(
                                                id,
                                                "checkboxDefaultGeometry",
                                                SolrConfigurationPage.this);
                                f.add(
                                        new CheckBox(
                                                "defaultGeometry",
                                                new PropertyModel<Boolean>(
                                                        itemModel, "defaultGeometry")));
                                return f;
                            } else {
                                Fragment f = new Fragment(id, "empty", SolrConfigurationPage.this);
                                return f;
                            }
                        }

                        return null;
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
    private static class GeometryTypeRenderer extends ChoiceRenderer<Class<?>> {

        private static final long serialVersionUID = -6371918467884222834L;

        public Object getDisplayValue(Class<?> object) {
            return ((Class<?>) object).getSimpleName();
        }

        public String getIdValue(Class<?> object, int index) {
            return (String) getDisplayValue(object);
        }
    }

    /**
     * Abstract method to implements in panel that opens the dialog to close the dialog itself <br>
     * This method is called after modal executes its operation
     *
     * @param target ajax response target
     * @param layerInfo contains attribute configuration
     * @param isNew used to communicate to parent if the attributes configuration if for new or for
     *     existing layer
     * @see {@link #onSave}
     * @see {@link #onCancel}
     */
    abstract void done(AjaxRequestTarget target, ResourceInfo layerInfo);
}
