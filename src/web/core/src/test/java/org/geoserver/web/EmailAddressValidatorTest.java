/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmailAddressValidatorTest {

    @Test
    public void shouldPassSavingEmailIsValid() {
        StringValidatable validatable = new StringValidatable("test@mail.com");
        EmailAddressValidator.getInstance().validate(validatable);
        assertTrue(validatable.isValid());
    }

    @Test
    public void shouldPassIfUnicodeEmailIsValid() {
        StringValidatable validatable = new StringValidatable("用户名@领域.电脑");
        EmailAddressValidator.getInstance().validate(validatable);
        assertTrue(validatable.isValid());
    }

    @Test
    public void shouldFailIfEmailIsInvalid() {
        StringValidatable validatable = new StringValidatable("test@gmail.com\"><script>alert('XSS')</script>");
        EmailAddressValidator.getInstance().validate(validatable);
        assertFalse(validatable.isValid());
    }
}
