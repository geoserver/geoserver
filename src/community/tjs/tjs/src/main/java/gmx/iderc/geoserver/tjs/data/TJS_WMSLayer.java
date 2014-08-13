package gmx.iderc.geoserver.tjs.data;

import com.sun.rowset.CachedRowSetImpl;
import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.map.WMSLayer;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.xml.ApplicationSchemaXSD;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.GML;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.ows.Layer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleAttributeExtractor;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.tjs.TJS;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.geotools.xs.XSConfiguration;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.xml.sax.SAXException;

import javax.sql.RowSet;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/8/12
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class TJS_WMSLayer extends WMSLayer {
    DatasetInfo datasetInfo;
    HashMap<String, Integer> index = new HashMap<String, Integer>();
    CachedRowSetImpl rst;
    Layer layer;
    FeatureTypeInfo featureTypeInfo;

    SimpleFeatureType layerFatureType = null;

    public TJS_WMSLayer(TJS_WebMapServer webMapServer, Layer layer, FeatureTypeInfo featureTypeInfo, DatasetInfo datasetInfo) throws IOException {
        super(webMapServer, layer);
        this.layer = layer;
        this.datasetInfo = datasetInfo;
        this.featureTypeInfo = featureTypeInfo;
        try {
            rst = new CachedRowSetImpl();
            RowSet remote = datasetInfo.getTJSDatasource().getRowSet();
            rst.populate(remote);
            remote.close();
            indexRowSet();
        } catch (SQLException ex) {
            Logger.getLogger(TJS_WMSLayer.class).error(ex.getMessage());
        }
    }

    WMS getWMS() {
        WMS wms = (WMS) GeoServerExtensions.bean(WMS.class);
        return wms;
    }

    GeoServer getGeoserver() {
        return getWMS().getGeoServer();
    }

    Catalog getCatalog() {
        return getGeoserver().getCatalog();
    }

    public SimpleFeatureType getFeatureType() {
        if (layerFatureType != null) {
            return layerFatureType;
        } else {
            try {
                FeatureType ftype = getCatalog().getResourcePool().getFeatureType(featureTypeInfo);
                layerFatureType = extendFeatureType((SimpleFeatureType) ftype);
                return layerFatureType; //faltaba esto!!!!, Alvaro Javier
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public String translateStyle(String styleName) throws IOException {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(styleName);
        ByteArrayOutputStream bstyle = new ByteArrayOutputStream();
        BufferedReader reader = catalog.getResourcePool().readStyle(style);
        StyledLayerDescriptor sld = Styles.parse(reader);
        StyleVisitor ruleStyleVisitor = new StyleAttributeExtractor() {

            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

            @Override
            public void visit(Rule rule) {
                try {
                    ArrayList<Filter> ids = new ArrayList<Filter>();
                    RowSet rst = datasetInfo.getTJSDatasource().getRowSet(rule.getFilter());
                    while (rst.next()) {
                        int keyIndex = rst.findColumn(datasetInfo.getGeoKeyField());
                        ids.add(ff.equal(ff.property(datasetInfo.getFramework().getFrameworkKey().getName()), ff.literal(rst.getObject(keyIndex)), false));
                    }
                    rst.close();
                    if (ids.size() == 1) {
                        rule.setFilter(ids.get(0));
                    }
                    if (ids.size() > 1) {
                        rule.setFilter(ff.or(ids));
                    }
                } catch (SQLException ex) {

                }
            }
        };
        sld.accept(ruleStyleVisitor);

        StyleInfo newStyleInfo = catalog.getFactory().createStyle();
        newStyleInfo.setName(styleName + String.valueOf(System.currentTimeMillis()));
        String styleFileName = newStyleInfo.getName() + ".sld";
        newStyleInfo.setFilename(styleFileName);
        getCatalog().add(newStyleInfo);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Styles.encode(sld, newStyleInfo.getSLDVersion(), false, out);
        out.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        catalog.getResourcePool().writeStyle(newStyleInfo, in);
        in.close();

        return newStyleInfo.getName();
    }

    public SimpleFeatureType extendFeatureType(SimpleFeatureType type) {
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.addAll(type.getAttributeDescriptors());
        for (ColumnInfo column : datasetInfo.getColumns()) {
            featureTypeBuilder.add(column.getName(), column.getSQLClassBinding());
        }
        featureTypeBuilder.setName(datasetInfo.getName());
        SimpleFeatureType newtype = featureTypeBuilder.buildFeatureType();
        return newtype;
    }

    public SimpleFeatureCollection extendFeatureInfo(SimpleFeatureCollection featureCollection) {

        SimpleFeatureType type = getFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        Name frameworkKey = null;
        try {
            //lo ideal ser'ia esto pero no puede ser...
            //frameworkKey = datasetInfo.getFramework().getFrameworkKey().getAttribute().getName();

            String frameworkKeyName = datasetInfo.getFramework().getFrameworkKey().getName();
            FeatureType ftype = getCatalog().getResourcePool().getFeatureType(featureTypeInfo);
            frameworkKey = ftype.getDescriptor(frameworkKeyName).getName();
        } catch (IOException ex) {
            Logger.getLogger(TJS_WMSLayer.class).error(ex.getMessage());
        }

        MemoryFeatureCollection memFC = new MemoryFeatureCollection(type);
        SimpleFeatureIterator features = featureCollection.features();
        while (features.hasNext()) {
            SimpleFeature feature = features.next();
            featureBuilder.addAll(feature.getAttributes());
            if (frameworkKey != null) {
                Object keyValue = feature.getAttribute(frameworkKey);
                for (ColumnInfo column : datasetInfo.getColumns()) {
                    featureBuilder.set(column.getName(), lookup(keyValue, column.getName()));
                }
            }
            memFC.add(featureBuilder.buildFeature(feature.getID()));
        }
        return memFC;
    }

    private Object lookup(Object keyValue, String fieldName) {
        int absRow = index.get(keyValue.toString()).intValue();
        try {
            if (rst.absolute(absRow)) {
                int findex = rst.findColumn(fieldName);
                return rst.getObject(findex);
            }
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
        return null;
    }

    private RowSet filter(Filter filter) {
        CachedRowSetImpl result = null;
        try {
            result = new CachedRowSetImpl();
            result.populate(rst);
            result.first();
            while (rst.next()) {
                if (!filter.evaluate(rst)) {
                    result.deleteRow();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
        return result;
    }

    private void indexRowSet() throws SQLException {
        int keyIndex = rst.findColumn(datasetInfo.getGeoKeyField());
        while (rst.next()) {
            index.put(rst.getObject(keyIndex).toString(), rst.getRow());
        }
    }

    private InputStream copy(InputStream source) {
        try {
            File file = File.createTempFile("gdas", ".xml");
            FileOutputStream fos = new FileOutputStream(file);
            StreamUtils.copy(source, fos);
            fos.close();
            System.out.println("file copied in: " + file.toString());
            return new FileInputStream(file);
        } catch (IOException ex) {
            System.out.println("Problem coping file: " + ex.getMessage());
        }
        return null;
    }

    protected String getBaseURL() {
        try {
            Request owsRequest = ((ThreadLocal<Request>) Dispatcher.REQUEST).get();
            return owsRequest.getHttpRequest().getRequestURL().toString();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public InputStream getFeatureInfo(ReferencedEnvelope bbox, int width, int height, int x, int y, String infoFormat, int featureCount) throws IOException {
        String gmlFormat = "application/vnd.ogc.gml";
        String htmlFormat = "text/html";
        String textFormat = "text/plain";

        InputStream infoStream = super.getFeatureInfo(bbox, width, height, x, y, gmlFormat, featureCount);

        WMS wms = (WMS) GeoServerExtensions.bean(WMS.class);
        GeoServer geoserver = wms.getGeoServer();
        Catalog catalog = geoserver.getCatalog();

        ArrayList featureTypes = new ArrayList();
        featureTypes.add(featureTypeInfo);
        ApplicationSchemaXSD xsd = new ApplicationSchemaXSD(null, catalog, getBaseURL(),
                                                                   org.geotools.wfs.v1_0.WFS.getInstance(), featureTypes);

        Configuration configuration = new Configuration(xsd) {
            {
                addDependency(new XSConfiguration());
                addDependency(new org.geotools.gml2.GMLConfiguration());
            }

            protected void registerBindings(java.util.Map bindings) {
            }
        };
        SimpleFeatureCollection featureCollection = null;
        Parser parser = new Parser(configuration);
        parser.setStrict(false);
        try {
            Object obj = parser.parse(infoStream);
            featureCollection = (SimpleFeatureCollection) (obj);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        } catch (SAXException ex) {
            Logger.getLogger(TJSFeatureReader.class).error(ex.getMessage());
        }
        featureCollection = extendFeatureInfo(featureCollection);
        GetFeatureInfoOutputFormat outputFormat = null;
        if (infoFormat.compareToIgnoreCase(gmlFormat) == 0) {
            return encodeGML(featureCollection);
        } else if (infoFormat.compareToIgnoreCase(htmlFormat) == 0) {
            return encodeHTML(featureCollection);
        } else {//return text format
            return encodeText(featureCollection);
        }
    }

    private InputStream encodeText(SimpleFeatureCollection featureCollection) throws IOException {
        return null;
    }

    private InputStream encodeHTML(SimpleFeatureCollection featureCollection) throws IOException {
        return null;
    }

    private InputStream encodeGML(SimpleFeatureCollection featureCollection) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        GML encode = new GML(org.geotools.GML.Version.WFS1_0);
        encode.setNamespace("tjs", TJS.NAMESPACE);
        encode.encode(out, featureCollection);
        out.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        return in;
    }

    GetFeatureInfoRequest getFeatureInfoRequest(ReferencedEnvelope bbox, int width, int height, int x, int y, String infoFormat, int featureCount) {
        GetMapRequest mapRequest = new GetMapRequest();
        mapRequest.setBbox(bbox);
        mapRequest.setWidth(width);
        mapRequest.setHeight(height);
        mapRequest.setFormat("image/jpeg");
        try {
            String code = CRS.lookupIdentifier(bbox.getCoordinateReferenceSystem(), false);
            mapRequest.setSRS(code);
        } catch (FactoryException ex) {
            mapRequest.setSRS("EPSG:4326");
        }
        GetFeatureInfoRequest featureInfoRequest = new GetFeatureInfoRequest();
        featureInfoRequest.setFeatureCount(featureCount);
        featureInfoRequest.setInfoFormat(infoFormat);
        featureInfoRequest.setGetMapRequest(mapRequest);
        featureInfoRequest.setXPixel(x);
        featureInfoRequest.setYPixel(y);
        return featureInfoRequest;
    }


}
