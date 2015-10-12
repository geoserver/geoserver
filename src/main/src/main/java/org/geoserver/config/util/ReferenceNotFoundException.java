package org.geoserver.config.util;

import org.geoserver.ows.HttpErrorCodeException;

/**
 * Exception thrown during XStream while evaluating a referenced object that
 * cannot be located in the catalog
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class ReferenceNotFoundException extends RuntimeException {

    private Class clazz;
    private String ref;
    private String pre;

    public ReferenceNotFoundException(String ref, String pre, Class clazz) {
        super("Failed to locate reference to '" + (pre != null ? (pre + ":" + ref ) : ref) + "' of type " + clazz.getSimpleName());
        this.pre = pre;
        this.ref = ref;
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getRef() {
        return ref;
    }

    public String getPre() {
        return pre;
    }

    
    
}
