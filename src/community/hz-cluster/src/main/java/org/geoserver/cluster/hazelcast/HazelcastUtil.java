/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import java.net.InetSocketAddress;

public class HazelcastUtil {

    public static InetSocketAddress localAddress(HazelcastInstance hz) {
        return hz.getCluster().getLocalMember().getSocketAddress();
    }

    public static String localIPAsString(HazelcastInstance hz) {
        InetSocketAddress addr = localAddress(hz);
        return addressString(addr);
    }

    public static String addressString(InetSocketAddress addr) {
        return addr.getAddress().getHostAddress() + ":" + addr.getPort();
    }

    public static String nodeId(HzCluster cluster) {
        try {
            return localIPAsString(cluster.getHz());
        } catch (RuntimeException e) {
            return "nodeId not available: " + e.getMessage();
        }
    }
}
