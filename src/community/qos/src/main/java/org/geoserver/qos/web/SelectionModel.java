/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.io.Serializable;

public abstract class SelectionModel<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T object;
    private SerializableConsumer<T> onSave = o -> {};

    public SelectionModel(T obj) {
        this.object = obj;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public void apply() {
        onSave.accept(object);
    }

    public SerializableConsumer<T> getOnSave() {
        return onSave;
    }

    public void setOnSave(SerializableConsumer<T> onSave) {
        this.onSave = onSave;
    }

    public static class EditionModel<T extends Serializable> extends SelectionModel<T> {
        private static final long serialVersionUID = 1L;

        public EditionModel(T obj) {
            super(obj);
        }
    }

    public static class CreationModel<T extends Serializable> extends SelectionModel<T> {
        private static final long serialVersionUID = 1L;

        public CreationModel(T obj) {
            super(obj);
        }
    }
}
