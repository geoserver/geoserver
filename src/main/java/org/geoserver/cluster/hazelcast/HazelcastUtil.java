package org.geoserver.cluster.hazelcast;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastUtil {

    public static String localIPAsString(HazelcastInstance hz) {
        return hz.getCluster().getLocalMember().getInetSocketAddress().getAddress().getHostAddress();
    }

}
