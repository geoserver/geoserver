/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model.impl;

import java.util.UUID;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.service.GlobalModelService;
import org.geoserver.web.GeoServerApplication;

public class GlobalModel<T> implements IModel<T> {

    private static final long serialVersionUID = -933926008434334649L;

    private UUID key = UUID.randomUUID();

    public GlobalModel() {}

    public GlobalModel(T value) {
        setObject(value);
    }

    @Override
    public void detach() {}

    @SuppressWarnings("unchecked")
    @Override
    public T getObject() {
        GlobalModelService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(GlobalModelService.class);
        return (T) service.get(key);
    }

    @Override
    public void setObject(T value) {
        GlobalModelService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(GlobalModelService.class);
        service.put(key, value);
    }

    public void cleanUp() {
        GlobalModelService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(GlobalModelService.class);
        service.delete(key);
    }

    public UUID getKey() {
        return key;
    }
}
