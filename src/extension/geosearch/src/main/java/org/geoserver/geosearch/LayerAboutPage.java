package org.geoserver.geosearch;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.vividsolutions.jts.geom.Envelope;

import freemarker.template.SimpleHash;

public class LayerAboutPage extends GeoServerProxyAwareRestlet {
	private static final Logger 
        LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.geosearch");	
	
    private final DataFormat format =
        new FreemarkerFormat("layerpage.ftl", getClass(), MediaType.TEXT_HTML);

    private Catalog catalog;

    public void setCatalog(Catalog cat){
        catalog = cat;
    }

    public Catalog getCatalog(){
        return catalog;
    }

    public void handle(Request request, Response response){
        if (request.getMethod().equals(Method.GET)) doGet(request, response);
        else response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    public void doGet(Request request, Response response){
        String namespace = (String)request.getAttributes().get("namespace");
        String layer = (String)request.getAttributes().get("layer");

        response.setEntity(format.toRepresentation(getContext(namespace, layer, request)));
    }
    
    SimpleHash getContext(String namespace, String layer, Request request){
    	FeatureTypeInfo info = lookupType(namespace, layer);
         
        if (!(Boolean)info.getMetadata().get("indexingEnabled")) {
            throw new RestletException(
                    "Layer indexing disabled",
                    Status.CLIENT_ERROR_FORBIDDEN
                    );
        }
    	
    	SimpleHash map = new SimpleHash();
    	
    	//basic
    	map.put("title", info.getTitle());
    	map.put("abstract", info.getAbstract());
    	
    	//Metadata
    	map.put("keywords", info.getKeywords());
		map.put("declaredCRS", info.getCRS());	    	
		map.put("metadataLinks", info.getMetadataLinks());
		try {
            Object o = info.getNativeCRS();
            if (o != null) {
                map.put("nativeCRS", info.getNativeCRS());
            } else {
                map.put("nativeCRS", "No native CRS configured for layer");
            }
		} catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error trying to get nativeCRS from " 
                    + info.getName() 
                    + "FeatureTypeInfo",
                    e
                    );
		}

		String baseUrl = RESTUtils.getBaseURL(request);
		map.put("base", baseUrl);    			
		
		//general parameters for data requests
    	map.put("name", info.getPrefixedName());
    	map.put("srs", info.getSRS());
    	
    	ReferencedEnvelope bbox = getBBOX(info);
    	
    	String bboxString = bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + ","
        + bbox.getMaxY();    	
    	map.put("bbox", bboxString);
    	
    	map.put("tilesOrigin", bbox.getMinX()+","+bbox.getMinY());    	
    	
    	int[] imageBox = getMapWidthHeight(bbox);
        map.put("width", imageBox[0]);
        map.put("height", imageBox[1]);
    	
        map.put("maxResolution", getMaxResolution(bbox));
        
    	try{        	
        	map.put("boundingBox", info.boundingBox());
        	map.put("lonLatBoundingBox", info.getLatLonBoundingBox());
    	} catch(Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to access bounding box or lonLatBoundingBox for " + info.getName() + "FeatureTypeInfo", e);
    	}	
    	
    	//Fields of Access
    	map.put("gwc", isGWCAround() + "");
    	
    	String gwcLink = baseUrl.substring(0,baseUrl.length()-4) + "gwc/";
    	map.put("gwcLink", gwcLink);
    	
    	map.put("attributes", info.getAttributes());
    	
