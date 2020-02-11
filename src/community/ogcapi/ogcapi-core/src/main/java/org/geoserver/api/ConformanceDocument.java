/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

/** Represents the conformance response, responses will encode in the desired formats */
public class ConformanceDocument extends AbstractDocument {

    List<String> conformsTo;

    public ConformanceDocument(List<String> conformsTo) {
        // keep it editable, regardless of how the source has been provided
        this.conformsTo = new ArrayList<>(conformsTo);
    }

    /** Returns the lists of conformance classes */
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> getConformsTo() {
        return conformsTo;
    }
}
