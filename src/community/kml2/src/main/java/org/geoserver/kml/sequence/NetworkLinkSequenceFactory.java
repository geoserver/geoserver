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
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.map.Layer;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;

/**
 * A sequence that generates one network link per layer
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class NetworkLinkSequenceFactory implements SequenceFactory<Feature> {

    protected KmlEncodingContext context;

    protected List<KmlDecorator> decorators;

    public NetworkLinkSequenceFactory(KmlEncodingContext context) {
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

                // setup the folder and let it be decorated
                NetworkLink nl = new NetworkLink();
                nl.setName(layer.getTitle());
                nl.setName(layer.getTitle());
                nl.setVisibility(true);
                nl.setOpen(true);

                // set bbox to null so its not included in the request, google
                // earth will append it for us
                GetMapRequest request = context.getRequest();
                request.setBbox(null);
                
                // adjust the format of backlinks that will be generated from this request
                if (KMLMapOutputFormat.NL_KML_MIME_TYPE.equals(request.getFormat())) {
                    request.setFormat(KMLMapOutputFormat.MIME_TYPE);
                } else {
                    request.setFormat(KMZMapOutputFormat.MIME_TYPE);
                }

                String href = WMSRequests.getGetMapUrl(request, layer,
                        KMLUtils.getLayerIndex(mapContent, layer), null, null);
                try {
                    // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                    // 6 doesn't like URL encoded parameters. See GEOS-4483
                    href = URLDecoder.decode(href, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                Link url = nl.createAndSetUrl();
                url.setHref(href);
                url.setViewRefreshMode(ViewRefreshMode.ON_STOP);
                url.setViewRefreshTime(1);

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

    }

}
