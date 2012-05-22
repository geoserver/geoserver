package org.geoserver.kml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSRequests;
import org.geotools.styling.Style;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Encodes a KML document contianing a network link.
 * <p>
 * This transformer transforms a {@link GetMapRequest} object.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class KMLNetworkLinkTransformer extends KMLMapTransformer {

    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");
    
    /**
     * flag controlling whether the network link should be a super overlay.
     */
    boolean encodeAsRegion = false;
    
    /**
     * flag controlling whether the network link should be a direct GWC one when possible
     */
    boolean cachedMode = false;

    /**
     * The map context
     */
    private final WMSMapContext mapContext;
    
    private WMS wms;
 
    public KMLNetworkLinkTransformer(WMS wms, WMSMapContext mapContext){
        super(wms, mapContext, null);
        
        this.wms = wms;
        this.mapContext = mapContext;
    }
    
    public void setCachedMode(boolean cachedMode) {
        this.cachedMode = cachedMode;
    }

    public Translator createTranslator(ContentHandler handler) {
        return new KMLNetworkLinkTranslator( handler );
    }
    
    public void setEncodeAsRegion(boolean encodeAsRegion) {
        this.encodeAsRegion = encodeAsRegion;
    }
    
    class KMLNetworkLinkTranslator extends TranslatorSupport {

        public KMLNetworkLinkTranslator(ContentHandler contentHandler) {
            super(contentHandler, null,null);
        }
        
        public void encode(Object o) throws IllegalArgumentException {
            GetMapRequest request;
            if (o instanceof GetMapRequest) {
                request = (GetMapRequest) o;
            } else {
                request = ((WMSMapContext) o).getRequest();
            }
            
            // restore target mime type for the network links
            if (NetworkLinkMapOutputFormat.KML_MIME_TYPE.equals(request.getFormat())) {
                request.setFormat(KMLMapOutputFormat.MIME_TYPE);
            } else {
                request.setFormat(KMZMapOutputFormat.MIME_TYPE);
            }
            
            if (isStandAlone())
            {
                start( "kml" );
            }
            start( "Folder" );
        
            String kmltitle = (String) mapContext.getRequest().getFormatOptions().get("kmltitle");
            element("name", (kmltitle != null ? kmltitle : ""));
            
            if ( encodeAsRegion ) {
                encodeAsSuperOverlay( request );
            }
            else {
                encodeAsOverlay( request );
            }
            
            //look at
            encodeLookAt( request );
            
            end( "Folder" );
            
            if (isStandAlone())
            {
                end( "kml" );
            }
        }
        
        protected void encodeAsSuperOverlay(GetMapRequest request) {
            List<MapLayerInfo> layers = request.getLayers();
            List<Style> styles = request.getStyles();
            for ( int i = 0; i < layers.size(); i++ ) {
                if("cached".equals(KMLUtils.getSuperoverlayMode(request, wms)) &&
                        KMLUtils.isRequestGWCCompatible(request, i, wms)) {
                    encodeGWCLink(request, layers.get(i));
                } else {
                    encodeLayerSuperOverlay(request, layers, styles, i);
                }
            }
        }
        
        public void encodeGWCLink(GetMapRequest request, MapLayerInfo mapLayer) {
            start("NetworkLink");
            String prefixedName = mapLayer.getResource().getPrefixedName();
            element("name", "GWC-" + prefixedName);
            
            start("Link");

            String type = mapLayer.getType() == MapLayerInfo.TYPE_RASTER ? "png" : "kml";
            String url = ResponseUtils.buildURL(request.getBaseUrl(), "gwc/service/kml/" + 
                    prefixedName + "." + type + ".kml", null, URLType.SERVICE);
            element("href", url);
            element("viewRefreshMode", "never");

            end("Link");

            end("NetworkLink");
        }

        private void encodeLayerSuperOverlay(GetMapRequest request, List<MapLayerInfo> layers,
                List<Style> styles, int i) {
            start("NetworkLink");
            element( "name", layers.get(i).getName() );
            element( "open", "1" );
            element( "visibility", "1" );
          
            //region
            start( "Region" );
            
            Envelope bbox = request.getBbox();
            start( "LatLonAltBox" );
            element( "north", ""+bbox.getMaxY() );
            element( "south", ""+bbox.getMinY() );
            element( "east", ""+bbox.getMaxX() );
            element( "west", ""+bbox.getMinX() );
            end( "LatLonAltBox");
            
            start( "Lod" );
            element( "minLodPixels", "128" );
            element( "maxLodPixels", "-1" );
            end( "Lod" );
            
            end( "Region" );
            
            //link
            start("Link" );
  
            String style = i < styles.size()? styles.get(i).getName() : null;
            String href = WMSRequests.getGetMapUrl(request, layers.get(i).getName(), i, style, null, null);
            try {
                // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                // 6 doesn't like URL encoded parameters. See GEOS-4483
                href = URLDecoder.decode(href, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }            start( "href" );
            cdata( href );
            end( "href" );
            
//                element( "viewRefreshMode", "onRegion" );
            end( "Link" );
            
            end( "NetworkLink");
        }
        
        protected void encodeAsOverlay( GetMapRequest request ) {
            List<MapLayerInfo> layers = request.getLayers();
            List<Style> styles = request.getStyles();
            for ( int i = 0; i < layers.size(); i++ ) {
                start("NetworkLink");
                element( "name", layers.get(i).getName() );
                element( "open", "1" );
                element( "visibility", "1" );
                
                start( "Url" );
                
                //set bbox to null so its not included in the request, google 
                // earth will append it for us
                request.setBbox(null);
                
                String style = i < styles.size()? styles.get(i).getName() : null;
                String href = WMSRequests.getGetMapUrl(request, layers.get(i).getName(), i, style, null, null);
                try {
                    // WMSRequests.getGetMapUrl returns a URL encoded query string, but GoogleEarth
                    // 6 doesn't like URL encoded parameters. See GEOS-4483
                    href = URLDecoder.decode(href, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }                
                start( "href" );
                cdata( href );
                end( "href" );
                
                element( "viewRefreshMode", "onStop" );
                element( "viewRefreshTime", "1" );
                end( "Url" );
                
                end( "NetworkLink" );
            }
        }
        
        private void encodeLookAt(GetMapRequest request){
            
            Envelope e = new Envelope();
            e.setToNull();
            
            for ( int i = 0; i < request.getLayers().size(); i++ ) {
                MapLayerInfo layer = request.getLayers().get(i);
                
                Envelope b = null;
                try {
                    b = request.getLayers().get(i).getLatLongBoundingBox();
                } catch (IOException e1) {
                    LOGGER.warning( "Unable to calculate bounds for " + layer.getName() );
                    continue;
                } 
                if ( e.isNull() ) {
                    e.init( b );
                }
                else {
                    e.expandToInclude( b );
                }
            }
            
            if ( e.isNull() ) {
                return;
            }
            
            double lon1 = e.getMinX();
            double lat1 = e.getMinY();
            double lon2 = e.getMaxX();
            double lat2 = e.getMaxY();
            
            double R_EARTH = 6.371 * 1000000; // meters
            double VIEWER_WIDTH = 22 * Math.PI / 180; // The field of view of the google maps camera, in radians
            double[] p1 = getRect(lon1, lat1, R_EARTH);
            double[] p2 = getRect(lon2, lat2, R_EARTH);
            double[] midpoint = new double[]{
              (p1[0] + p2[0])/2,
                (p1[1] + p2[1])/2,
                (p1[2] + p2[2])/2
            };
            
            midpoint = getGeographic(midpoint[0], midpoint[1], midpoint[2]);
            
            double distance = distance(p1, p2);
            
            double height = distance/ (2 * Math.tan(VIEWER_WIDTH));
            
            LOGGER.fine("lat1: " + lat1 + "; lon1: " + lon1);
            LOGGER.fine("lat2: " + lat2 + "; lon2: " + lon2);
            LOGGER.fine("latmid: " + midpoint[1] + "; lonmid: " + midpoint[0]);
            
            
            start( "LookAt" );
            element( "longitude", ""+midpoint[0] );
            element( "latitude", "" +midpoint[1] );
            element( "altitude", "0" );
            element( "range", ""+ distance );
            element( "tilt", "0" );
            element( "heading", "0" );
            element( "altitudeMode", "clampToGround" );
            end( "LookAt" );
          }
          
          private double[] getRect(double lat, double lon, double radius){
            double theta = (90 - lat) * Math.PI/180;
            double phi   = (90 - lon) * Math.PI/180;
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            return new double[]{x, y, z};
          }
          
          private double[] getGeographic(double x, double y, double z){
            double theta, phi, radius;
            radius = distance(new double[]{x, y, z}, new double[]{0,0,0});
            theta = Math.atan2(Math.sqrt(x * x + y * y) , z);
            phi = Math.atan2(y , x);
            
            double lat = 90 - (theta * 180 / Math.PI);
            double lon = 90 - (phi * 180 / Math.PI);
            
            return new double[]{(lon > 180 ? lon - 360 : lon), lat, radius};
          }
          
          private double distance(double[] p1, double[] p2){
            double dx = p1[0] - p2[0];
            double dy = p1[1] - p2[1];
            double dz = p1[2] - p2[2];
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
          }
    }
}
