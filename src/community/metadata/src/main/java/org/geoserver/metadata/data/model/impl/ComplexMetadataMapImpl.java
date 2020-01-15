/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;

public class ComplexMetadataMapImpl implements ComplexMetadataMap {

    private static final long serialVersionUID = 1857277796433431947L;

    private static final String PATH_SEPARATOR = "/";

    /** the underlying flat map */
    private Map<String, Serializable> delegate;

    /** for submaps */
    private String[] basePath;

    /** for submaps */
    private ComplexMetadataIndexReference baseIndexRef;

    /** indexes, helps attributes to auto-update their indexes after delete */
    private HashMap<String, ArrayList<ComplexMetadataIndexReference>> indexes =
            new HashMap<String, ArrayList<ComplexMetadataIndexReference>>();

    public ComplexMetadataMapImpl(Map<String, Serializable> delegate) {
        this.delegate = delegate;
        this.basePath = new String[] {};
        this.baseIndexRef = new ComplexMetadataIndexReference(new int[] {});
    }

    protected ComplexMetadataMapImpl(
            ComplexMetadataMapImpl parent,
            String[] basePath,
            ComplexMetadataIndexReference baseIndexRef) {
        this.delegate = parent.getDelegate();
        this.indexes = parent.getIndexes();
        this.basePath = basePath;
        this.baseIndexRef = baseIndexRef;
    }

    public ComplexMetadataMapImpl(ComplexMetadataMapImpl other) {
        this.delegate = other.getDelegate();
        this.indexes = other.getIndexes();
        this.basePath = other.basePath;
        this.baseIndexRef = other.baseIndexRef;
    }

    @Override
    public <T extends Serializable> ComplexMetadataAttribute<T> get(
            Class<T> clazz, String path, int... index) {
        String strPath = String.join(PATH_SEPARATOR, concat(basePath, path));
        int[] fullIndex = concat(baseIndexRef.getIndex(), index);
        return new ComplexMetadataAttributeImpl<T>(
                getDelegate(), strPath, getOrCreateIndex(strPath, fullIndex), clazz);
    }

    @Override
    public ComplexMetadataMap subMap(String path, int... index) {
        String[] fullPath = concat(basePath, path);
        return new ComplexMetadataMapImpl(
                this,
                fullPath,
                getOrCreateIndex(
                        String.join(PATH_SEPARATOR, fullPath),
                        concat(baseIndexRef.getIndex(), index)));
    }

    protected ComplexMetadataIndexReference getOrCreateIndex(String strPath, int[] index) {
        ComplexMetadataIndexReference result = null;
        ArrayList<ComplexMetadataIndexReference> list = getIndexes().get(strPath);
        if (list == null) {
            list = new ArrayList<>();
            getIndexes().put(strPath, list);
        } else {
            for (ComplexMetadataIndexReference item : list) {
                if (Arrays.equals(item.getIndex(), index)) {
                    result = item;
                    break;
                }
            }
        }
        if (result == null) {
            result = new ComplexMetadataIndexReference(index);
            list.add(result);
        }
        return result;
    }

    @Override
    public int size(String path, int... index) {
        String strPath = String.join(PATH_SEPARATOR, concat(basePath, path));
        int[] fullIndex = concat(baseIndexRef.getIndex(), index);
        if (getDelegate().containsKey(strPath)) {
            return sizeInternal(strPath, fullIndex);
        } else {
            // subtype
            int size = 0;
            for (String key : getDelegate().keySet()) {
                if (key.startsWith(strPath + PATH_SEPARATOR)) {
                    size = Math.max(size, sizeInternal(key, fullIndex));
                }
            }
            return size;
        }
    }

    @SuppressWarnings("unchecked")
    private int sizeInternal(String path, int[] index) {
        Object object = getDelegate().get(path);
        for (int i = 0; i < index.length; i++) {
            if (object instanceof List<?>) {
                if (index[i] < ((List<Object>) object).size()) {
                    object = ((List<Object>) object).get(index[i]);
                } else {
                    object = null;
                }
            }
        }
        if (object instanceof List<?>) {
            return ((List<Object>) object).size();
        } else {
            return object == null ? 0 : 1;
        }
    }

