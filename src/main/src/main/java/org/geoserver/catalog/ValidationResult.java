/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ValidationResult encapsulates the result of running Catalog::validate(), accounting for the fact
 * that there may be multiple validation errors.
 */
public class ValidationResult {
    private final List<RuntimeException> errorList;

    public ValidationResult(List<RuntimeException> errors) {
        if (errors == null) {
            this.errorList = Collections.emptyList();
        } else {
            this.errorList = Collections.unmodifiableList(new ArrayList<RuntimeException>(errors));
        }
    }

    public boolean isValid() {
        return errorList.isEmpty();
    }

    public void throwIfInvalid() {
        if (!isValid()) {
            int n = errorList.size();
            String msg = errorList.get(0).getMessage();
            throw new RuntimeException(
                    "Validation failed with " + n + " errors.  First error message is: " + msg);
        }
    }

    /**
     * Returns the list of errors associated to this validation result.
     *
     * @return list of exceptions, is never NULL
     */
    public List<RuntimeException> getErrors() {
        return errorList;
    }

    /**
     * Concatenates all the messages of the exceptions associated to this validation results using
     * the provided separator.
     *
     * @return concatenation of all exceptions messages
     */
    public String getErrosAsString(String separator) {
        return errorList
                .stream()
                .map(RuntimeException::getMessage)
                .collect(Collectors.joining(separator));
    }
}
