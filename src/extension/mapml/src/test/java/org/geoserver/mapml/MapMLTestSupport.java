/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geowebcache.grid.GridSubsetFactory.createGridSubSet;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.wms.WMSTestSupport;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.TextMime;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** MapML test support class */
public class MapMLTestSupport extends WMSTestSupport {

    /**
     * Get the response as a MapML object
     *
     * @param path the path to the resource
     * @return the MapML object
     * @throws Exception if an error occurs
     */
    protected Mapml getAsMapML(final String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        return mapml(response);
    }

    /**
     * Convert the response to a MapML object
     *
     * @param response the response
     * @return the MapML object
     * @throws JAXBException if an error occurs
     * @throws UnsupportedEncodingException if an error occurs
     */
    protected Mapml mapml(MockHttpServletResponse response) throws JAXBException, UnsupportedEncodingException {
        MapMLEncoder encoder = new MapMLEncoder();
        StringReader reader = new StringReader(response.getContentAsString());
        Mapml mapml = null;
        try {
            mapml = encoder.decode(reader);
        } catch (DataBindingException e) {
            fail("MapML response is not valid XML");
        }
        return mapml;
    }

    protected void enableTileCaching(QName layerName, Catalog catalog) {
        enableTileCaching(layerName, getCatalog(), GWC.get());
    }

