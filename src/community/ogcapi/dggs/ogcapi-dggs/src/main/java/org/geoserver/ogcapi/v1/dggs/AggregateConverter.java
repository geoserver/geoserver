/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.ogcapi.APIException;
import org.geotools.feature.visitor.Aggregate;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Converts a comma separated list of aggregate names to an Aggregate[] */
@Component
public class AggregateConverter implements Converter<String, Aggregate[]> {

    private static final Map<String, Aggregate> AGGREGATES = new HashMap<>();

    static {
        Arrays.stream(Aggregate.values())
                .forEach(
                        a -> {
                            if (a == Aggregate.STD_DEV) AGGREGATES.put("std-dev", a);
                            else if (a == Aggregate.AVERAGE) AGGREGATES.put("mean", a);
                            else if (a != Aggregate.SUMAREA && a != Aggregate.MEDIAN)
                                AGGREGATES.put(a.name().toLowerCase(), a);
                        });
    }

    public static Map<String, Aggregate> getAggregates() {
        return Collections.unmodifiableMap(AGGREGATES);
    }

    @Override
    public Aggregate[] convert(String s) {
        return Arrays.stream(s.split("\\s*,\\s*"))
                .map(n -> mapAggregate(n))
                .toArray(n -> new Aggregate[n]);
    }

    private Aggregate mapAggregate(String name) {
        Aggregate agg = AGGREGATES.get(name);
        if (agg == null)
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Un-recognized aggregate : " + name,
                    HttpStatus.BAD_REQUEST);
        return agg;
    }
}
