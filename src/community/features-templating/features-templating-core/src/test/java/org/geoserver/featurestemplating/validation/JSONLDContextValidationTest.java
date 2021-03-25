/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.validation;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JSONLDContextValidationTest extends GeoServerSystemTestSupport {

    @Rule public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testValidationSuccessFull() throws IOException {
        JSONLDContextValidation validator = new JSONLDContextValidation();
        File file = validator.init();
        FileUtils.copyURLToFile(getClass().getResource("json-ld-success-validation.json"), file);
        validator.validate();
        assertEquals(0, validator.getFailedFields().size());
    }

    @Test
    public void testValidationFails() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(
                "Validation failed. Unable to resolve "
                        + "the following fields against the @context: geologicUnitType,dataType,CGI_TermValue,nilReason.");
        JSONLDContextValidation validator = new JSONLDContextValidation();
        File file = validator.init();
        FileUtils.copyURLToFile(getClass().getResource("json-ld-failed-validation.json"), file);
        validator.validate();
        assertEquals(0, validator.getFailedFields().size());
    }
}
