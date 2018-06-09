/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
 * A model that can be applied on top of another model returning a "live collection", that is, a
 * list that is supposed to be modified directly, as opposed thru setting it again with a property
 * setter
 */
public abstract class LiveCollectionModel<S, T extends Collection<S>> implements IModel<T> {
    private static final long serialVersionUID = 3505518156788420409L;

    IModel<? extends Collection<S>> wrapped;

    public LiveCollectionModel(IModel<? extends Collection<S>> wrapped) {
        if (wrapped == null)
            throw new NullPointerException("Live list model cannot wrap a null model");
        this.wrapped = wrapped;
    }

    public void setObject(T object) {
        Collection<S> collection = wrapped.getObject();
        collection.clear();
        if (object != null) {
            collection.addAll(object);
        }
    }

    public void detach() {
        wrapped.detach();
    }

    /** Returns a model for live lists */
    public static <S> LiveCollectionModel<S, List<S>> list(
            IModel<? extends Collection<S>> wrapped) {
        return new LiveCollectionModel<S, List<S>>(wrapped) {

            private static final long serialVersionUID = 3182237972594668864L;

            public List<S> getObject() {
                return new ArrayList<S>(wrapped.getObject());
            }
        };
    }

    /** Returns a model for live sets */
    public static <S> LiveCollectionModel<S, Set<S>> set(IModel<? extends Collection<S>> wrapped) {
        return new LiveCollectionModel<S, Set<S>>(wrapped) {

            private static final long serialVersionUID = 7638792616781214296L;

            public Set<S> getObject() {
                return new HashSet<S>(wrapped.getObject());
            }
        };
    }
}