    @Override
    public void delete(String path, int... index) {
        String strPath = String.join(PATH_SEPARATOR, concat(basePath, path));
        int[] fullIndex = concat(baseIndexRef.getIndex(), index);
        if (fullIndex.length > 0) {
            deleteFromList(strPath, fullIndex, index.length == 0);
            for (String key : getDelegate().keySet()) {
                if ("".equals(strPath) || key.startsWith(strPath + PATH_SEPARATOR)) {
                    deleteFromList(key, fullIndex, index.length == 0);
                }
            }
        } else {
            getDelegate().remove(strPath);
            Iterator<String> it = getDelegate().keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if ("".equals(strPath) || key.startsWith(strPath + PATH_SEPARATOR)) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public ComplexMetadataMap clone() {
        Map<String, Serializable> newDelegate = new HashMap<String, Serializable>();
        String strPath = String.join(PATH_SEPARATOR, basePath);
        for (String key : getDelegate().keySet()) {
            if ("".equals(strPath) || key.startsWith(strPath + PATH_SEPARATOR)) {
                String strippedKey = "".equals(strPath) ? key : key.substring(strPath.length() + 1);
                newDelegate.put(
                        strippedKey, dimCopy(get(Serializable.class, strippedKey).getValue()));
            }
        }
        return new ComplexMetadataMapImpl(newDelegate);
    }

    @SuppressWarnings("unchecked")
    private void deleteFromList(String path, int[] index, boolean keepSpace) {
        Object object = getDelegate().get(path);
        for (int i = 0; i < index.length - 1; i++) {
            if (object instanceof List<?>) {
                if (index[i] < ((List<Object>) object).size()) {
                    object = ((List<Object>) object).get(index[i]);
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (object instanceof List<?>) {
            if (index[index.length - 1] < ((List<Object>) object).size()) {
                if (keepSpace) {
                    ((List<Object>) object).set(index[index.length - 1], null);
                } else {
                    ((List<Object>) object).remove(index[index.length - 1]);
                }
            } else {
                return;
            }
        }
        if (!keepSpace) {
            // update indexes
            ArrayList<ComplexMetadataIndexReference> list = getIndexes().get(path);
            if (list != null) {
                Iterator<ComplexMetadataIndexReference> it = list.iterator();
                while (it.hasNext()) {
                    ComplexMetadataIndexReference item = it.next();
                    if (item.getIndex().length >= index.length) {
                        int[] rootItemIndex =
                                Arrays.copyOfRange(item.getIndex(), 0, index.length - 1);
                        int[] rootIndex = Arrays.copyOfRange(index, 0, index.length - 1);
                        if (Arrays.equals(rootIndex, rootItemIndex)) {
                            if (item.getIndex()[index.length - 1] == index[index.length - 1]) {
                                item.setIndex(null);
                                it.remove();
                            } else if (item.getIndex()[index.length - 1]
                                    > index[index.length - 1]) {
                                item.getIndex()[index.length - 1]--;
                            }
                        }
                    }
                }
            }
        }
    }

    protected static String[] concat(String[] first, String... second) {
        return (String[]) ArrayUtils.addAll(first, second);
    }

    protected static int[] concat(int[] first, int... second) {
        return ArrayUtils.addAll(first, second);
    }

    private Map<String, Serializable> getDelegate() {
        return delegate;
    }

    private HashMap<String, ArrayList<ComplexMetadataIndexReference>> getIndexes() {
        return indexes;
    }

    @Override
    public int getIndex() {
        return baseIndexRef.getIndex()[baseIndexRef.getIndex().length - 1];
    }

    public static Serializable dimCopy(Serializable source) {
        if (source instanceof List) {
            ArrayList<Serializable> list = new ArrayList<>();
            for (Object item : ((List<?>) source)) {
                list.add(dimCopy((Serializable) item));
            }
            return list;
        } else {
            return source;
        }
    }
}
