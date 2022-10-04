/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * A base ChainedProcessListener that does nothing, subclasses can extend to only implement the
 * methods they are interested into
 *
 * @author etj (Emanuele Tajariol @ GeoSolutions)
 */
public class ChainedProcessListenerAdapter implements ChainedProcessListener {

    @Override
    public void started(String executionId, String processName, boolean chained) {}

    @Override
    public void completed(String executionId, String processName) {}

    @Override
    public void dismissed(String executionId, String processName) {}

    @Override
    public void failed(String executionId, String processName, Exception e) {}
}
