/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.math.BigDecimal;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * Helps edit a time period
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class PeriodEditor extends FormComponentPanel<BigDecimal> {

    static final long yearMS = 31536000000L;

    static final long monthMS = 2628000000L;

    static final long weekMS = 604800000L;

    static final long dayMS = 86400000L;

    static final long hourMS = 3600000L;

    static final long minuteMS = 60000L;

    static final long secondMS = 1000L;

    int years;

    int months;

    int weeks;

    int days;

    int hours;

    int minutes;

    int seconds;

    public PeriodEditor(String id, IModel<BigDecimal> model) {
        super(id, model);
        initComponents();
    }

    void initComponents() {
        updateFields();

        final RangeValidator<Integer> validator = new RangeValidator<Integer>(0, Integer.MAX_VALUE);
        add(
                new TextField<Integer>("years", new PropertyModel<Integer>(this, "years"))
                        .add(validator));
        add(
                new TextField<Integer>("months", new PropertyModel<Integer>(this, "months"))
                        .add(validator));
        add(
                new TextField<Integer>("weeks", new PropertyModel<Integer>(this, "weeks"))
                        .add(validator));
        add(
                new TextField<Integer>("days", new PropertyModel<Integer>(this, "days"))
                        .add(validator));
        add(
                new TextField<Integer>("hours", new PropertyModel<Integer>(this, "hours"))
                        .add(validator));
        add(
                new TextField<Integer>("minutes", new PropertyModel<Integer>(this, "minutes"))
                        .add(validator));
        add(
                new TextField<Integer>("seconds", new PropertyModel<Integer>(this, "seconds"))
                        .add(validator));
    }

    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }

    private void updateFields() {
        final BigDecimal modelObject = getModelObject();
        long time;
        if (modelObject != null) {
            time = modelObject.longValue();
        } else {
            time = 0;
        }
        years = (int) (time / yearMS);
        time %= yearMS;
        months = (int) (time / monthMS);
        time %= monthMS;
        weeks = (int) (time / weekMS);
        time %= weekMS;
        days = (int) (time / dayMS);
        time %= dayMS;
        hours = (int) (time / hourMS);
        time %= hourMS;
        minutes = (int) (time / minuteMS);
        time %= minuteMS;
        seconds = (int) (time / secondMS);
    }

    @Override
    public void convertInput() {
        visitChildren(
                TextField.class,
                (component, visit) -> {
                    ((TextField) component).processInput();
                });

        long time =
                seconds * secondMS
                        + minutes * minuteMS
                        + hours * hourMS
                        + days * dayMS
                        + weeks * weekMS
                        + months * monthMS
                        + years * yearMS;
        setConvertedInput(new BigDecimal(time));
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
