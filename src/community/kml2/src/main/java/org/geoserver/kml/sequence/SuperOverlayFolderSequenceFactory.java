/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.utils.KMLUtils;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
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
 * Creates a sequence of folders mapping the layers in the map content, using either GWC links
 * or superOverlay hierarchies
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SuperOverlayFolderSequenceFactory extends AbstractFolderSequenceFactory {

    public SuperOverlayFolderSequenceFactory(KmlEncodingContext context) {
        super(context);
    }

    @Override
    public Sequence<Feature> newSequence() {
        return new SuperOverlayFolderGenerator();
    }

    public class SuperOverlayFolderGenerator extends AbstractFolderGenerator {

        protected void encodeFolderContents(Layer layer, Folder folder) {
            if ("cached".equals(context.getSuperOverlayMode())
                    && KMLUtils.isRequestGWCCompatible(context.getMapContent(), layer,
                            context.getWms())) {
                encodeAsCachedTiles(layer, folder);
            } else {
                encodeAsSuperOverlay(layer, folder);
            }
        }

        private void encodeAsSuperOverlay(Layer layer, Folder folder) {
            GetMapRequest request = context.getRequest();

            // basics
            NetworkLink nl = folder.createAndAddNetworkLink();
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
            Link link = nl.createAndSetLink();
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

        }

        private void encodeAsCachedTiles(Layer layer, Folder folder) {
            NetworkLink nl = folder.createAndAddNetworkLink();
            // here we rely on the fact that GetMap sets the prefixed name
            String prefixedName = layer.getTitle();
            nl.setName("GWC-" + prefixedName);
            Link link = nl.createAndSetLink();
            String type = (layer instanceof RasterLayer) ? "png" : "kml";
            String url = ResponseUtils.buildURL(context.getRequest().getBaseUrl(),
                    "gwc/service/kml/" + prefixedName + "." + type + ".kml", null, URLType.SERVICE);
            link.setHref(url);
            link.setViewRefreshMode(ViewRefreshMode.NEVER);
        }

    }
}