    	return map;
    }

    private FeatureTypeInfo lookupType(String namespace, String layer){
        NamespaceInfo ns = catalog.getNamespaceByPrefix(namespace);
        if (ns == null) {
            throw new RestletException(
                    "No such namespace: " + namespace,
                    Status.CLIENT_ERROR_NOT_FOUND 
                    );
        }

        FeatureTypeInfo featureType = null;
        try {
            featureType = catalog.getFeatureTypeByName(ns, layer);
        } catch(Exception e) {
            throw new RestletException(
                "No such featuretype: " + namespace + ":" + layer,
                 Status.CLIENT_ERROR_NOT_FOUND 
            );
        }

        if (featureType == null) {
            throw new RestletException(
                    "No such featuretype: " + namespace + ":" + layer,
                     Status.CLIENT_ERROR_NOT_FOUND 
                    );
        }

        return featureType;
    }
    
    
    private ReferencedEnvelope getBBOX(FeatureTypeInfo layer){
    	
        String bboxList;
    	
        // We need to create a 4326 CRS for comparison to layer's crs
        CoordinateReferenceSystem latLonCrs = null;

        try { // get the CRS object for lat/lon 4326
            latLonCrs = CRS.decode("EPSG:" + 4326);
        } catch (NoSuchAuthorityCodeException e) {
            String msg = "Error looking up SRS for EPSG: " + 4326 + ":" + e.getLocalizedMessage();
            //currently does nothing with this string
        } catch (FactoryException e) {
            String msg = "Error looking up SRS for EPSG: " + 4326 + ":" + e.getLocalizedMessage();
            //currently does nothing with this string                        
        }
    	
    	//yoinked from MapPreviewAction
        try {

            CoordinateReferenceSystem layerCrs = layer.getCRS();

            /* A quick and efficient way to grab the bounding box is to get it
             * from the featuretype info where the lat/lon bbox is loaded
             * from the DTO. We do this with layer.getLatLongBoundingBox().
             * We need to reproject the bounding box from lat/lon to the layer crs
             * for display
             */
            Envelope orig_bbox = layer.getLatLonBoundingBox();

            if ((orig_bbox.getWidth() == 0) || (orig_bbox.getHeight() == 0)) {
                orig_bbox.expandBy(0.1);
            }

            ReferencedEnvelope bbox = new ReferencedEnvelope(orig_bbox, latLonCrs);

            if (!CRS.equalsIgnoreMetadata(layerCrs, latLonCrs)) {
                // first check if we have a native bbox
                bbox = layer.boundingBox();
            }

            // we now have a bounding box in the same CRS as the layer
            if ((bbox.getWidth() == 0) || (bbox.getHeight() == 0)) {
                bbox.expandBy(0.1);
            }
            
            if (layer.isEnabled()) {
                // expand bbox by 5% to allow large symbolizers to fit the map
                bbox.expandBy(bbox.getWidth() / 20, bbox.getHeight() / 20);
                return bbox;
            }
        } catch(Exception e) {

        }
        
        return null;
    }
    
    //yoinked from MapPreviewAction
    private int[] getMapWidthHeight(Envelope bbox) {
        int width;
        int height;
        double ratio = bbox.getHeight() / bbox.getWidth();

        if (ratio < 1) {
            width = 750;
            height = (int) Math.round(750 * ratio);
        } else {
            width = (int) Math.round(550 / ratio);
            height = 550;
        }

        // make sure we reach some minimal dimensions (300 pixels is more or less 
        // the height of the zoom bar)
        if (width < 300) {
            width = 300;
        }

        if (height < 300) {
            height = 300;
        }

        // add 50 pixels horizontally to account for the zoom bar
        return new int[] { width + 50, height };
    }
    

	private double getMaxResolution(ReferencedEnvelope areaOfInterest) {
		double w = areaOfInterest.getWidth();
		double h = areaOfInterest.getHeight();

		return ((w > h) ? w : h) / 256;
	}

    //returns true if this GeoServer instance uses GWC, false otherwise.
    private boolean isGWCAround(){
    	boolean GWCisAround = false;

    	try {
    	  Class.forName("org.geowebcache.GeoWebCacheDispatcher");
    	  GWCisAround = true;
    	} catch (ClassNotFoundException cnfe) {
    	  // guess it's not there.
    	}
    	    	
    	return GWCisAround;
    }
}
