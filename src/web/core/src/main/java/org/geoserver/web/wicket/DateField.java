/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.settings.JavaScriptLibrarySettings;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DateConverter;

/**
 * DateField that uses some jquery to provide a DatePicker. It supports date and dateTime as well as
 * passing different date formats. Default formats is yyyy-MM-dd for date and HH:mm:ss for time.
 */
public class DateField extends DateTextField {

    private DateConverter dateConverter;
    private boolean dateTime = false;
    private String format;
    private String dateTimeSep;
    private String DEF_DATE_FORMAT = "yyyy-MM-dd";
    private String DEF_DATE_TIME_FORMAT = DEF_DATE_FORMAT.concat(" HH:mm");

    /**
     * @param id wicket id.
     * @param model a Date IModel.
     * @param dateTime true if the field is for a date time value. False otherwise.
     * @param format the date/dateTime format. If null default will be used.
     * @param dateTimeSep the character separating date and time in the format. Will be ignored if
     *     only date format is used.
     */
    public DateField(
            String id, IModel<Date> model, boolean dateTime, String format, String dateTimeSep) {
        super(id, model);
        this.dateTime = dateTime;
        this.dateTimeSep = dateTimeSep == null ? " " : dateTimeSep;
        if (format == null) {
            if (dateTime) format = DEF_DATE_TIME_FORMAT;
            else format = DEF_DATE_FORMAT;
        }
        this.format = format;
        this.dateConverter =
                new DateConverter() {
                    @Override
                    public DateFormat getDateFormat(Locale locale) {
                        if (locale == null) {
                            locale = Locale.getDefault(Locale.Category.FORMAT);
                        }
                        return new SimpleDateFormat(DateField.this.format, locale);
                    }
                };
    }

    /**
     * @param id wicket id.
     * @param model a Date IModel.
     * @param dateTime true if the field is for a date time value. False otherwise.
     */
    public DateField(String id, IModel<Date> model, boolean dateTime) {
        this(id, model, dateTime, null, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
    }

    @Override
    public <Date> IConverter<Date> getConverter(Class<Date> type) {
        @SuppressWarnings("unchecked")
        IConverter<Date> converter = (IConverter<Date>) dateConverter;
        return converter;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        // if component is disabled we don't have to load the JQueryUI datepicker
        if (!isEnabledInHierarchy()) return;
        // add bundled JQuery
        JavaScriptLibrarySettings javaScriptSettings =
                getApplication().getJavaScriptLibrarySettings();
        response.render(JavaScriptHeaderItem.forReference(javaScriptSettings.getJQueryReference()));
        // add package resources
        response.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(getClass(), "js/datepicker/moment.min.js")));
        response.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                getClass(), "js/datepicker/jquery.datetimepicker.full.min.js")));
        response.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(
                                getClass(), "js/datepicker/jquery.datetimepicker.min.css")));
        // add custom file JQDatePicker.js. Reference JQDatePickerRef is a static field
        response.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(getClass(), "DatePicker.js")));

        String initScript =
                ";initJQDatepicker('"
                        + getMarkupId()
                        + "',"
                        + dateTime
                        + ",'"
                        + format
                        + "','"
                        + dateTimeSep
                        + "');";
        response.render(OnLoadHeaderItem.forScript(initScript));
    }
}
