/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.SimpleInternationalString;

@DescribeProcess(
    title = "MultiOutputEcho",
    description = "Echo string process used to test processes with multiple outputs"
)
public class MultiOutputEchoProcess implements org.geoserver.wps.gs.GeoServerProcess {

    static final ProcessFactory getFactory() {
        return new MultiOutputEchoProcessProcessFactory();
    }

    private static class MultiOutputEchoProcessProcessFactory extends AnnotatedBeanProcessFactory {

        public MultiOutputEchoProcessProcessFactory() {
            super(
                    new SimpleInternationalString("MultiOutputEcho"),
                    "gs",
                    MultiOutputEchoProcess.class);
        }
    }

    @DescribeResult(name = "result", description = "output result")
    public String execute(
            @DescribeParameter(name = "text", description = "text to return") String text) {
        return "Echo='" + text + "'";
    }
}
