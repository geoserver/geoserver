package org.geoserver.bxml.base.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.gvsig.bxml.stream.BxmlStreamReader;

public abstract class ArrayWrapper {

    public abstract Object getArray();

    public abstract void expand(int valueCount);

    public abstract void addNewValues(List<String> values);

    public abstract void addNewValues(BxmlStreamReader r) throws Exception;

    /**
     * Append.
     * 
     * @param values
     *            the values
     * @param sb
     *            the sb
     * @return the t[]
     */
    public Object append(final StringBuilder sb) {
        final int len = sb.length();

        StringBuilder curr = new StringBuilder();

        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < len; i++) {
            char c = sb.charAt(i);
            if (!Character.isWhitespace(c)) {
                curr.append(c);
            } else if (curr.length() > 0) {
                strings.add(curr.toString());
                curr.setLength(0);
            }
        }
        if (curr.length() > 0) {
            strings.add(curr.toString());
            curr.setLength(0);
        }
        addNewValues(strings);
        return getArray();
    }

}
