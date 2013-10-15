package org.geoserver.wfs.notification;

public class Util {
    private Util() {
        
    }
    
    public static <T> T coalesce(T... val) {
        for(T t : val) {
            if(t != null) {
                return t;
            }
        }

        return null;
    }
}
