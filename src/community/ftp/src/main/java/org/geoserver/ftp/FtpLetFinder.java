/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.Ftplet;
import org.geoserver.platform.GeoServerExtensions;

/**
 * Finds out the implementations of {@link FTPCallback} in the application context and adapts them
 * as {@link Ftplet}s for the {@link FTPServerManager} to add them to the list of callbacks in the
 * backing {@link FtpServer}.
 * 
 * @author groldan
 * 
 */
class FtpLetFinder {

    public FtpLetFinder() {
        //
    }

    /**
     * @return a Map of {@link Ftplet} adapters from the {@link FTPCallback} extensions with the
     *         same iteration order as provided by the {@link GeoServerExtensions#extensions(Class)
     *         extension lookup}
     */
    public Map<String, Ftplet> getFtpLets() {
        LinkedHashMap<String, Ftplet> ftplets = new LinkedHashMap<String, Ftplet>();
        List<FTPCallback> callbacks = GeoServerExtensions.extensions(FTPCallback.class);

        /*
         * The name is an artifact needed by the ftplet API but useless for us, making up the names
         * out of the FTPCallBack class names
         */
        Map<String, Integer> names = new HashMap<String, Integer>();
        for (FTPCallback callback : callbacks) {
            FtpLetCallBackAdapter ftplet = new FtpLetCallBackAdapter(callback);
            String name = callback.getClass().getName();
            Integer index = names.containsKey(name) ? (names.get(name) + 1) : 1;
            names.put(name, index);
            ftplets.put(name + "-" + index, ftplet);
        }

        return ftplets;
    }
}
