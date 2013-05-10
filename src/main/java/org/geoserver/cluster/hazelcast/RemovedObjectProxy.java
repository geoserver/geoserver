package org.geoserver.cluster.hazelcast;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RemovedObjectProxy implements InvocationHandler {

    String id;
    String name;

    public RemovedObjectProxy(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("id".equalsIgnoreCase(method.getName())) {
            return id;
        }
        if ("getname".equalsIgnoreCase(method.getName())) {
            return name;
        }
        return null;
    }

}
