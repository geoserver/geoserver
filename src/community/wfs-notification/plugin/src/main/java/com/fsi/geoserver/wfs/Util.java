package com.fsi.geoserver.wfs;

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
