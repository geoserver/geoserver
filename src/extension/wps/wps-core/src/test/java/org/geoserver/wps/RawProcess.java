/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import java.io.InputStream;
import org.geoserver.wps.process.AbstractRawData;
import org.geoserver.wps.process.RawData;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.SimpleInternationalString;

@DescribeProcess(title = "Raw", description = "Process used to test raw inputs and outputs")
public class RawProcess {

    @DescribeResult(
        name = "result",
        description = "Output raster",
        meta = {"mimeTypes=application/json,text/xml", "chosenMimeType=outputMimeType"}
    )
    public RawData execute(
            @DescribeParameter(
                        name = "data",
                        description = "Input features",
                        meta = {"mimeTypes=application/json,text/xml"}
                    )
                    final RawData input,
            @DescribeParameter(name = "outputMimeType", min = 0) final String outputMimeType,
            @DescribeParameter(name = "returnNull", min = 0, defaultValue = "false")
                    final boolean returnNull) {
        if (returnNull) {
            return null;
        }
        return new RawData() {

            @Override
            public String getMimeType() {
                return outputMimeType;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return input.getInputStream();
            }

            @Override
            public String getFileExtension() {
                return AbstractRawData.DEFAULT_EXTENSION;
            }
        };
    }

    static final ProcessFactory getFactory() {
        return new AnnotatedBeanProcessFactory(
                new SimpleInternationalString("Raw data process"), "gs", RawProcess.class);
    }
}
