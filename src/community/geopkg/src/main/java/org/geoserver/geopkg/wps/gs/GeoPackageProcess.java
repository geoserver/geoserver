package org.geoserver.geopkg.wps.gs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.Wfs20Factory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.geopkg.GeoPackageGetMapOutputFormat;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wps.WPSStorageCleaner;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.Entry;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.wps.GeoPackageProcessRequest;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.FeaturesLayer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.Layer;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.LayerType;
import org.geotools.geopkg.wps.GeoPackageProcessRequest.TilesLayer;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.styling.Style;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

@DescribeProcess(title="GeoPackage", description="Geopackage Process")
public class GeoPackageProcess implements GSProcess {
    
   private Catalog catalog;
    
   private WPSStorageCleaner storage;
   
   private GetFeature getFeatureDelegate;
      
   private GeoPackageGetMapOutputFormat mapOutput;
   
   private FilterFactory2 filterFactory;
   
   public GeoPackageProcess(GeoServer geoServer, GeoPackageGetMapOutputFormat mapOutput, WPSStorageCleaner storage, FilterFactory2 filterFactory) {
       this.storage = storage;
       this.mapOutput = mapOutput;
       this.filterFactory = filterFactory;
       catalog = geoServer.getCatalog();
       getFeatureDelegate = new GetFeature(geoServer.getService(WFSInfo.class), catalog);
       getFeatureDelegate.setFilterFactory(filterFactory);
   }
   
   private static final int TEMP_DIR_ATTEMPTS = 10000;
   
