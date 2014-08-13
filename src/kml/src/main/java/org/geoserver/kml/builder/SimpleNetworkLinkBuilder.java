package org.geoserver.kml.builder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.SystemUtils;
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

import com.vividsolutions.jts.geom.Envelope;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;

/**
 * Builds a KML document that has a network link for each layer, no superoverlays involved
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SimpleNetworkLinkBuilder extends AbstractNetworkLinkBuilder {
    
    static final Logger LOGGER = Logging.getLogger(SimpleNetworkLinkBuilder.class);
    

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
        List<ReferencedEnvelope> layerBounds = new ArrayList<ReferencedEnvelope>(mapContent
                .layers().size());
        ReferencedEnvelope aggregatedBounds = computePerLayerQueryBounds(mapContent, layerBounds,
                null);
        if (aggregatedBounds != null) {
            LookAt la = lookAtFactory.buildLookAt(aggregatedBounds, lookAtOptions, false);
            container.setAbstractView(la);
        }

        final List<MapLayerInfo> layers = request.getLayers();
        final List<Style> styles = request.getStyles();
        for (int i = 0; i < layers.size(); i++) {
            MapLayerInfo layerInfo = layers.get(i);
            NetworkLink nl = container.createAndAddNetworkLink();
            nl.setName(layerInfo.getName());
            nl.setVisibility(true);
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
            String href = WMSRequests.getGetMapUrl(requestCopy, layers.get(i).getName(), i, style,
                    null, null);
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
        }
    }

   
    
    
}
