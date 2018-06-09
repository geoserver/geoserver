/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import com.thoughtworks.xstream.XStream;
import org.geowebcache.config.ContextualConfigurationProvider;
import org.geowebcache.config.Info;
import org.geowebcache.config.XMLConfigurationProvider;

/**
 * GWC xml configuration {@link XMLConfigurationProvider} contributor so that GWC knows how to
 * (x)stream instances of {@link GeoServerTileLayerInfo} for the configuration storage subsystem.
 *
 * <p>Note this provider is to store the tile layer configuration representations, which is
 * different from the {@link GWCGeoServerRESTConfigurationProvider REST provider}, which helps in
 * marshaling and unmarshaling {@link GeoServerTileLayer} objects for the GWC REST API.
 */
public class GWCGeoServerConfigurationProvider implements ContextualConfigurationProvider {

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerTileLayer", GeoServerTileLayerInfo.class);
        xs.processAnnotations(GeoServerTileLayerInfoImpl.class);
        xs.processAnnotations(StyleParameterFilter.class);
        xs.addDefaultImplementation(GeoServerTileLayerInfoImpl.class, GeoServerTileLayerInfo.class);
        return xs;
    }

    @Override
    public boolean appliesTo(Context ctxt) {
        return Context.PERSIST == ctxt;
    }

    /**
     * @see ContextualConfigurationProvider#canSave(Info)
     *     <p>Always returns false, as persistence is done by GeoServer's own Configuration, not
     *     XMLConfiguration
     * @param i Info to save
     * @return <code>false</code>
     */
    @Override
    public boolean canSave(Info i) {
        return false;
    }
}
