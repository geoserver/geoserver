/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.wicket.DecimalTextField;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.NumberRange;

/**
 * A form component for a {@link Range} object.
 *
 * @author Andrea Aime, OpenGeo
 */
@SuppressWarnings("serial")
public class RangePanel extends FormComponentPanel<NumberRange> {

    protected Double min, max;
    protected Label minLabel, maxLabel;
    protected DecimalTextField minInput, maxInput;

    public RangePanel(String id) {
        super(id);

        initComponents();
    }

    public RangePanel(String id, ReferencedEnvelope e) {
        this(id, new Model(e));
    }

    public RangePanel(String id, IModel model) {
        super(id, model);

        initComponents();
    }

    void initComponents() {
        updateFields();

        add(minLabel = new Label("minLabel", new ResourceModel("min")));
        add(maxLabel = new Label("maxLabel", new ResourceModel("max")));

        add(minInput = new DecimalTextField("min", new PropertyModel(this, "min")));
        add(maxInput = new DecimalTextField("max", new PropertyModel(this, "max")));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        NumberRange range = getModelObject();
        if (range != null) {
            this.min = range.getMinimum();
            this.max = range.getMaximum();
        }
    }

    public RangePanel setReadOnly(final boolean readOnly) {
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

        // update the envelope model
        if (min != null && max != null) {
            setConvertedInput(new NumberRange<>(Double.class, min, max));
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
