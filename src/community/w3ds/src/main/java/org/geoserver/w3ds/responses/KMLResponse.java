/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.kml.KMLTransformer;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.w3ds.types.GetSceneRequest;
import org.geoserver.w3ds.types.GetTileRequest;
import org.geoserver.w3ds.types.Scene;
import org.geoserver.w3ds.types.W3DSLayer;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geoserver.wms.map.XMLTransformerMapResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class KMLResponse extends Response {
    public KMLResponse() {
        super(Scene.class);
    }
    
    public boolean canHandle(Operation operation) {
    	Object o = operation.getParameters()[0];
    	if(o instanceof GetTileRequest) {
    		GetTileRequest gs = (GetTileRequest) operation.getParameters()[0];
            return "GetTile".equalsIgnoreCase(operation.getId()) && 
                    operation.getService().getId().equals("w3ds") &&
                    gs.getFormat()
    				.getMimeType()
    				.equalsIgnoreCase(
    						org.geoserver.w3ds.utilities.Format.KML
    								.getMimeType());
    	}
    	if(o instanceof GetSceneRequest) {
    	GetSceneRequest gs = (GetSceneRequest) operation.getParameters()[0];
        return "GetScene".equalsIgnoreCase(operation.getId()) && 
                operation.getService().getId().equals("w3ds") &&
                gs.getFormat()
				.getMimeType()
				.equalsIgnoreCase(
						org.geoserver.w3ds.utilities.Format.KML
								.getMimeType());
    	}
    	return false;
    }

    public String getMimeType(Object value, Operation operation) {
        return "application/vnd.google-earth.kml+xml";
    }
    
    public String getAttachmentFileName(Object value, Operation operation) {
        return "modelo.kml";
    }
    
    public static FeatureLayer[] getLayers(List<FeatureCollection<? extends FeatureType, ? extends Feature>> features, WMS wms) throws IOException {
    	ArrayList<FeatureLayer> layers = new ArrayList<FeatureLayer>();
    	for (int i = 0; i < features.size(); i++) {
            FeatureCollection collection = (FeatureCollection) features.get(i);
            layers.add(new FeatureLayer(collection, null));
    	}
    	for (FeatureLayer l : layers) {
    		String title = l.getFeatureSource().getName().getLocalPart();
    		l.setTitle(title);
    	}
    	StyleBuilder stb = new StyleBuilder();
    	for (FeatureLayer l : layers) {
    		 LayerInfo layerInfo = wms.getLayerByName(l.getTitle());
    		 Style style = null;
             if (layerInfo != null) {
            	 Set<StyleInfo> styles_u = layerInfo.getStyles();
            		for(StyleInfo s : styles_u) {
            			style = s.getStyle();
            			if(style != null) {
            				break;
            			}
            	 }
            		if(style == null ) {
                   	 style = layerInfo.getDefaultStyle().getStyle();
                    }
             } else {
                throw new ServiceException("Could not find layer " + l.getTitle(),
                             "LayerNotDefined");
             }
    		l.setStyle(style);
    	}
    	return (FeatureLayer[]) layers.toArray(new FeatureLayer[0]);
    }

    public static GetMapRequest getRequest(FeatureLayer[] layers) {
    	GetMapRequest request = new GetMapRequest();
    	ArrayList<MapLayerInfo> info = new ArrayList<MapLayerInfo>();
    	for (int i = 0; i < layers.length; i++) {
    		info.add(new MapLayerInfo(layers[i].getSimpleFeatureSource()));
    	}
    	request.setLayers(info);
    	return request;
    }
    
    public void write(Object o, OutputStream output, Operation operation)
        throws IOException {
    	GetSceneRequest gs = (GetSceneRequest) operation.getParameters()[0];
    	Scene scene = (Scene) o;
    	List<FeatureCollection<? extends FeatureType, ? extends Feature>> features = new ArrayList<FeatureCollection<? extends FeatureType, ? extends Feature>>();;
    	for (W3DSLayer wl : scene.getLayers()) {
			features.add(wl.getFeatures());
    	}
    	WMS wms = new WMS(gs.getGeoServer());
		WMSMapContent mapContent = new WMSMapContent(getLayers(features, wms));
		mapContent.setRequest(getRequest(getLayers(features, wms)));
		KMLTransformer transformer = new KMLTransformer(wms);
		transformer.setIndentation(3);
		Charset encoding = wms.getCharSet();
		transformer.setEncoding(encoding);
		mapContent.setMapHeight(623);
		mapContent.setMapWidth(623);
		XMLTransformerMap map = new XMLTransformerMap(mapContent, transformer,
				mapContent, "application/vnd.google-earth.kml+xml");
		map.setContentDispositionHeader(mapContent, ".kml");
		XMLTransformerMapResponse response = new XMLTransformerMapResponse();
    	response.write(map, output);
   }
    
}
