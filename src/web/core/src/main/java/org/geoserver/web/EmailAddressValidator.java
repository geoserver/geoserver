/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import org.apache.wicket.validation.validator.PatternValidator;

public class EmailAddressValidator extends PatternValidator {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final EmailAddressValidator INSTANCE = new EmailAddressValidator();

    public static EmailAddressValidator getInstance() {
        return INSTANCE;
    }

    protected EmailAddressValidator() {
        super(
                "^(?=.{1,64}@)[\\p{L}0-9_-]+(\\.[\\p{L}0-9_-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$",
                2);
    }
}
