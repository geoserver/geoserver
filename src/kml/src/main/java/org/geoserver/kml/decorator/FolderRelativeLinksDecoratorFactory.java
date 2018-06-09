/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import java.io.IOException;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSInfo;

/**
 * Encodes previous/next network links when paging is used TODO: move this in GeoSearch, as it
 * references its REST services
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FolderRelativeLinksDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator makes sense only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        // we decorate only the feature collection folders
        if (!(featureClass.equals(Folder.class))) {
            return null;
        }

        // see if we have to encode relative links
        GetMapRequest request = context.getRequest();
        String relLinks = (String) request.getFormatOptions().get("relLinks");
        // Add prev/next links if requested
        if (request.getMaxFeatures() != null
                && relLinks != null
                && relLinks.equalsIgnoreCase("true")) {
            return new FolderRelativeLinksDecorator();
        } else {
            return null;
        }
    }

    static class FolderRelativeLinksDecorator extends AbstractGeoSearchDecorator {

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            // if not a layer link, move on
            if (context.getCurrentLayer() == null
                    || context.getCurrentFeatureCollection() == null) {
                return feature;
            }

            Folder folder = (Folder) feature;

            String linkbase = "";
            try {
                linkbase = getFeatureTypeURL(context);
                linkbase += ".kml";
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            GetMapRequest request = context.getRequest();
            int maxFeatures = request.getMaxFeatures();
            int startIndex =
                    (request.getStartIndex() == null) ? 0 : request.getStartIndex().intValue();
            int prevStart = startIndex - maxFeatures;
            int nextStart = startIndex + maxFeatures;

            // Previous page, if any
            if (prevStart >= 0) {
                encodeSequentialNetworkLink(
                        folder, linkbase, prevStart, maxFeatures, "prev", "Previous page");
            }

            // Next page, if potentially any
            if (context.getCurrentFeatureCollection().size() >= maxFeatures) {
                encodeSequentialNetworkLink(
                        folder, linkbase, nextStart, maxFeatures, "next", "Next page");
            }

            return folder;
        }

        private void encodeSequentialNetworkLink(
                Folder folder,
                String linkbase,
                int start,
                int maxFeatures,
                String id,
                String readableName) {
            NetworkLink nl = folder.createAndAddNetworkLink();
            Link link = nl.createAndSetLink();
            link.setHref(linkbase + "?startindex=" + start + "&maxfeatures=" + maxFeatures);
            nl.setDescription(readableName);
            nl.setId(id);
        }
    }
}
