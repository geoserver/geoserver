/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geotools.referencing.CRS;

public class SRSListTextArea extends TextArea<List<String>> {

    private static final long serialVersionUID = -4851891710707750564L;

    public SRSListTextArea(String id, IModel<List<String>> model) {
        super(id, model);

        add(new SRSListValidator());
        setType(List.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (List.class.isAssignableFrom(type)) {
            return (IConverter<C>) new SRSListConverter();
        }
        return super.getConverter(type);
    }

    private static class SRSListConverter implements IConverter<List<String>> {
        private static final long serialVersionUID = 6381056789141754260L;
        static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*", Pattern.MULTILINE);

        public String convertToString(List<String> srsList, Locale locale) {
            if (srsList.isEmpty()) return "";

            StringBuffer sb = new StringBuffer();
            for (String srs : srsList) {
                sb.append(srs).append(", ");
            }
            sb.setLength(sb.length() - 2);
            return sb.toString();
        }

        public List<String> convertToObject(String value, Locale locale) {
            if (value == null || value.trim().equals("")) return Collections.emptyList();
            return new ArrayList<String>(Arrays.asList(COMMA_SEPARATED.split(value)));
        }
    }

    private static class SRSListValidator implements IValidator<List<String>> {

        private static final long serialVersionUID = -6376260926391668771L;

        @Override
        public void validate(IValidatable<List<String>> validatable) {
            List<String> srsList = validatable.getValue();
            List<String> invalid = new ArrayList<>();
            srsList.stream()
                    .forEach(
                            (srs) -> {
                                try {
                                    CRS.decode("EPSG:" + srs);
                                } catch (Exception e) {
                                    invalid.add(srs);
                                }
                            });

            if (invalid.size() > 0) {
                IValidationError err =
                        new ValidationError("SRSListTextArea.unknownEPSGCodes")
                                .addKey("SRSListTextArea.unknownEPSGCodes")
                                .setVariable("codes", invalid.toString());
                validatable.error(err);
            }
        }
    }
}
