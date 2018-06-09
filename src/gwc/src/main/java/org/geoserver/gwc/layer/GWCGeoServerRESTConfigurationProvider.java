/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geowebcache.config.ContextualConfigurationProvider;
import org.geowebcache.config.Info;
import org.geowebcache.config.XMLConfigurationProvider;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.rest.exception.RestException;
import org.springframework.http.HttpStatus;

/**
 * GWC xml configuration {@link XMLConfigurationProvider contributor} so that GWC knows how to
 * marshal and unmarshal {@link GeoServerTileLayer} instances for its REST API.
 *
 * <p>Note this provider is different than {@link GWCGeoServerConfigurationProvider}, which is used
 * to save the configuration objects. In contrast, this one is used only for the GWC REST API, as it
 * doesn't distinguish betwee {@link TileLayer} objects and tile layer configuration objects (as the
 * GWC/GeoServer integration does with {@link GeoServerTileLayer} and {@link GeoServerTileLayerInfo}
 * ).
 */
public class GWCGeoServerRESTConfigurationProvider implements ContextualConfigurationProvider {

    private final Catalog catalog;

    public GWCGeoServerRESTConfigurationProvider(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public XStream getConfiguredXStream(XStream xs) {
        xs.alias("GeoServerLayer", GeoServerTileLayer.class);
        xs.processAnnotations(GeoServerTileLayerInfoImpl.class);
        xs.processAnnotations(StyleParameterFilter.class);
        xs.registerConverter(new RESTConverterHelper());
        xs.addDefaultImplementation(GeoServerTileLayerInfoImpl.class, GeoServerTileLayerInfo.class);

        // Omit the values cached from the backing layer.  They are only needed for the
        // persisted config file.
        xs.omitField(StyleParameterFilter.class, "availableStyles");
        xs.omitField(StyleParameterFilter.class, "defaultStyle");

        // Omit autoCacheStyles as it is no longer needed.
        // It'd be better to read it but not write it, but blocking it from REST is good enough and
        // a lot easier to get XStream to do.
        // TODO Remove this
        // xs.omitField(GeoServerTileLayerInfoImpl.class, "autoCacheStyles");
        return xs;
    }

    /** @author groldan */
    private final class RESTConverterHelper implements Converter {
        @Override
        public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
            return GeoServerTileLayer.class.equals(type);
        }

        @Override
        public GeoServerTileLayer unmarshal(
                HierarchicalStreamReader reader, UnmarshallingContext context) {

            Object current = new GeoServerTileLayerInfoImpl();
            Class<?> type = GeoServerTileLayerInfo.class;
            GeoServerTileLayerInfo info =
                    (GeoServerTileLayerInfo) context.convertAnother(current, type);
            String id = info.getId();
            String name = info.getName();
            if (id != null && id.length() == 0) {
                id = null;
            }
            if (name != null && name.length() == 0) {
                name = null;
            }
            if (name == null) { // name is mandatory
                throw new RestException("Layer name not provided", HttpStatus.BAD_REQUEST);
            }
            LayerInfo layer = null;
            LayerGroupInfo layerGroup = null;
            if (id != null) {
                layer = catalog.getLayer(id);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroup(id);
                    if (layerGroup == null) {
                        throw new RestException(
                                "No GeoServer Layer or LayerGroup exists with id '" + id + "'",
                                HttpStatus.BAD_REQUEST);
                    }
                }
            } else {
                layer = catalog.getLayerByName(name);
                if (layer == null) {
                    layerGroup = catalog.getLayerGroupByName(name);
                    if (layerGroup == null) {
                        throw new RestException(
                                "GeoServer Layer or LayerGroup '" + name + "' not found",
                                HttpStatus.NOT_FOUND);
                    }
                }
            }

            final String actualId = layer != null ? layer.getId() : layerGroup.getId();
            final String actualName =
                    layer != null ? GWC.tileLayerName(layer) : GWC.tileLayerName(layerGroup);

            if (id != null && !name.equals(actualName)) {
                throw new RestException(
                        "Layer with id '"
                                + id
                                + "' found but name does not match: '"
                                + name
                                + "'/'"
                                + actualName
                                + "'",
                        HttpStatus.BAD_REQUEST);
            }

            info.setId(actualId);
            info.setName(actualName);

            GeoServerTileLayer tileLayer;
            final GridSetBroker gridsets = GWC.get().getGridSetBroker();
            if (layer != null) {
                tileLayer = new GeoServerTileLayer(layer, gridsets, info);
            } else {
                tileLayer = new GeoServerTileLayer(layerGroup, gridsets, info);
            }
            return tileLayer;
        }

        @Override
        public void marshal(
                /* GeoServerTileLayer */ Object source,
                HierarchicalStreamWriter writer,
                MarshallingContext context) {
            GeoServerTileLayer tileLayer = (GeoServerTileLayer) source;
            GeoServerTileLayerInfo info = tileLayer.getInfo();
            context.convertAnother(info);
        }
    }

    @Override
    public boolean appliesTo(Context ctxt) {
        return Context.REST == ctxt;
    }

    /**
     * @see ContextualConfigurationProvider#canSave(Info)
     *     <p>Always returns false, as persistence is not relevant for REST.
     * @param i Info to save
     * @return <code>false</code>
     */
    @Override
    public boolean canSave(Info i) {
        return false;
    }
}
