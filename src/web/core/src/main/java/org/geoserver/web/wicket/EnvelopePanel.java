/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A form component for a {@link Envelope} object.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, OpenGeo
 */
public class EnvelopePanel extends FormComponentPanel<ReferencedEnvelope> {

    private static final long serialVersionUID = -2975427786330616705L;

    protected Label minXLabel, minYLabel, maxXLabel, maxYLabel, minZLabel, maxZLabel;
    protected Double minX, minY, maxX, maxY, minZ, maxZ;
    protected DecimalTextField minXInput, minYInput, maxXInput, maxYInput, minZInput, maxZInput;

    protected CoordinateReferenceSystem crs;
    protected WebMarkupContainer crsContainer;
    protected CRSPanel crsPanel;
    protected boolean crsRequired;

    public EnvelopePanel(String id) {
        super(id);

        initComponents();
    }

    public EnvelopePanel(String id, ReferencedEnvelope e) {
        this(id, new Model<ReferencedEnvelope>(e));
    }

    public EnvelopePanel(String id, IModel<ReferencedEnvelope> model) {
        super(id, model);

        initComponents();
    }

    public void setCRSFieldVisible(boolean visible) {
        crsContainer.setVisible(visible);
    }

    public boolean isCRSFieldVisible() {
        return crsContainer.isVisible();
    }

    public boolean isCrsRequired() {
        return crsRequired;
    }

    /**
     * Makes the CRS bounds a required component of the envelope. It is warmly suggested that the
     * crs field be made visible too
     */
    public void setCrsRequired(boolean crsRequired) {
        this.crsRequired = crsRequired;
    }

    public boolean is3D() {
        return crs != null && crs.getCoordinateSystem().getDimension() >= 3;
    }

    public void setLabelsVisibility(boolean visible) {
        minXLabel.setVisible(visible);
        minYLabel.setVisible(visible);
        maxXLabel.setVisible(visible);
        maxYLabel.setVisible(visible);
        minZLabel.setVisible(visible && is3D());
        maxZLabel.setVisible(visible && is3D());
    }

    void initComponents() {
        updateFields();

        add(minXLabel = new Label("minXL", new ResourceModel("minX")));
        add(minYLabel = new Label("minYL", new ResourceModel("minY")));
        add(minZLabel = new Label("minZL", new ResourceModel("minZ")));
        add(maxXLabel = new Label("maxXL", new ResourceModel("maxX")));
        add(maxYLabel = new Label("maxYL", new ResourceModel("maxY")));
        add(maxZLabel = new Label("maxZL", new ResourceModel("maxZ")));

        add(minXInput = new DecimalTextField("minX", new PropertyModel<Double>(this, "minX")));
        add(minYInput = new DecimalTextField("minY", new PropertyModel<Double>(this, "minY")));
        add(minZInput = new DecimalTextField("minZ", new PropertyModel<Double>(this, "minZ")));
        add(maxXInput = new DecimalTextField("maxX", new PropertyModel<Double>(this, "maxX")));
        add(maxYInput = new DecimalTextField("maxY", new PropertyModel<Double>(this, "maxY")));
        add(maxZInput = new DecimalTextField("maxZ", new PropertyModel<Double>(this, "maxZ")));

        minZInput.setVisible(is3D());
        minZLabel.setVisible(is3D());
        maxZInput.setVisible(is3D());
        maxZLabel.setVisible(is3D());

        crsContainer = new WebMarkupContainer("crsContainer");
        crsContainer.setVisible(false);
        crsPanel = new CRSPanel("crs", new PropertyModel<CoordinateReferenceSystem>(this, "crs"));
        crsContainer.add(crsPanel);
        add(crsContainer);
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        ReferencedEnvelope e = (ReferencedEnvelope) getModelObject();
        if (e != null) {
            this.minX = e.getMinX();
            this.minY = e.getMinY();
            this.maxX = e.getMaxX();
            this.maxY = e.getMaxY();
            this.crs = e.getCoordinateReferenceSystem();
            if (is3D()) {
                if (e instanceof ReferencedEnvelope3D) {
                    this.minZ = ((ReferencedEnvelope3D) e).getMinZ();
                    this.maxZ = ((ReferencedEnvelope3D) e).getMaxZ();
                } else {
                    this.minZ = Double.NaN;
                    this.maxZ = Double.NaN;
                }
            } else {
                this.minZ = Double.NaN;
                this.maxZ = Double.NaN;
            }
        }
    }

    public EnvelopePanel setReadOnly(final boolean readOnly) {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    component.setEnabled(!readOnly);
                });
        crsPanel.setReadOnly(readOnly);

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<String>) component).processInput();
                });

        if (isCRSFieldVisible()) {
            crsPanel.processInput();
        }

        // update the envelope model
        if (minX != null && maxX != null && minY != null && maxY != null) {
            if (crsRequired && crs == null) {
                setConvertedInput(null);
            } else {
                if (is3D()) {
                    double minZsafe = minZ == null ? Double.NaN : minZ;
                    double maxZsafe = maxZ == null ? Double.NaN : maxZ;
                    setConvertedInput(
                            new ReferencedEnvelope3D(
                                    minX, maxX, minY, maxY, minZsafe, maxZsafe, crs));
                } else {
                    setConvertedInput(new ReferencedEnvelope(minX, maxX, minY, maxY, crs));
                }
            }
        } else {
            setConvertedInput(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<String>) component).clearInput();
                });
    }

    /** Returns the coordinate reference system added by the user in the GUI, if any and valid */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }
}
