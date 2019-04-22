/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Allows the user to edit a complex input parameter providing a variety of different editors
 * depending on the parameter type
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class ComplexInputPanel extends Panel {
    static final Logger LOGGER = Logging.getLogger(WPSRequestBuilderPanel.class);

    private DropDownChoice typeChoice;

    PropertyModel valueModel;

    List<String> mimeTypes;

    ModalWindow subprocesswindow;

    public ComplexInputPanel(String id, InputParameterValues pv, int valueIndex) {
        super(id);
        setOutputMarkupId(true);
        setDefaultModel(new PropertyModel(pv, "values[" + valueIndex + "]"));
        valueModel = new PropertyModel(getDefaultModel(), "value");
        mimeTypes = pv.getSupportedMime();

        List<ParameterType> ptypes = pv.getSupportedTypes();
        ptypes.remove(ParameterType.LITERAL);
        typeChoice =
                new DropDownChoice(
                        "type", new PropertyModel(getDefaultModelObject(), "type"), ptypes);
        add(typeChoice);

        subprocesswindow = new ModalWindow("subprocessPopupWindow");
        subprocesswindow.setInitialWidth(700);
        subprocesswindow.setInitialHeight(500);
        add(subprocesswindow);
        subprocesswindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        return new SubProcessBuilder(
                                (ExecuteRequest) subprocesswindow.getDefaultModelObject(),
                                subprocesswindow);
                    }
                });

        updateEditor();

        typeChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateEditor();
                        target.add(ComplexInputPanel.this);
                    }
                });
    }

    void updateEditor() {
        // remove the old editor
        if (get("editor") != null) {
            remove("editor");
        }

        ParameterType pt = (ParameterType) typeChoice.getModelObject();
        if (pt == ParameterType.TEXT) {
            // an internal vector layer
            if (!(valueModel.getObject() instanceof String)) {
                valueModel.setObject("");
            }

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
            if (!(valueModel.getObject() instanceof VectorLayerConfiguration)) {
                valueModel.setObject(new VectorLayerConfiguration());
            }

            new PropertyModel(getDefaultModel(), "mime").setObject("text/xml");
            Fragment f = new Fragment("editor", "vectorLayer", this);
            DropDownChoice layer =
                    new Select2DropDownChoice(
                            "layer",
                            new PropertyModel(valueModel, "layerName"),
                            getVectorLayerNames());
            f.add(layer);
            add(f);
        } else if (pt == ParameterType.RASTER_LAYER) {
            // an internal raster layer
            if (!(valueModel.getObject() instanceof RasterLayerConfiguration)) {
                valueModel.setObject(new RasterLayerConfiguration());
            }

            Fragment f = new Fragment("editor", "rasterLayer", this);
            final DropDownChoice layer =
                    new Select2DropDownChoice(
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
        } else if (pt == ParameterType.REFERENCE) {
            // an external reference
            if (!(valueModel.getObject() instanceof ReferenceConfiguration)) {
                valueModel.setObject(new ReferenceConfiguration());
            }

            Fragment f = new Fragment("editor", "reference", this);
            final DropDownChoice method =
                    new DropDownChoice(
                            "method",
                            new PropertyModel(valueModel, "method"),
                            Arrays.asList(
                                    ReferenceConfiguration.Method.GET,
                                    ReferenceConfiguration.Method.POST));
            f.add(method);

            DropDownChoice mimeChoice =
                    new DropDownChoice("mime", new PropertyModel(valueModel, "mime"), mimeTypes);
            f.add(mimeChoice);

            f.add(new TextField("url", new PropertyModel(valueModel, "url")).setRequired(true));
            final TextArea body = new TextArea("body", new PropertyModel(valueModel, "body"));
            add(body);

            final WebMarkupContainer bodyContainer = new WebMarkupContainer("bodyContainer");
            f.add(bodyContainer);
            bodyContainer.setOutputMarkupId(true);
            bodyContainer.add(body);
            bodyContainer.setVisible(false);

            method.add(
                    new AjaxFormComponentUpdatingBehavior("change") {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            boolean post =
                                    method.getModelObject() == ReferenceConfiguration.Method.POST;
                            bodyContainer.setVisible(post);
                            body.setRequired(post);
                            target.add(ComplexInputPanel.this);
                        }
                    });

            add(f);
        } else if (pt == ParameterType.SUBPROCESS) {
            if (!(valueModel.getObject() instanceof ExecuteRequest)) {
                valueModel.setObject(new ExecuteRequest());
            }

            Fragment f = new Fragment("editor", "subprocess", this);
            f.add(
                    new AjaxLink("edit") {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            subprocesswindow.setDefaultModel(valueModel);
                            subprocesswindow.show(target);
                        }
                    });

            final TextArea xml = new TextArea("xml");
            if (((ExecuteRequest) valueModel.getObject()).processName != null) {
                try {
                    xml.setModelObject(getExecuteXML());
                } catch (Throwable t) {
                    xml.setModel(new Model(""));
                }
            } else {
                xml.setModel(new Model(""));
            }
            xml.setOutputMarkupId(true);
            f.add(xml);

            subprocesswindow.setWindowClosedCallback(
                    new ModalWindow.WindowClosedCallback() {

                        public void onClose(AjaxRequestTarget target) {
                            // turn the GUI request into an actual WPS request
                            xml.setModelObject(getExecuteXML());

                            target.add(xml);
                        }
                    });

            add(f);
        } else {
            error("Unsupported parameter type");
        }
    }

    String getExecuteXML() {
        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setEntityResolver(
                GeoServerApplication.get().getCatalog().getResourcePool().getEntityResolver());
        tx.setIndentation(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            tx.transform(valueModel.getObject(), out);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error generating xml request", e);
            error(e);
        }
        String executeXml = out.toString();
        return executeXml;
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
