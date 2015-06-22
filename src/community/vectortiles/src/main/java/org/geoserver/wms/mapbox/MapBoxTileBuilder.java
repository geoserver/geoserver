package org.geoserver.wms.mapbox;

import static org.geoserver.wms.mapbox.MapBoxTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import no.ecc.vectortile.VectorTileEncoder;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.RendererUtilities;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * 
 * @author Niels Charlier
 *
 */
public class MapBoxTileBuilder implements VectorTileBuilder {
        
    protected final static CoordinateReferenceSystem standardCrs;
    
    static {
        try {
            standardCrs = CRS.decode("EPSG:900913");
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected List<SimpleFeature> features = new ArrayList<SimpleFeature>();    
    protected boolean forceCrs;   
    private Rectangle mapSize;
    private ReferencedEnvelope mapArea;
      
    public MapBoxTileBuilder(Rectangle mapSize, ReferencedEnvelope mapArea, boolean forceCrs) {
        this.mapSize = mapSize;
        this.mapArea = mapArea;        
        this.forceCrs = forceCrs;
    }

    @Override
    public void addFeature(SimpleFeature feature) {
        features.add(feature);
    }

    protected Map<String, Object> propertiesToAttributes(Collection<Property> properties) {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Iterator<Property> it = properties.iterator();
        while (it.hasNext()) {
            Property property = it.next();
            if (!(property.getValue() instanceof Geometry)) {
                attributes.put(property.getName().getLocalPart(), property.getValue());
            }
        }
        return attributes;
    }

    protected static Geometry convertMapCoordsToTileCoords(Geometry geometry, CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem targetCRS, AffineTransform2D worldToScreen) {
        Geometry targetGeometry = null;
            
        try {
            //make sure in right CRS first
            if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                targetGeometry = JTS.transform(geometry, CRS.findMathTransform(sourceCRS, targetCRS));
            } else {
                targetGeometry = geometry;
            }
            //to screen coordinates
            targetGeometry = JTS.transform(targetGeometry, worldToScreen);
            
        } catch (MismatchedDimensionException | TransformException | FactoryException e) {
            throw new RuntimeException(e);
        }
        
        return targetGeometry;        
    }

    @Override
    public WebMap build(WMSMapContent mapContent) throws IOException {
        int extent = Math.max(256, Math.max((int) mapSize.getWidth(), (int) mapSize.getHeight()));        
        CoordinateReferenceSystem targetCRS = forceCrs || mapContent.getRequest().getCrs() == null ? 
                standardCrs : mapContent.getRequest().getCrs();
            
        //make sure mapArea in right CRS
        ReferencedEnvelope targetMapArea;
        if (!CRS.equalsIgnoreMetadata(mapArea.getCoordinateReferenceSystem(), targetCRS)) {
            try {
                targetMapArea = mapArea.transform(targetCRS, true);
            } catch (TransformException | FactoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            targetMapArea = mapArea;
        }
        
        //create world to screen transform
        AffineTransform2D worldToScreen = new AffineTransform2D(RendererUtilities.worldToScreenTransform(targetMapArea, mapSize));
        
        //encode features
        VectorTileEncoder encoder = new VectorTileEncoder(extent, extent / 32, false);        
        for (SimpleFeature feature : features) {
            Collection<Property> properties = feature.getProperties();
            Map<String, Object> attributes = propertiesToAttributes(properties);
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Geometry tilegeometry = convertMapCoordsToTileCoords(geometry, 
                    feature.getFeatureType().getCoordinateReferenceSystem(), 
                    targetCRS, worldToScreen);
            encoder.addFeature(feature.getName().getLocalPart(), attributes, tilegeometry);
        }
        
        return new RawMap(mapContent, encoder.encode(), MIME_TYPE);
    }

}
