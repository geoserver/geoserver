/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.wps.process.ByteArrayRawData;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.StringRawData;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.geotools.util.SimpleInternationalString;

/**
 * A test process with multiple raw outputs
 *
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(
    title = "MultiRaw",
    description = "Process used to test processes with multiple raw outputs"
)
public class MultiRawProcess {

    static final ProcessFactory getFactory() {
        return new MultiRawProcessFactory();
    }

    private static class MultiRawProcessFactory extends AnnotatedBeanProcessFactory {

        public MultiRawProcessFactory() {
            super(new SimpleInternationalString("Multiraw"), "gs", MultiRawProcess.class);
        }
    }

    @DescribeResults({
        @DescribeResult(
            name = "text",
            description = "Text output",
            meta = {"mimeTypes=text/plain"},
            type = RawData.class
        ),
        @DescribeResult(
            name = "binary",
            description = "Binary output",
            meta = {"mimeTypes=application/zip,image/png", "chosenMimeType=binaryMimeType"},
            type = RawData.class
        ),
        @DescribeResult(name = "literal", description = "A string", type = String.class)
    })
    public Map<String, Object> execute(
            @DescribeParameter(name = "id") String id,
            @DescribeParameter(name = "binaryMimeType", min = 0) String binaryMimeType)
            throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("literal", id);
        result.put("text", new StringRawData("This is the raw text", "text/plain"));
        result.put("binary", new ByteArrayRawData(new byte[100], binaryMimeType));

        return result;
    }
}
