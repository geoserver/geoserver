/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.geoserver.kml.KMLMapOutputFormat;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.NetworkLinkMapOutputFormat;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.utils.KMLUtils;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.map.Layer;
import org.geotools.map.RasterLayer;

import com.vividsolutions.jts.geom.Envelope;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.LatLonAltBox;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.Lod;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Region;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;

/**
 * A sequence that generates one network link per layer
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SuperOverlaySequenceFactory implements SequenceFactory<Feature> {

    protected KmlEncodingContext context;

    protected List<KmlDecorator> decorators;

    public SuperOverlaySequenceFactory(KmlEncodingContext context) {
        this.context = context;
        this.decorators = context.getDecoratorsForClass(NetworkLink.class);
    }

    @Override
    public Sequence<Feature> newSequence() {
        return new NetworkLinkGenerator();
    }

    public class NetworkLinkGenerator implements Sequence<Feature> {
        protected int i = 0;

        protected int size;

        public NetworkLinkGenerator() {
            this.size = context.getMapContent().layers().size();
        }

        @Override
        public Feature next() {
            while (i < size) {
                WMSMapContent mapContent = context.getMapContent();
                List<Layer> layers = mapContent.layers();
                Layer layer = layers.get(i++);
                context.setCurrentLayer(layer);

                NetworkLink nl;
                if ("cached".equals(context.getSuperOverlayMode())
                        && KMLUtils.isRequestGWCCompatible(context.getMapContent(), layer,
                                context.getWms())) {
                    nl = encodeAsCachedTiles(layer);
                } else {
                    nl = encodeAsSuperOverlay(layer);
                }

                // have the link be decorated
                for (KmlDecorator decorator : decorators) {
                    nl = (NetworkLink) decorator.decorate(nl, context);
                    if (nl == null) {
                        continue;
                    }
                }

                return nl;
            }
            return null;
        }
        
        protected void encodeFolderContents(Layer layer) {
            
        }

        private NetworkLink encodeAsSuperOverlay(Layer layer) {
            GetMapRequest request = context.getRequest();

            // basics
            NetworkLink nl = new NetworkLink();
            nl.setName(layer.getTitle());
            nl.setOpen(true);
            nl.setVisibility(true);

            // region
            Region region = nl.createAndSetRegion();
            LatLonAltBox box = region.createAndSetLatLonAltBox();
            Envelope requestBox = request.getBbox();
            box.setNorth(requestBox.getMaxY());
            box.setSouth(requestBox.getMinY());
            box.setEast(requestBox.getMaxX());
            box.setWest(requestBox.getMinX());
            Lod lod = region.createAndSetLod();
            lod.setMinLodPixels(128);
            lod.setMaxLodPixels(-1);
            
            // link
            Link link = nl.createAndSetUrl();
            int index = KMLUtils.getLayerIndex(context.getMapContent(), layer);
            try {
                // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                // 6 doesn't like URL encoded parameters. See GEOS-4483
                String href = WMSRequests.getGetMapUrl(request, layer, index, null, null);
                href = URLDecoder.decode(href, "UTF-8");
                link.setHref(href);
            } catch (UnsupportedEncodingException e) {
                throw new ServiceException(e);
            }

            return nl;
        }

        private NetworkLink encodeAsCachedTiles(Layer layer) {
            NetworkLink nl = new NetworkLink();
            // here we rely on the fact that GetMap sets the prefixed name
            String prefixedName = layer.getTitle();
            nl.setName("GWC-" + prefixedName);
            Link link = nl.createAndSetLink();
            String type = (layer instanceof RasterLayer) ? "png" : "kml";
            String url = ResponseUtils.buildURL(context.getRequest().getBaseUrl(),
                    "gwc/service/kml/" + prefixedName + "." + type + ".kml", null, URLType.SERVICE);
            link.setHref(url);
            link.setViewRefreshMode(ViewRefreshMode.NEVER);
            
            return nl;
        }

    }

}
