/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.control;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.urlchecker.GeoserverURLConfigService;
import org.geoserver.security.urlchecker.URLEntry;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;

/** @author ImranR */
public class URLEntryPage extends GeoServerSecuredPage {

    /** serialVersionUID */
    private static final long serialVersionUID = -730807061816337523L;

    static final Logger LOGGER = Logging.getLogger(URLEntryPage.class);

    public static final String NAME = "name";

    private boolean isNew = false;

    Form<URLEntry> form;

    public URLEntryPage(PageParameters parameters) {

        StringValue name = parameters.get(NAME);
        isNew = name.isNull();
        GeoserverURLConfigService geoserverURLConfigServiceBean =
                GeoServerExtensions.bean(GeoserverURLConfigService.class);
        URLEntry entry =
                geoserverURLConfigServiceBean.getGeoserverURLChecker().get(name.toString());

        if (entry == null) entry = new URLEntry();
        initUI(new Model<URLEntry>(entry));
    }

    private void initUI(IModel<URLEntry> bean) {

        form = new Form<URLEntry>("form", new CompoundPropertyModel<>(bean));

        FormComponent<String> nameFeild =
                new TextField<String>("name", new PropertyModel<String>(bean, "name"));
        nameFeild.setEnabled(isNew);
        nameFeild.setRequired(true);
        // to make sure url entry with same name is not entered again
        if (isNew) nameFeild.add(new DuplicationValidator());

        FormComponent<String> descriptionField =
                new TextField<String>(
                        "description", new PropertyModel<String>(bean, "description"));

        FormComponent<String> regexExpressionField =
                new TextField<String>(
                        "regexExpression", new PropertyModel<String>(bean, "regexExpression"));
        regexExpressionField.add(new RegexValidator());

        regexExpressionField.setRequired(true);

        FormComponent<Boolean> enableCheckBox =
                new CheckBox("enable", new PropertyModel<Boolean>(bean, "enable"));
        form.add(nameFeild);
        form.add(descriptionField);
        form.add(regexExpressionField);
        form.add(enableCheckBox);
        form.add(submitLink());
        form.add(new BookmarkablePageLink<ControlPage>("cancel", ControlPage.class));
        add(form);
    }

    private SubmitLink submitLink() {
        return new SubmitLink("submit") {

            private static final long serialVersionUID = -3462848930497720229L;

            @Override
            public void onSubmit() {
                try {
                    GeoserverURLConfigService geoserverURLConfigServiceBean =
                            GeoServerExtensions.bean(GeoserverURLConfigService.class);
                    geoserverURLConfigServiceBean.addAndsave(form.getModel().getObject());
                    doReturn(ControlPage.class);
                } catch (Exception e) {
                    // gui
                    error("An Error occurred " + e.getMessage());
                    // log
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        };
    }

    private class RegexValidator implements IValidator<String> {

        /** serialVersionUID */
        private static final long serialVersionUID = 2608646704842153110L;

        @Override
        public void validate(IValidatable<String> validatable) {
            final String regexValue = (String) validatable.getValue();
            try {
                Pattern p = Pattern.compile(regexValue); // . represents single character
            } catch (Exception e) {
                validatable.error(
                        new ValidationError(regexValue + " is not a valid Regex expression"));
            }
        }
    }

    private class DuplicationValidator implements IValidator<String> {

        /** serialVersionUID */
        private static final long serialVersionUID = 2608646704842153110L;

        @Override
        public void validate(IValidatable<String> validatable) {
            final String nameValue = validatable.getValue();
            GeoserverURLConfigService geoserverURLConfigServiceBean =
                    GeoServerExtensions.bean(GeoserverURLConfigService.class);
            if (geoserverURLConfigServiceBean.getGeoserverURLChecker().get(nameValue) != null) {
                validatable.error(
                        new ValidationError(
                                "Another URL Entry with name " + nameValue + " exists already"));
            }
        }
    }
}
