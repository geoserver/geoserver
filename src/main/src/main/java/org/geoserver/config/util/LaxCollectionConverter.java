/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Collection;

/**
 * Custom collection converter.
 *
 * <p>Used because because the basic only handles a handlful of collection implementations. And
 * things like hibernate have there own implementation which leads to problems.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LaxCollectionConverter extends CollectionConverter {

    public LaxCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return Collection.class.isAssignableFrom(type);
    }
}
