/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import com.github.jsonldjava.utils.JsonUtils;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.validation.JSONLDContextValidation;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;

/**
 * This class provides methods to validate a template output, performing different validations
 * according to the output format.
 */
public class TemplateOutputValidator {

    private SupportedFormat outputFormat;

    private String message;

    public TemplateOutputValidator(SupportedFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public boolean validate(String input) {
        boolean result = true;
        if (outputFormat.equals(SupportedFormat.JSONLD)) result = validateJSONLD(input);
        else if (outputFormat.equals(SupportedFormat.GML)) result = validateGML(input);
        else if (outputFormat.equals(SupportedFormat.GEOJSON)
                || outputFormat.equals(SupportedFormat.HTML)) result = validateGeoJSON(input);
        if (result) this.message = "Result is valid";
        return result;
    }

    private boolean validateJSONLD(String input) {
        boolean result = true;
        try {
            Object json = JsonUtils.fromString(input);
            JSONLDContextValidation contextValidation = new JSONLDContextValidation();
            contextValidation.validate(json);
        } catch (Exception e) {
            message = e.getMessage();
            result = false;
        }
        return result;
    }

    private boolean validateGML(String input) {
        boolean result = true;
        Configuration configuration = new org.geotools.wfs.v2_0.WFSConfiguration();
        List<String> validationErrors = new ArrayList<>();
        try (ByteArrayInputStream is = new ByteArrayInputStream(input.getBytes())) {
            Parser parser = new Parser(configuration);
            parser.setValidating(true);
            parser.parse(is);
            validationErrors.addAll(
                    parser.getValidationErrors().stream()
                            .map(e -> e.getMessage())
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            validationErrors.add(e.getMessage());
            result = false;
        }
        if (!validationErrors.isEmpty()) {
            result = false;
            StringBuilder builder =
                    new StringBuilder(
                            "The following errors occured while validating the gml output: ");
            for (int i = 0; i < validationErrors.size(); i++) {
                String error = validationErrors.get(i);
                builder.append(i + 1).append(" ").append(error).append("");
            }
            this.message = builder.toString();
        }

        return result;
    }

    private boolean validateGeoJSON(String input) {
        return true;
    }

    public String getMessage() {
        return message;
    }
}
