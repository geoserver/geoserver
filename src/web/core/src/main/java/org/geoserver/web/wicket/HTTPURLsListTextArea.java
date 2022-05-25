/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/** TextArea for specifying a list of HTTP Urls (separated by new lines). */
public class HTTPURLsListTextArea extends TextArea<List<String>> {

    private static final long serialVersionUID = -8195179437229644665L;

    public HTTPURLsListTextArea(String id, IModel<List<String>> model) {
        super(id, model);

        add(new HTTPURLsListValidator());
        setType(List.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (List.class.isAssignableFrom(type)) {
            return (IConverter<C>) new URLsListConverter();
        }
        return super.getConverter(type);
    }

    private static class URLsListConverter implements IConverter<List<String>> {
        private static final long serialVersionUID = 1083795866666107798L;

        static final Pattern NEW_LINE_SEPARATED =
                Pattern.compile("\\s*\\r?\\n\\s*", Pattern.MULTILINE);

        @Override
        public String convertToString(List<String> urlsList, Locale locale) {
            if (urlsList.isEmpty()) return "";

            StringBuffer sb = new StringBuffer();
            for (String url : urlsList) {
                sb.append(url).append("\n");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public List<String> convertToObject(String value, Locale locale) {
            if (value == null || value.trim().equals("")) return Collections.emptyList();
            return new ArrayList<>(Arrays.asList(NEW_LINE_SEPARATED.split(value)));
        }
    }

    private static class HTTPURLsListValidator implements IValidator<List<String>> {

        private static final long serialVersionUID = 8041469734553805086L;

        @Override
        public void validate(IValidatable<List<String>> validatable) {
            List<String> urlsList = validatable.getValue();
            List<String> invalid =
                    urlsList.stream().filter(url -> !isValid(url)).collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                IValidationError err =
                        new ValidationError()
                                .addKey("HTTPURLsListTextArea.invalidURL")
                                .setVariable("url", invalid.toString());
                validatable.error(err);
            }
        }

        private boolean isValid(String url) {
            boolean isValid = false;
            try {
                // Only valid URLs and HTTP Urls are supported
                URL validURL = new URL(url);
                isValid = validURL.getProtocol().startsWith("http");
            } catch (Exception e) {
                isValid = false;
            }
            return isValid;
        }
    }
}
