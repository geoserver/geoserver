/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes.echo;

import java.util.LinkedHashMap;
import java.util.Map;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.springframework.stereotype.Component;

/**
 * A simple process that echoes back the input parameters. This process is used by the OGC API - Processes CITE test
 * suite to verify the functionality of process execution and response handling.
 */
@DescribeProcess(title = "Echo", description = "Echoes back the input parameters provided to the process.")
@Component
public class EchoProcess implements GeoServerProcess {

    @DescribeResults({
        @DescribeResult(name = "stringOutput", description = "Echoes stringInput", type = String.class),
        @DescribeResult(name = "doubleOutput", description = "Echoes doubleInput", type = Double.class),
    })
    public Map<String, Object> execute(
            @DescribeParameter(
                            name = "stringInput",
                            description = "This is an example of a STRING literal input.",
                            min = 0)
                    String stringInput,
            @DescribeParameter(
                            name = "doubleInput",
                            description =
                                    "This is an example of a DOUBLE literal input that is bounded between a value greater than 0 and 10.  The default value is 5.",
                            min = 0,
                            minValue = 0,
                            maxValue = 10,
                            defaultValue = "5")
                    Double doubleInput) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (stringInput != null) result.put("stringOutput", stringInput);
        if (doubleInput != null) {
            result.put("doubleOutput", doubleInput);
        }

        return result;
    }
}
