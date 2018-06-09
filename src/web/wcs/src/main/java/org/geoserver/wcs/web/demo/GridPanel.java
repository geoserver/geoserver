/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.awt.Rectangle;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geotools.coverage.grid.GridEnvelope2D;

/**
 * A form component for a {@link GridEnvelope2D} object.
 *
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, OpenGeo
 */
public class GridPanel extends FormComponentPanel {

    Integer minX, minY, maxX, maxY;

    public GridPanel(String id) {
        super(id);

        initComponents();
    }

    public GridPanel(String id, GridEnvelope2D e) {
        this(id, new Model(e));
    }

    public GridPanel(String id, IModel model) {
        super(id, model);

        initComponents();
    }

    void initComponents() {
        updateFields();

        add(new TextField("minX", new PropertyModel(this, "minX")));
        add(new TextField("minY", new PropertyModel(this, "minY")));
        add(new TextField("maxX", new PropertyModel(this, "maxX")));
        add(new TextField("maxY", new PropertyModel(this, "maxY")));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        GridEnvelope2D e = (GridEnvelope2D) getModelObject();
        if (e != null) {
            this.minX = (int) e.getMinX();
            this.minY = (int) e.getMinY();
            this.maxX = (int) e.getMaxX();
            this.maxY = (int) e.getMaxY();
        }
    }

    public GridPanel setReadOnly(final boolean readOnly) {
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
                    ((TextField) component).processInput();
                });

        // update the grid envelope
        if (minX != null && maxX != null && minY != null && maxX != null) {
            final int width = maxX - minX;
            final int height = maxY - minY;
            setConvertedInput(new GridEnvelope2D(new Rectangle(minX, minY, width, height)));
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
                    ((TextField) component).clearInput();
                });
    }
}
