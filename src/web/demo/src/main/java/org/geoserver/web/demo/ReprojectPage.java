/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeometryTextArea;
import org.geoserver.web.wicket.SRSToCRSModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * A simple reprojection console panel, shows details about a SRS1 -> SRS2 transformation and allows
 * to reproject simple points or WKT geometries
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ReprojectPage extends GeoServerBasePage {

    String sourceCRS;

    String targetCRS;

    GeometryTextArea sourceGeom;

    GeometryTextArea targetGeom;

    /** pop-up window for transformation details * */
    ModalWindow popupWindow;

    GeoServerAjaxFormLink wktLink;

    SimpleAjaxLink<Void> sourceDetails;

    SimpleAjaxLink<Void> targetDetails;

    public ReprojectPage(PageParameters params) {
        if (params != null) {
            // get the params, if any
            sourceCRS = params.get("fromSRS").toOptionalString();
            targetCRS = params.get("toSRS").toOptionalString();
        }

        // the popup for transformation details
        popupWindow = new ModalWindow("popup");
        add(popupWindow);

        // the main form
        Form form = new Form("form");
        add(form);

        // the source CRS
        CRSPanel sourcePanel =
                new CRSPanel("sourceCRS", new SRSToCRSModel(new PropertyModel(this, "sourceCRS"))) {
                    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
                        sourceCRS = srs;
                        updateTransformation(target);
                    };
                };
        sourcePanel.setRequired(true);
        form.add(sourcePanel);

        // the target CRS
        CRSPanel targetPanel =
                new CRSPanel("targetCRS", new SRSToCRSModel(new PropertyModel(this, "targetCRS"))) {
                    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
                        targetCRS = srs;
                        updateTransformation(target);
                    };
                };
        targetPanel.setRequired(true);
        form.add(targetPanel);

        // The link showing
        wktLink =
                new GeoServerAjaxFormLink("wkt", form) {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form form) {
                        popupWindow.setInitialHeight(525);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setContent(new WKTPanel(popupWindow.getContentId()));
                        popupWindow.setTitle(sourceCRS + " -> " + targetCRS);
                        popupWindow.show(target);
                    }
                };
        wktLink.setEnabled(false);
        form.add(wktLink);

        sourceGeom = new GeometryTextArea("sourceGeom");
        form.add(sourceGeom);
        sourceGeom.setOutputMarkupId(true);
        targetGeom = new GeometryTextArea("targetGeom");
        targetGeom.setOutputMarkupId(true);
        form.add(targetGeom);

        AjaxSubmitLink forward =
                new AjaxSubmitLink("forward", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget at, Form<?> form) {
                        Geometry source = sourceGeom.getModelObject();
                        if (source == null) {
                            error(
                                    getLocalizer()
                                            .getString(
                                                    "ReprojectPage.sourcePointNotSpecifiedError",
                                                    ReprojectPage.this,
                                                    "Source Geometry is not specified"));
                        } else {
                            MathTransform mt = getTransform();
                            if (mt != null) {
                                try {
                                    Geometry target = JTS.transform(source, mt);
                                    targetGeom.setModelObject(target);
                                    at.add(targetGeom);
                                } catch (Exception e) {
                                    error(e.getMessage());
                                }
                            }
                        }
                        addFeedbackPanels(at);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        addFeedbackPanels(target);
                    }
                };
        form.add(forward);

        AjaxSubmitLink backward =
                new AjaxSubmitLink("backward", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget at, Form<?> form) {
                        Geometry target = targetGeom.getModelObject();
                        if (target == null) {
                            error(
                                    getLocalizer()
                                            .getString(
                                                    "ReprojectPage.targetPointNotSpecifiedError",
                                                    ReprojectPage.this,
                                                    "Target Geometry is not specified"));
                        } else {
                            MathTransform mt = getTransform();
                            if (mt != null) {
                                try {
                                    Geometry source = JTS.transform(target, mt.inverse());
                                    sourceGeom.setModelObject(source);
                                    at.add(sourceGeom);
                                } catch (Exception e) {
                                    error(e.getMessage());
                                }
                            }
                        }
                        addFeedbackPanels(at);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        addFeedbackPanels(target);
                    }
                };
        form.add(backward);
    }

    protected void updateTransformation(AjaxRequestTarget ajaxTarget) {
        if (sourceCRS != null && targetCRS != null) {
            MathTransform mt = getTransform();
            if (mt != null) {
                wktLink.setEnabled(true);
                ajaxTarget.add(wktLink);
            }
        }
    }

    protected MathTransform getTransform() {
        try {
            CoordinateReferenceSystem source = CRS.decode(sourceCRS);
            CoordinateReferenceSystem target = CRS.decode(targetCRS);
            return CRS.findMathTransform(source, target, true);
        } catch (Exception e) {
            error(e.getMessage());
            return null;
        }
    }

    /*
     * Panel for displaying the well known text for the transformation.
     */
    class WKTPanel extends Panel {

        public WKTPanel(String id) {
            super(id);

            MultiLineLabel wktLabel = new MultiLineLabel("wkt");

            add(wktLabel);

            MathTransform mt = getTransform();
            if (mt != null) {
                wktLabel.setDefaultModel(new Model<String>(mt.toString()));
            }
        }
    }
}
