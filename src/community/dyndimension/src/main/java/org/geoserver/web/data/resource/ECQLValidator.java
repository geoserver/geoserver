/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.expression.Expression;

/**
 * Validates that a ECQL expression is syntactically valid, and if a set of valid attribute is
 * provided, that the expression only uses those attributes
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ECQLValidator extends AbstractValidator<String> {
    private static final long serialVersionUID = 2268953695122233556L;

    Set<String> validAttributes;

    public ECQLValidator setValidAttributes(String... validAttributes) {
        this.validAttributes = new LinkedHashSet<String>(Arrays.asList(validAttributes));
        return this;
    }

    public ECQLValidator setValidAttributes(Collection<String> validAttributes) {
        this.validAttributes = new LinkedHashSet<String>(validAttributes);
        return this;
    }

    @Override
    protected void onValidate(IValidatable<String> validatable) {
        // Extraction of the expression for the validation
        String expression = validatable.getValue();
        if (expression == null || expression.isEmpty() || expression.trim().isEmpty()) {
            return;
        }

        Expression ecql = null;
        // First check on the Syntax of the expression
        try {
            ecql = ECQL.toExpression(expression);
        } catch (CQLException e) {
            error(validatable, "ecql.invalidExpression",
                    map("expression", expression, "error", e.getMessage()));
        }

        // Selection of a FilterAttributeExtractor
        if (ecql != null && validAttributes != null) {
            FilterAttributeExtractor filter = new FilterAttributeExtractor();
            ecql.accept(filter, null);
            // Extraction of the attributes, and filter out the valid ones
            List<String> invalids = new ArrayList<String>(Arrays.asList(filter.getAttributeNames()));

            invalids.removeAll(validAttributes);

            // if and only if an invalid attribute is present
            if (!invalids.isEmpty()) {

                String invalidList = commaSeparated(invalids);
                String validList = commaSeparated(validAttributes);

                error(validatable, "ecql.invalidAttributes",
                        map("expression", expression, "invalidAttributes", invalidList,
                                "validAttributes", validList));
            }
        }

    }

    private String commaSeparated(Collection<String> invalids) {
        StringBuilder string = new StringBuilder();

        for (String attribute : invalids) {
            string.append(attribute);
            string.append(", ");
        }
        string.setLength(string.length() - 2);
        return string.toString();
    }

    Map<String, Object> map(String... entries) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(entries[i], entries[i + 1]);
        }

        return result;
    }

}
