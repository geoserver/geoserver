/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Allows the user to edit a bounding box parameter
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class BoundingBoxInputPanel extends Panel {

    private DropDownChoice typeChoice;

    PropertyModel valueModel;

    List<String> mimeTypes;

    public BoundingBoxInputPanel(String id, InputParameterValues pv, int valueIndex) {
        super(id);
        setOutputMarkupId(true);
        setDefaultModel(new PropertyModel(pv, "values[" + valueIndex + "]"));
        valueModel = new PropertyModel(getDefaultModel(), "value");
        mimeTypes = pv.getSupportedMime();

        typeChoice =
                new DropDownChoice(
                        "type",
                        new PropertyModel(getDefaultModelObject(), "type"),
                        Arrays.asList(ParameterType.values()));
        add(typeChoice);

        updateEditor();

        typeChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateEditor();
                        target.add(BoundingBoxInputPanel.this);
                    }
                });
    }

    void updateEditor() {
        // remove the old editor
        if (get("editor") != null) {
            remove("editor");
        }

        // reset the previous value
        valueModel.setObject(null);

        ParameterType pt = (ParameterType) typeChoice.getModelObject();
        if (pt == ParameterType.TEXT) {
            // data as plain text
            Fragment f = new Fragment("editor", "text", this);
            DropDownChoice mimeChoice =
                    new DropDownChoice(
                            "mime", new PropertyModel(getDefaultModel(), "mime"), mimeTypes);
            f.add(mimeChoice);

            f.add(new TextArea("textarea", valueModel));
            add(f);
        } else if (pt == ParameterType.VECTOR_LAYER) {
            // an internal vector layer
            valueModel.setObject(new VectorLayerConfiguration());
            Fragment f = new Fragment("editor", "vectorLayer", this);
            DropDownChoice layer =
                    new DropDownChoice(
                            "layer",
                            new PropertyModel(valueModel, "layerName"),
                            getVectorLayerNames());
            f.add(layer);
            add(f);
        } else if (pt == ParameterType.RASTER_LAYER) {
            // an internal raster layer
            valueModel.setObject(new RasterLayerConfiguration());

            Fragment f = new Fragment("editor", "rasterLayer", this);
            final DropDownChoice layer =
                    new DropDownChoice(
                            "layer",
                            new PropertyModel(valueModel, "layerName"),
                            getRasterLayerNames());
            f.add(layer);
            add(f);

            // we need to update the raster own bounding box as wcs requests
            // mandate a spatial extent (why oh why???)
            layer.add(
                    new AjaxFormComponentUpdatingBehavior("change") {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            String name = layer.getDefaultModelObjectAsString();
                            LayerInfo li =
                                    GeoServerApplication.get().getCatalog().getLayerByName(name);
                            ReferencedEnvelope spatialDomain =
                                    li.getResource().getNativeBoundingBox();
                            ((RasterLayerConfiguration) valueModel.getObject())
                                    .setSpatialDomain(spatialDomain);
                        }
                    });
        } else {
            error("Unsupported parameter type");
        }
    }

    List<String> getVectorLayerNames() {
        Catalog catalog = GeoServerApplication.get().getCatalog();

        List<String> result = new ArrayList<String>();
        for (LayerInfo li : catalog.getLayers()) {
            if (li.getResource() instanceof FeatureTypeInfo) {
                result.add(li.getResource().prefixedName());
            }
        }
        return result;
    }

    List<String> getRasterLayerNames() {
        Catalog catalog = GeoServerApplication.get().getCatalog();

        List<String> result = new ArrayList<String>();
        for (LayerInfo li : catalog.getLayers()) {
            if (li.getResource() instanceof CoverageInfo) {
                result.add(li.getResource().prefixedName());
            }
        }
        return result;
    }
}
