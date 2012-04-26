package org.geoserver.bxml;

import java.util.Date;

import javax.xml.namespace.QName;

import org.geoserver.bxml.base.DateDecoder;
import org.geoserver.bxml.base.PrimitiveDecoder;
import org.geoserver.bxml.base.PrimitiveListDecoder;
import org.geoserver.bxml.base.StringDecoder;
import org.geotools.feature.type.DateUtil;

public class PrimitiveValuesDecoderTest extends PrimitiveValuesTestSupport {

    public void testReadBoolean() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, "true", true, new PrimitiveDecoder<Boolean>(testElement,
                Boolean.class));
        testDecodeValue(testElement, "false", false, new PrimitiveDecoder<Boolean>(testElement,
                Boolean.class));
        testDecodeValue(testElement, true,
                new PrimitiveDecoder<Boolean>(testElement, Boolean.class));
        testDecodeValue(testElement, false, new PrimitiveDecoder<Boolean>(testElement,
                Boolean.class));
    }

    public void testReadByte() throws Exception {
        QName testElement = new QName("test");
        byte expected = 10;
        testDecodeValue(testElement, expected, new PrimitiveDecoder<Byte>(testElement, Byte.class));
        testDecodeValue(testElement, "10", expected, new PrimitiveDecoder<Byte>(testElement,
                Byte.class));
    }

    public void testReadInteger() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, 25, new PrimitiveDecoder<Integer>(testElement, Integer.class));
        testDecodeValue(testElement, "125", 125, new PrimitiveDecoder<Integer>(testElement,
                Integer.class));
    }

    public void testReadLong() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, 25l, new PrimitiveDecoder<Long>(testElement, Long.class));
        testDecodeValue(testElement, "125", 125l, new PrimitiveDecoder<Long>(testElement,
                Long.class));
    }

    public void testReadFloat() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, "32.21", new Float(32.21), new PrimitiveDecoder<Float>(
                testElement, Float.class));
        testDecodeValue(testElement, new Float(125.15), new PrimitiveDecoder<Float>(testElement,
                Float.class));
    }

    public QName testReadDouble() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, "25.0", 25.0, new PrimitiveDecoder<Double>(testElement,
                Double.class));
        testDecodeValue(testElement, new Double(26.2), new PrimitiveDecoder<Double>(testElement,
                Double.class));
        testDecodeValue(testElement, new Double(5445.542), new PrimitiveDecoder<Double>(
                testElement, Double.class));
        return testElement;
    }

    public void testReadString() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, "ba d ef ghy", new StringDecoder(testElement));
    }

    public void testReadListBoolean() throws Exception {
        QName testElement = new QName("test");
        boolean[] expected = new boolean[] { true, false, true };
        boolean[] actual = (boolean[]) getDecodedValue(testElement, expected,
                new PrimitiveListDecoder<boolean[]>(testElement, boolean[].class));
        assertEqualsArray(expected, actual);

        String insert2 = "true false true";
        actual = (boolean[]) getDecodedValue(testElement, insert2,
                new PrimitiveListDecoder<boolean[]>(testElement, boolean[].class));
        assertEqualsArray(expected, actual);
    }

    public void testReadListByte() throws Exception {
        QName testElement = new QName("test");
        byte[] expected = new byte[] { 10, 25, 50 };
        byte[] actual = (byte[]) getDecodedValue(testElement, expected,
                new PrimitiveListDecoder<byte[]>(testElement, byte[].class));
        assertEqualsArray(expected, actual);

        String insert2 = "10 25 50";
        actual = (byte[]) getDecodedValue(testElement, insert2, new PrimitiveListDecoder<byte[]>(
                testElement, byte[].class));
        assertEqualsArray(expected, actual);
    }

    public void testReadListInteger() throws Exception {
        QName testElement = new QName("test");
        int[] expected = new int[] { 10, 25, 50 };
        int[] actual = (int[]) getDecodedValue(testElement, expected,
                new PrimitiveListDecoder<int[]>(testElement, int[].class));
        assertEqualsArray(expected, actual);

        String insert2 = "10 25 50";
        actual = (int[]) getDecodedValue(testElement, insert2, new PrimitiveListDecoder<int[]>(
                testElement, int[].class));
        assertEqualsArray(expected, actual);
    }

    public void testReadListLong() throws Exception {
        QName testElement = new QName("test");
        long[] expected = new long[] { 10l, 25l, 50l };
        long[] actual = (long[]) getDecodedValue(testElement, expected,
                new PrimitiveListDecoder<long[]>(testElement, long[].class));
        assertEqualsArray(expected, actual);

        String insert2 = "10 25 50";
        actual = (long[]) getDecodedValue(testElement, insert2, new PrimitiveListDecoder<long[]>(
                testElement, long[].class));
        assertEqualsArray(expected, actual);
    }

    public void testReadListFloat() throws Exception {
        QName testElement = new QName("test");
        float[] array = new float[] { 1.1f, 2.3f, 6.4f };
        float[] array2 = (float[]) getDecodedValue(testElement, array,
                new PrimitiveListDecoder<float[]>(testElement, float[].class));
        assertEqualsArray(array, array2);
    }

    public void testReadListDouble() throws Exception {
        QName testElement = new QName("test");
        double[] array = new double[] { 1.1, 2.3, 6.4 };
        double[] array2 = (double[]) getDecodedValue(testElement, array,
                new PrimitiveListDecoder<double[]>(testElement, double[].class));
        assertEqualsArray(array, array2);
    }

    public void testReadDate() throws Exception {
        QName testElement = new QName("test");
        testDecodeValue(testElement, "2011-07-07T22:47:06.507Z",
                new Date(DateUtil.parseDateTime("2011-07-07T22:47:06.507Z")), new DateDecoder(
                        testElement));
    }

}
