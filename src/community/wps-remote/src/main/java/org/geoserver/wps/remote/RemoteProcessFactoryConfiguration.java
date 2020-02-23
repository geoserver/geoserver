/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;

/**
 * Bean that includes the configurations parameters for the remote process factory and client
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RemoteProcessFactoryConfiguration {

    /** Configuration Default Values */
    public static final long DEFAULT_SLEEP_TIME = 100;

    public static final String DEFAULT_SLEEP_TIME_NAME = "remoteProcessStubCycleSleepTime";

    private long remoteProcessStubCycleSleepTime;

    private final Map<String, String> configKvPs;

    /** Constructor */
    public RemoteProcessFactoryConfiguration(
            long remoteProcessStubCycleSleepTime, Map<String, String> configKvPs) {
        this.remoteProcessStubCycleSleepTime = remoteProcessStubCycleSleepTime;

        this.configKvPs = configKvPs;
    }

    /** @return the remoteProcessStubCycleSleepTime */
    public long getRemoteProcessStubCycleSleepTime() {
        return remoteProcessStubCycleSleepTime;
    }

    /** @param remoteProcessStubCycleSleepTime the remoteProcessStubCycleSleepTime to set */
    public void setRemoteProcessStubCycleSleepTime(long remoteProcessStubCycleSleepTime) {
        this.remoteProcessStubCycleSleepTime = remoteProcessStubCycleSleepTime;
    }

    /** @return the configKvPs */
    public Map<String, String> getConfigKvPs() {
        return configKvPs;
    }

    /** A method to access generic parsed property keys from the properties configuration file */
    public String get(String prop) {
        return (configKvPs != null ? configKvPs.get(prop) : null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RemoteProcessFactoryConfiguration [remoteProcessStubCycleSleepTime=")
                .append(remoteProcessStubCycleSleepTime)
                .append("]");
        return builder.toString();
    }
}