    protected void enableTileCaching(QName layerName, Catalog catalog, GWC gwc) {
        GWCConfig defaults = GWCConfig.getOldDefaults();
        // it seems just the fact of retrieving the bean causes the
        // GridSets to be added to the gwc GridSetBroker, but if you don't do
        // this, they are not added automatically
        MapMLGridsets mgs = applicationContext.getBean(MapMLGridsets.class);
        GridSubset wgs84gridset = createGridSubSet(mgs.getGridSet("WGS84").get());
        GridSubset osmtilegridset = createGridSubSet(mgs.getGridSet("OSMTILE").get());
        LayerInfo layerInfo = catalog.getLayerByName(layerName.getLocalPart());
        GeoServerTileLayer layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gwc.getGridSetBroker());
        layerInfoTileLayer.addGridSubset(wgs84gridset);
        layerInfoTileLayer.addGridSubset(osmtilegridset);
        layerInfoTileLayer.getInfo().getMimeFormats().add(TextMime.txtMapml.getMimeType());
        gwc.save(layerInfoTileLayer);
    }

    protected void disableTileCaching(QName layerName, Catalog catalog) {
        GWC gwc = applicationContext.getBean(GWC.class);
        GWCConfig defaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = catalog.getLayerByName(layerName.getLocalPart());
        GeoServerTileLayer layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gwc.getGridSetBroker());
        layerInfoTileLayer.removeGridSubset("OSMTILE");
        layerInfoTileLayer.removeGridSubset("WGS84");
        gwc.save(layerInfoTileLayer);
    }

    @Override
    protected MockHttpServletRequest createRequest(String path) {
        return super.createRequest(path);
    }

    @Override
    protected MockHttpServletRequest createRequest(String path, Map kvp) {
        return super.createRequest(path, kvp);
    }

    public class MapMLWMSRequest {
        private String name;
        private Map kvp;
        private Locale locale;
        private String bbox;
        private String srs;
        private String styles;
        private String cql;
        private String width;
        private String height;
        private String format;
        private boolean feature;
        private boolean createFeatureLinks;
        private boolean multiExtent;
        private boolean tile;

        public String getName() {
            return name;
        }

        public Map getKvp() {
            return kvp;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getBbox() {
            return bbox;
        }

        public String getSrs() {
            return srs;
        }

        public String getStyles() {
            return styles;
        }

        public String getCql() {
            return cql;
        }

        public String getWidth() {
            return width;
        }

        public String getHeight() {
            return height;
        }

        public String getFormat() {
            return format;
        }

        public boolean isFeature() {
            return feature;
        }

        public boolean isCreateFeatureLinks() {
            return createFeatureLinks;
        }

        public boolean isMultiExtent() {
            return multiExtent;
        }

        public boolean isTile() {
            return tile;
        }

        public MapMLWMSRequest cql(String cql) {
            this.cql = cql;
            return this;
        }

        public MapMLWMSRequest name(String name) {
            this.name = name;
            return this;
        }

        public MapMLWMSRequest kvp(Map kvp) {
            this.kvp = kvp;
            return this;
        }

        public MapMLWMSRequest locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public MapMLWMSRequest bbox(String bbox) {
            this.bbox = bbox;
            return this;
        }

        public MapMLWMSRequest srs(String srs) {
            this.srs = srs;
            return this;
        }

        public MapMLWMSRequest styles(String styles) {
            this.styles = styles;
            return this;
        }

        public MapMLWMSRequest width(String width) {
            this.width = width;
            return this;
        }

        public MapMLWMSRequest height(String height) {
            this.height = height;
            return this;
        }

        public MapMLWMSRequest format(String format) {
            this.format = format;
            return this;
        }

        public MapMLWMSRequest feature(boolean feature) {
            this.feature = feature;
            return this;
        }

        public MapMLWMSRequest createFeatureLinks(boolean createFeatureLinks) {
            this.createFeatureLinks = createFeatureLinks;
            return this;
        }

        public MapMLWMSRequest tile(boolean tile) {
            this.tile = tile;
            return this;
        }

        public MapMLWMSRequest multiExtent(boolean multiExtent) {
            this.multiExtent = multiExtent;
            return this;
        }

        /** Get a MapML request */
        protected MockHttpServletRequest toHttpRequest() throws Exception {
            String path = null;
            cql(getCql() != null ? getCql() : "");
            MockHttpServletRequest httpRequest = null;
            String formatOptions = isFeature()
                    ? MapMLConstants.MAPML_FEATURE_FO + ":true;"
                    : getFormat() != null ? MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION + ":image/png;" : "";

            if (isCreateFeatureLinks()) {
                formatOptions += MapMLConstants.MAPML_CREATE_FEATURE_LINKS + ":true;";
            }
            if (isMultiExtent()) {
                formatOptions += MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT + ":true;";
            }
            if (isTile()) {
                formatOptions += MapMLConstants.MAPML_USE_TILES_REP + ":true;";
            }
            if (getKvp() != null) {
                path = "wms";
                httpRequest = createRequest(path, getKvp());
            } else {
                path = "wms?LAYERS="
                        + getName()
                        + "&STYLES="
                        + (getStyles() != null ? getStyles() : "")
                        + "&FORMAT="
                        + (getFormat() != null ? getFormat() : MapMLConstants.MAPML_MIME_TYPE)
                        + "&SERVICE=WMS&VERSION=1.3.0"
                        + "&REQUEST=GetMap"
                        + "&SRS="
                        + getSrs()
                        + "&BBOX="
                        + (getBbox() != null ? getBbox() : "0,0,1,1")
                        + "&WIDTH="
                        + (getWidth() != null ? getWidth() : "150")
                        + "&HEIGHT="
                        + (getHeight() != null ? getHeight() : "150")
                        + "&cql_filter="
                        + getCql()
                        + "&format_options="
                        + formatOptions;
                httpRequest = createRequest(path);
            }

            return httpRequest;
        }

        /** Get the WMS response as a MapML object */
        protected Mapml getAsMapML() throws Exception {
            MockHttpServletRequest request = toHttpRequest();
            MockHttpServletResponse response = dispatch(request);
            return mapml(response);
        }

        /** Get the WMS response as a string */
        protected String getAsString() throws Exception {
            MockHttpServletRequest request = toHttpRequest();
            MockHttpServletResponse response = dispatch(request);
            return response.getContentAsString();
        }
    }
}
