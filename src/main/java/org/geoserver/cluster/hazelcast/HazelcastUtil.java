package org.geoserver.cluster.hazelcast;

import java.net.InetSocketAddress;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastUtil {

    public static InetSocketAddress localAddress(HazelcastInstance hz) {
        return hz.getCluster().getLocalMember().getInetSocketAddress();
    }

    public static String localIPAsString(HazelcastInstance hz) {
        InetSocketAddress addr = localAddress(hz);
        return addr.getAddress().getHostAddress()+":"+addr.getPort();
    }

}
