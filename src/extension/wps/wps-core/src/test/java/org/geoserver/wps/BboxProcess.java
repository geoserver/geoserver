/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.SimpleInternationalString;

@DescribeProcess(title = "BboxProcess", description = "Test process accepting a BoundingBox input")
public class BboxProcess {

    @DescribeResult(name = "result", description = "Output summary of the bounds")
    public String execute(
            @DescribeParameter(name = "bounds", description = "Bounding box parameter") ReferencedEnvelope bounds) {
        if (bounds == null) {
            return "No bounds provided";
        }
        return "MinX: " + bounds.getMinX() + ", MinY: " + bounds.getMinY();
    }

    public static final ProcessFactory getFactory() {
        return new BboxProcessFactory();
    }

    private static class BboxProcessFactory extends AnnotatedBeanProcessFactory {
        public BboxProcessFactory() {
            super(new SimpleInternationalString("Bbox Process"), "gs", BboxProcess.class);
        }
    }
}
