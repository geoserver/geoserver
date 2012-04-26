package org.geoserver.bxml.base;

import java.io.IOException;

import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Class PrimitiveReader.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class PrimitiveReader<T> {

    /**
     * Read.
     * 
     * @param r
     *            the r
     * @param toType
     *            the to type
     * @param type
     *            the type
     * @return the t
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("rawtypes")
    public T read(BxmlStreamReader r, Class toType, EventType type) throws Exception {
        Object value = readValue(r, type);
        if (value == null) {
            return null;
        }
        return convertToType(value, toType);
    }

    /**
     * Convert the given <code>value</code> to type given in <code>type</code> parameter.
     * 
     * @param value
     *            the value
     * @param type
     *            the type
     * @return the t
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T convertToType(Object value, Class type) {

        if (value == null) {
            return null;
        }

        if (type.equals(Object.class)) {
            return (T) value;
        }

        if (type.equals(String.class)) {
            return (T) value.toString();
        }

        if (type.equals(Boolean.class)) {
            return (T) Boolean.valueOf(value.toString());
        }

        if (type.equals(Double.class)) {
            return (T) new Double(value.toString());
        }

        if (type.equals(Float.class)) {
            return (T) new Float(value.toString());
        }

        if (type.equals(Byte.class)) {
            return (T) new Byte(value.toString());
        }

        if (type.equals(Integer.class)) {
            return (T) new Integer(value.toString());
        }

        if (type.equals(Long.class)) {
            return (T) new Long(value.toString());
        }

        return null;
    }

    /**
     * Return a value from r given an EventType.Value_...
     * 
     * @param r
     *            the r
     * @param type
     *            the type
     * @return the object
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws Exception
     *             the exception
     */
    public Object readValue(BxmlStreamReader r, EventType type) throws IOException, Exception {
        Object value = null;
        switch (type) {
        case VALUE_BOOL:
            value = r.getBooleanValue();
            break;
        case VALUE_DOUBLE:
            value = r.getDoubleValue();
            break;
        case VALUE_FLOAT:
            value = r.getFloatValue();
            break;
        case VALUE_BYTE:
            value = r.getByteValue();
            break;
        case VALUE_INT:
            value = r.getIntValue();
            break;
        case VALUE_LONG:
            value = r.getLongValue();
            break;
        case VALUE_STRING:
            value = new StringValueDecoder().decode(r);
            break;
        case VALUE_CDATA:
            value = new StringValueDecoder().decode(r);
            break;
        }
        return value;
    }

}
