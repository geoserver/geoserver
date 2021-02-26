/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.wcs.responses.covjson.Coverage;
import org.geoserver.wcs.responses.covjson.CoverageJson;
import org.geotools.coverage.grid.GridCoverage2D;

/** Writes out a CoverageJSON (the write parameters are provided during construction */
public class CoverageJsonEncoder {

    GridCoverage2D source;

    public CoverageJsonEncoder(GridCoverage2D source) {
        this.source = source;
    }

    /** Writes out the CoverageJSON file */
    void write(OutputStream stream) throws IOException {

        CoverageJson covJson = new Coverage(source);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(stream, covJson);
    };
}
