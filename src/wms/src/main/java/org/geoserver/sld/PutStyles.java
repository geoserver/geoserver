/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyleFactoryFinder;
import org.vfny.geoserver.Response;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.vfny.geoserver.servlets.AbstractService;
import org.vfny.geoserver.util.SLDValidator;
import org.vfny.geoserver.util.requests.readers.KvpRequestReader;
import org.vfny.geoserver.util.requests.readers.XmlRequestReader;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class PutStyles extends AbstractService {
    private static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.sld.servlets");
    public final String success_mime_type = "application/vnd.ogc.success+xml";
    private static final StyleFactory styleFactory = StyleFactoryFinder.createStyleFactory();

    public PutStyles(WMS config) {
        super("WMS", "PutStyles", config.getServiceInfo());
    }

    protected boolean isServiceEnabled(HttpServletRequest req) {
        return true;
    }

    protected Response getResponseHandler() {
        throw new UnsupportedOperationException("not implemented");
    }

    protected KvpRequestReader getKvpReader(Map params) {
        return new PutStylesKvpReader(params,(WMS) getServiceRef());
    }

    protected XmlRequestReader getXmlRequestReader() {
        /**
        * @todo Implement this org.vfny.geoserver.servlets.AbstractService
        *       abstract method
        */
        throw new java.lang.UnsupportedOperationException(
            "Method getXmlRequestReader() not yet implemented.");
    }

    /**
     * doGet:
     *
     *
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        LOGGER.info("PutStyles.doGet()");

        Map requestParams = new HashMap();
        String paramName;
        String paramValue;

        // gather the parameters
        for (Enumeration pnames = request.getParameterNames(); pnames.hasMoreElements();) {
            paramName = (String) pnames.nextElement();
            paramValue = request.getParameter(paramName);
            requestParams.put(paramName.toUpperCase(), paramValue);
        }

        PutStylesKvpReader requestReader = new PutStylesKvpReader(requestParams, (WMS) getServiceRef());

        PutStylesRequest serviceRequest; // the request object we will deal with

        try {
            serviceRequest = (PutStylesRequest) requestReader.getRequest(request);
        } catch (ServiceException e) {
            e.printStackTrace();

            return;
        }

        ServletContext context = request.getSession().getServletContext();

        try {
            processSLD(serviceRequest, request, response, context);
        } catch (SldException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * doPost:
     *
     *
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        LOGGER.fine("PutStyles POST");
        File temp = null;
        
        try {
            Reader requestXml = new BufferedReader(XmlCharsetDetector.getCharsetAwareReader(
                            request.getInputStream()));
    
            temp = File.createTempFile("putStylesPost", "xml");
    
            FileOutputStream fos = new FileOutputStream(temp);
            BufferedOutputStream out = new BufferedOutputStream(fos);
            StringBuffer sb = new StringBuffer();
    
            if (requestXml == null) {
                throw new NullPointerException();
            }
    
            int c;
    
            while (-1 != (c = requestXml.read())) {
                char chr = (char) c;
                out.write(c);
                sb.append(chr);
            }
    
            requestXml.close();
            out.flush();
            out.close();
            requestXml = new BufferedReader(new FileReader(temp)); // pretend like nothing has happened
    
            PutStylesRequest serviceRequest = new PutStylesRequest((WMS) getServiceRef());
            serviceRequest.setSldBody(sb.toString()); // save the SLD body in the request object
    
            ServletContext context = request.getSession().getServletContext();

            processSLD(serviceRequest, request, response, context);
        } catch (SldException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            if(temp != null)
                temp.delete();
        }
    }

    private Node generateDOM(Reader reader) {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory
            .newInstance();

        dbf.setExpandEntityReferences(false);
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);

        javax.xml.parsers.DocumentBuilder db;
        Node rootNode = null;

        try {
            db = dbf.newDocumentBuilder();

            InputSource input = new InputSource(reader);
            org.w3c.dom.Document dom = db.parse(input);

            rootNode = dom.getDocumentElement();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rootNode;
    }

    /**
    * Give a node and the name of a child of that node, return it. This doesnt
    * do anything complex.
    *
    * @param parentNode
    * @param wantedChildName
    *
    * @return
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
     * Convenience method to get the value from the specified node.
     *
     * @param node
     * @return
     */
    public String getNodeValue(Node node) {
        return node.getChildNodes().item(0).getNodeValue();
    }

    /**
     * Give a node and the name of a child of that node, find its (string)
     * value. This doesnt do anything complex.
     *
     * @param parentNode
     * @param wantedChildName
     *
     * @return
     */
    public String getNodeChildValue(Node parentNode, String wantedChildName) {
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

    /**
     * returns true if this node is named "name".  Ignores case and namespaces.
     *
     * @param n
     * @param name
     *
     * @return
     */
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
     * processSLD:
     *
     * Makes the SLD into a DOM object and validates it.
     * It will then get the layer names and update for each layer.
     *
     * @param sld
     * @param rootNode the root node of the DOM document for parsing
     * @param response
     * @throws IOException
     * @throws WmsException
     */
    private void processSLD(PutStylesRequest serviceRequest, HttpServletRequest request,
        HttpServletResponse response, ServletContext context)
        throws IOException, SldException {
        LOGGER.info("Processing SLD");

        String sld_remote = serviceRequest.getSLD();

        if ((sld_remote != null) && !sld_remote.equals("")) {
            throw new java.lang.UnsupportedOperationException(
                "SLD= param not yet implemented. Use SLD_BODY=");
        }

        String sld_body = serviceRequest.getSldBody(); // the actual SLD body

        if ((sld_body == null) || (sld_body == "")) {
            throw new IllegalArgumentException("The body of the SLD cannot be empty!");
        }

        // write out SLD so we can read it in and validate it
        File temp = null;
        Node rootNode = null;
        try {
            temp = File.createTempFile("putStyles", "xml");
    
            FileOutputStream fos = new FileOutputStream(temp);
            BufferedOutputStream tempOut = new BufferedOutputStream(fos);
    
            byte[] bytes = sld_body.getBytes();
    
            for (int i = 0; i < bytes.length; i++) {
                tempOut.write(bytes[i]);
            }
    
            tempOut.flush();
            tempOut.close();
    
            BufferedInputStream fs = new BufferedInputStream(new FileInputStream(temp));
    
            // finish making our tempory file stream (for SLD validation)
            CharArrayReader xml = new CharArrayReader(sld_body.toCharArray()); // put the xml into a 'Reader'
    
            rootNode = generateDOM(xml);
    
            // validate the SLD
            SLDValidator validator = new SLDValidator();
            String baseURL = ResponseUtils.baseURL(request);
            List errors = validator.validateSLD(fs, baseURL);
    
            if (errors.size() != 0) {
                throw new SldException(SLDValidator.getErrorMessage(xml, errors));
            }
        } finally {
            if(temp != null)
                temp.delete();
        }

        Node n_namedLayer = getNode(rootNode, "NamedLayer");
        Node n_layerName = getNode(n_namedLayer, "Name");
        Node n_userStyle = getNode(n_namedLayer, "UserStyle");
        Node n_styleName = getNode(n_userStyle, "Name");

        // "ftname_styleLayerName": ignore "_style"
        String layerName = getNodeValue(n_layerName); //.split("_styleLayerName")[0];
        String styleName = getNodeValue(n_styleName);
        LOGGER.info("PutStyles SLD:\nLayer: " + layerName + ", style: " + styleName);

        // store the SLD
        StyleInfoImpl style = new StyleInfoImpl(getCatalog());
        style.setId(styleName);

        // make the SLD file in the data_dir/styles directory
        File data_dir = GeoserverDataDirectory.getGeoserverDataDirectory();
        File style_dir;

        try {
            style_dir = GeoserverDataDirectory.findConfigDir(data_dir, "styles");
        } catch (ConfigurationException cfe) {
            LOGGER.warning("no style dir found, creating new one");
            //if for some bizarre reason we don't fine the dir, make a new one.
            style_dir = new File(data_dir, "styles");
        }

        File styleFile = new File(style_dir, styleName + ".sld"); // styleName.sld
                                                                  //styleFile.createNewFile();

        LOGGER.info("Saving new SLD file to " + styleFile.getPath());

        // populate it with the style code
        StringBuffer sldText = new StringBuffer();
        sldText.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sldText.append("<StyledLayerDescriptor version=\"1.0.0\"\n");
        sldText.append(
            "	xsi:schemaLocation=\"http://www.opengis.net/sld StyledLayerDescriptor.xsd\"\n");
        sldText.append(
            "	xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"\n");
        sldText.append("	xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n");
        sldText.append("	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        FileOutputStream style_fos = new FileOutputStream(styleFile); // save the sld to a file

        String sldBody = serviceRequest.getSldBody();
        int start = sldBody.indexOf("<NamedLayer>");
        int end = sldBody.indexOf("</NamedLayer>");

        sldText.append(sldBody.substring(start, end));
        sldText.append("</NamedLayer>\n");
        sldText.append("</StyledLayerDescriptor>");
        style_fos.write(sldText.toString().getBytes());
        style_fos.flush();
        style_fos.close();

        style.setFilename(styleFile.getAbsolutePath());

        // update the data config to tell it about our new style
        getCatalog().add(style);

        // SLD is set up now, so tell the feature type to use it

        // get our featureType by the layerName
        List<LayerInfo> keys = getCatalog().getLayers();
        Iterator<LayerInfo> it = keys.iterator();
        layerName = null;

        while (it.hasNext()) // get the full featureType name that has the datastore prefix
         {
            String o = it.next().getName();
            String[] os = o.split(":");

            String name = os.length == 1 ? os[0] : os[1];
            if (name.equalsIgnoreCase(layerName)) {
                layerName = o;

                break;
            }
        }

        // get the feature type and save the style for it, if the feature type exists yet
        // If there is no FT there that may mean that the user is just creating it.
        if (layerName != null) {
            LayerInfo featureTypeConfig = getCatalog().getLayerByName(layerName);
            featureTypeConfig.setDefaultStyle(style);
        }

        // if successful, return "success"
        //response.setContentType(success_mime_type);
        LOGGER.info("sending back result");

        String message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<sld:success>success</sld:success>";
        BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
        byte[] msg = message.getBytes();
        out.write(msg);
        out.flush();
    }
}
