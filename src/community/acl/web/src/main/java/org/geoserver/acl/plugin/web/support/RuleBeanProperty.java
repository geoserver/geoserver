/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.RuleBeanProperty)
 */
package org.geoserver.acl.plugin.web.support;

import java.util.Comparator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;

/** Makes columns unsortable and display "*" instead of empty when null */
@SuppressWarnings("serial")
public class RuleBeanProperty<T> extends BeanProperty<T> {

    public RuleBeanProperty(String key, String propertyPath) {
        super(key, propertyPath);
    }

    public static <P> RuleBeanProperty<P> of(String key) {
        return of(key, key);
    }

    public static <P> RuleBeanProperty<P> of(String key, String propertyPath) {
        return new RuleBeanProperty<>(key, propertyPath);
    }

    @Override
    public Comparator<T> getComparator() { // unsortable
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IModel getModel(IModel<T> itemModel) { // replace null by *
        return new PropertyModel<>(itemModel, getPropertyPath()) {
            @Override
            public Object getObject() {
                Object o = super.getObject();
                return o == null ? "*" : o;
            }
        };
    }
}
