/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/** Represents the conformance response, responses will encode in the desired formats */
@JacksonXmlRootElement(localName = "ConformsTo")
public class ConformanceDocument {

    public static final String CORE = "http://www.opengis.net/spec/wfs-1/3.0/req/core";
    public static final String HTML = "http://www.opengis.net/spec/wfs-1/3.0/req/html";
    public static final String GEOJSON = "http://www.opengis.net/spec/wfs-1/3.0/req/geojson";
    public static final String GMLSF0 = "http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf0";
    public static final String GMLSF2 = "http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf2";
    public static final String OAS30 = "http://www.opengis.net/spec/wfs-1/3.0/req/oas30";

    List<String> conformsTo;

    public ConformanceDocument(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    /** Returns the lists of conformance classes */
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> getConformsTo() {
        return conformsTo;
    }
}
