/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.v1.coverages;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import javax.xml.bind.annotation.XmlTransient;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ogcapi.AbstractDocument;
import org.geotools.api.coverage.grid.GridCoverage;

/**
 * A Coverages response that contains both the WCS request and response, to help reusing the
 * traditional WCS output formats
 */
@JsonIgnoreType // not meant for jackson serialization
@XmlTransient
public class CoveragesResponse extends AbstractDocument {

    private final EObject request;
    private final GridCoverage response;

    public CoveragesResponse(EObject request, GridCoverage response) {
        this.request = request;
        this.response = response;
    }

    public EObject getRequest() {
        return request;
    }

    public GridCoverage getResponse() {
        return response;
    }
}
