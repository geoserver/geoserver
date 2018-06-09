/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.regex.Pattern;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Checks a string conforms to the XML Name production as declared at {@link http
 * ://www.w3.org/TR/REC-xml/#NT-Name}
 *
 * @author aaime
 */
@SuppressWarnings("serial")
public class XMLNameValidator implements IValidator<String> {
    private static Pattern XML_NAME_PATTERN;

    static {
        // Definitions coming from
        // NameStartChar ::= ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] |
        // [#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] |
        // [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
        // NameChar ::= NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
        // Name ::= NameStartChar (NameChar)*
        String nameStartCharSet =
                "A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F"
                        + "\u1FFF\u200C\u200D\u2070-\u218F\u2C00\u2FEF\u3001\uD7FF\uF900-\uFDCF"
                        + "\uFDF0-\uFFFD";
        String nameStartChar = "[" + nameStartCharSet + "]";
        String nameChar = ("[" + nameStartCharSet + "\\-.0-9\u0087\u0300-\u036F\u203F-\u2040]");
        String name = "(?:" + nameStartChar + nameChar + "*)";
        XML_NAME_PATTERN = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = (String) validatable.getValue();
        if (!XML_NAME_PATTERN.matcher(value).matches()) {
            validatable.error(
                    new ValidationError("invalidXMLName")
                            .addKey("invalidXMLName")
                            .setVariable("name", value));
        }
    }
}
