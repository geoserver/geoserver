/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.Arrays;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geotools.util.Converters;

public class ComplexMetadataAttributeImpl<T extends Serializable>
        implements ComplexMetadataAttribute<T> {

    private static final long serialVersionUID = 7309095204153589550L;

    private Map<String, Serializable> map;

    private String strPath;

    private ComplexMetadataIndexReference indexRef;

    private Class<T> clazz;

    public ComplexMetadataAttributeImpl(
            Map<String, Serializable> map,
            String strPath,
            ComplexMetadataIndexReference indexRef,
            Class<T> clazz) {
        this.map = map;
        this.strPath = strPath;
        this.indexRef = indexRef;
        this.clazz = clazz;
    }

    @Override
    public T getValue() {
        Object val = map.get(strPath);
        for (int i : indexRef.getIndex()) {
            if (val instanceof List<?>) {
                List<?> list = (List<?>) val;
                try {
                    val = list.get(i);
                } catch (IndexOutOfBoundsException e) {
                    val = null;
                }
            } else {
                break;
            }
        }
        if (val == null) {
            return null;
        } else if (clazz.isInstance(val)) {
            return clazz.cast(val);
        } else {
            return Converters.convert(val, clazz);
        }
    }

    @Override
    public void setValue(T value) {
        map.put(strPath, setValueInternal(map.get(strPath), indexRef.getIndex(), value));
    }

    protected Serializable setValueInternal(
            Serializable originalValue, int[] index, Serializable newValue) {
        if (index.length == 0) {
            return newValue;
        } else if (originalValue instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) originalValue;
            while (list.size() < index[0] + 1) {
                list.add(null);
            }
            if (index.length > 1) {
                int[] subIndex = Arrays.copyOfRange(index, 1, index.length);
                list.set(index[0], setValueInternal(list.get(index[0]), subIndex, newValue));
            } else {
                list.set(index[0], newValue);
            }
            return (Serializable) list;
        } else {
            ArrayList<Object> list = new ArrayList<Object>();
            if (index.length > 1) {
                int[] subIndex = Arrays.copyOfRange(index, 1, index.length);
                while (list.size() < index[0] + 1) {
                    list.add(null);
                }
                list.set(index[0], setValueInternal(originalValue, subIndex, newValue));
            } else {
                for (int i = 0; i < index[0] + 1; i++) {
                    list.add(originalValue);
                }
                list.set(index[0], newValue);
            }
            return list;
        }
    }

    @Override
    public Integer getIndex() {
        return indexRef.getIndex()[indexRef.getIndex().length - 1];
    }
}
