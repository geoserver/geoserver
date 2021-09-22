/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.model.IModel;

/** Serializable collection implementation backed by a String list wicket IModel. */
class ListModelCollection implements Collection<String>, Serializable {

    private static final long serialVersionUID = 1L;

    IModel<List<String>> markFactoryList;

    public ListModelCollection(IModel<List<String>> markFactoryList) {
        this.markFactoryList = markFactoryList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] arg0) {
        return (T[]) markFactoryList.getObject().toArray();
    }

    @Override
    public Object[] toArray() {
        return markFactoryList.getObject().toArray();
    }

    @Override
    public int size() {
        return markFactoryList.getObject().size();
    }

    @Override
    public boolean retainAll(Collection<?> arg0) {
        List<String> list = markFactoryList.getObject();
        boolean removeAll = markFactoryList.getObject().retainAll(arg0);
        markFactoryList.setObject(list);
        return removeAll;
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        List<String> list = markFactoryList.getObject();
        boolean removeAll = markFactoryList.getObject().removeAll(arg0);
        markFactoryList.setObject(list);
        return removeAll;
    }

    @Override
    public boolean remove(Object arg0) {
        List<String> list = markFactoryList.getObject();
        boolean removeAll = markFactoryList.getObject().remove(arg0);
        markFactoryList.setObject(list);
        return removeAll;
    }

    @Override
    public Iterator<String> iterator() {
        return markFactoryList.getObject().iterator();
    }

    @Override
    public boolean isEmpty() {
        return markFactoryList.getObject().isEmpty();
    }

    @Override
    public boolean containsAll(Collection<?> arg0) {
        return markFactoryList.getObject().containsAll(arg0);
    }

    @Override
    public boolean contains(Object arg0) {
        return markFactoryList.getObject().contains(arg0);
    }

    @Override
    public void clear() {
        List<String> list = markFactoryList.getObject();
        list.clear();
        markFactoryList.setObject(list);
    }

    @Override
    public boolean addAll(Collection<? extends String> arg0) {
        List<String> list = markFactoryList.getObject();
        boolean addAll = list.addAll(arg0);
        markFactoryList.setObject(list);
        return addAll;
    }

    @Override
    public boolean add(String arg0) {
        List<String> list = markFactoryList.getObject();
        boolean addAll = list.add(arg0);
        markFactoryList.setObject(list);
        return addAll;
    }
}
