/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.crs.ForceCoordinateSystemFeatureReader;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.ExpressionDOMParser;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDParser;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.util.GETMAPValidator;
import org.vfny.geoserver.util.SLDValidator;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * reads in a GetFeature XML WFS request from a XML stream
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class GetMapXmlReader extends org.geoserver.ows.XmlRequestReader {

    private static final Logger LOGGER = Logging.getLogger(GetMapXmlReader.class);

    private static final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();

    private WMS wms;

    /**
     * Creates a new GetMapXmlReader object.
     *
     * @param wms The WMS config object.
     */
    public GetMapXmlReader(WMS wms) {
        super(OWS.NAMESPACE, "GetMap");
        this.wms = wms;
    }

    public WMS getWMS() {
        return wms;
    }

    /** Reads the GetMap XML request into a GetMap Request object. */
    @SuppressWarnings("rawtypes")
    @Override
    public Object read(Object request, Reader reader, Map kvp) throws Exception {

        GetMapRequest getMapRequest = new GetMapRequest();

        boolean validateSchema = kvp.containsKey("validateschema");

        try {
            parseGetMapXML(reader, getMapRequest, validateSchema);
        } catch (java.net.UnknownHostException unh) {
            // J--
            // http://www.oreilly.com/catalog/jenut2/chapter/ch19.html ---
            // There is one complication to this example. Most web.xml files contain a <!DOCTYPE>
            // tag that specifies
            // the document type (or DTD). Despite the fact that Example 19.1 specifies that the
            // parser should not
            // validate the document, a conforming XML parser must still read the DTD for any
            // document that has a
            // <!DOCTYPE> declaration. Most web.xml have a declaration like this:
            // ..
            // In order to read the DTD, the parser must be able to read the specified URL. If your
            // system is not
            // connected to the Internet when you run the example, it will hang.
            // . Another workaround to this DTD problem is to simply remove (or comment out) the
            // <!DOCTYPE> declaration from the web.xml file you process with ListServlets1./
            //
            // also see:
            // http://doctypechanger.sourceforge.net/
            // J+
            throw new ServiceException(
                    "unknown host - "
                            + unh.getLocalizedMessage()
                            + " - if its in a !DOCTYPE, remove the !DOCTYPE tag.");
        } catch (SAXParseException se) {
            throw new ServiceException(
                    "line "
                            + se.getLineNumber()
                            + " column "
                            + se.getColumnNumber()
                            + " -- "
                            + se.getLocalizedMessage());
        } catch (Exception e) {
            throw new ServiceException(e);
        }

        return getMapRequest;
    }

    /**
     * Actually read in the XML request and stick it in the request object. We do this using the DOM
     * parser (because the SLD parser is DOM based and we can integrate). 1. parse into DOM 2. parse
     * the SLD 3. grab the rest of the attribute 4. stuff #3 attributes in the request object 5.
     * stuff the SLD info into the request object 6. return GetMap schema is at
     * http://www.opengeospatial.org/docs/02-017r1.pdf (page 18) NOTE: see handlePostGet() for
     * people who put the SLD in the POST and the parameters in the GET.
     */
    private void parseGetMapXML(Reader xml, GetMapRequest getMapRequest, boolean validateSchema)
            throws Exception {
        File temp = null;

        try {
            if (validateSchema) { // copy POST to a file
                // make tempfile
                temp = File.createTempFile("getMapPost", "xml");

                FileOutputStream fos = new FileOutputStream(temp);
                BufferedOutputStream out = new BufferedOutputStream(fos);

                int c;

                while (-1 != (c = xml.read())) {
                    out.write(c);
                }

                xml.close();
                out.flush();
                out.close();
                xml = new BufferedReader(new FileReader(temp)); // pretend like nothing has happened
            }

            javax.xml.parsers.DocumentBuilderFactory dbf =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();

            dbf.setExpandEntityReferences(false);
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);

            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            EntityResolver entityResolver = wms.getCatalog().getResourcePool().getEntityResolver();
            if (entityResolver != null) {
                db.setEntityResolver(entityResolver);
            }

            InputSource input = new InputSource(xml);
            org.w3c.dom.Document dom = db.parse(input);

            SLDParser sldParser = new SLDParser(styleFactory);

            Node rootNode = dom.getDocumentElement();

            // we have the SLD component, now we get all the GetMAp components
            // step a -- attribute "version"
            Node nodeGetMap = rootNode;

            if (!(nodeNameEqual(nodeGetMap, "getmap"))) {
                if (nodeNameEqual(nodeGetMap, "StyledLayerDescriptor")) // oopsy!! its a SLD POST
                // with get parameters!
                {
                    if (validateSchema) {
                        validateSchemaSLD(temp, getMapRequest);
                    }

                    handlePostGet(rootNode, sldParser, getMapRequest);

                    return;
                }

                throw new Exception(
                        "GetMap XML parser - start node isnt 'GetMap' or 'StyledLayerDescriptor' tag");
            }

            if (validateSchema) {
                validateSchemaGETMAP(temp, getMapRequest);
            }

            NamedNodeMap atts = nodeGetMap.getAttributes();
            Node wmsVersion = atts.getNamedItem("version");

            if (wmsVersion == null) {
                throw new Exception(
                        "GetMap XML parser - couldnt find attribute 'version' in GetMap tag");
            }

            getMapRequest.setVersion(wmsVersion.getNodeValue());

            // ignore the OWSType since we know its supposed to be WMS
            // step b -bounding box
            parseBBox(getMapRequest, nodeGetMap);

            // for SLD we already have it (from above) (which we'll handle as layers later)
            StyledLayerDescriptor sld =
                    sldParser.parseDescriptor(getNode(rootNode, "StyledLayerDescriptor"));
            processStyles(getMapRequest, sld);

            // step c - "Output"
            parseXMLOutput(nodeGetMap, getMapRequest); // make this function easier to read

            // step d - "exceptions"
            getMapRequest.setExceptions(getNodeValue(nodeGetMap, "Exceptions"));

            // step e - "VendorType
            // we dont actually do anything with this, so...
            // step f - rebuild SLD info. Ie. fill in the Layer and Style information, just like SLD
            // post-get
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    /**
     * This is the hybrid SLD POST way. Normal is the GetMap - with a built in SLD. The alternate,
     * for stupid people, is the WMS parameters in the GET, and the SLD in the post. This handles
     * that case.
     */
    private void handlePostGet(Node rootNode, SLDParser sldParser, GetMapRequest getMapRequest)
            throws Exception {
        // get the GET parmeters

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(new StringBuffer("reading request: ").append(getMapRequest).toString());
        }

        // Map<String, String> requestParams = getMapRequest.getRawKvp();

        // GetMapKvpRequestReader kvpReader = new GetMapKvpRequestReader(getWMS());

        // String version = kvpReader.getRequestVersion();
        // getMapRequest.setVersion(version);

        // kvpReader.parseMandatoryParameters(getMapRequest, false); //false means dont do
        // styles/layers (see below)
        // kvpReader.parseOptionalParameters(getMapRequest);

        // get styles/layers from the sld.
        StyledLayerDescriptor sld = sldParser.parseDescriptor(rootNode); // root =
        // <StyledLayerDescriptor>
        processStyles(getMapRequest, sld);
    }

    /** taken from the kvp reader, with modifications */
    private void processStyles(GetMapRequest getMapRequest, StyledLayerDescriptor sld)
            throws Exception {
        final StyledLayer[] styledLayers = sld.getStyledLayers();
        final int slCount = styledLayers.length;

        if (slCount == 0) {
            throw new ServiceException("SLD document contains no layers");
        }

        final List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
        final List<Style> styles = new ArrayList<Style>();
        final List<Filter> filters = new ArrayList<Filter>();
        MapLayerInfo currLayer;

        StyledLayer sl = null;

        for (int i = 0; i < slCount; i++) {
            sl = styledLayers[i];

            String layerName = sl.getName();

            if (null == layerName) {
                throw new ServiceException("A UserLayer without layer name was passed");
            }

            // TODO: add support for remote WFS here
            // handle the InLineFeature stuff
            if ((sl instanceof UserLayer)
                    && ((((UserLayer) sl)).getInlineFeatureDatastore() != null)) {
                // SPECIAL CASE - we make the temporary version
                UserLayer ul = ((UserLayer) sl);
                CoordinateReferenceSystem crs =
                        (getMapRequest.getCrs() == null)
                                ? DefaultGeographicCRS.WGS84
                                : getMapRequest.getCrs();
                currLayer = initializeInlineFeatureLayer(ul, crs);
                addStyles(wms, getMapRequest, currLayer, styledLayers[i], layers, styles, filters);
            } else {

                LayerGroupInfo layerGroup = getWMS().getLayerGroupByName(layerName);

                if (layerGroup != null) {
                    List<LayerInfo> layerGroupLayers = layerGroup.layers();
                    List<StyleInfo> layerGroupStyles = layerGroup.getStyles();
                    for (int j = 0; j < layerGroupStyles.size(); j++) {
                        StyleInfo si = layerGroupStyles.get(j);
                        LayerInfo layer = layerGroupLayers.get(j);
                        currLayer = new MapLayerInfo(layer);
                        if (si != null) {
                            currLayer.setStyle(si.getStyle());
                        }
                        addStyles(
                                wms,
                                getMapRequest,
                                currLayer,
                                styledLayers[i],
                                layers,
                                styles,
                                filters);
                    }
                } else {
                    LayerInfo layerInfo = getWMS().getLayerByName(layerName);
                    if (layerInfo == null) {
                        throw new ServiceException("Layer not found: " + layerName);
                    }
                    currLayer = new MapLayerInfo(layerInfo);
                    addStyles(
                            wms,
                            getMapRequest,
                            currLayer,
                            styledLayers[i],
                            layers,
                            styles,
                            filters);
                }
            }
        }

        getMapRequest.setLayers(layers);
        getMapRequest.setStyles(styles);
        getMapRequest.setFilter(filters);
    }

    /**
     * the correct thing to do its grab the style from styledLayers[i] inside the styledLayers[i]
     * will either be : a) nothing - in which case grab the layer's default style b) a set of: i)
     * NameStyle -- grab it from the pre-loaded styles ii)UserStyle -- grab it from the sld the user
     * uploaded
     *
     * <p>NOTE: we're going to get a set of layer->style pairs for (b). these are added to
     * layers,styles
     *
     * <p>NOTE: we also handle some featuretypeconstraints
     */
    public static void addStyles(
            WMS wms,
            GetMapRequest request,
            MapLayerInfo currLayer,
            StyledLayer layer,
            List<MapLayerInfo> layers,
            List<Style> styles,
            List<Filter> filters)
            throws ServiceException, IOException {
        if (currLayer == null) {
            return; // protection
        }

        Style[] layerStyles = null;
        FeatureTypeConstraint[] ftcs = null;

        if (layer instanceof NamedLayer) {
            ftcs = ((NamedLayer) layer).getLayerFeatureConstraints();
            layerStyles = ((NamedLayer) layer).getStyles();
            if (shouldUseLayerStyle(layerStyles, currLayer)) {
                layerStyles = new Style[] {currLayer.getStyle()};
            }
        } else if (layer instanceof UserLayer) {
            ftcs = ((UserLayer) layer).getLayerFeatureConstraints();
            layerStyles = ((UserLayer) layer).getUserStyles();
        }

        // DJB: TODO: this needs to do the whole thing, not just names
        if (ftcs != null) {
            FeatureTypeConstraint ftc;
            final int length = ftcs.length;

            for (int t = 0; t < length; t++) {
                ftc = ftcs[t];

                if (ftc.getFeatureTypeName() != null) {
                    String ftc_name = ftc.getFeatureTypeName();

                    // taken from lite renderer
                    boolean matches;

                    try {
                        final FeatureType currSchema = currLayer.getFeature().getFeatureType();
                        matches =
                                currSchema.getName().getLocalPart().equalsIgnoreCase(ftc_name)
                                        || FeatureTypes.isDecendedFrom(currSchema, null, ftc_name);
                    } catch (Exception e) {
                        matches = false; // bad news
                    }

                    if (!matches) {
                        continue; // this layer is filtered out
                    }

                    // add filter
                    if (ftc.getFilter() != null) {
                        filters.add(ftc.getFilter());
                    }
                }
            }
        }

        // handle no styles -- use default
        if ((layerStyles == null) || (layerStyles.length == 0)) {
            layers.add(currLayer);
            styles.add(currLayer.getDefaultStyle());

            return;
        }

        final int length = layerStyles.length;
        Style s;

        for (int t = 0; t < length; t++) {
            if (layerStyles[t] instanceof NamedStyle) {
                layers.add(currLayer);
                String styleName = ((NamedStyle) layerStyles[t]).getName();
                s = wms.getStyleByName(styleName);

                if (s == null) {
                    throw new ServiceException("couldnt find style named '" + styleName + "'");
                }

                styles.add(s);
            } else {
                if (wms.isDynamicStylingDisabled()) {
                    throw new ServiceException("Dynamic style usage is forbidden");
                }
                layers.add(currLayer);
                styles.add(layerStyles[t]);
            }
        }
    }

    /**
     * Performs a check to see if we should use the style from the layer info or from the given set
     * of styles from the request.
     */
    private static boolean shouldUseLayerStyle(Style[] layerStyles, MapLayerInfo currLayer) {
        boolean noSldLayerStyles = (layerStyles == null || layerStyles.length == 0);
        boolean layerHasStyle = currLayer.getStyle() != null;
        boolean shouldUseLayerStyle = noSldLayerStyles && layerHasStyle;
        return shouldUseLayerStyle;
    }

    private static MapLayerInfo initializeInlineFeatureLayer(
            UserLayer ul, CoordinateReferenceSystem requestCrs) throws Exception {
        // SPECIAL CASE - we make the temporary version
        final DataStore inlineDatastore = ul.getInlineFeatureDatastore();

        final SimpleFeatureSource source;
        // what if they didn't put an "srsName" on their geometry in their
        // inlinefeature?
        // I guess we should assume they mean their geometry to exist in the
        // output SRS of the
        // request they're making.
        if (ul.getInlineFeatureType().getCoordinateReferenceSystem() == null) {
            LOGGER.warning(
                    "No CRS set on inline features default geometry.  "
                            + "Assuming the requestor has their inlinefeatures in the boundingbox CRS.");

            SimpleFeatureType currFt = ul.getInlineFeatureType();
            Query q = new Query(currFt.getTypeName(), Filter.INCLUDE);
            FeatureReader<SimpleFeatureType, SimpleFeature> ilReader;
            ilReader = inlineDatastore.getFeatureReader(q, Transaction.AUTO_COMMIT);
            ForceCoordinateSystemFeatureReader reader =
                    new ForceCoordinateSystemFeatureReader(ilReader, requestCrs);
            MemoryDataStore reTypedDS = new MemoryDataStore(reader);
            source = reTypedDS.getFeatureSource(reTypedDS.getTypeNames()[0]);
        } else {
            source = inlineDatastore.getFeatureSource(inlineDatastore.getTypeNames()[0]);
        }

        MapLayerInfo mapLayer = new MapLayerInfo(source);
        return mapLayer;
    }

    /** xs:element name="BoundingBox" type="gml:BoxType"/> dont forget the SRS! */
    private void parseBBox(GetMapRequest getMapRequest, Node nodeGetMap) throws Exception {
        Node bboxNode = getNode(nodeGetMap, "BoundingBox");

        if (bboxNode == null) {
            throw new Exception(
                    "GetMap XML parser - couldnt find node 'BoundingBox' in GetMap tag");
        }

        List coordList =
                new ExpressionDOMParser(CommonFactoryFinder.getFilterFactory2()).coords(bboxNode);

        if (coordList.size() != 2) {
            throw new Exception(
                    "GetMap XML parser - node 'BoundingBox' in GetMap tag should have 2 coordinates in it");
        }

        org.locationtech.jts.geom.Envelope env = new org.locationtech.jts.geom.Envelope();
        final int size = coordList.size();

        for (int i = 0; i < size; i++) {
            env.expandToInclude((Coordinate) coordList.get(i));
        }

        getMapRequest.setBbox(env);

        // SRS
        NamedNodeMap atts = bboxNode.getAttributes();
        Node srsNode = atts.getNamedItem("srsName");

        if (srsNode != null) {
            String srs = srsNode.getNodeValue();
            String epsgCode = srs.substring(srs.indexOf('#') + 1);
            epsgCode = "EPSG:" + epsgCode;

            try {
                CoordinateReferenceSystem mapcrs = CRS.decode(epsgCode);
                getMapRequest.setCrs(mapcrs);
                getMapRequest.setSRS(epsgCode);
            } catch (Exception e) {
                // couldnt make it - we send off a service exception with the correct info
                throw new ServiceException(e.getLocalizedMessage(), "InvalidSRS");
            }
        }
    }

    // J--
    /**
     * <xs:element name="Format" type="ogc:FormatType"/> <xs:element name="Transparent"
     * type="xs:boolean" minOccurs="0"/> <xs:element name="BGcolor" type="xs:string" minOccurs="0"/>
     * <xs:element name="Size"> <xs:complexType> <xs:sequence> <xs:element name="Width"
     * type="xs:positiveInteger"/> <xs:element name="Height" type="xs:positiveInteger"/>
     * </xs:sequence> </xs:complexType> <xs:element name="Buffer" type="xs:integer" minOccurs="0"/>
     * </xs:element>
     * <!--Size-->
     */

    // J+
    private void parseXMLOutput(Node nodeGetMap, GetMapRequest getMapRequest) throws Exception {
        Node outputNode = getNode(nodeGetMap, "Output");

        if (outputNode == null) {
            throw new Exception("GetMap XML parser - couldnt find node 'Output' in GetMap tag");
        }

        // Format
        String format = getNodeValue(outputNode, "Format");

        if (format == null) {
            throw new Exception(
                    "GetMap XML parser - couldnt find node 'Format' in GetMap/Output tag");
        }

        getMapRequest.setFormat(format);

        // Transparent
        String trans = getNodeValue(outputNode, "Transparent");

        if (trans != null) {
            if (trans.equalsIgnoreCase("false") || trans.equalsIgnoreCase("0")) {
                getMapRequest.setTransparent(false);
            } else {
                getMapRequest.setTransparent(true);
            }
        }

        // Buffer
        String bufferValue = getNodeValue(outputNode, "Buffer");
        if (bufferValue == null) {
            // fall back on the alias
            getNodeValue(outputNode, "Radius");
        }
        if (bufferValue != null) {
            getMapRequest.setBuffer(Integer.parseInt(bufferValue));
        }

        // BGColor
        String bgColor = getNodeValue(outputNode, "BGcolor");

        if (bgColor != null) {
            getMapRequest.setBgColor(Color.decode(bgColor));
        }

        // SIZE
        Node sizeNode = getNode(outputNode, "Size");

        if (sizeNode == null) {
            throw new Exception(
                    "GetMap XML parser - couldnt find node 'Size' in GetMap/Output tag");
        }

        // Size/Width
        String width = getNodeValue(sizeNode, "Width");

        if (width == null) {
            throw new Exception(
                    "GetMap XML parser - couldnt find node 'Width' in GetMap/Output/Size tag");
        }

        getMapRequest.setWidth(Integer.parseInt(width));

        // Size/Height
        String height = getNodeValue(sizeNode, "Height");

        if (height == null) {
            throw new Exception(
                    "GetMap XML parser - couldnt find node 'Height' in GetMap/Output/Size tag");
        }

        getMapRequest.setHeight(Integer.parseInt(height));
    }

    /**
     * Give a node and the name of a child of that node, return it. This doesnt do anything complex.
     */
    public Node getNode(Node parentNode, String wantedChildName) {
        NodeList children = parentNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            String childName = child.getLocalName();

            if (childName == null) {
                childName = child.getNodeName();
            }

            if (childName.equalsIgnoreCase(wantedChildName)) {
                return child;
            }
        }

        return null;
    }

    /**
     * Give a node and the name of a child of that node, find its (string) value. This doesnt do
     * anything complex.
     */
    public String getNodeValue(Node parentNode, String wantedChildName) {
        NodeList children = parentNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }

            String childName = child.getLocalName();

            if (childName == null) {
                childName = child.getNodeName();
            }

            if (childName.equalsIgnoreCase(wantedChildName)) {
                return child.getChildNodes().item(0).getNodeValue();
            }
        }

        return null;
    }

    /** returns true if this node is named "name". Ignores case and namespaces. */
    public boolean nodeNameEqual(Node n, String name) {
        if (n.getNodeName().equalsIgnoreCase(name)) {
            return true;
        }

        String nname = n.getNodeName();
        int idx = nname.indexOf(':');

        if (idx == -1) {
            return false;
        }

        if (nname.substring(idx + 1).equalsIgnoreCase(name)) {
            return true;
        }

        return false;
    }

    /**
     * This should only be called if the xml starts with StyledLayerDescriptor Don't use on a
     * GetMap.
     */
    public void validateSchemaSLD(File f, GetMapRequest getMapRequest) throws Exception {
        SLDValidator validator = new SLDValidator();
        List errors = null;

        try {
            FileInputStream in = null;

            try {
                in = new FileInputStream(f);
                errors = validator.validateSLD(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }

            if (errors.size() != 0) {
                in = new FileInputStream(f);
                throw new ServiceException(SLDValidator.getErrorMessage(in, errors));
            }
        } catch (IOException e) {
            String msg =
                    new StringBuffer("Creating remote SLD url: ").append(e.getMessage()).toString();

            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, msg, e);
            }

            throw new ServiceException(e, msg, "parseSldParam");
        }
    }

    /** This should only be called if the xml starts with GetMap Don't use on a SLD. */
    public void validateSchemaGETMAP(File f, GetMapRequest getMapRequest) throws Exception {
        GETMAPValidator validator = new GETMAPValidator();
        List errors = null;

        try {
            FileInputStream in = null;

            try {
                in = new FileInputStream(f);
                errors = validator.validateGETMAP(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }

            if (errors.size() != 0) {
                in = new FileInputStream(f);
                throw new ServiceException(GETMAPValidator.getErrorMessage(in, errors));
            }
        } catch (IOException e) {
            String msg =
                    new StringBuffer("Creating remote GETMAP url: ")
                            .append(e.getMessage())
                            .toString();

            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, msg, e);
            }

            throw new ServiceException(e, msg, "GETMAP validator");
        }
    }

    public GetMapRequest createRequest() {
        GetMapRequest request = new GetMapRequest();
        return request;
    }
}
