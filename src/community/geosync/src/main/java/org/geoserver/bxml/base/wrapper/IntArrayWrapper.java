package org.geoserver.bxml.base.wrapper;

import java.util.List;

import org.gvsig.bxml.stream.BxmlStreamReader;

public class IntArrayWrapper extends ArrayWrapper {

    private int[] array = new int[0];

    private int pos = 0;

    public int[] getArray() {
        return array;
    }

    public void expand(int valueCount) {
        int newLength = array.length + valueCount;
        int[] expanded = new int[newLength];
        System.arraycopy(array, 0, expanded, 0, array.length);
        array = expanded;
    }

    @Override
    public void addNewValues(List<String> strings) {
        final int startIndex = array.length;
        expand(strings.size());
        for (int i = 0; i < strings.size(); i++) {
            array[startIndex + i] = Integer.parseInt(strings.get(i));
        }
    }

    @Override
    public void addNewValues(BxmlStreamReader r) throws Exception {
        final int valueCount = r.getValueCount();
        expand(valueCount);
        for (int i = 0; i < valueCount; i++, pos++) {
            array[pos] = r.getIntValue();
        }
    }

}