   public static File createTempDir(File baseDir) {        
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException("Failed to create directory within " + TEMP_DIR_ATTEMPTS
                + " attempts (tried " + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1)
                + ')');
    }

   @DescribeResult(name="geopackage", description="Link to Compiled Geopackage File")
   public URL execute(@DescribeParameter(name="contents", description="xml scheme describing geopackage contents") GeoPackageProcessRequest contents) throws IOException {
       
       final File file = new File( createTempDir(storage.getStorage()), contents.getName()+ ".gpkg");
       
       GeoPackage gpkg = new GeoPackage(file);
              
       for (int i=0; i < contents.getLayerCount() ; i++) {
           Layer layer = contents.getLayer(i);
           
           if (layer.getType() == LayerType.FEATURES){
               FeaturesLayer features = (FeaturesLayer) layer;
               
               QueryType queryExpression = Wfs20Factory.eINSTANCE.createQueryType();
               queryExpression.getTypeNames().add(features.getFeatureType());
               queryExpression.setSrsName(features.getSrs());
               if (features.getPropertyNames() != null) {
                   queryExpression.getPropertyNames().addAll(features.getPropertyNames());
               }
               Filter filter = features.getFilter();
               //add bbox to filter if there is one
               if (features.getBbox() != null){
                   String defaultGeometry = catalog.getFeatureTypeByName(features.getFeatureType().getLocalPart())
                           .getFeatureType().getGeometryDescriptor().getLocalName();
                   Filter bboxFilter = filterFactory.bbox(filterFactory.property(defaultGeometry), ReferencedEnvelope.reference(features.getBbox()));
                   if (filter == null) {
                       filter = bboxFilter;
                   } else {
                       filter = filterFactory.and(filter, bboxFilter);
                   }
               }
               queryExpression.setFilter(filter);
               
               GetFeatureType getFeature = Wfs20Factory.eINSTANCE.createGetFeatureType();
               getFeature.getAbstractQueryExpression().add(queryExpression);

               FeatureCollectionResponse fc = getFeatureDelegate.run(GetFeatureRequest.adapt(getFeature));
               
               for (FeatureCollection collection: fc.getFeatures()) {                   
                   if (! (collection instanceof SimpleFeatureCollection)) {
                       throw new ServiceException("GeoPackage OutputFormat does not support Complex Features.");
                   }
                   
                   FeatureEntry e = new FeatureEntry();
                   e.setTableName(layer.getName());                   
                   addLayerMetadata(e, features);
                   ReferencedEnvelope bounds = collection.getBounds();
                   if (features.getBbox() != null){
                       bounds = ReferencedEnvelope.reference(bounds.intersection(features.getBbox()));
                   }
                   
                   e.setBounds(bounds);
                  
                   gpkg.add(e, (SimpleFeatureCollection)  collection);
               }
               
           } else if (layer.getType() == LayerType.TILES) {
               TilesLayer tiles = (TilesLayer) layer;               
               GetMapRequest request = new GetMapRequest();
               
               request.setLayers(new ArrayList<MapLayerInfo>());
               for (QName layerQName : tiles.getLayers()) {
                   LayerInfo layerInfo = null;
                   if ("".equals(layerQName.getNamespaceURI())) {
                       layerInfo = catalog.getLayerByName(layerQName.getLocalPart());
                   } else {
                       layerInfo = catalog.getLayerByName(new NameImpl(layerQName.getNamespaceURI(), layerQName.getLocalPart()));
                   }
                   if (layerInfo == null) {
                       throw new ServiceException("Layer not found: " + layerQName);
                   }
                   request.getLayers().add(new MapLayerInfo(layerInfo));
               }
               
               request.setBbox(tiles.getBbox());
               request.setBgColor(tiles.getBgColor());
               if (tiles.getSrs() != null) {
                   request.setSRS(tiles.getSrs().toString());
               }
               request.setTransparent(tiles.isTransparent());
               request.setSldBody(tiles.getSldBody());
               if (tiles.getSld() != null) {
                   request.setSld(tiles.getSld().toURL());
               }
               else if (tiles.getSldBody() != null) {
                   request.setSldBody(tiles.getSldBody());
               }
               else {
                   request.setStyles(new ArrayList<Style>());
                   if (tiles.getStyles() != null) {
                       for (String styleName : tiles.getStyles()) {
                           StyleInfo info = catalog.getStyleByName(styleName);
                           if (info != null){
                               request.getStyles().add(info.getStyle());
                           }                       
                       }
                   }
                   if (request.getStyles().isEmpty()) {
                       for (MapLayerInfo layerInfo : request.getLayers()) {
                           request.getStyles().add(layerInfo.getDefaultStyle());
                       }
                   }
               }
               request.setFormat("none");
               Map formatOptions = new HashMap();
               formatOptions.put("format",tiles.getFormat());
               if (tiles.getCoverage() != null) {
                   if (tiles.getCoverage().getMinZoom() != null) {
                       formatOptions.put("min_zoom", tiles.getCoverage().getMinZoom());
                   }
                   if (tiles.getCoverage().getMaxZoom() != null) {
                       formatOptions.put("max_zoom", tiles.getCoverage().getMaxZoom());
                   }
                   if (tiles.getCoverage().getMinColumn() != null) {                       
                       formatOptions.put("min_column", tiles.getCoverage().getMinColumn());
                   }
                   if (tiles.getCoverage().getMaxColumn() != null) {
                       formatOptions.put("max_column", tiles.getCoverage().getMaxColumn());
                   }
                   if (tiles.getCoverage().getMinRow() != null) {
                       formatOptions.put("min_row", tiles.getCoverage().getMinRow());
                   }
                   if (tiles.getCoverage().getMaxRow() != null) {
                       formatOptions.put("max_row", tiles.getCoverage().getMaxRow());
                   }
               }
               if (tiles.getGridSetName() != null) {
                   formatOptions.put("gridset", tiles.getGridSetName());
               }
               request.setFormatOptions(formatOptions);
                              
               TileEntry e = new TileEntry();                 
               addLayerMetadata(e, tiles);
               
               if (tiles.getGrids() != null) {
                   mapOutput.addTiles(gpkg, e, request, tiles.getGrids(), layer.getName());
               } else {
                   mapOutput.addTiles(gpkg, e, request, layer.getName());
               }
           }
       }
       
       gpkg.close();
              
       return storage.getURL(file);
       
   }

    private void addLayerMetadata(Entry e, Layer layer) {
        e.setDescription(layer.getDescription());
        e.setIdentifier(layer.getIdentifier());
    }

}
