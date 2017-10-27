/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.XStreamCatalogListConverter;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class TransformationsJsonConverter extends XStreamCatalogListConverter.JSONXStreamListConverter {

    @Override
    protected void aliasCollection(Object data, XStream xstream, Class clazz, RestListWrapper wrapper) {
        xstream.alias("transforms", Collection.class, data.getClass());
    }

    @Override
    protected String getItemName(XStreamPersister xp, Class clazz) {
        return "transform";
    }
}