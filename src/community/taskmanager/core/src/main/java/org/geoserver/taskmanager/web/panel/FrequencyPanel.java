/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormModelUpdateListener;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.taskmanager.util.FrequencyUtil;
import org.geoserver.web.wicket.ParamResourceModel;
import org.quartz.CronExpression;

public class FrequencyPanel extends Panel implements IFormModelUpdateListener {

    private static final long serialVersionUID = -1661388086707143932L;

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d\\d?):(\\d\\d?)$");

    protected enum Type {
        NEVER,
        DAILY,
        WEEKLY,
        MONTHLY,
        CUSTOM
    }

    protected IModel<Type> typeModel = new Model<Type>();
    private IModel<Integer> dayOfMonthModel = new Model<Integer>();
    private IModel<DayOfWeek> dayOfWeekModel = new Model<DayOfWeek>();
    private IModel<String> timeModel = new Model<String>();

    public FrequencyPanel(String id, IModel<String> model) {
        super(id, model);

        dayOfWeekModel.setObject(DayOfWeek.MONDAY);
        dayOfMonthModel.setObject(1);
        timeModel.setObject("00:00");
        if (model.getObject() == null) {
            typeModel.setObject(Type.NEVER);
        } else {
            typeModel.setObject(Type.CUSTOM);

            Matcher matcher = FrequencyUtil.DAILY_PATTERN.matcher(model.getObject());
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int hour = Integer.parseInt(matcher.group(2));
                if (minutes <= 60 && hour < 24) {
                    typeModel.setObject(Type.DAILY);
                    timeModel.setObject(
                            String.format("%02d", hour) + ":" + String.format("%02d", minutes));
                }
            } else {
                matcher = FrequencyUtil.WEEKLY_PATTERN.matcher(model.getObject());
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int hour = Integer.parseInt(matcher.group(2));
                    DayOfWeek day = FrequencyUtil.findDayOfWeek(matcher.group(3));
                    if (minutes <= 60 && hour < 24 && day != null) {
                        typeModel.setObject(Type.WEEKLY);
                        timeModel.setObject(
                                String.format("%02d", hour) + ":" + String.format("%02d", minutes));
                        dayOfWeekModel.setObject(day);
                    }
                } else {
                    matcher = FrequencyUtil.MONTHLY_PATTERN.matcher(model.getObject());
                    if (matcher.matches()) {
                        int minutes = Integer.parseInt(matcher.group(1));
                        int hour = Integer.parseInt(matcher.group(2));
                        int day = Integer.parseInt(matcher.group(3));
                        if (minutes <= 60 && hour < 24 && day > 0 && day <= 28) {
                            typeModel.setObject(Type.MONTHLY);
                            timeModel.setObject(
                                    String.format("%02d", hour)
                                            + ":"
                                            + String.format("%02d", minutes));
                            dayOfMonthModel.setObject(day);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onInitialize() {
        super.onInitialize();
        add(
                new DropDownChoice<Type>(
                                "type",
                                typeModel,
                                Arrays.asList(Type.values()),
                                new EnumChoiceRenderer<Type>(this))
                        .add(
                                new AjaxFormSubmitBehavior("change") {
                                    private static final long serialVersionUID =
                                            -7698014209707408962L;

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target) {
                                        updateVisibility();
                                        target.add(FrequencyPanel.this);
                                    }
                                }));

        add(new WebMarkupContainer("dayOfWeekLabel"));
        add(
                new DropDownChoice<DayOfWeek>(
                        "dayOfWeek",
                        dayOfWeekModel,
                        Arrays.asList(DayOfWeek.values()),
                        new EnumChoiceRenderer<DayOfWeek>() {
                            private static final long serialVersionUID = 246492731661118407L;

                            @Override
                            public Object getDisplayValue(DayOfWeek object) {
                                return object.getDisplayName(TextStyle.FULL, getLocale());
                            }
                        }));
        add(new WebMarkupContainer("dayOfMonthLabel"));
        add(
                new DropDownChoice<Integer>(
                        "dayOfMonth",
                        dayOfMonthModel,
                        Arrays.asList(
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                                20, 21, 22, 23, 24, 25, 26, 27, 28)));
        add(new WebMarkupContainer("timeLabel"));
        add(
                new TextField<String>("time", timeModel)
                        .add(
                                new IValidator<String>() {
                                    private static final long serialVersionUID =
                                            -3170061736947280260L;

                                    @Override
                                    public void validate(IValidatable<String> validatable) {
                                        if (findParent(Form.class).findSubmittingButton() != null) {
                                            Matcher matcher =
                                                    TIME_PATTERN.matcher(validatable.getValue());

                                            if (!(matcher.matches()
                                                    && Integer.parseInt(matcher.group(1)) < 24
                                                    && Integer.parseInt(matcher.group(2)) < 60)) {
                                                error(
                                                        new ParamResourceModel(
                                                                        "timeFormatError",
                                                                        FrequencyPanel.this)
                                                                .getString());
                                            }
                                        }
                                    }
                                }));
        add(
                new TextField<String>("custom", (IModel<String>) getDefaultModel())
                        .add(
                                new IValidator<String>() {
                                    private static final long serialVersionUID =
                                            -3170061736947280260L;

                                    @Override
                                    public void validate(IValidatable<String> validatable) {
                                        if (findParent(Form.class).findSubmittingButton() != null) {
                                            try {
                                                CronExpression.validateExpression(
                                                        validatable.getValue());
                                            } catch (ParseException e) {
                                                error(e.getLocalizedMessage());
                                            }
                                        }
                                    }
                                }));

        setOutputMarkupId(true);
        updateVisibility();
    }

    private void updateVisibility() {
        boolean weeklyVisible = typeModel.getObject().equals(Type.WEEKLY);
        boolean monthlyVisible = typeModel.getObject().equals(Type.MONTHLY);
        boolean timeVisible =
                !typeModel.getObject().equals(Type.CUSTOM)
                        && !typeModel.getObject().equals(Type.NEVER);
        get("dayOfWeek").setVisible(weeklyVisible);
        get("dayOfWeekLabel").setVisible(weeklyVisible);
        get("dayOfMonth").setVisible(monthlyVisible);
        get("dayOfMonthLabel").setVisible(monthlyVisible);
        get("time").setVisible(timeVisible);
        get("timeLabel").setVisible(timeVisible);
        get("custom").setVisible(typeModel.getObject().equals(Type.CUSTOM));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updateModel() {
        if (findParent(Form.class).findSubmittingButton() != null
                && typeModel.getObject() != Type.CUSTOM
                && timeModel.getObject() != null) {
            if (typeModel.getObject() == Type.NEVER) {
                ((IModel<String>) getDefaultModel()).setObject(null);
            } else {
                Matcher matcher = TIME_PATTERN.matcher(timeModel.getObject());
                if (matcher.matches()) {
                    String hour = matcher.group(1);
                    String minute = matcher.group(2);

                    if (typeModel.getObject() == Type.DAILY) {
                        ((IModel<String>) getDefaultModel())
                                .setObject("0 " + minute + " " + hour + " * * ?");
                    } else if (typeModel.getObject() == Type.WEEKLY) {
                        ((IModel<String>) getDefaultModel())
                                .setObject(
                                        "0 "
                                                + minute
                                                + " "
                                                + hour
                                                + " ? * "
                                                + dayOfWeekModel
                                                        .getObject()
                                                        .getDisplayName(
                                                                TextStyle.SHORT, Locale.ENGLISH));
                    } else if (typeModel.getObject() == Type.MONTHLY) {
                        ((IModel<String>) getDefaultModel())
                                .setObject(
                                        "0 "
                                                + minute
                                                + " "
                                                + hour
                                                + " "
                                                + dayOfMonthModel.getObject()
                                                + " * ?");
                    }
                } else {
                    throw new IllegalStateException(
                            new ParamResourceModel("timeFormatError", this).getString());
                }
            }
        }
    }
}
