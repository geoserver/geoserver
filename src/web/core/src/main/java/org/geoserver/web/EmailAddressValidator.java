/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serial;
import org.apache.wicket.validation.validator.PatternValidator;
import org.geotools.util.SuppressFBWarnings;

// Spotbugs suppression justification: This class is a singleton pattern that implements
// Serializable. The singleton pattern is implemented correctly with a readResolve method to
// ensure that deserialization does not create a new instance.
@SuppressFBWarnings("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE")
public class EmailAddressValidator extends PatternValidator {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final EmailAddressValidator INSTANCE = new EmailAddressValidator();

    public static EmailAddressValidator getInstance() {
        return INSTANCE;
    }

    private EmailAddressValidator() {
        super(
                "^(?=.{1,64}@)[\\p{L}0-9_-]+(\\.[\\p{L}0-9_-]+)*@[^-][\\p{L}0-9-]+(\\.[\\p{L}0-9-]+)*(\\.[\\p{L}]{2,})$",
                2);
    }

    // Ensure singleton on deserialization
    private Object readResolve() throws java.io.ObjectStreamException {
        return INSTANCE;
    }
}
