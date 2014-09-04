/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.model.IModel;


/**
 * A model that can be applied on top of another model returning a
 * "live collection", that is, a list that is supposed to be modified directly,
 * as opposed thru setting it again with a property setter
 */
@SuppressWarnings("serial")
public abstract class LiveCollectionModel implements IModel {
    IModel wrapped;

    public LiveCollectionModel(IModel wrapped) {
        if (wrapped == null)
            throw new NullPointerException(
                    "Live list model cannot wrap a null model");
        this.wrapped = wrapped;
    }

    public void setObject(Object object) {
        Collection collection = (Collection) wrapped.getObject();
        collection.clear();
        collection.addAll((Collection) object);
    }

    public void detach() {
        wrapped.detach();
    }

    /**
     * Returns a model for live lists
     */
    public static LiveCollectionModel list(IModel wrapped) {
        return new LiveCollectionModel(wrapped) {

            public Object getObject() {
                return new ArrayList((List) wrapped.getObject());
            }
            
        };
    }
    
    /**
     * Returns a model for live sets
     */
    public static LiveCollectionModel set(IModel wrapped) {
        return new LiveCollectionModel(wrapped) {

            public Object getObject() {
                return new HashSet((Set) wrapped.getObject());
            }
            
        };
    }
}
