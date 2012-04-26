package org.geoserver.bxml.base.wrapper;

import java.util.List;

import org.gvsig.bxml.stream.BxmlStreamReader;

public class FloatArrayWrapper extends ArrayWrapper {

    private float[] array = new float[0];

    private int pos = 0;

    public float[] getArray() {
        return array;
    }

    public void expand(int valueCount) {
        int newLength = array.length + valueCount;
        float[] expanded = new float[newLength];
        System.arraycopy(array, 0, expanded, 0, array.length);
        array = expanded;
    }

    @Override
    public void addNewValues(List<String> strings) {
        final int startIndex = array.length;
        expand(strings.size());
        for (int i = 0; i < strings.size(); i++) {
            array[startIndex + i] = Float.parseFloat(strings.get(i));
        }
    }

    @Override
    public void addNewValues(BxmlStreamReader r) throws Exception {
        final int valueCount = r.getValueCount();
        expand(valueCount);
        for (int i = 0; i < valueCount; i++, pos++) {
            array[pos] = r.getFloatValue();
        }
    }

}
