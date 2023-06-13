/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.url;

import java.io.IOException;
import java.util.HashMap;
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
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class RegexCheckPage extends GeoServerSecuredPage {

    static final Logger LOGGER = Logging.getLogger(RegexCheckPage.class);

    public static final String NAME = "name";

    private boolean isNew = false;

    Form<RegexURLCheck> form;

    public RegexCheckPage(PageParameters parameters) throws Exception {

        String name = parameters.get(NAME).toString();
        isNew = name == null;
        URLCheckDAO dao = getUrlCheckDAO();
        RegexURLCheck entry = (RegexURLCheck) dao.getCheckByName(name);

        if (entry == null) entry = new RegexURLCheck();
        initUI(new Model<>(entry));
    }

    private URLCheckDAO getUrlCheckDAO() {
        return getGeoServerApplication().getBeanOfType(URLCheckDAO.class);
    }

    private void initUI(IModel<RegexURLCheck> bean) {
        form = new Form<>("form", new CompoundPropertyModel<>(bean));

        FormComponent<String> nameField =
                new TextField<>("name", new PropertyModel<>(bean, "name"));
        nameField.setEnabled(isNew);
        nameField.setRequired(true);
        // to make sure url entry with same name is not entered again
        if (isNew) nameField.add(new DuplicationValidator());

        FormComponent<String> descriptionField =
                new TextField<>("description", new PropertyModel<>(bean, "description"));

        FormComponent<String> regexField =
                new TextField<>("regex", new PropertyModel<>(bean, "regex"));
        regexField.add(new RegexValidator());
        if (regexField.getModelObject() == null) {
            regexField.setModelObject(getDefaultRegex());
        }
        regexField.setRequired(true);

        FormComponent<Boolean> enabledCheckBox =
                new CheckBox("enabled", new PropertyModel<>(bean, "enabled"));
        form.add(nameField);
        form.add(descriptionField);
        form.add(regexField);
        form.add(enabledCheckBox);
        form.add(submitLink());
        form.add(new BookmarkablePageLink<>("cancel", URLChecksPage.class));
        add(form);
    }

    /**
     * Generate an example reglar expression.
     *
     * <p>This example processes {@link #getOWSURL()} to demonstrate:
     *
     * <ul>
     *   <li>Escaping {@code \.} characters
     *   <li>Allow anything after {@code ?} query
     * </ul>
     *
     * @return example regular expression based on open web services api endpoint
     */
    private String getDefaultRegex() {
        String url = getOWSURL();
        url = url.replace(".", "\\.");

        return "^" + url + "\\?.*$";
    }

    /**
     * Determine a sensible example open web service URL to use as an example RegEX.
     *
     * @return Open web service URL to use as an example RegEx
     */
    private String getOWSURL() {
        String proxyBase = getGeoServer().getSettings().getProxyBaseUrl();
        if (proxyBase != null) {
            ProxifyingURLMangler mangler =
                    getGeoServerApplication().getBeanOfType(ProxifyingURLMangler.class);
            String baseURL =
                    ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            StringBuilder base = new StringBuilder(baseURL);
            StringBuilder path = new StringBuilder("ows");
            mangler.mangleURL(base, path, new HashMap<>(), URLMangler.URLType.SERVICE);
            return ResponseUtils.appendPath(base.toString(), path.toString());
        }
        return "http://localhost:8080/geoserver/ows";
    }

    private SubmitLink submitLink() {
        return new SubmitLink("submit") {

            private static final long serialVersionUID = -3462848930497720229L;

            @Override
            public void onSubmit() {
                try {
                    RegexURLCheck check = form.getModelObject();
                    if (isNew) getUrlCheckDAO().add(check);
                    else getUrlCheckDAO().save(check);

                    doReturn(URLChecksPage.class);
                } catch (Exception e) {
                    error("An Error occurred " + e.getMessage());
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        };
    }

    private class RegexValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            final String regex = validatable.getValue();
            try {
                Pattern.compile(regex);
            } catch (Exception e) {
                String message =
                        new ParamResourceModel("invalidRegex", RegexCheckPage.this, regex)
                                .getString();
                validatable.error(new ValidationError(message));
            }
        }
    }

    private class DuplicationValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            final String name = validatable.getValue();
            URLCheckDAO dao = getUrlCheckDAO();
            try {
                if (dao.getCheckByName(name) != null) {
                    String message =
                            new ParamResourceModel("duplicateRule", RegexCheckPage.this, name)
                                    .getString();
                    validatable.error(new ValidationError(message));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
