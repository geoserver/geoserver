/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.io.Serializable;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.PatternValidator;
import org.geoserver.qos.xml.DayOfWeek;
import org.geoserver.qos.xml.OperatingInfoTime;

public class OperatingInfoTimeModalPanel extends Panel {
    private static final long serialVersionUID = 1L;

    protected SerializableConsumer<OperatingInfoTime> onSave;
    protected SerializableConsumer<AjaxTargetAndModel<OperatingInfoTime>> onDelete;
    protected SerializableConsumer<AjaxRequestTarget> onAjaxRequest;
    protected TimesText timesText;
    protected ModalWindow modalWindow;

    public OperatingInfoTimeModalPanel(String id, IModel<OperatingInfoTime> model) {
        this(id, model, null);
    }

    public OperatingInfoTimeModalPanel(
            String id,
            IModel<OperatingInfoTime> model,
            SerializableConsumer<OperatingInfoTime> onSave) {
        super(id, model);
        this.onSave = onSave;
        this.timesText = new TimesText(model.getObject());

        // form
        final WebMarkupContainer form = new WebMarkupContainer("opInfoTimeForm");
        add(form);

        // multiselector for List<DayOfWeek>
        if (model.getObject().getDays() == null) model.getObject().setDays(new ArrayList<>());
        CheckGroup<DayOfWeek> daysChecks =
                new CheckGroup<>("daysOfWeekCheckGroup", model.getObject().getDays());
        daysChecks.setOutputMarkupId(true);
        form.add(daysChecks);
        ListView<DayOfWeek> checksList =
                new ListView<DayOfWeek>(
                        "days", new ArrayList<>(Arrays.asList(DayOfWeek.values()))) {
                    @Override
                    protected void populateItem(ListItem<DayOfWeek> item) {
                        Check<DayOfWeek> check = new Check<>("daysCheck", item.getModel());
                        check.setLabel(Model.of(item.getModel().getObject().value()));
                        item.add(check);
                        item.add(new SimpleFormComponentLabel("day", check));
                    }
                };
        daysChecks.add(checksList);

        // calendar startTime, TextField
        final TextField<String> startTimeField =
                new TextField<String>(
                        "startTimeField", new PropertyModel<>(timesText, "startTime"));
        addTimeRegexValidator(startTimeField);
        form.add(startTimeField);

        // calendar endDate
        final TextField<String> endTimeField =
                new TextField<>("endTimeField", new PropertyModel<>(timesText, "endTime"));
        addTimeRegexValidator(endTimeField);
        form.add(endTimeField);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    public void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        onDelete(target);
                    }
                };
        form.add(deleteLink);
    }

    private void addTimeRegexValidator(final TextField<String> field) {
        // Pattern validator
        PatternValidator validator =
                new PatternValidator("^\\d{2}:\\d{2}(:\\d{2}(Z|[\\+\\-]{1}\\d{2}:\\d{2}))?$") {
                    @Override
                    protected IValidationError decorate(
                            IValidationError error, IValidatable<String> validatable) {
                        getSession().error("invalid time format");
                        return new ValidationError("invalid time format");
                    }
                };
        field.add(validator);
    }

    public void onDelete(AjaxRequestTarget target) {
        Optional.ofNullable(onDelete)
                .ifPresent(
                        x ->
                                x.accept(
                                        new AjaxTargetAndModel<OperatingInfoTime>(
                                                (OperatingInfoTime) this.getDefaultModelObject(),
                                                target)));
    }

    public void dispatchOnSave() {
        if (onSave != null) onSave.accept((OperatingInfoTime) this.getDefaultModelObject());
    }

    public SerializableConsumer<OperatingInfoTime> getOnSave() {
        return onSave;
    }

    public void setOnSave(SerializableConsumer<OperatingInfoTime> onSave) {
        this.onSave = onSave;
    }

    public ModalWindow getModalWindow() {
        return modalWindow;
    }

    public void setModalWindow(ModalWindow modalWindow) {
        this.modalWindow = modalWindow;
    }

    public SerializableConsumer<AjaxRequestTarget> getOnAjaxRequest() {
        return onAjaxRequest;
    }

    public void setOnAjaxRequest(SerializableConsumer<AjaxRequestTarget> onAjaxRequest) {
        this.onAjaxRequest = onAjaxRequest;
    }

    public void setOnDelete(SerializableConsumer<AjaxTargetAndModel<OperatingInfoTime>> onDelete) {
        this.onDelete = onDelete;
    }

    public static class TimesText implements Serializable {
        private OperatingInfoTime model;
        private String startTime;
        private String endTime;

        public TimesText(OperatingInfoTime model) {
            this.model = model;
            startTime = format(model.getStartTime());
            endTime = format(model.getEndTime());
        }

        public String format(OffsetTime time) {
            if (time == null) return null;
            DateTimeFormatter formateer =
                    DateTimeFormatter.ofPattern(OperatingInfoTime.TIME_PATTERN);
            return time.format(formateer);
        }

        public OffsetTime parse(String text) {
            if (text == null) return null;
            // simple time pattern support (12:00 to 12:00:00+00:00)
            text = extractFullTimeString(text);
            try {
                DateTimeFormatter formateer =
                        DateTimeFormatter.ofPattern(OperatingInfoTime.TIME_PATTERN);
                return OffsetTime.parse(text, formateer);
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        private String extractFullTimeString(String text) {
            if (text.trim().matches("^\\d{2}:\\d{2}$")) {
                text = text.trim() + ":00+00:00";
            }
            return text;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
            model.setStartTime(parse(startTime));
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
            model.setEndTime(parse(endTime));
        }
    }
}
