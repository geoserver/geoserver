package org.geoserver.platform.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Content read from a resource (or file).
 * 
 * @author Jody Garnett (Boundless).
 * @param <T>
 */
public interface Content<T> {
    /**
     * Read content from an InputStream.
     * 
     * Example:
     * 
     * <pre>
     * <code>
     * new Content.Read<Properties>() {
     *   Properties read(InputStream in){
     *     Properties p = new LinkedProperties();
     *     p.load(in);
     *     return p;
     *   }
     * }
     * </code>
     * </pre>
     * 
     * @author Jody Garnett (Boundless).
     * @param <T>
     */
    public static interface Read<C> {
        C read(InputStream in) throws Exception;
    }

    /**
     * Contents of watched resource (or file).
     * 
     * @see Wathcer.Read
     * @return parsed contents from resource
     */
    T content();
}
