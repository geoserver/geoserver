/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Date;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

/** Wicket page with some {@link org.geoserver.web.wicket.DateField} for test purpose. */
public class DateTestPage extends WebPage {

    public static final String FORM = "form";

    public DateTestPage() {
        Form<?> form = new Form<>(FORM);
        form.add(new DateField("date", Model.of(new Date()), false));
        form.add(new DateField("dateTime", Model.of(new Date()), true));
        form.add(new DateField("date2", Model.of(new Date()), false, "YYYY/MM/DD", null));
        form.add(
                new DateField(
                        "dateTime2", Model.of(new Date()), true, "YYYY/MM/DD HH:mm:ss.SSS", " "));
        add(form);
    }
}
