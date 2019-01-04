/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.other;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

/** No argument process, used for the sake of testing */
@DescribeProcess(title = "NoArgWPS", description = "NoArgWPS - test case for no argument process")
public class NoArgWPS implements GeoServerProcess {

    @DescribeResult(name = "result", description = "output result")
    public String execute() {
        return "Completed!";
    }
}
