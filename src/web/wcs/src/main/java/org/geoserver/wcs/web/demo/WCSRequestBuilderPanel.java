/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.web.demo.GetCoverageRequest.Version;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.demo.DemoRequest;
import org.geoserver.web.demo.DemoRequestResponse;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * Small embedded WCS client enabling users to visually build a WCS GetCoverage request
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WCSRequestBuilderPanel extends Panel {

    /**
     * How the WCS 1.1 target layout is going to be built
     *
     * @author Andrea Aime - GeoSolutions
     */
    enum TargetLayout {
        Automatic,
        Resolution,
        Affine
    };

    static final Logger LOGGER = Logging.getLogger(WCSRequestBuilderPanel.class);

    GetCoverageRequest getCoverage;

    String description;

    ModalWindow responseWindow;

    private Component feedback;

    private WebMarkupContainer details;

    private EnvelopePanel envelope;

    private DropDownChoice<String> coverage;

    private DropDownChoice<String> formats;

    private CRSPanel targetCRS;

    private CheckBox manualGrid;

    private GridPanel sourceGridRange;

    private AffineTransformPanel g2w;

    private DropDownChoice<TargetLayout> targetLayoutChooser;

    private WebMarkupContainer targetlayoutContainer;

    private WebMarkupContainer sourceGridContainer;

    private GeoServerAjaxFormLink describeLink;

    public WCSRequestBuilderPanel(String id, GetCoverageRequest getCoverage) {
        super(id);
        setOutputMarkupId(true);
        setDefaultModel(new Model(getCoverage));
        this.getCoverage = getCoverage;

        // the feedback panel, for validation errors
        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        // the version chooser
        final DropDownChoice<Version> version =
                new DropDownChoice<Version>(
                        "version",
                        new PropertyModel<Version>(getCoverage, "version"),
                        Arrays.asList(Version.values()));
        add(version);

        // the action that will setup the form once the coverage has been chosen
        version.add(
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        onSubmit(target);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        if (version.getModelObject() == Version.v1_0_0) {
                            sourceGridContainer.setVisible(true);
                            targetlayoutContainer.setVisible(false);
                            manualGrid.setModelObject(false);
                            sourceGridRange.setVisible(false);
                        } else {
                            targetlayoutContainer.setVisible(true);
                            sourceGridContainer.setVisible(false);
                            targetLayoutChooser.setModelObject(TargetLayout.Automatic);
                            g2w.setModelObject(null);
                            g2w.setVisible(false);
                        }
                        target.add(WCSRequestBuilderPanel.this);
                    }
                });

        // the coverage id chooser
        coverage =
                new DropDownChoice<String>(
                        "coverage",
                        new PropertyModel<String>(getCoverage, "coverage"),
                        new CoverageNamesModel());
        add(coverage);

        // the action that will setup the form once the coverage has been chosen
        coverage.add(
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        onSubmit(target);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        details.setVisible(true);
                        String coverageName = coverage.getModelObject();
                        Catalog catalog = GeoServerApplication.get().getCatalog();
                        CoverageInfo ci = catalog.getCoverageByName(coverageName);
                        ReferencedEnvelope ri = ci.getNativeBoundingBox();
                        final GetCoverageRequest gc = WCSRequestBuilderPanel.this.getCoverage;
                        gc.bounds = ri;
                        gc.targetCRS = ri.getCoordinateReferenceSystem();
                        gc.sourceGridRange = null;
                        describeLink.setEnabled(true);
                        target.add(WCSRequestBuilderPanel.this);
                    }
                });

        // the details container
        details = new WebMarkupContainer("details");
        details.setOutputMarkupId(true);
        details.setVisible(false);
        add(details);

        // the envelope chooser
        envelope = new EnvelopePanel("envelope", new PropertyModel(getCoverage, "bounds"));
        envelope.setCRSFieldVisible(true);
        envelope.setCrsRequired(true);
        details.add(envelope);

        // the grid panel (for WCS 1.0 requests)
        buildGridPanel();

        // the format chooser
        CoverageResponseDelegateFinder responseFactory =
                (CoverageResponseDelegateFinder)
                        GeoServerApplication.get().getBean("coverageResponseDelegateFactory");
        formats =
                new DropDownChoice<String>(
                        "format",
                        new PropertyModel(getCoverage, "outputFormat"),
                        responseFactory.getOutputFormats());
        details.add(formats);

        // the target CRS
        targetCRS = new CRSPanel("targetCRS", new PropertyModel(getCoverage, "targetCRS"));
        details.add(targetCRS);

        // the target grid to world (for WCS 1.1 ones)
        buildAffinePanel();

        // the describe response window
        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DemoRequest request = new DemoRequest(null);
                        HttpServletRequest http =
                                GeoServerApplication.get().servletRequest(getRequest());
                        String url =
                                ResponseUtils.buildURL(
                                        ResponseUtils.baseURL(http),
                                        "ows",
                                        Collections.singletonMap("strict", "true"),
                                        URLType.SERVICE);
                        request.setRequestUrl(url);
                        request.setRequestBody((String) responseWindow.getDefaultModelObject());
                        return new DemoRequestResponse(new Model(request));
                    }
                });

        // the describe coverage link
        describeLink =
                new GeoServerAjaxFormLink("describeCoverage") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form form) {
                        version.processInput();
                        coverage.processInput();
                        final String coverageName =
                                WCSRequestBuilderPanel.this.getCoverage.coverage;
                        if (coverageName != null) {
                            responseWindow.setDefaultModel(new Model(getDescribeXML(coverageName)));
                            responseWindow.show(target);
                        }
                    }
                };
        describeLink.setEnabled(false);
        describeLink.setOutputMarkupId(true);
        add(describeLink);
    }

    protected String getDescribeXML(String processId) {
        if (getCoverage.version == Version.v1_0_0) {
            return "<DescribeCoverage\n"
                    + "  version=\"1.0.0\"\n"
                    + "  service=\"WCS\"\n"
                    + "  xmlns=\"http://www.opengis.net/wcs\"\n"
                    + "  xmlns:nurc=\"http://www.nurc.nato.int\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd\">\n"
                    + "  \n"
                    + "    <Coverage>"
                    + getCoverage.coverage
                    + "</Coverage>\n"
                    + "    \n"
                    + "</DescribeCoverage>";
        } else {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                    + //
                    "<wcs:DescribeCoverage service=\"WCS\" "
                    + //
                    "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                    + //
                    "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n"
                    + //
                    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                    + //
                    "  version=\"1.1.1\" >\r\n"
                    + //
                    "  <wcs:Identifier>"
                    + getCoverage.coverage
                    + "</wcs:Identifier>\r\n"
                    + //
                    "</wcs:DescribeCoverage>";
        }
    }

    private void buildAffinePanel() {
        targetlayoutContainer = new WebMarkupContainer("targetLayoutContainer");
        details.add(targetlayoutContainer);
        targetlayoutContainer.setVisible(false);

        targetLayoutChooser =
                new DropDownChoice<TargetLayout>(
                        "targetLayout",
                        new Model(TargetLayout.Automatic),
                        Arrays.asList(TargetLayout.values()),
                        new TargetLayoutRenderer());
        targetlayoutContainer.add(targetLayoutChooser);

        g2w =
                new AffineTransformPanel(
                        "targetGridToWorld", new PropertyModel(getCoverage, "targetGridToWorld"));
        targetlayoutContainer.add(g2w);
        g2w.setVisible(false);
        g2w.setOutputMarkupId(true);

        targetLayoutChooser.add(
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        onSubmit(target);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        if (targetLayoutChooser.getModelObject() == TargetLayout.Affine) {
                            AffineTransform at = guessGridToWorld(false);
                            g2w.setResolutionModeEnabled(false);
                            g2w.setModelObject(at);
                            g2w.setVisible(true);
                        } else if (targetLayoutChooser.getModelObject()
                                == TargetLayout.Resolution) {
                            AffineTransform at = guessGridToWorld(true);
                            g2w.setResolutionModeEnabled(true);
                            g2w.setModelObject(at);
                            g2w.setVisible(true);
                        } else {
                            g2w.setModelObject(null);
                            g2w.setVisible(false);
                        }
                        target.add(WCSRequestBuilderPanel.this);
                    }
                });
    }

    private void buildGridPanel() {
        sourceGridContainer = new WebMarkupContainer("sourceGridContainer");
        details.add(sourceGridContainer);

        manualGrid = new CheckBox("manualGrid", new Model(Boolean.FALSE));
        sourceGridContainer.add(manualGrid);

        sourceGridRange =
                new GridPanel("sourceGrid", new PropertyModel(getCoverage, "sourceGridRange"));
        sourceGridContainer.add(sourceGridRange);
        sourceGridRange.setVisible(false);
        sourceGridRange.setOutputMarkupId(true);

        // the action that will setup the form once the coverage has been chosen
        manualGrid.add(
                new AjaxFormSubmitBehavior("change") {

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        onSubmit(target);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        if (manualGrid.getModelObject() == Boolean.TRUE) {

                            GridEnvelope2D grid = guessGridLimits();
                            sourceGridRange.setModelObject(grid);
                            sourceGridRange.setVisible(true);
                        } else {
                            sourceGridRange.setModelObject(null);
                            sourceGridRange.setVisible(false);
                        }
                        target.add(WCSRequestBuilderPanel.this);
                    }
                });
    }

    protected String getDescribeXML(String coverageId, Version version) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<DescribeProcess service=\"WPS\" version=\"1.0.0\" "
                + "xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "    <ows:Identifier>"
                + coverageId
                + "</ows:Identifier>\n"
                + "</DescribeProcess>";
    }

    public Component getFeedbackPanel() {
        return feedback;
    }

    GetCoverageRequest getCoverageRequest() {
        if (getCoverage.version == Version.v1_0_0) {
            if (manualGrid.getModelObject() != Boolean.TRUE) {
                getCoverage.sourceGridRange = guessGridLimits();
            }
        } else {
            if (targetLayoutChooser.getModelObject() == TargetLayout.Automatic) {
                getCoverage.targetGridToWorld = guessGridToWorld(true);
            }
        }

        return getCoverage;
    }

    GridEnvelope2D guessGridLimits() {
        try {
            String coverageName = coverage.getModelObject();
            Catalog catalog = GeoServerApplication.get().getCatalog();
            CoverageInfo ci = catalog.getCoverageByName(coverageName);
            ReferencedEnvelope boundsNative = getCoverage.bounds.transform(ci.getCRS(), true);
            MathTransform w2g = ci.getGrid().getGridToCRS().inverse();
            Envelope ge = JTS.transform(boundsNative, w2g);
            GridEnvelope2D grid =
                    new GridEnvelope2D(
                            new Rectangle(0, 0, (int) ge.getWidth(), (int) ge.getHeight()));
            return grid;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to guess native grid", e);
            error("Failed to guess native grid: " + e.getMessage());
            return null;
        }
    }

    AffineTransform guessGridToWorld(boolean resolutionMode) {
        try {
            String coverageName = coverage.getModelObject();
            Catalog catalog = GeoServerApplication.get().getCatalog();
            CoverageInfo ci = catalog.getCoverageByName(coverageName);

            ReferencedEnvelope nativeBounds = getCoverage.bounds.transform(ci.getCRS(), true);
            ReferencedEnvelope targetBounds = nativeBounds.transform(getCoverage.targetCRS, true);
            GridEnvelope2D gridLimits = guessGridLimits();
            GridGeometry2D gg = new GridGeometry2D(gridLimits, targetBounds);
            AffineTransform at = (AffineTransform) gg.getGridToCRS(PixelInCell.CELL_CORNER);
            if (resolutionMode) {
                return AffineTransform.getScaleInstance(at.getScaleX(), at.getScaleY());
            } else {
                return at;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to guess target affine transform", e);
            error("Failed to guess native grid: " + e.getMessage());
            return null;
        }
    }

    class TargetLayoutRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            final String name = ((TargetLayout) object).name();
            return new StringResourceModel("tl." + name, WCSRequestBuilderPanel.this, null)
                    .getString();
        }

        public String getIdValue(Object object, int index) {
            return ((TargetLayout) object).name();
        }
    }
}
