/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.geotools.data.Parameter;

import com.vividsolutions.jts.geom.Envelope;

public class OpenSearchParameters {

    public static String PARAM_PREFIX = "parameterPrefix";

    public static String MIN_INCLUSIVE = "minInclusive";

    public static String MAX_INCLUSIVE = "maxInclusive";

    private static final List<Parameter> BASIC_OPENSEARCH;

    private static final List<Parameter> GEO_TIME_OPENSEARCH;

    static {
        BASIC_OPENSEARCH = basicOpenSearchParameters();
        GEO_TIME_OPENSEARCH = geoTimeOpenSearchParameters();
    }

    private static List<Parameter> basicOpenSearchParameters() {
        return Arrays.asList( //
                new Parameter<>("searchTerms", String.class, 0, 1),
                new ParameterBuilder("startIndex", Integer.class).minimumInclusive(0).build());
    }

    private static List<Parameter> geoTimeOpenSearchParameters() {
        return Arrays.asList( //
                new ParameterBuilder("uid", String.class).prefix("geo").build(),
                new ParameterBuilder("box", Envelope.class).prefix("geo").build(),
                new ParameterBuilder("name", String.class).prefix("geo").build(),
                new ParameterBuilder("lat", Double.class).prefix("geo").minimumInclusive(-90)
                        .maximumInclusive(90).build(),
                new ParameterBuilder("lon", Double.class).prefix("geo").minimumInclusive(-180)
                        .maximumInclusive(180).build(),
                new ParameterBuilder("radius", Double.class).prefix("geo").minimumInclusive(0)
                        .build(),
                new ParameterBuilder("start", Date.class).prefix("time").build(),
                new ParameterBuilder("end", Date.class).prefix("time").build());
    }

    /**
     * Returns the basic opensearch search parameters
     * 
     * @return
     */
    public static List<Parameter> getBasicOpensearch(OSEOInfo info) {
        List<Parameter> result = new ArrayList<Parameter>(BASIC_OPENSEARCH);

        ParameterBuilder count = new ParameterBuilder("count", Integer.class);
        count.minimumInclusive(0);
        if (info.getMaximumRecords() > 0) {
            count.maximumInclusive(info.getMaximumRecords());
        }
        result.add(count.build());

        return result;
    }

    /**
     * Returns the OGC geo/time extension parameters
     * 
     * @return
     */
    public static List<Parameter> getGeoTimeOpensearch() {
        return GEO_TIME_OPENSEARCH;
    }
}
