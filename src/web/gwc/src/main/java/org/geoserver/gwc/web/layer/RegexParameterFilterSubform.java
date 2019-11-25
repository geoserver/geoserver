/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.RegexParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public class RegexParameterFilterSubform
        extends AbstractParameterFilterSubform<RegexParameterFilter> {

    private static final IValidator<String> REGEXP_VALIDATOR =
            new IValidator<String>() {

                private static final long serialVersionUID = 3753607592277740081L;

                @Override
                public void validate(IValidatable<String> validatable) {
                    final String regex = validatable.getValue();
                    try {
                        Pattern.compile(regex);
                    } catch (PatternSyntaxException ex) {
                        ValidationError error = new ValidationError();
                        error.setMessage("Invalid Regular expression");
                        error.addKey(getClass().getSimpleName() + "." + "invalidRegularExpression");
                        validatable.error(error);
                    }
                }
            };

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private Component normalize;

    public RegexParameterFilterSubform(String id, IModel<RegexParameterFilter> model) {
        super(id, model);

        final Component defaultValue;

        defaultValue =
                new TextField<String>(
                        "defaultValue", new PropertyModel<String>(model, "defaultValue"));
        add(defaultValue);

        final TextField<String> regex;

        regex = new TextField<String>("regex", new PropertyModel<String>(model, "regex"));

        regex.add(REGEXP_VALIDATOR);

        add(regex);

        normalize =
                new CaseNormalizerSubform(
                        "normalize", new PropertyModel<CaseNormalizer>(model, "normalize"));
        add(normalize);
    }
}
