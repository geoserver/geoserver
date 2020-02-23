/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * A form component for a {@link Point} object.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class PointPanel extends FormComponentPanel<Point> {
    private static final long serialVersionUID = -1046819530873258172L;

    GeometryFactory gf = new GeometryFactory();

    protected Label xLabel, yLabel;
    protected Double x, y;
    protected DecimalTextField xInput, yInput;

    public PointPanel(String id) {
        super(id, new Model<Point>(null));

        initComponents();
    }

    public PointPanel(String id, Point p) {
        this(id, new Model<Point>(p));
    }

    public PointPanel(String id, IModel<Point> model) {
        super(id, model);

        initComponents();
    }

    public void setLabelsVisibility(boolean visible) {
        xLabel.setVisible(visible);
        yLabel.setVisible(visible);
    }

    void initComponents() {
        updateFields();

        add(xLabel = new Label("xL", new ResourceModel("x")));
        add(yLabel = new Label("yL", new ResourceModel("y")));

        add(xInput = new DecimalTextField("x", new PropertyModel<Double>(this, "x")));
        add(yInput = new DecimalTextField("y", new PropertyModel<Double>(this, "y")));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        Point p = (Point) getModelObject();
        if (p != null) {
            this.x = p.getX();
            this.y = p.getY();
        }
    }

    public PointPanel setReadOnly(final boolean readOnly) {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    component.setEnabled(!readOnly);
                });

        return this;
    }

    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<?>) component).processInput();
                });

        // update the point model
        if (x != null && y != null) {
            setConvertedInput(gf.createPoint(new Coordinate(x, y)));
        } else {
            setConvertedInput(null);
        }
    }

    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField<?>) component).clearInput();
                });
    }

    /** Sets the max number of digits for the */
    public void setMaximumFractionDigits(int maximumFractionDigits) {
        xInput.setMaximumFractionDigits(maximumFractionDigits);
        yInput.setMaximumFractionDigits(maximumFractionDigits);
    }
}
