package org.geoserver.bxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlOutputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.BxmlStreamWriter;

public abstract class PrimitiveValuesTestSupport extends BxmlTestSupport {

    public void assertEqualsArray(boolean[] expected, boolean[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(byte[] expected, byte[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(int[] expected, int[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(long[] expected, long[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(float[] expected, float[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(double[] expected, double[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void assertEqualsArray(String[] expected, String[] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @SuppressWarnings("rawtypes")
    public void testDecodeValue(QName testElement, Object value, Decoder decoder) throws Exception {
        testDecodeValue(testElement, value, value, decoder);
    }

    @SuppressWarnings("rawtypes")
    public void testDecodeValue(QName testElement, Object value, Object expected, Decoder decoder)
            throws Exception {
        Object decodedValue = getDecodedValue(testElement, value, decoder);
        assertEquals(expected, decodedValue);
    }

    @SuppressWarnings("rawtypes")
    public Object getDecodedValue(QName testElement, Object value, Decoder decoder)
            throws IOException, Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BxmlOutputFactory outputFactory = BxmlFactoryFinder.newOutputFactory();
        BxmlStreamWriter w = outputFactory.createSerializer(out);
        w.writeStartDocument();
        w.writeStartElement(testElement);
        writeValue(value, w);

        w.writeEndElement();
        w.writeEndDocument();
        w.flush();
        out.flush();

        byte[] buf = out.toByteArray();
        // String string = new String(buf);

        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        BxmlStreamReader r = getReader(in);
        r.nextTag();
        Object decodedValue = decoder.decode(r);
        return decodedValue;
    }

    private void writeValue(Object value, BxmlStreamWriter w) throws IOException {
        if (value instanceof Boolean) {
            w.writeValue((Boolean) value);
        } else if (value instanceof Byte) {
            w.writeValue((Byte) value);
        } else if (value instanceof Integer) {
            w.writeValue((Integer) value);
        } else if (value instanceof Long) {
            w.writeValue((Long) value);
        } else if (value instanceof Float) {
            w.writeValue((Float) value);
        } else if (value instanceof Double) {
            w.writeValue((Double) value);
        } else if (value instanceof String) {
            w.writeValue((String) value);
        } else if (value instanceof boolean[]) {
            boolean[] array = (boolean[]) value;
            w.writeValue(array, 0, array.length);
        } else if (value instanceof byte[]) {
            byte[] array = (byte[]) value;
            w.writeValue(array, 0, array.length);
        } else if (value instanceof int[]) {
            int[] array = (int[]) value;
            w.writeValue(array, 0, array.length);
        } else if (value instanceof long[]) {
            long[] array = (long[]) value;
            w.writeValue(array, 0, array.length);
        } else if (value instanceof float[]) {
            float[] array = (float[]) value;
            w.writeValue(array, 0, array.length);
        } else if (value instanceof double[]) {
            double[] array = (double[]) value;
            w.writeValue(array, 0, array.length);
        }
    }
}
