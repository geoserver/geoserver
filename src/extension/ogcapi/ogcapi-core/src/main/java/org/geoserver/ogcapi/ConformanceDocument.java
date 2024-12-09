/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.ows.util.ResponseUtils;

/** Represents the conformance response, responses will encode in the desired formats */
public class ConformanceDocument extends AbstractDocument {

    List<String> conformsTo;
    String apiName;

    public ConformanceDocument(String apiName, List<String> conformsTo) {
        this.apiName = apiName;
        // keep it editable, regardless of how the source has been provided
        this.conformsTo = new ArrayList<>(conformsTo);

        addSelfLinks(
                ResponseUtils.appendPath(
                        APIRequestInfo.get().getServiceLandingPage(), "conformance"));
    }

    /**
     * Get the API that this conformance document applies to.
     *
     * <p>For example, for the Features spec, this is "OGC API Features".
     *
     * @return a printable (human readable) display name for the API.
     */
    @JsonIgnore
    public String getApiName() {
        return this.apiName;
    }

    /** Returns the lists of conformance classes */
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> getConformsTo() {
        return conformsTo;
    }
}
