/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests.readers;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.kvp.CQLFilterKvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.util.XCQL;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterFilter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.gml.GMLFilterDocument;
import org.geotools.gml.GMLFilterGeometry;
import org.geotools.util.Converters;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.springframework.context.ApplicationContextAware;
import org.vfny.geoserver.Request;
import org.vfny.geoserver.util.requests.FilterHandlerImpl;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;

/**
 * Base class for all KVP readers, with some generalized convenience methods.
 *
 * <p>If you pass this utility a KVP request (everything after the '?' in the GET request URI), it
 * will translate this into a list of key-word value pairs.These pairs represent every element in
 * the KVP GET request, legal or otherwise. This class may then be subclassed and used by
 * request-specific classes. Because there is no error checking for the KVPs in this class,
 * subclasses must check for validity of their KVPs before passing the their requests along, but -
 * in return - this parent class is quite flexible. For example, native KVPs may be easily parsed in
 * its subclasses, since they are simply read and stored (without analysis) in the constructer in
 * this class. Note that all keys are translated to upper case to avoid case conflicts.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Roldan
 * @version $Id$
 */
public abstract class KvpRequestReader implements ApplicationContextAware {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests.readers");

    /** Delimeter for KVPs in the raw string */
    private static final String KEYWORD_DELIMITER = "&";

    /** Delimeter that seperates keywords from values */
    private static final String VALUE_DELIMITER = "=";

    /** Delimeter for outer value lists in the KVPs */
    protected static final String OUTER_DELIMETER = "()";

    /** Delimeter for inner value lists in the KVPs */
    protected static final String INNER_DELIMETER = ",";

