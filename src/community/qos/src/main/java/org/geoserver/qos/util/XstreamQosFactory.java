/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.util;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.qos.QosXstreamAliasConfigurator;

/**
 * Creates a SecureXstream instance with all Qos classes allowed
 *
 * @author Fernando Miño, Geosolutions
 */
public class XstreamQosFactory {

    /**
     * Creates a SecureXstream instance with all Qos classes allowed
     *
     * @return SecureXStream
     */
    public static XStream getInstance() {
        XStream xstream = new SecureXStream();
        QosXstreamAliasConfigurator.instance().configure(xstream);
        return xstream;
    }
}
