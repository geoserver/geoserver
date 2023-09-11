/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;

/**
 * Base class for all the writers it provides some common fields as well as few common functionality
 * that can ben shared among the writers.
 */
public abstract class TemplateOutputWriter implements AutoCloseable {

    protected long numberReturned = 0L;

    protected CoordinateReferenceSystem crs;

    protected CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    /**
     * Write an element name. An element name may be the field name in a JSON or an element tag name
     * in case of a gml output.
     *
     * @param elementName an object representing the element name.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void writeElementName(Object elementName, EncodingHints encodingHints)
            throws IOException;

    /**
     * Write an element value. An element value may be i.e. a field value in a JSON or an element
     * content in a gml output.
     *
     * @param elementValue an object representing the element value.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void writeElementValue(Object elementValue, EncodingHints encodingHints)
            throws IOException;

    /**
     * @param key the element name to be encoded.
     * @param elementValue the element value to be encoded.
     * @param encodingHints encodingHints a map eventually holding hints about how to encode the
     *     value.
     * @throws IOException
     */
    public abstract void writeElementNameAndValue(
            String key, Object elementValue, EncodingHints encodingHints) throws IOException;

    /**
     * Write a static content, no matter how nested.
     *
     * @param name the name of the element.
     * @param staticContent an object representing some static content.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void writeStaticContent(
            String name, Object staticContent, EncodingHints encodingHints) throws IOException;

    /**
     * Write the start of an object.
     *
     * @param name the name of the object.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void startObject(String name, EncodingHints encodingHints) throws IOException;

    /**
     * Write the end of an object.
     *
     * @param name the name of the object to close.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void endObject(String name, EncodingHints encodingHints) throws IOException;

    /**
     * Start a list
     *
     * @param name the name of the list to start if needed.
     * @param encodingHints a map eventually holding hints about how to encode the value.
     * @throws IOException
     */
    public abstract void startArray(String name, EncodingHints encodingHints) throws IOException;

    /**
     * @param name the name of the array to end.
     * @param encodingHints a map eventually holding hints about how to encode the value
     * @throws IOException
     */
    public abstract void endArray(String name, EncodingHints encodingHints) throws IOException;

    /**
     * @param encodingHints a map eventually holding hints about how to start the template output or
     *     additional data to write.
     * @throws IOException
     */
    public abstract void startTemplateOutput(EncodingHints encodingHints) throws IOException;

    /**
     * @param encodingHints a map eventually holding hints about how to end the template output or
     *     additional data to write.
     * @throws IOException
     */
    public abstract void endTemplateOutput(EncodingHints encodingHints) throws IOException;

    /**
     * Write the featureCount value to the output.
     *
     * @param featureCount the featureCount.
     * @throws IOException
     */
    public abstract void writeCollectionCounts(BigInteger featureCount) throws IOException;

    /**
     * Write the CRS to the output.
     *
     * @throws IOException
     */
    public abstract void writeCrs() throws IOException;

    /**
     * Write the collection bounds to the output.
     *
     * @param bounds the collection bounds.
     * @throws IOException
     */
    public abstract void writeCollectionBounds(ReferencedEnvelope bounds) throws IOException;

    /**
     * Write the timeStamp.
     *
     * @throws IOException
     */
    public abstract void writeTimeStamp() throws IOException;

    /**
     * Write the numberReturned value.
     *
     * @throws IOException
     */
    public abstract void writeNumberReturned() throws IOException;

    /**
     * Increment the numberReturned value of 1. Can be used to keep track of the number of features
     * returned while iterating a FeatureCollection.
     */
    public void incrementNumberReturned() {
        numberReturned += 1;
    }

    /**
     * Get the CRS used by this TemplateOutputWriter.
     *
     * @return the CRS used by this TemplateOutputWriter.
     */
    public CoordinateReferenceSystem getCrs() {
        return this.crs;
    }

    /**
     * Set the CRS that this TemplateOutputWriter will use.
     *
     * @param crs the CRS.
     */
    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
        setAxisOrder(CRS.getAxisOrder(crs));
    }

    /**
     * Set the axisOrder that this TemplateOutputWriter will use.
     *
     * @param axisOrder the axisOrder.
     */
    public void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }

    /**
     * Found the CRS identifier, suitable to be encoded.
     *
     * @param crs the CoordinateReferenceSystem from which extract the crs identifier.
     * @return the crs identifier.
     * @throws IOException
     */
    protected static String getCRSIdentifier(CoordinateReferenceSystem crs) throws IOException {
        try {
            String identifier = null;
            Integer code = CRS.lookupEpsgCode(crs, true);
            if (code != null) {
                if (code != null) {
                    identifier = SrsSyntax.OGC_URN.getPrefix() + code;
                }
            } else {
                identifier = CRS.lookupIdentifier(crs, true);
            }
            return identifier;
        } catch (FactoryException e) {
            throw new IOException(e);
        }
    }

    protected boolean isNull(Object value) {
        boolean isNull = false;
        if (value == null || value.equals("null") || "".equals(value)) isNull = true;
        else if (value instanceof List) {
            isNull = ((List) value).isEmpty();
        } else if (value.getClass().isArray()) {
            List list = Converters.convert(value, List.class);
            isNull = list.isEmpty();
        }
        return isNull;
    }

    protected <T> T getEncodingHintIfPresent(
            EncodingHints encodingHints, String name, Class<T> cast) {
        T result = null;
        if (encodingHints != null) {
            result = encodingHints.get(name, cast);
        }
        return result;
    }
}
