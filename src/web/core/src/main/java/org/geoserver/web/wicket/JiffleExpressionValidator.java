/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.geoserver.catalog.JiffleFormulaParser.TOKEN_PATTERN;
import static org.geoserver.catalog.JiffleFormulaParser.extractOutputAssignments;
import static org.geoserver.catalog.JiffleFormulaParser.isJiffleReservedWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.JiffleFormulaParser;

/**
 * Validator for Jiffle expressions, ensuring proper syntax and band configuration.
 *
 * <p>Validates Jiffle script expressions by checking: - Presence of output assignments - Continuous output band indices
 * - Correct usage of coverage names and band indices - Avoidance of reserved Jiffle keywords
 */
public class JiffleExpressionValidator implements IValidator<String> {

    private final IModel<Map<String, Integer>> bandInfoModel;

    public JiffleExpressionValidator(IModel<Map<String, Integer>> bandInfoModel) {
        this.bandInfoModel = bandInfoModel;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String formula = validatable.getValue();
        JiffleFormulaParser.JiffleOutputResult parsed = extractOutputAssignments(formula);

        if (parsed.outputVar == null || parsed.expressions.isEmpty()) {
            validatable.error(new ValidationError(
                    "Script must contain at least one output assignment (e.g. 'res = ...;' or 'res[0] = ...;')"));
            return;
        }

        Set<Integer> indices = parsed.expressions.keySet();
        if (indices.size() > 1) {
            List<Integer> sorted = new ArrayList<>(indices);
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i) != i) {
                    validatable.error(new ValidationError(
                            "Output band indices must be continuous starting from 0 (e.g. res[0], res[1], res[2])"));
                    return;
                }
            }
        }

        Map<String, Integer> bandInfo = bandInfoModel.getObject();
        Set<String> coverageNames = bandInfo.keySet();

        List<String> errors = new ArrayList<>();

        String[] lines = formula.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Extract the outputBand
            int eqIdx = line.indexOf('=');
            if (eqIdx == -1) {
                errors.add("Missing '=' in expression: " + line);
                continue;
            }
            String outputBand = line.substring(0, eqIdx).trim();
            if (isJiffleReservedWord(outputBand)) {
                errors.add("Output band name '" + outputBand + "' is a reserved Jiffle keyword or function.");
            }
            String composingFormula = line.substring(eqIdx + 1);
            Matcher matcher = TOKEN_PATTERN.matcher(composingFormula);
            String pendingCoverageName = null;

            // Extract the input coverage names from the formula
            while (matcher.find()) {
                String token = matcher.group();

                if (token.startsWith("[")) {
                    if (pendingCoverageName != null) {
                        int index = Integer.parseInt(token.substring(1, token.length() - 1));
                        Integer bandCount = bandInfo.get(pendingCoverageName);
                        if (bandCount == null) {
                            errors.add("Unknown coverage: " + pendingCoverageName);
                        } else if (index >= bandCount) {
                            errors.add("Coverage '" + pendingCoverageName + "' has only " + bandCount
                                    + " band(s), index " + index + " is out of range.");
                        }
                        pendingCoverageName = null;
                    } else {
                        errors.add("Unexpected index usage: " + token);
                    }
                } else {
                    if (coverageNames.contains(token)) {
                        pendingCoverageName = token;
                    } else if (!isJiffleReservedWord(token)) {
                        // Not a known coverage or function/constant
                        errors.add("Unknown identifier: " + token);
                        pendingCoverageName = null;
                    } else {
                        pendingCoverageName = null;
                    }
                }
            }
        }

        for (String error : errors) {
            validatable.error(new ValidationError(error));
        }
    }
}
