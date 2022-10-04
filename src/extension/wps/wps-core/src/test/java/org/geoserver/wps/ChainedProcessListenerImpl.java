/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.List;

/** @author etj (Emanuele Tajariol @ GeoSolutions) */
public class ChainedProcessListenerImpl implements ChainedProcessListener {

    public List<String> recorded = new ArrayList<>();

    @Override
    public void started(String executionId, String processName, boolean chained) {
        recorded.add("started " + processName);
    }

    @Override
    public void completed(String executionId, String processName) {
        recorded.add("completed " + processName);
    }

    @Override
    public void dismissed(String executionId, String processName) {
        recorded.add("dismissed " + processName);
    }

    @Override
    public void failed(String executionId, String processName, Exception e) {
        recorded.add("failed " + processName);
    }

    public void cleanup() {
        recorded = new ArrayList<>();
    }
}
