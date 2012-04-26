package org.geoserver.bxml;

import java.io.IOException;

import org.gvsig.bxml.stream.BxmlStreamWriter;

public interface Encoder<T> {

    /**
     * @param obj
     * @param w
     * @throws IOException
     */
    public abstract void encode(final T obj, final BxmlStreamWriter w) throws IOException;

}