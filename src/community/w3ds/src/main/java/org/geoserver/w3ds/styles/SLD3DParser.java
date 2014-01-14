package org.geoserver.w3ds.styles;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.ExpressionDOMParser;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.ContrastEnhancementImpl;
import org.geotools.styling.Displacement;
import org.geotools.styling.Extent;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeConstraintImpl;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.LabelPlacement;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedLayerImpl;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.OtherText;
import org.geotools.styling.OtherTextImpl;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.RemoteOWS;
import org.geotools.styling.RemoteOWSImpl;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDInlineFeatureParser;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.SelectedChannelTypeImpl;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.ShadedReliefImpl;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizer2;
import org.geotools.styling.UomOgcMapping;
import org.geotools.styling.UserLayer;
import org.geotools.styling.UserLayerImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SLD3DParser {

	private static final java.util.logging.Logger LOGGER = org.geotools.util.logging.Logging
			.getLogger("org.geotools.styling");

	private static final String channelSelectionString = "ChannelSelection";

	private static final String graphicSt = "Graphic"; // to make pmd to shut up

	private static final String geomString = "Geometry"; // to make pmd to shut
															// up

	private static final String fillSt = "Fill";

	private static final String opacityString = "Opacity";

	private static final String overlapBehaviorString = "OverlapBehavior";

	private static final String colorMapString = "ColorMap";

	private static final String colorMapOpacityString = "opacity";

	private static final String colorMapColorString = "color";

	private static final String contrastEnhancementString = "ContrastEnhancement";

	private static final String shadedReliefString = "ShadedRelief";

	private static final String imageOutlineString = "ImageOutline";

	private static final String colorMapQuantityString = "quantity";

	private static final String colorMapLabelString = "label";

	private static final String strokeString = "Stroke";

	private static final String uomString = "uom";

	private static final String VendorOptionString = "VendorOption";

	private static final Pattern WHITESPACES = Pattern.compile("\\s+",
			Pattern.MULTILINE);
	private static final Pattern LEADING_WHITESPACES = Pattern.compile("^\\s+");
	private static final Pattern TRAILING_WHITESPACES = Pattern
			.compile("\\s+$");

	private FilterFactory ff;

	// protected java.io.InputStream instream;
	protected InputSource source;

	private org.w3c.dom.Document dom;

	protected StyleFactory factory;

	/** useful for detecting relative onlineresources */
	private URL sourceUrl;

	/**
	 * Create a Stylereader - use if you already have a dom to parse.
	 * 
	 * @param factory
	 *            The StyleFactory to use to build the style
	 */
	public SLD3DParser(StyleFactory factory) {
		this(factory, CommonFactoryFinder.getFilterFactory(GeoTools
				.getDefaultHints()));
	}

	public SLD3DParser(StyleFactory factory, FilterFactory filterFactory) {
		this.factory = factory;
		this.ff = filterFactory;
	}

	/**
	 * Creates a new instance of SLDStyler
	 * 
	 * @param factory
	 *            The StyleFactory to use to read the file
	 * @param filename
	 *            The file to be read.
	 * 
	 * @throws java.io.FileNotFoundException
	 *             - if the file is missing
	 */
	public SLD3DParser(StyleFactory factory, String filename)
			throws java.io.FileNotFoundException {
		this(factory);

		File f = new File(filename);
		setInput(f);
	}

	/**
	 * Creates a new SLDStyle object.
	 * 
	 * @param factory
	 *            The StyleFactory to use to read the file
	 * @param f
	 *            the File to be read
	 * 
	 * @throws java.io.FileNotFoundException
	 *             - if the file is missing
	 */
	public SLD3DParser(StyleFactory factory, File f)
			throws java.io.FileNotFoundException {
		this(factory);
		setInput(f);
	}

	/**
	 * Creates a new SLDStyle object.
	 * 
	 * @param factory
	 *            The StyleFactory to use to read the file
	 * @param url
	 *            the URL to be read.
	 * 
	 * @throws java.io.IOException
	 *             - if something goes wrong reading the file
	 */
	public SLD3DParser(StyleFactory factory, java.net.URL url)
			throws java.io.IOException {
		this(factory);
		setInput(url);
	}

	/**
	 * Creates a new SLDStyle object.
	 * 
	 * @param factory
	 *            The StyleFactory to use to read the file
	 * @param s
	 *            The inputstream to be read
	 */
	public SLD3DParser(StyleFactory factory, java.io.InputStream s) {
		this(factory);
		setInput(s);
	}

	/**
	 * Creates a new SLDStyle object.
	 * 
	 * @param factory
	 *            The StyleFactory to use to read the file
	 * @param r
	 *            The inputstream to be read
	 */
	public SLD3DParser(StyleFactory factory, java.io.Reader r) {
		this(factory);
		setInput(r);
	}

	/**
	 * set the file to read the SLD from
	 * 
	 * @param filename
	 *            the file to read the SLD from
	 * 
	 * @throws java.io.FileNotFoundException
	 *             if the file is missing
	 */
	public void setInput(String filename) throws java.io.FileNotFoundException {
		File f = new File(filename);
		source = new InputSource(new java.io.FileInputStream(f));
		try {
			sourceUrl = f.toURI().toURL();
		} catch (MalformedURLException e) {
			LOGGER.warning("Can't build URL for file " + f.getAbsolutePath());
		}
	}

	/**
	 * Sets the file to use to read the SLD from
	 * 
	 * @param f
	 *            the file to use
	 * 
	 * @throws java.io.FileNotFoundException
	 *             if the file is missing
	 */
	public void setInput(File f) throws java.io.FileNotFoundException {
		source = new InputSource(new java.io.FileInputStream(f));
		try {
			sourceUrl = f.toURI().toURL();
		} catch (MalformedURLException e) {
			LOGGER.warning("Can't build URL for file " + f.getAbsolutePath());
		}
	}

	/**
	 * sets an URL to read the SLD from
	 * 
	 * @param url
	 *            the url to read the SLD from
	 * 
	 * @throws java.io.IOException
	 *             If anything goes wrong opening the url
	 */
	public void setInput(java.net.URL url) throws java.io.IOException {
		source = new InputSource(url.openStream());
		sourceUrl = url;
	}

	/**
	 * Sets the input stream to read the SLD from
	 * 
	 * @param in
	 *            the inputstream used to read the SLD from
	 */
	public void setInput(java.io.InputStream in) {
		source = new InputSource(in);
	}

	/**
	 * Sets the input stream to read the SLD from
	 * 
	 * @param in
	 *            the inputstream used to read the SLD from
	 */
	public void setInput(java.io.Reader in) {
		source = new InputSource(in);
	}

	/**
	 * Read the xml inputsource provided and create a Style object for each user
	 * style found
	 * 
	 * @return Style[] the styles constructed.
	 * 
	 * @throws RuntimeException
	 *             if a parsing error occurs
	 */
	public Style[] readXML() {
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory
				.newInstance();
		dbf.setNamespaceAware(true);
		try {
			javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(source);
		} catch (javax.xml.parsers.ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		} catch (org.xml.sax.SAXException se) {
			throw new RuntimeException(se);
		} catch (java.io.IOException ie) {
			throw new RuntimeException(ie);
		}

		return readDOM(dom);
	}

	/**
	 * Read styles from the dom that was previously parsed.
	 */
	public Style[] readDOM() {
		if (dom == null) {
			throw new NullPointerException("dom is null");
		}
		return readDOM(dom);
	}

	/**
	 * Read the DOM provided and create a Style object for each user style found
	 * 
	 * @param document
	 *            a dom containing the SLD
	 * 
	 * @return Style[] the styles constructed.
	 */
	public Style[] readDOM(org.w3c.dom.Document document) {
		this.dom = document;

		// for our next trick do something with the dom.
		NodeList nodes = findElements(document, "UserStyle");
		final int length = nodes.getLength();

		if (nodes == null)
			return new Style[0];

		Style[] styles = new Style[length];

		for (int i = 0; i < length; i++) {
			styles[i] = parseStyle(nodes.item(i));
		}

		return styles;
	}

	/**
	 * @param document
	 * @param name
	 */
	private NodeList findElements(final org.w3c.dom.Document document,
			final String name) {
		NodeList nodes = document.getElementsByTagNameNS("*", name);

		if (nodes.getLength() == 0) {
			nodes = document.getElementsByTagName(name);
		}

		return nodes;
	}

	private NodeList findElements(final org.w3c.dom.Element element,
			final String name) {
		NodeList nodes = element.getElementsByTagNameNS("*", name);

		if (nodes.getLength() == 0) {
			nodes = element.getElementsByTagName(name);
		}

		return nodes;
	}

	public StyledLayerDescriptor parseSLD() {
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory
				.newInstance();
		dbf.setNamespaceAware(true);

		try {
			javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(source);
			// for our next trick do something with the dom.

			// NodeList nodes = findElements(dom, "StyledLayerDescriptor");

			StyledLayerDescriptor sld = parseDescriptor(dom
					.getDocumentElement());// should only be
											// one per file
			return sld;

		} catch (javax.xml.parsers.ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		} catch (org.xml.sax.SAXException se) {
			throw new RuntimeException(se);
		} catch (java.io.IOException ie) {
			throw new RuntimeException(ie);
		}
	}

	public StyledLayerDescriptor parseDescriptor(Node root) {
		StyledLayerDescriptor sld = factory.createStyledLayerDescriptor();
		// StyledLayer layer = null;
		// LineSymbolizer symbol = factory.createLineSymbolizer();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();

		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("Name")) {
				sld.setName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Title")) {
				sld.setTitle(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Abstract")) {
				sld.setAbstract(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("NamedLayer")) {
				NamedLayer layer = parseNamedLayer(child);
				sld.addStyledLayer(layer);
			} else if (childName.equalsIgnoreCase("UserLayer")) {
				StyledLayer layer = parseUserLayer(child);
				sld.addStyledLayer(layer);
			}
		}

		return sld;
	}

	/**
	 * Returns the first child node value, or null if there is no child
	 * 
	 * @param child
	 * @return
	 */
	String getFirstChildValue(Node child) {
		if (child.getFirstChild() != null)
			return child.getFirstChild().getNodeValue();
		else
			return null;
	}

	private StyledLayer parseUserLayer(Node root) {
		UserLayer layer = new UserLayerImpl();
		// LineSymbolizer symbol = factory.createLineSymbolizer();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("InlineFeature")) {
				parseInlineFeature(child, layer);
			} else if (childName.equalsIgnoreCase("UserStyle")) {
				Style user = parseStyle(child);
				layer.addUserStyle(user);
			} else if (childName.equalsIgnoreCase("Name")) {
				String layerName = getFirstChildValue(child);
				layer.setName(layerName);
				if (LOGGER.isLoggable(Level.INFO))
					LOGGER.info("layer name: " + layer.getName());
			} else if (childName.equalsIgnoreCase("RemoteOWS")) {
				RemoteOWS remoteOws = parseRemoteOWS(child);
				layer.setRemoteOWS(remoteOws);
			} else if (childName.equalsIgnoreCase("LayerFeatureConstraints")) {
				layer.setLayerFeatureConstraints(parseLayerFeatureConstraints(child));
			}

		}

		return layer;
	}

	private FeatureTypeConstraint[] parseLayerFeatureConstraints(Node root) {
		List<FeatureTypeConstraint> featureTypeConstraints = new ArrayList<FeatureTypeConstraint>();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName.equalsIgnoreCase("FeatureTypeConstraint")) {
				final FeatureTypeConstraint ftc = parseFeatureTypeConstraint(child);
				if (ftc != null)
					featureTypeConstraints.add(ftc);
			}
		}
		return (FeatureTypeConstraint[]) featureTypeConstraints
				.toArray(new FeatureTypeConstraint[featureTypeConstraints
						.size()]);
	}

	protected FeatureTypeConstraint parseFeatureTypeConstraint(Node root) {
		FeatureTypeConstraint ftc = new FeatureTypeConstraintImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName.equalsIgnoreCase("FeatureTypeName")) {
				ftc.setFeatureTypeName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Filter")) {
				ftc.setFilter(parseFilter(child));
			}
		}
		ftc.setExtents(new Extent[0]);
		if (ftc.getFeatureTypeName() == null)
			return null;
		else
			return ftc;
	}

	protected RemoteOWS parseRemoteOWS(Node root) {
		RemoteOWS ows = new RemoteOWSImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();

			if (childName.equalsIgnoreCase("Service")) {
				ows.setService(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("OnlineResource")) {
				ows.setOnlineResource(parseOnlineResource(child));
			}
		}
		return ows;
	}

	/**
	 * 
	 * @param child
	 * @param layer
	 */
	private void parseInlineFeature(Node root, UserLayer layer) {
		try {
			SLDInlineFeatureParser inparser = new SLDInlineFeatureParser(root);
			layer.setInlineFeatureDatastore(inparser.dataStore);
			layer.setInlineFeatureType(inparser.featureType);
		} catch (Exception e) {
			throw (IllegalArgumentException) new IllegalArgumentException()
					.initCause(e);
		}

	}

	/**
	 * Parses a NamedLayer.
	 * <p>
	 * The NamedLayer schema is:
	 * 
	 * <pre>
	 * &lt;code&gt;
	 * &lt;xsd:element name=&quot;NamedLayer&quot;&gt;
	 *  &lt;xsd:annotation&gt;
	 *   &lt;xsd:documentation&gt; A NamedLayer is a layer of data that has a name advertised by a WMS. &lt;/xsd:documentation&gt;
	 *  &lt;/xsd:annotation&gt;
	 *  &lt;xsd:complexType&gt;
	 *   &lt;xsd:sequence&gt;
	 *    &lt;xsd:element ref=&quot;sld:Name&quot;/&gt;
	 *    &lt;xsd:element ref=&quot;sld:LayerFeatureConstraints&quot; minOccurs=&quot;0&quot;/&gt;
	 *    &lt;xsd:choice minOccurs=&quot;0&quot; maxOccurs=&quot;unbounded&quot;&gt;
	 *     &lt;xsd:element ref=&quot;sld:NamedStyle&quot;/&gt;
	 *     &lt;xsd:element ref=&quot;sld:UserStyle&quot;/&gt;
	 *    &lt;/xsd:choice&gt;
	 *   &lt;/xsd:sequence&gt;
	 *  &lt;/xsd:complexType&gt;
	 * &lt;/xsd:element&gt;
	 * &lt;/code&gt;
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param root
	 */
	private NamedLayer parseNamedLayer(Node root) {
		NamedLayer layer = new NamedLayerImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("Name")) {
				layer.setName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("NamedStyle")) {
				NamedStyle style = parseNamedStyle(child);
				layer.addStyle(style);
			} else if (childName.equalsIgnoreCase("UserStyle")) {
				Style user = parseStyle(child);
				layer.addStyle(user);
			} else if (childName.equalsIgnoreCase("LayerFeatureConstraints")) {
				layer.setLayerFeatureConstraints(parseLayerFeatureConstraints(child));
			}
		}

		return layer;
	}

	/**
	 * Parses a NamedStyle from node.
	 * <p>
	 * A NamedStyle is used to refer to a style that has a name in a WMS, and is
	 * defined as:
	 * 
	 * <pre>
	 * &lt;code&gt;
	 * &lt;xsd:element name=&quot;NamedStyle&quot;&gt;
	 *  &lt;xsd:annotation&gt;
	 *   &lt;xsd:documentation&gt; A NamedStyle is used to refer to a style that has a name in a WMS. &lt;/xsd:documentation&gt;
	 *  &lt;/xsd:annotation&gt;
	 *  &lt;xsd:complexType&gt;
	 *   &lt;xsd:sequence&gt;
	 *    &lt;xsd:element ref=&quot;sld:Name&quot;/&gt;
	 *   &lt;/xsd:sequence&gt;
	 *  &lt;/xsd:complexType&gt;
	 * &lt;/xsd:element&gt;
	 * &lt;/code&gt;
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param n
	 */
	public NamedStyle parseNamedStyle(Node n) {
		if (dom == null) {
			try {
				javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory
						.newInstance();
				javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
				dom = db.newDocument();
			} catch (javax.xml.parsers.ParserConfigurationException pce) {
				throw new RuntimeException(pce);
			}
		}

		NamedStyle style = factory.createNamedStyle();

		NodeList children = n.getChildNodes();
		final int length = children.getLength();
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("" + children.getLength() + " children to process");
		}

		for (int j = 0; j < length; j++) {
			Node child = children.item(j);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)
					|| (child.getFirstChild() == null)) {
				continue;
			}
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("processing " + child.getLocalName());
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("Name")) {
				style.setName(getFirstChildValue(child));
			}
		}
		return style;
	}

	/**
	 * build a style for the Node provided
	 * 
	 * @param n
	 *            the node which contains the style to be parsed.
	 * 
	 * @return the Style constructed.
	 * 
	 * @throws RuntimeException
	 *             if an error occurs setting up the parser
	 */
	public Style parseStyle(Node n) {
		if (dom == null) {
			try {
				javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory
						.newInstance();
				javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
				dom = db.newDocument();
			} catch (javax.xml.parsers.ParserConfigurationException pce) {
				throw new RuntimeException(pce);
			}
		}

		Style style = factory.createStyle();

		NodeList children = n.getChildNodes();
		final int length = children.getLength();
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("" + children.getLength() + " children to process");
		}

		for (int j = 0; j < length; j++) {
			Node child = children.item(j);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)
					|| (child.getFirstChild() == null)) {
				continue;
			}
			// System.out.println("The child is: " + child.getNodeName() + " or
			// " + child.getLocalName() + " prefix is " +child.getPrefix());
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("processing " + child.getLocalName());
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			String firstChildValue = getFirstChildValue(child);
			if (childName.equalsIgnoreCase("Name")) {
				style.setName(firstChildValue);
			} else if (childName.equalsIgnoreCase("Title")) {
				style.setTitle(firstChildValue);
			} else if (childName.equalsIgnoreCase("Abstract")) {
				style.setAbstract(firstChildValue);
			} else if (childName.equalsIgnoreCase("IsDefault")) {
				if ("1".equals(firstChildValue)) {
					style.setDefault(true);
				} else {
					style.setDefault(Boolean.valueOf(firstChildValue)
							.booleanValue());
				}
			} else if (childName.equalsIgnoreCase("FeatureTypeStyle")) {
				style.addFeatureTypeStyle(parseFeatureTypeStyle(child));
			}
		}

		return style;
	}

	/** Internal parse method - made protected for unit testing */
	protected FeatureTypeStyle parseFeatureTypeStyle(Node style) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("Parsing featuretype style " + style.getLocalName());
		}

		FeatureTypeStyle ft = factory.createFeatureTypeStyle();
		ArrayList<Rule> rules = new ArrayList<Rule>();
		ArrayList<String> sti = new ArrayList<String>();
		NodeList children = style.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}

			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("processing " + child.getLocalName());
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("Name")) {
				ft.setName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Title")) {
				ft.setTitle(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Abstract")) {
				ft.setAbstract(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("FeatureTypeName")) {
				ft.setFeatureTypeName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("SemanticTypeIdentifier")) {
				sti.add(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Rule")) {
				rules.add(parseRule(child));
			} else if (childName.equalsIgnoreCase("Transformation")) {
				ExpressionDOMParser parser = new ExpressionDOMParser(
						CommonFactoryFinder.getFilterFactory2(null));
				Expression tx = parser.expression(getFirstNonTextChild(child));
				ft.setTransformation(tx);
			}
		}

		if (sti.size() > 0) {
			ft.setSemanticTypeIdentifiers((String[]) sti.toArray(new String[0]));
		}
		ft.setRules((Rule[]) rules.toArray(new Rule[0]));

		return ft;
	}

	private Node getFirstNonTextChild(Node node) {
		NodeList children = node.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}

			return child;
		}

		return null;
	}

	/** Internal parse method - made protected for unit testing */
	protected Rule parseRule(Node ruleNode) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("Parsing rule " + ruleNode.getLocalName());
		}

		Rule rule = factory.createRule();
		List<Symbolizer> symbolizers = new ArrayList<Symbolizer>();
		NodeList children = ruleNode.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.indexOf(':') != -1) {
				// the DOM parser wasnt properly set to handle namespaces...
				childName = childName.substring(childName.indexOf(':') + 1);
			}

			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("processing " + child.getLocalName());
			}

			if (childName.equalsIgnoreCase("Name")) {
				rule.setName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Title")) {
				rule.setTitle(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("Abstract")) {
				rule.setAbstract(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("MinScaleDenominator")) {
				rule.setMinScaleDenominator(Double
						.parseDouble(getFirstChildValue(child)));
			} else if (childName.equalsIgnoreCase("MaxScaleDenominator")) {
				rule.setMaxScaleDenominator(Double
						.parseDouble(getFirstChildValue(child)));
			} else if (childName.equalsIgnoreCase("Filter")) {
				Filter filter = parseFilter(child);
				rule.setFilter(filter);
			} else if (childName.equalsIgnoreCase("ElseFilter")) {
				rule.setElseFilter(true);
			} else if (childName.equalsIgnoreCase("LegendGraphic")) {
				findElements(((Element) child), graphicSt);
				NodeList g = findElements(((Element) child), graphicSt);
				List<Graphic> legends = new ArrayList<Graphic>();
				final int l = g.getLength();
				for (int k = 0; k < l; k++) {
					legends.add(parseGraphic(g.item(k)));
				}

				rule.setLegendGraphic((Graphic[]) legends
						.toArray(new Graphic[0]));
			} else if (childName.equalsIgnoreCase("LineSymbolizer")) {
				symbolizers.add(parseLineSymbolizer(child));
			} else if (childName.equalsIgnoreCase("PolygonSymbolizer")) {
				symbolizers.add(parsePolygonSymbolizer(child));
			} else if (childName.equalsIgnoreCase("PointSymbolizer")) {
				symbolizers.add(parsePointSymbolizer(child));
			} else if (childName.equalsIgnoreCase("TextSymbolizer")) {
				symbolizers.add(parseTextSymbolizer(child));
			} else if (childName.equalsIgnoreCase("RasterSymbolizer")) {
				symbolizers.add(parseRasterSymbolizer(child));
			}
		}

		rule.setSymbolizers((Symbolizer[]) symbolizers
				.toArray(new Symbolizer[0]));

		return rule;
	}

	/** Internal parse method - made protected for unit testing */
	protected Filter parseFilter(Node child) {
		// this sounds stark raving mad, but this is actually how the dom parser
		// works...
		// instead of passing in the parent element, pass in the first child and
		// its
		// siblings will also be parsed
		Node firstChild = child.getFirstChild();
		while (firstChild != null
				&& firstChild.getNodeType() != Node.ELEMENT_NODE) {
			// advance to the first actual element (rather than whitespace)
			firstChild = firstChild.getNextSibling();
		}
		Filter filter = org.geotools.filter.FilterDOMParser
				.parseFilter(firstChild);
		return filter;
	}

	/**
	 * parses the SLD for a linesymbolizer
	 * 
	 * @param root
	 *            a w2c Dom Node
	 * 
	 * @return the linesymbolizer
	 */
	protected LineSymbolizer parseLineSymbolizer(Node root) {
		LineSymbolizer symbol = factory.createLineSymbolizer();

		NamedNodeMap namedNodeMap = root.getAttributes();
		Node uomNode = namedNodeMap.getNamedItem(uomString);
		if (uomNode != null) {
			UomOgcMapping uomMapping = UomOgcMapping
					.get(uomNode.getNodeValue());
			symbol.setUnitOfMeasure(uomMapping.getUnit());
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase(geomString)) {
				symbol.setGeometry(parseGeometry(child));
			} else if (childName.equalsIgnoreCase(strokeString)) {
				symbol.setStroke(parseStroke(child));
			} else if (childName.equalsIgnoreCase(VendorOptionString)) {
				parseVendorOption(symbol, child);
			}
		}

		return symbol;
	}

	/**
	 * parses the SLD for a polygonsymbolizer
	 * 
	 * @param root
	 *            w3c dom node
	 * 
	 * @return the polygon symbolizer
	 */
	protected PolygonSymbolizer parsePolygonSymbolizer(Node root) {
		PolygonSymbolizer symbol = new PolygonSymbolizerImpl3D();
		symbol.setFill((Fill) null);
		symbol.setStroke((Stroke) null);

		NamedNodeMap namedNodeMap = root.getAttributes();
		Node uomNode = namedNodeMap.getNamedItem(uomString);
		if (uomNode != null) {
			UomOgcMapping uomMapping = UomOgcMapping
					.get(uomNode.getNodeValue());
			symbol.setUnitOfMeasure(uomMapping.getUnit());
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase(geomString)) {
				symbol.setGeometry(parseGeometry(child));
			} else if (childName.equalsIgnoreCase(strokeString)) {
				symbol.setStroke(parseStroke(child));
			} else if (childName.equalsIgnoreCase(fillSt)) {
				Fill fill = parseFill(child);
				symbol.setFill(parseFill(child));
			} else if (childName.equalsIgnoreCase(VendorOptionString)) {
				parseVendorOption(symbol, child);
			}
		}
		return symbol;
	}

	/**
	 * parses the SLD for a text symbolizer
	 * 
	 * @param root
	 *            w3c dom node
	 * 
	 * @return the TextSymbolizer
	 */
	protected TextSymbolizer parseTextSymbolizer(Node root) {
		TextSymbolizer symbol = factory.createTextSymbolizer();
		symbol.setFill(null);

		NamedNodeMap namedNodeMap = root.getAttributes();
		Node uomNode = namedNodeMap.getNamedItem(uomString);
		if (uomNode != null) {
			UomOgcMapping uomMapping = UomOgcMapping
					.get(uomNode.getNodeValue());
			symbol.setUnitOfMeasure(uomMapping.getUnit());
		}

		List<Font> fonts = new ArrayList<Font>();
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase(geomString)) {
				symbol.setGeometry(parseGeometry(child));
			} else if (childName.equalsIgnoreCase(fillSt)) {
				symbol.setFill(parseFill(child));
			} else if (childName.equalsIgnoreCase("Label")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("parsing label " + child.getNodeValue());
				// the label parser should collapse whitespaces to one, so
				// we call parseCssParameter with trimWhiteSpace=false
				symbol.setLabel(parseCssParameter(child, false));
				if (symbol.getLabel() == null) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.warning("parsing TextSymbolizer node - couldnt find anything in the Label element!");
				}
			}

			if (childName.equalsIgnoreCase("Font")) {
				fonts.add(parseFont(child));
			} else if (childName.equalsIgnoreCase("LabelPlacement")) {
				symbol.setPlacement(parseLabelPlacement(child));
			} else if (childName.equalsIgnoreCase("Halo")) {
				symbol.setHalo(parseHalo(child));
			} else if (childName.equalsIgnoreCase("Graphic")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("Parsing non-standard Graphic in TextSymbolizer");
				if (symbol instanceof TextSymbolizer2) {
					((TextSymbolizer2) symbol).setGraphic(parseGraphic(child));
				}
			} else if (childName.equalsIgnoreCase("Snippet")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("Parsing non-standard Abstract in TextSymbolizer");
				if (symbol instanceof TextSymbolizer2)
					((TextSymbolizer2) symbol).setSnippet(parseCssParameter(
							child, false));
			} else if (childName.equalsIgnoreCase("FeatureDescription")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("Parsing non-standard Description in TextSymbolizer");
				if (symbol instanceof TextSymbolizer2)
					((TextSymbolizer2) symbol)
							.setFeatureDescription(parseCssParameter(child,
									false));
			} else if (childName.equalsIgnoreCase("OtherText")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("Parsing non-standard OtherText in TextSymbolizer");
				if (symbol instanceof TextSymbolizer2)
					((TextSymbolizer2) symbol)
							.setOtherText(parseOtherText(child));
			} else if (childName.equalsIgnoreCase("priority")) {
				symbol.setPriority(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase(VendorOptionString)) {
				parseVendorOption(symbol, child);
			}

		}

		symbol.setFonts((Font[]) fonts.toArray(new Font[0]));

		return symbol;
	}

	protected OtherText parseOtherText(Node root) {
		// TODO: add methods to the factory to create OtherText instances
		OtherText ot = new OtherTextImpl();
		final Node targetAttribute = root.getAttributes()
				.getNamedItem("target");
		if (targetAttribute == null)
			throw new IllegalArgumentException(
					"OtherLocation does not have the "
							+ "required 'target' attribute");
		String target = targetAttribute.getNodeValue();
		Expression text = parseCssParameter(root, true);
		ot.setTarget(target);
		ot.setText(text);
		return ot;
	}

	/**
	 * adds the key/value pair from the node
	 * ("<VendorOption name="...">...</VendorOption>"). This can be generalized
	 * for other symbolizers in the future
	 * 
	 * @param symbol
	 * @param child
	 */
	private void parseVendorOption(Symbolizer symbol, Node child) {
		String key = child.getAttributes().getNamedItem("name").getNodeValue();
		String value = getFirstChildValue(child);

		symbol.getOptions().put(key, value);
	}

	/**
	 * parses the SLD for a text symbolizer
	 * 
	 * @param root
	 *            w3c dom node
	 * 
	 * @return the TextSymbolizer
	 */
	protected RasterSymbolizer parseRasterSymbolizer(Node root) {
		final RasterSymbolizer symbol = factory.getDefaultRasterSymbolizer();

		NamedNodeMap namedNodeMap = root.getAttributes();
		Node uomNode = namedNodeMap.getNamedItem(uomString);
		if (uomNode != null) {
			UomOgcMapping uomMapping = UomOgcMapping
					.get(uomNode.getNodeValue());
			symbol.setUnitOfMeasure(uomMapping.getUnit());
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase(geomString)) {
				symbol.setGeometry(parseGeometry(child));
			}
			if (childName.equalsIgnoreCase(opacityString)) {
				try {
					final String opacityString = getFirstChildValue(child);
					Expression opacity = parseParameterValueExpression(child,
							false);
					symbol.setOpacity(opacity);
				} catch (Throwable e) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			} else if (childName.equalsIgnoreCase(channelSelectionString)) {
				symbol.setChannelSelection(parseChannelSelection(child));
			} else if (childName.equalsIgnoreCase(overlapBehaviorString)) {
				try {
					final String overlapString = child.getFirstChild()
							.getLocalName();
					symbol.setOverlap(ff.literal(overlapString));
				} catch (Throwable e) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			} else if (childName.equalsIgnoreCase(colorMapString)) {
				symbol.setColorMap(parseColorMap(child));
			} else if (childName.equalsIgnoreCase(contrastEnhancementString)) {
				symbol.setContrastEnhancement(parseContrastEnhancement(child));
			} else if (childName.equalsIgnoreCase(shadedReliefString)) {
				symbol.setShadedRelief(parseShadedRelief(child));
			} else if (childName.equalsIgnoreCase(imageOutlineString)) {
				symbol.setImageOutline(parseLineSymbolizer(child));
			}
		}

		return symbol;
	}

	/**
	 * Many elements in an SLD extends ParameterValueType (allowing for the
	 * definition of Expressions) - this method will try and produce an
	 * expression for the provided node.
	 * <p>
	 * As an example:
	 * <ul>
	 * <li>"sld:Opacity" is defined as a parameter value type:<br>
	 * &lt;sld:Opacity&gt;0.75&lt;\sld:Opacity&gt;
	 * <li>"sld:Label" is defined as a "mixed" parameter value type:<br>
	 * &lt;sld:Label&gt;Hello
	 * &lt;sld:PropertyName&gt;name&lt;\sld:PropertyName&gt;&lt;\sld:Label&gt;
	 * </ul>
	 * From the SLD 1.0 spec: "ParameterValueType" uses WFS-Filter expressions
	 * to give values for SLD graphic parameters. A "mixed" element-content
	 * model is used with textual substitution for values.
	 */
	Expression parseParameterValueExpression(Node root, boolean mixedText) {
		ExpressionDOMParser parser = new ExpressionDOMParser(
				(FilterFactory2) ff);
		Expression expr = parser.expression(root); // try the provided node
													// first
		if (expr != null)
			return expr;
		NodeList children = root.getChildNodes();
		// if there is only one CharacterData node - we can make a literal out
		// of it
		if (children.getLength() == 1
				&& root.getFirstChild() instanceof CharacterData) {
			Node textNode = root.getFirstChild();
			String text = textNode.getNodeValue();
			return ff.literal(text.trim());
		}
		List<Expression> expressionList = new ArrayList<Expression>();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child instanceof CharacterData) {
				if (mixedText) {
					String text = child.getNodeValue();
					Expression childExpr = ff.literal(text);
					expressionList.add(childExpr);
				}
			} else {
				Expression childExpr = parser.expression(child);
				if (childExpr != null) {
					expressionList.add(childExpr);
				}
			}
		}
		if (expressionList.isEmpty()) {
			return Expression.NIL;
		} else if (expressionList.size() == 1) {
			return expressionList.get(0);
		} else if (expressionList.size() == 2) {
			Expression[] expressionArray = expressionList
					.toArray(new Expression[0]);
			return ff.function("strConcat", expressionArray);
		} else {
			Expression[] expressionArray = expressionList
					.toArray(new Expression[0]);
			return ff.function("Concatenate", expressionArray);
		}
	}

	/** Internal parse method - made protected for unit testing */
	protected ColorMapEntry parseColorMapEntry(Node root) {
		ColorMapEntry symbol = factory.createColorMapEntry();
		NamedNodeMap atts = root.getAttributes();
		if (atts.getNamedItem(colorMapLabelString) != null) {
			symbol.setLabel(atts.getNamedItem(colorMapLabelString)
					.getNodeValue());
		}
		if (atts.getNamedItem(colorMapColorString) != null) {
			symbol.setColor(ff.literal(atts.getNamedItem(colorMapColorString)
					.getNodeValue()));
		}
		if (atts.getNamedItem(colorMapOpacityString) != null) {
			symbol.setOpacity(ff.literal(atts.getNamedItem(
					colorMapOpacityString).getNodeValue()));
		}
		if (atts.getNamedItem(colorMapQuantityString) != null) {
			symbol.setQuantity(ff.literal(atts.getNamedItem(
					colorMapQuantityString).getNodeValue()));
		}

		return symbol;
	}

	/** Internal parse method - made protected for unit testing */
	protected ColorMap parseColorMap(Node root) {
		ColorMap symbol = factory.createColorMap();

		if (root.hasAttributes()) {
			// parsing type attribute
			final NamedNodeMap atts = root.getAttributes();
			Node typeAtt = atts.getNamedItem("type");
			if (typeAtt != null) {
				final String type = typeAtt.getNodeValue();

				if ("ramp".equalsIgnoreCase(type)) {
					symbol.setType(ColorMap.TYPE_RAMP);
				} else if ("intervals".equalsIgnoreCase(type)) {
					symbol.setType(ColorMap.TYPE_INTERVALS);
				} else if ("values".equalsIgnoreCase(type)) {
					symbol.setType(ColorMap.TYPE_VALUES);
				} else if (LOGGER.isLoggable(Level.FINE))
					LOGGER.fine(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,
							"ColorMapType", type));

			}

			// parsing extended colors
			typeAtt = atts.getNamedItem("extended");
			if (typeAtt != null) {
				final String type = typeAtt.getNodeValue();

				if ("true".equalsIgnoreCase(type)) {
					symbol.setExtendedColors(true);
				} else if ("false".equalsIgnoreCase(type)) {
					symbol.setExtendedColors(false);
				} else if (LOGGER.isLoggable(Level.FINE))
					LOGGER.fine(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,
							"Extended", type));

			}
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("ColorMapEntry")) {
				symbol.addColorMapEntry(parseColorMapEntry(child));
			}
		}

		return symbol;
	}

	/** Internal parse method - made protected for unit testing */
	protected SelectedChannelType parseSelectedChannel(Node root) {
		SelectedChannelType symbol = new SelectedChannelTypeImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();

			if (childName == null) {
				childName = child.getNodeName();
			} else if (childName.equalsIgnoreCase("SourceChannelName")) {
				if (child.getFirstChild() != null
						&& child.getFirstChild().getNodeType() == Node.TEXT_NODE)
					symbol.setChannelName(getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("ContrastEnhancement")) {
				symbol.setContrastEnhancement(parseContrastEnhancement(child));

				/*
				 * try { if (child.getFirstChild() != null &&
				 * child.getFirstChild().getNodeType() == Node.TEXT_NODE)
				 * symbol.setContrastEnhancement((Expression) ExpressionBuilder
				 * .parse(child.getFirstChild().getNodeValue())); } catch
				 * (Exception e) { // TODO: handle exception }
				 */
			}
		}

		return symbol;
	}

	/** Internal parse method - made protected for unit testing */
	protected ChannelSelection parseChannelSelection(Node root) {
		List<SelectedChannelType> channels = new ArrayList<SelectedChannelType>();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			} else if (childName.equalsIgnoreCase("GrayChannel")) {
				channels.add(parseSelectedChannel(child));
			} else if (childName.equalsIgnoreCase("RedChannel")) {
				channels.add(parseSelectedChannel(child));
			} else if (childName.equalsIgnoreCase("GreenChannel")) {
				channels.add(parseSelectedChannel(child));
			} else if (childName.equalsIgnoreCase("BlueChannel")) {
				channels.add(parseSelectedChannel(child));
			}
		}

		ChannelSelection dap = factory
				.createChannelSelection((SelectedChannelType[]) channels
						.toArray(new SelectedChannelType[channels.size()]));

		return dap;
	}

	/** Internal parse method - made protected for unit testing */
	protected ContrastEnhancement parseContrastEnhancement(Node root) {
		ContrastEnhancement symbol = new ContrastEnhancementImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("Normalize")) {
				symbol.setNormalize();
			} else if (childName.equalsIgnoreCase("Histogram")) {
				symbol.setHistogram();
			} else if (childName.equalsIgnoreCase("Logarithmic")) {
				symbol.setLogarithmic();
			} else if (childName.equalsIgnoreCase("Exponential")) {
				symbol.setExponential();
			} else if (childName.equalsIgnoreCase("GammaValue")) {
				try {
					final String gammaString = getFirstChildValue(child);
					symbol.setGammaValue(ff.literal(Double
							.parseDouble(gammaString)));
				} catch (Exception e) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}

		return symbol;
	}

	/** Internal parse method - made protected for unit testing */
	protected ShadedRelief parseShadedRelief(Node root) {
		ShadedRelief symbol = new ShadedReliefImpl();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if ("BrightnessOnly".equalsIgnoreCase(childName)) {
				symbol.setBrightnessOnly(Boolean
						.getBoolean(getFirstChildValue(child)));
			} else if ("ReliefFactor".equalsIgnoreCase(childName)) {
				try {
					final String reliefString = getFirstChildValue(child);
					Expression relief = ExpressionDOMParser
							.parseExpression(child);
					symbol.setReliefFactor(relief);
				} catch (Exception e) {
					if (LOGGER.isLoggable(Level.WARNING))
						LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}

		return symbol;
	}

	/**
	 * parses the SLD for a point symbolizer
	 * 
	 * @param root
	 *            a w3c dom node
	 * 
	 * @return the pointsymbolizer
	 */
	protected PointSymbolizer parsePointSymbolizer(Node root) {
		PointSymbolizer symbol = factory.getDefaultPointSymbolizer();
		// symbol.setGraphic(null);

		NamedNodeMap namedNodeMap = root.getAttributes();
		Node uomNode = namedNodeMap.getNamedItem(uomString);
		if (uomNode != null) {
			UomOgcMapping uomMapping = UomOgcMapping
					.get(uomNode.getNodeValue());
			symbol.setUnitOfMeasure(uomMapping.getUnit());
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase(geomString)) {
				symbol.setGeometry(parseGeometry(child));
			} else if (childName.equalsIgnoreCase(graphicSt)) {
				symbol.setGraphic(parseGraphic(child));
			} else if (childName.equalsIgnoreCase(VendorOptionString)) {
				parseVendorOption(symbol, child);
			}
		}

		return symbol;
	}

	/** Internal parse method - made protected for unit testing */
	protected Graphic parseGraphic(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("processing graphic " + root);
		}

		Node isModel = root.getAttributes().getNamedItem("model");
		if (isModel != null) {
			if (isModel.getTextContent().equalsIgnoreCase("true")) {
				return parseModel(root);
			}
		}

		Graphic graphic = factory.getDefaultGraphic();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		boolean firstGraphic = true;
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("ExternalGraphic")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("parsing extgraphic " + child);
				if (firstGraphic) {
					graphic.graphicalSymbols().clear();
					firstGraphic = false;
				}
				graphic.graphicalSymbols().add(parseExternalGraphic(child));
			} else if (childName.equalsIgnoreCase("Mark")) {
				if (firstGraphic) {
					graphic.graphicalSymbols().clear();
					firstGraphic = false;
				}
				graphic.graphicalSymbols().add(parseMark(child));
			} else if (childName.equalsIgnoreCase(opacityString)) {
				graphic.setOpacity(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("size")) {
				graphic.setSize(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("displacement")) {
				graphic.setDisplacement(parseDisplacement(child));
			} else if (childName.equalsIgnoreCase("rotation")) {
				graphic.setRotation(parseCssParameter(child));
			}
		}

		return graphic;
	}

	/** Internal parse method - made protected for unit testing */
	protected String parseGeometryName(Node root) {
		Expression result = parseGeometry(root);
		if (result instanceof PropertyName) {
			return ((PropertyName) result).getPropertyName();
		}
		return null;
	}

	/** Internal parse method - made protected for unit testing */
	protected Expression parseGeometry(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("parsing GeometryExpression");
		}

		return parseCssParameter(root);
	}

	/** Internal parse method - made protected for unit testing */
	protected Mark parseMark(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing mark");
		}

		Mark mark = factory.createMark();
		mark.setFill(null);
		mark.setStroke(null);

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase(strokeString)) {
				mark.setStroke(parseStroke(child));
			} else if (childName.equalsIgnoreCase(fillSt)) {
				mark.setFill(parseFill(child));
			} else if (childName.equalsIgnoreCase("WellKnownName")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("setting mark to "
							+ getFirstChildValue(child));
				mark.setWellKnownName(parseCssParameter(child));
			}
		}

		return mark;
	}

	/** Internal parse method - made protected for unit testing */
	protected ExternalGraphic parseExternalGraphic(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("processing external graphic ");
		}

		String format = "";
		String uri = "";
		Map<String, Object> paramList = new HashMap<String, Object>();

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("OnLineResource")) {
				uri = parseOnlineResource(child);
			}

			if (childName.equalsIgnoreCase("format")) {
				LOGGER.finest("format child is " + child);
				LOGGER.finest("seting ExtGraph format "
						+ getFirstChildValue(child));
				format = (getFirstChildValue(child));
			} else if (childName.equalsIgnoreCase("customProperty")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("custom child is " + child);
				String propName = child.getAttributes().getNamedItem("name")
						.getNodeValue();
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("seting custom property " + propName + " to "
							+ getFirstChildValue(child));
				Expression value = parseCssParameter(child);
				paramList.put(propName, value);

			}
		}

		URL url = null;
		try {
			url = new URL(uri);
		} catch (MalformedURLException mfe) {
			LOGGER.fine("Looks like " + uri + " is a relative path..");
			if (sourceUrl != null) {
				try {
					url = new URL(sourceUrl, uri);
				} catch (MalformedURLException e) {
					LOGGER.warning("can't parse " + uri + " as relative to"
							+ sourceUrl.toExternalForm());
				}
			}
			if (url == null) {
				url = getClass().getResource(uri);
				if (url == null)
					LOGGER.warning("can't parse " + uri
							+ " as a java resource present in the classpath");
			}
		}

		ExternalGraphic extgraph;
		if (url == null) {
			extgraph = factory.createExternalGraphic(uri, format);
		} else {
			extgraph = factory.createExternalGraphic(url, format);
		}
		extgraph.setCustomProperties(paramList);
		return extgraph;
	}

	/** Internal parse method - made protected for unit testing */
	protected String parseOnlineResource(Node root) {
		Element param = (Element) root;
		org.w3c.dom.NamedNodeMap map = param.getAttributes();
		final int length = map.getLength();
		LOGGER.finest("attributes " + map.toString());

		for (int k = 0; k < length; k++) {
			String res = map.item(k).getNodeValue();
			String name = map.item(k).getNodeName();
			// if(name == null){
			// name = map.item(k).getNodeName();
			// }
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("processing attribute " + name + "=" + res);
			}

			// TODO: process the name space properly
			if (name.equalsIgnoreCase("xlink:href")
					|| name.equalsIgnoreCase("href")) {
				if (LOGGER.isLoggable(Level.FINEST))
					LOGGER.finest("seting ExtGraph uri " + res);
				return res;
			}
		}
		return null;
	}

	/** Internal parse method - made protected for unit testing */
	protected Stroke parseStroke(Node root) {
		Stroke stroke = factory.getDefaultStroke();
		NodeList list = findElements(((Element) root), "GraphicFill");
		int length = list.getLength();
		if (length > 0) {
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("stroke: found a graphic fill " + list.item(0));

			NodeList kids = list.item(0).getChildNodes();

			for (int i = 0; i < kids.getLength(); i++) {
				Node child = kids.item(i);

				if ((child == null)
						|| (child.getNodeType() != Node.ELEMENT_NODE)) {
					continue;
				}
				String childName = child.getLocalName();
				if (childName == null) {
					childName = child.getNodeName();
				}
				if (childName.equalsIgnoreCase(graphicSt)) {
					Graphic g = parseGraphic(child);
					if (LOGGER.isLoggable(Level.FINEST))
						LOGGER.finest("setting stroke graphicfill with " + g);
					stroke.setGraphicFill(g);
				}
			}
		}

		list = findElements(((Element) root), "GraphicStroke");
		length = list.getLength();
		if (length > 0) {
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("stroke: found a graphic stroke " + list.item(0));

			NodeList kids = list.item(0).getChildNodes();

			for (int i = 0; i < kids.getLength(); i++) {
				Node child = kids.item(i);

				if ((child == null)
						|| (child.getNodeType() != Node.ELEMENT_NODE)) {
					continue;
				}
				String childName = child.getLocalName();
				if (childName == null) {
					childName = child.getNodeName();
				}
				if (childName.equalsIgnoreCase(graphicSt)) {
					Graphic g = parseGraphic(child);
					if (LOGGER.isLoggable(Level.FINEST))
						LOGGER.finest("setting stroke graphicStroke with " + g);
					stroke.setGraphicStroke(g);
				}
			}
		}

		list = findElements(((Element) root), "CssParameter");
		length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}

			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("now I am processing " + child);
			}

			Element param = (Element) child;
			org.w3c.dom.NamedNodeMap map = param.getAttributes();
			final int mapLength = map.getLength();
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("attributes " + map.toString());
			}

			for (int k = 0; k < mapLength; k++) {
				String res = map.item(k).getNodeValue();

				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.finest("processing attribute " + res);
				}
				// process the css entry
				//
				if (res.equalsIgnoreCase(strokeString)) {
					Expression color = parseParameterValueExpression(child,
							false);
					stroke.setColor(color);
				} else if (res.equalsIgnoreCase("width")
						|| res.equalsIgnoreCase("stroke-width")) {
					Expression width = parseParameterValueExpression(child,
							false);
					stroke.setWidth(width);
				} else if (res.equalsIgnoreCase(opacityString)
						|| res.equalsIgnoreCase("stroke-opacity")) {
					Expression opacity = parseParameterValueExpression(child,
							false);
					stroke.setOpacity(opacity);
				} else if (res.equalsIgnoreCase("linecap")
						|| res.equalsIgnoreCase("stroke-linecap")) {
					// since these are system-dependent just pass them through
					// and hope.
					stroke.setLineCap(parseCssParameter(child));
				} else if (res.equalsIgnoreCase("linejoin")
						|| res.equalsIgnoreCase("stroke-linejoin")) {
					// since these are system-dependent just pass them through
					// and hope.
					stroke.setLineJoin(parseCssParameter(child));
				} else if (res.equalsIgnoreCase("dasharray")
						|| res.equalsIgnoreCase("stroke-dasharray")) {
					String dashString = null;
					if (child.getChildNodes().getLength() == 1
							&& child.getFirstChild().getNodeType() == Node.TEXT_NODE) {
						dashString = getFirstChildValue(child);
					} else {
						Expression definition = parseCssParameter(child);
						if (definition instanceof Literal) {
							dashString = ((Literal) definition).getValue()
									.toString();
						} else {
							LOGGER.warning("Only literal stroke-dasharray supported at this time:"
									+ definition);
						}
					}
					if (dashString != null) {
						StringTokenizer stok = new StringTokenizer(
								dashString.trim(), " ");
						float[] dashes = new float[stok.countTokens()];
						for (int l = 0; l < dashes.length; l++) {
							dashes[l] = Float.parseFloat(stok.nextToken());
						}
						stroke.setDashArray(dashes);
					} else {
						LOGGER.fine("Unable to parse stroke-dasharray");
					}
				} else if (res.equalsIgnoreCase("dashoffset")
						|| res.equalsIgnoreCase("stroke-dashoffset")) {
					stroke.setDashOffset(parseCssParameter(child));
				}
			}
		}

		return stroke;
	}

	/** Internal parse method - made protected for unit testing */
	protected Fill parseFill(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing fill ");
		}

		Fill fill = new FillImpl3D();
		NodeList list = findElements(((Element) root), "GraphicFill");
		int length = list.getLength();
		if (length > 0) {
			if (LOGGER.isLoggable(Level.FINEST))
				LOGGER.finest("fill found a graphic fill " + list.item(0));

			NodeList kids = list.item(0).getChildNodes();

			for (int i = 0; i < kids.getLength(); i++) {
				Node child = kids.item(i);

				if ((child == null)
						|| (child.getNodeType() != Node.ELEMENT_NODE)) {
					continue;
				}
				String childName = child.getLocalName();
				if (childName == null) {
					childName = child.getNodeName();
				}
				if (childName.equalsIgnoreCase(graphicSt)) {
					Graphic g = parseGraphic(child);
					if (LOGGER.isLoggable(Level.FINEST))
						LOGGER.finest("setting fill graphic with " + g);
					fill.setGraphicFill(g);
				}
			}
		}

		list = findElements(((Element) root), "CssParameter");
		length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}

			Element param = (Element) child;
			org.w3c.dom.NamedNodeMap map = param.getAttributes();
			final int mapLength = map.getLength();
			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("now I am processing " + child);
			}

			if (LOGGER.isLoggable(Level.FINEST)) {
				LOGGER.finest("attributes " + map.toString());
			}

			for (int k = 0; k < mapLength; k++) {
				String res = map.item(k).getNodeValue();

				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.finest("processing attribute " + res);
				}

				if (res.equalsIgnoreCase(fillSt)) {
					fill.setColor(parseCssParameter(child));
				} else if (res.equalsIgnoreCase(opacityString)
						|| res.equalsIgnoreCase("fill-opacity")) {
					fill.setOpacity(parseCssParameter(child));
				}
			}
		}

		/*************************/
		/** PROVISORY SUGESTION **/
		/*************************/

		list = findElements(((Element) root), "DiffuseColor");
		length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			((FillImpl3D) fill).setDiffuseColor(parseCssParameter(child));
		}

		list = findElements(((Element) root), "TextureUrl");
		length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			((FillImpl3D) fill).setTextureUrl(parseCssParameter(child));
		}

		list = findElements(((Element) root), "EmissiveColor");
		length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			((FillImpl3D) fill).setEmissiveColor(parseCssParameter(child));
		}

		/*************************/
		/** PROVISORY SUGESTION **/
		/*************************/

		return fill;
	}

	/**
	 * Concatenates the given expressions (through the strConcat FunctionFilter
	 * expression)
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	private Expression manageMixed(Expression left, Expression right) {
		if (left == null)
			return right;
		if (right == null)
			return left;
		Function mixed = ff.function("strConcat", new Expression[] { left,
				right });
		return mixed;
	}

	/**
	 * Parses a css parameter. Default implementation trims whitespaces from
	 * text nodes.
	 * 
	 * @param root
	 *            node to parse
	 * @return
	 */
	private Expression parseCssParameter(Node root) {
		return parseCssParameter(root, true);
	}

	/**
	 * Parses a css parameter. You can choose if the parser must trim whitespace
	 * from text nodes or not.
	 * 
	 * @param root
	 *            node to parse
	 * @param trimWhiteSpace
	 *            true to trim whitespace from text nodes. If false, whitespaces
	 *            will be collapsed into one
	 * @return
	 */
	private Expression parseCssParameter(Node root, boolean trimWhiteSpace) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsingCssParam " + root);
		}

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		List<Expression> expressions = new ArrayList<Expression>();
		List<Boolean> cdatas = new ArrayList<Boolean>();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			// Added mixed="true" management through concatenation of text and
			// expression nodes
			if ((child == null)) {
				continue;
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				String value = child.getNodeValue();
				if (value == null)
					continue;

				if (trimWhiteSpace) {
					value = value.trim();
				} else {
					// by spec the inner spaces should collapsed into one,
					// leading and trailing
					// space should be eliminated too
					// http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/
					// (4.3.6 whiteSpace)

					// remove inside spaces
					value = WHITESPACES.matcher(value).replaceAll(" ");
					// we can't deal with leading and trailing whitespaces now
					// as the parser will return each line of whitespace as a
					// separate element
					// we have to do that as post processing
				}

				if (value != null && value.length() != 0) {
					Literal literal = ff.literal(value);

					if (LOGGER.isLoggable(Level.FINEST)) {
						LOGGER.finest("Built new literal " + literal);
					}
					// add the text node as a literal
					expressions.add(literal);
					cdatas.add(false);
				}
			} else if (child.getNodeType() == Node.ELEMENT_NODE) {

				if (LOGGER.isLoggable(Level.FINEST)) {
					LOGGER.finest("about to parse " + child.getLocalName());
				}
				// add the element node as an expression
				expressions.add(org.geotools.filter.ExpressionDOMParser
						.parseExpression(child));
				cdatas.add(false);
			} else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
				String value = child.getNodeValue();
				if (value != null && value.length() != 0) {
					// we build a literal straight, to preserve even cdata
					// sections
					// that have only spaces (as opposed to try and parse it as
					// a literal
					// using the expression dom parser)
					Literal literal = ff.literal(value);

					if (LOGGER.isLoggable(Level.FINEST)) {
						LOGGER.finest("Built new literal " + literal);
					}
					// add the text node as a literal
					expressions.add(literal);
					cdatas.add(true);
				}
			} else
				continue;

		}

		if (expressions.size() == 0 && LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("no children in CssParam");
		}

		if (!trimWhiteSpace) {
			// remove all leading white spaces, which means, find all
			// string literals, remove the white space ones, eventually
			// remove the leading white space form the first non white space one
			while (expressions.size() > 0) {
				Expression ex = expressions.get(0);

				// if it's not a string literal we're done
				if (!(ex instanceof Literal))
					break;
				Literal literal = (Literal) ex;
				if (!(literal.getValue() instanceof String))
					break;

				// ok, string literal.
				String s = (String) literal.getValue();
				if (!cdatas.get(0)) {
					if ("".equals(s.trim())) {
						// If it's whitespace, we have to remove it and continue
						expressions.remove(0);
						cdatas.remove(0);
					} else {
						// if it's not only whitespace, remove anyways the
						// eventual whitespace
						// at its beginning, and then exit, leading whitespace
						// removal is done
						if (s.startsWith(" ")) {
							s = LEADING_WHITESPACES.matcher(s).replaceAll("");
							expressions.set(0, ff.literal(s));
						}
						break;
					}
				} else {
					break;
				}
			}

			// remove also all trailing white spaces the same way
			while (expressions.size() > 0) {
				final int idx = expressions.size() - 1;
				Expression ex = expressions.get(idx);

				// if it's not a string literal we're done
				if (!(ex instanceof Literal))
					break;
				Literal literal = (Literal) ex;
				if (!(literal.getValue() instanceof String))
					break;

				// ok, string literal.
				String s = (String) literal.getValue();
				if (!cdatas.get(idx)) {
					if ("".equals(s.trim())) {
						// If it's whitespace, we have to remove it and continue
						expressions.remove(idx);
						cdatas.remove(idx);
					} else {
						// if it's not only whitespace, remove anyways the
						// eventual whitespace
						// at its end, and then exit, trailing whitespace
						// removal is done
						if (s.endsWith(" ")) {
							s = TRAILING_WHITESPACES.matcher(s).replaceAll("");
							expressions.set(idx, ff.literal(s));
						}
						break;
					}
				} else {
					break;
				}
			}
		}

		// now combine all expressions into one
		Expression ret = null;
		for (Expression expression : expressions) {
			ret = manageMixed(ret, expression);
		}

		return ret;
	}

	/**
	 * Internal method to parse a Font Node; protected to allow for unit testing
	 */
	protected Font parseFont(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing font");
		}

		Font font = factory.getDefaultFont();
		NodeList list = findElements(((Element) root), "CssParameter");
		int length = list.getLength();
		for (int i = 0; i < length; i++) {
			Node child = list.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}

			Element param = (Element) child;
			org.w3c.dom.NamedNodeMap map = param.getAttributes();
			final int mapLength = map.getLength();
			for (int k = 0; k < mapLength; k++) {
				String res = map.item(k).getNodeValue();

				if (res.equalsIgnoreCase("font-family")) {
					font.setFontFamily(parseCssParameter(child));
				} else if (res.equalsIgnoreCase("font-style")) {
					font.setFontStyle(parseCssParameter(child));
				} else if (res.equalsIgnoreCase("font-size")) {
					font.setFontSize(parseCssParameter(child));
				} else if (res.equalsIgnoreCase("font-weight")) {
					font.setFontWeight(parseCssParameter(child));
				}
			}
		}

		return font;
	}

	/** Internal parse method - made protected for unit testing */
	protected LabelPlacement parseLabelPlacement(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing labelPlacement");
		}

		LabelPlacement ret = null;
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("PointPlacement")) {
				ret = parsePointPlacement(child);
			} else if (childName.equalsIgnoreCase("LinePlacement")) {
				ret = parseLinePlacement(child);
			}
		}

		return ret;
	}

	/** Internal parse method - made protected for unit testing */
	protected PointPlacement parsePointPlacement(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing pointPlacement");
		}

		Expression rotation = ff.literal(0.0);
		AnchorPoint ap = null;
		Displacement dp = null;

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("AnchorPoint")) {
				ap = (parseAnchorPoint(child));
			} else if (childName.equalsIgnoreCase("Displacement")) {
				dp = (parseDisplacement(child));
			} else if (childName.equalsIgnoreCase("Rotation")) {
				rotation = (parseCssParameter(child));
			}
		}

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("setting anchorPoint " + ap);
			LOGGER.fine("setting displacement " + dp);
		}

		PointPlacement dpp = factory.createPointPlacement(ap, dp, rotation);

		return dpp;
	}

	/** Internal parse method - made protected for unit testing */
	protected LinePlacement parseLinePlacement(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing linePlacement");
		}

		Expression offset = ff.literal(0.0);
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("PerpendicularOffset")) {
				offset = parseCssParameter(child);
			}
		}

		LinePlacement dlp = factory.createLinePlacement(offset);

		return dlp;
	}

	/**
	 * Internal method to parse an AnchorPoint node; protected visibility for
	 * testing.
	 * 
	 * @param root
	 * @return
	 */
	protected AnchorPoint parseAnchorPoint(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing anchorPoint");
		}

		Expression x = null;
		Expression y = null;

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("AnchorPointX")) {
				x = (parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("AnchorPointY")) {
				y = (parseCssParameter(child));
			}
		}

		AnchorPoint dap = factory.createAnchorPoint(x, y);

		return dap;
	}

	/** Internal parse method - made protected for unit testing */
	protected Displacement parseDisplacement(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing displacment");
		}

		Expression x = null;
		Expression y = null;
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase("DisplacementX")) {
				x = (parseCssParameter(child));
			}

			if (childName.equalsIgnoreCase("DisplacementY")) {
				y = (parseCssParameter(child));
			}
		}

		Displacement dd = factory.createDisplacement(x, y);

		return dd;
	}

	/** Internal parse method - made protected for unit testing */
	protected Halo parseHalo(Node root) {
		if (LOGGER.isLoggable(Level.FINEST)) {
			LOGGER.finest("parsing halo");
		}
		Halo halo = factory.createHalo(
				factory.createFill(ff.literal("#FFFFFF")), ff.literal(1.0));

		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);

			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}
			if (childName.equalsIgnoreCase(fillSt)) {
				halo.setFill(parseFill(child));
			} else if (childName.equalsIgnoreCase("Radius")) {
				halo.setRadius(parseCssParameter(child));
			}

		}

		return halo;
	}

	private Graphic parseModel(Node root) {
		ModelImpl model = new ModelImpl();
		NodeList children = root.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node child = children.item(i);
			if ((child == null) || (child.getNodeType() != Node.ELEMENT_NODE)) {
				continue;
			}
			String childName = child.getLocalName();
			if (childName == null) {
				childName = child.getNodeName();
			}

			if (childName.equalsIgnoreCase("ALTITUDEMODE")) {
				model.setAltitudeModel(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("ALTITUDE")) {
				model.setAltitude(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("HEADING")) {
				model.setHeading(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("TILT")) {
				model.setTilt(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("ROLL")) {
				model.setRoll(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("HREF")) {
				model.setHref(parseCssParameter(child));
			} else if (childName.equalsIgnoreCase("LABEL")) {
				model.setLabel(parseCssParameter(child));
			}
		}
		return (Graphic) model;
	}
}
