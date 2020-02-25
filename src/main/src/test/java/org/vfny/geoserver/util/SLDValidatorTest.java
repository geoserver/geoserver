/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class SLDValidatorTest {

    @Test
    public void testValid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("valid.sld"));

        // showErrors(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testInvalid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("invalid.sld"));

        // showErrors(errors);
        assertFalse(errors.isEmpty());
    }

    /** Tests validation for Localized tag. See GEOS-9132. */
    @Test
    public void testi18nValid() throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = validator.validateSLD(getClass().getResourceAsStream("validi18n.sld"));
        assertTrue(errors.isEmpty());
    }

    void showErrors(List errors) {
        for (Exception err : (List<Exception>) errors) {
            System.out.println(err.getMessage());
        }
    }
}