    /** Holds mappings between HTTP and ASCII encodings */
    protected static final FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);

    /** KVP pair listing; stores all data from the KVP request */
    protected Map kvpPairs = new HashMap(10);

    /** Reference to the service using the reader */
    // protected AbstractService service;
    protected ServiceInfo serviceConfig;

    protected EntityResolverProvider entityResolverProvider;

    /**
     * Creates a reader from paramters and a service configuration.
     *
     * @param kvpPairs The key-value pairs.
     * @param service The service configuration.
     */
    public KvpRequestReader(Map kvpPairs, ServiceInfo service) {
        this.kvpPairs = kvpPairs;
        this.serviceConfig = service;
        this.entityResolverProvider = GeoServerExtensions.bean(EntityResolverProvider.class);
    }

    /**
     * returns the value asociated with <code>key</code> on the set of key/value pairs of this
     * request reader
     *
     * @param key DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected String getValue(String key) {
        return Converters.convert(kvpPairs.get(key), String.class);
    }

    /**
     * DOCUMENT ME!
     *
     * @param key DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected boolean keyExists(String key) {
        return kvpPairs.containsKey(key);
    }

    /**
     * returns the propper Request subclass for the set of parameters it was setted up and the kind
     * of request it is specialized for
     *
     * @return DOCUMENT ME!
     */
    public abstract Request getRequest(HttpServletRequest request) throws ServiceException;

    /**
     * Attempts to parse out the proper typeNames from the FeatureId filters. It simply uses the
     * value before the '.' character.
     *
     * @param rawFidList the strings after the FEATUREID url component. Should be found using
     *     kvpPairs.get("FEATUREID") in this class or one of its children
     * @return A list of typenames, made from the featureId filters.
     * @throws WfsException If the structure can not be read.
     */
    protected List getTypesFromFids(String rawFidList) {
        List typeList = new ArrayList(10);
        List unparsed = readNested(rawFidList);
        Iterator i = unparsed.listIterator();

        while (i.hasNext()) {
            List ids = (List) i.next();
            ListIterator innerIterator = ids.listIterator();

            while (innerIterator.hasNext()) {
                String fid = innerIterator.next().toString();

                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("looking at featureId" + fid);
                }

                String typeName = fid.substring(0, fid.lastIndexOf("."));

                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("adding typename: " + typeName + " from fid");
                }

                typeList.add(typeName);
            }
        }

        return typeList;
    }

    /**
     * Reads a tokenized string and turns it into a list. In this method, the tokenizer is quite
     * flexible. Note that if the list is unspecified (ie. is null) or is unconstrained (ie. is ''),
     * then the method returns an empty list.
     *
     * @param rawList The tokenized string.
     * @param delimiter The delimeter for the string tokens.
     * @return A list of the tokenized string.
     */
    protected static List readFlat(String rawList, String delimiter) {
        return KvpUtils.readFlat(rawList, delimiter);
    }

    /**
     * Reads a nested tokenized string and turns it into a list. This method is much more specific
     * to the KVP get request syntax than the more general readFlat method. In this case, the outer
     * tokenizer '()' and inner tokenizer ',' are both from the specification. Returns a list of
     * lists.
     *
     * @param rawList The tokenized string.
     * @return A list of lists, containing outer and inner elements.
     * @throws WfsException When the string structure cannot be read.
     */
    protected static List readNested(String rawList) {
        return KvpUtils.readNested(rawList);
    }

    /**
     * creates a Map of key/value pairs from a HTTP style query String
     *
     * @param qString DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public static Map parseKvpSet(String qString) {
        // uses the request cleaner to remove HTTP junk
        String cleanRequest = clean(qString);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("clean request is " + cleanRequest);
        }

        Map kvps = null;
        kvps = new HashMap(10);

        // parses initial request sream into KVPs
        StringTokenizer requestKeywords =
                new StringTokenizer(cleanRequest.trim(), KEYWORD_DELIMITER);

        // parses KVPs into values and keywords and puts them in a HashTable
        while (requestKeywords.hasMoreTokens()) {
            String kvpPair = requestKeywords.nextToken();
            String key;
            String value;

            // a bit of a horrible hack for filters, which handles problems of
            // delimeters, which may appear in XML (such as '=' for
            // attributes. unavoidable and illustrates the problems with
            // mixing nasty KVP Get syntax and pure XML syntax!
            // JD-adding SLD_BODY, when wms moves to new architecture this
            // can be fixed
            if (kvpPair.toUpperCase().startsWith("FILTER")) {
                String filterVal = kvpPair.substring(7);

                // int index = filterVal.lastIndexOf("</Filter>");
                // String filt2 = kvpPair.subString
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("putting filter value " + filterVal);
                }

                kvps.put("FILTER", filterVal);
            } else if (kvpPair.toUpperCase().startsWith("SLD_BODY")) {
                String sldBodyVal = kvpPair.substring(9);
                kvps.put("SLD_BODY", sldBodyVal);
            } else {
                // handles all other standard cases by looking for the correct
                // delimeter and then sticking the KVPs into the hash table
                StringTokenizer requestValues = new StringTokenizer(kvpPair, VALUE_DELIMITER);

                // make sure that there is a key token
                if (requestValues.hasMoreTokens()) {
                    // assign key as uppercase to eliminate case conflict
                    key = requestValues.nextToken().toUpperCase();

                    // make sure that there is a value token
                    if (requestValues.hasMoreTokens()) {
                        // assign value and store in hash with key
                        value = requestValues.nextToken();
                        LOGGER.finest("putting kvp pair: " + key + ": " + value);
                        kvps.put(key, value);
                    } else {
                        kvps.put(key, "");
                    }
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("returning parsed " + kvps);
        }

        return kvps;
    }

    /**
     * Cleans an HTTP string and returns pure ASCII as a string.
     *
     * @param raw The HTTP-encoded string.
     * @return The string with the url escape characters replaced.
     */
    private static String clean(String raw) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("raw request: " + raw);
        }

        String clean = null;

        if (raw != null) {
            try {
                clean = java.net.URLDecoder.decode(raw, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Bad encoding for decoder " + e);
                }
            }
        } else {
            return "";
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("cleaned request: " + raw);
        }

        return clean;
    }

    //    /**
    //     * Returns the service handling request.
    //     */
    //    public AbstractService getServiceRef() {
    //        return service;
    //    }
    //
    //    /**
    //     * sets the service handling request.
    //     */
    //    public void setServiceRef(AbstractService service) {
    //        this.service = service;
    //    }

    /**
     * parses the BBOX parameter, wich must be a String of the form <code>minx,miny,maxx,maxy</code>
     * and returns a corresponding <code>Envelope</code> object
     *
     * @param bboxParam TODO
     * @return the <code>Envelope</code> represented by the request BBOX parameter
     * @throws WmsException if the value of the BBOX request parameter can't be parsed as four
     *     <code>double</code>'s
     */
    protected Envelope parseBbox(String bboxParam) throws ServiceException {
        Envelope bbox = null;
        Object[] bboxValues = readFlat(bboxParam, INNER_DELIMETER).toArray();

        if (bboxValues.length != 4) {
            throw new ServiceException(
                    bboxParam + " is not a valid pair of coordinates", getClass().getName());
        }

        try {
            double minx = Double.parseDouble(bboxValues[0].toString());
            double miny = Double.parseDouble(bboxValues[1].toString());
            double maxx = Double.parseDouble(bboxValues[2].toString());
            double maxy = Double.parseDouble(bboxValues[3].toString());
            bbox = new Envelope(minx, maxx, miny, maxy);

            if (minx > maxx) {
                throw new ServiceException(
                        "illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx);
            }

            if (miny > maxy) {
                throw new ServiceException(
                        "illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy);
            }
        } catch (NumberFormatException ex) {
            throw new ServiceException(
                    ex,
                    "Illegal value for BBOX parameter: " + bboxParam,
                    getClass().getName() + "::parseBbox()");
        }

        return bbox;
    }

    /**
     * Parses an OGC filter
     *
     * @param filter
     * @throws ServiceException
     */
    protected List readOGCFilter(String filter) throws ServiceException {
        List filters = new ArrayList();
        List unparsed;
        ListIterator i;
        LOGGER.finest("reading filter: " + filter);
        unparsed = readFlat(filter, OUTER_DELIMETER);
        i = unparsed.listIterator();

        while (i.hasNext()) {
            String next = (String) i.next();

            if (next.trim().equals("")) {
                filters.add(Filter.INCLUDE);
            } else {
                filters.add(parseXMLFilter(new StringReader(next)));
            }
        }

        return filters;
    }

    /**
     * Parses a CQL filter
     *
     * @param filter
     * @throws ServiceException
     * @deprecated use {@link CQLFilterKvpParser}
     */
    protected List readCQLFilter(String filter) throws ServiceException {
        try {
            return XCQL.toFilterList(filter);
        } catch (CQLException pe) {
            throw new ServiceException("Could not parse CQL filter list." + pe.getMessage(), pe);
        }
    }

    /**
     * Parses fid filters
     *
     * @param fid
     */
    protected List readFidFilter(String fid) {
        List filters = new ArrayList();
        List unparsed;
        ListIterator i;
        LOGGER.finest("reading fid filter: " + fid);
        unparsed = readNested(fid);
        i = unparsed.listIterator();

        HashSet set = new HashSet();
        while (i.hasNext()) {
            List ids = (List) i.next();
            ListIterator innerIterator = ids.listIterator();

            while (innerIterator.hasNext()) {
                set.add(factory.featureId((String) innerIterator.next()));
            }
        }
        Id fidFilter = factory.id(set);
        filters.add(fidFilter);
        LOGGER.finest("added fid filter: " + fidFilter);

        return filters;
    }

    /**
     * Reads the Filter XML request into a geotools Feature object.
     *
     * @param rawRequest The plain POST text from the client.
     * @return The geotools filter constructed from rawRequest.
     * @throws WfsException For any problems reading the request.
     * @deprecated use {@link FilterKvpParser}
     */
    protected Filter parseXMLFilter(Reader rawRequest) throws ServiceException {
        // translate string into a proper SAX input source
        InputSource requestSource = new InputSource(rawRequest);

        // locate the entity resolver (we might have entity resolution disabled)
        EntityResolver entityResolver = entityResolverProvider.getEntityResolver();

        // instantiante parsers and content handlers
        FilterHandlerImpl contentHandler = new FilterHandlerImpl();
        contentHandler.setEntityResolver(entityResolver);
        FilterFilter filterParser = new FilterFilter(contentHandler, null);
        GMLFilterGeometry geometryFilter = new GMLFilterGeometry(filterParser);
        GMLFilterDocument documentFilter = new GMLFilterDocument(geometryFilter);

        // read in XML file and parse to content handler
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ParserAdapter adapter = new ParserAdapter(parser.getParser());

            adapter.setEntityResolver(entityResolver);

            adapter.setContentHandler(documentFilter);
            adapter.parse(requestSource);
            LOGGER.fine("just parsed: " + requestSource);
        } catch (SAXException e) {
            // SAXException does not sets initCause(). Instead, it holds its own "exception" field.
            if (e.getException() != null && e.getCause() == null) {
                e.initCause(e.getException());
            }
            throw new ServiceException(
                    e,
                    "XML getFeature request SAX parsing error",
                    XmlRequestReader.class.getName());
        } catch (IOException e) {
            throw new ServiceException(
                    e, "XML get feature request input error", XmlRequestReader.class.getName());
        } catch (ParserConfigurationException e) {
            throw new ServiceException(
                    e, "Some sort of issue creating parser", XmlRequestReader.class.getName());
        }

        LOGGER.fine("passing filter: " + contentHandler.getFilter());

        return contentHandler.getFilter();
    }
}
