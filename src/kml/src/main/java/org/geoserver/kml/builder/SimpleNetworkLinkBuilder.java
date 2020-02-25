/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.builder;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.RefreshMode;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.LookAtDecoratorFactory;
import org.geoserver.kml.utils.LookAtOptions;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSRequests;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

/**
 * Builds a KML document that has a network link for each layer, no superoverlays involved
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SimpleNetworkLinkBuilder extends AbstractNetworkLinkBuilder {

    static final Logger LOGGER = Logging.getLogger(SimpleNetworkLinkBuilder.class);

    static final String REFRESH_KEY = "kmlrefresh";

    static final String VISIBLE_KEY = "kmlvisible";

    public SimpleNetworkLinkBuilder(KmlEncodingContext context) {
        super(context);
    }

    @Override
    void encodeDocumentContents(Document container) {
        WMSMapContent mapContent = context.getMapContent();
        GetMapRequest request = context.getRequest();
        Map formatOptions = request.getFormatOptions();
        LookAtDecoratorFactory lookAtFactory = new LookAtDecoratorFactory();
        LookAtOptions lookAtOptions = new LookAtOptions(formatOptions);

        // compute the layer bounds and the total bounds
        List<ReferencedEnvelope> layerBounds =
                new ArrayList<ReferencedEnvelope>(mapContent.layers().size());
        ReferencedEnvelope aggregatedBounds =
                computePerLayerQueryBounds(mapContent, layerBounds, null);
        if (aggregatedBounds != null) {
            LookAt la = lookAtFactory.buildLookAt(aggregatedBounds, lookAtOptions, false);
            container.setAbstractView(la);
        }

        final List<MapLayerInfo> layers = request.getLayers();
        final List<Style> styles = request.getStyles();
        for (int i = 0; i < layers.size(); i++) {
            MapLayerInfo layerInfo = layers.get(i);
            NetworkLink nl = container.createAndAddNetworkLink();
            nl.setName(layerInfo.getLabel());
            if (layerInfo.getDescription() != null && layerInfo.getDescription().length() > 0) {
                nl.setDescription(layerInfo.getDescription());
            }

            // Allow for all layers to be disabled by default.  This can be advantageous with
            // multiple large data-sets.
            if (formatOptions.get(VISIBLE_KEY) != null) {
                boolean visible = Boolean.parseBoolean(formatOptions.get(VISIBLE_KEY).toString());
                nl.setVisibility(visible);
            } else {
                nl.setVisibility(true);
            }
            nl.setOpen(true);

            // look at for this layer
            Envelope requestBox = context.getRequestBoxWGS84();

            if (requestBox != null) {
                LookAt la = lookAtFactory.buildLookAt(requestBox, lookAtOptions, false);
                nl.setAbstractView(la);
            }

            // set bbox to null so its not included in the request, google
            // earth will append it for us
            GetMapRequest requestCopy = (GetMapRequest) request.clone();
            requestCopy.setBbox(null);

            String style = i < styles.size() ? styles.get(i).getName() : null;
            String href =
                    WMSRequests.getGetMapUrl(
                            requestCopy, layers.get(i).getName(), i, style, null, null);
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
            url.setViewBoundScale(1);

            // Attempt to parse a value from kmlrefresh format_options parameter.
            // It can be either a set interval in seconds or "expires".
            // "expires" uses the HTTP max-age header and falls-back to expires header
            // to determine the time when a refresh should occur.
            if (formatOptions.get(REFRESH_KEY) != null) {
                String refreshValue = formatOptions.get(REFRESH_KEY).toString();
                if (refreshValue.equalsIgnoreCase("expires")) {
                    url.setRefreshMode(RefreshMode.ON_EXPIRE);
                } else {
                    int interval = Integer.parseInt(refreshValue);
                    url.setRefreshInterval(interval);
                    url.setRefreshMode(RefreshMode.ON_INTERVAL);
                }
            }
        }
    }
}
