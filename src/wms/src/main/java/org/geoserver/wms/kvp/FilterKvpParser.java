/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.filter.Filter;
import org.geotools.filter.v1_0.OGCConfiguration;
import org.geotools.filter.v2_0.FESConfiguration;
import org.geotools.gml.GMLFilterDocument;
import org.geotools.gml.GMLFilterGeometry;
import org.geotools.xml.filter.FilterFilter;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.vfny.geoserver.util.requests.FilterHandlerImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;

/**
 * Parses the custom <code>FILTER</code> parameter for a WMS GetMap request.
 *
 * <p>This parser makes a best effort to parse the provided filter by trying with the parsers for the different
 * supported OGC filter spec versions.
 *
 * @authod Justin Deoliveira
 * @author Gabriel Roldan
 * @since 1.7.x
 */
public class FilterKvpParser extends KvpParser {

    private EntityResolverProvider resolverProvider;

    public FilterKvpParser(EntityResolverProvider resolverProvider) {
        super("filter", List.class);
        this.resolverProvider = resolverProvider;
    }

    /** @return a {@code List<Filter>} with the parsed filters from the {@code FILTER=} request parameter */
    @Override
    @SuppressWarnings("unchecked")
    public Object parse(String value) throws Exception {
        // seperate the individual filter strings
        List unparsed = KvpUtils.readFlat(value, KvpUtils.OUTER_DELIMETER);
        List filters = new ArrayList<>();

        Iterator i = unparsed.listIterator();

        while (i.hasNext()) {
            String string = (String) i.next();
            if ("".equals(string.trim())) {
                filters.add(Filter.INCLUDE);
            } else {
                Filter filter;
                String exceptionMsg = "";

                try {
                    filter = parse(string, new FESConfiguration());
                } catch (Exception e) {
                    exceptionMsg = e.getMessage();
                    try {
                        // parsing failed with 2.0.0, try with a Filter 1.0.0 parser
                        filter = parse(string, new OGCConfiguration());
                    } catch (Exception e1) {
                        exceptionMsg += e1.getMessage();
                        // parsing failed with 2.0.0, 1.0.0, try with a Filter 1.1.0 parser
                        try {
                            filter = parse(string, new org.geotools.filter.v1_1.OGCConfiguration());
                        } catch (Exception e2) {
                            // parsing failed with 2.0.0, 1.0.0, 1.1.0, fall back to old parser
                            exceptionMsg += "Unable to parse filter: " + string;
                            LOGGER.log(Level.WARNING, exceptionMsg, e2);
                            filter = parseXMLFilterWithOldParser(new StringReader(string));
                        }
                    }
                }
                if (filter == null) {
                    throw new NullPointerException();
                }
                filters.add(filter);
            }
        }

        return filters;
    }

    private Filter parse(String input, Configuration configuration)
            throws IOException, ParserConfigurationException, SAXException {
        Parser parser = new Parser(configuration);
        parser.setEntityResolver(resolverProvider.getEntityResolver());
        parser.setValidating(true);
        Filter filter = (Filter) parser.parse(new StringReader(input));
        return filter;
    }

    /**
     * Reads the Filter XML request into a geotools Feature object.
     *
     * <p>This uses the "old" filter parser and is around to maintain some backwards compatability with cases in which
     * the new parser chokes on a filter that hte old one could handle.
     *
     * @param rawRequest The plain POST text from the client.
     * @return The geotools filter constructed from rawRequest.
     * @throws WfsException For any problems reading the request.
     */
    protected Filter parseXMLFilterWithOldParser(Reader rawRequest) throws ServiceException {
        // translate string into a proper SAX input source
        InputSource requestSource = new InputSource(rawRequest);

        // instantiate parsers and content handlers
        FilterHandlerImpl contentHandler = new FilterHandlerImpl();
        contentHandler.setEntityResolver(resolverProvider.getEntityResolver());
        FilterFilter filterParser = new FilterFilter(contentHandler, null);
        GMLFilterGeometry geometryFilter = new GMLFilterGeometry(filterParser);
        GMLFilterDocument documentFilter = new GMLFilterDocument(geometryFilter);

        // read in XML file and parse to content handler
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ParserAdapter adapter = new ParserAdapter(parser.getParser());
            adapter.setEntityResolver(resolverProvider.getEntityResolver());
            adapter.setContentHandler(documentFilter);
            adapter.parse(requestSource);
            LOGGER.fine("just parsed: " + requestSource);
        } catch (SAXException e) {
            throw new ServiceException(e, "XML getFeature request SAX parsing error", XmlRequestReader.class.getName());
        } catch (IOException e) {
            throw new ServiceException(e, "XML get feature request input error", XmlRequestReader.class.getName());
        } catch (ParserConfigurationException e) {
            throw new ServiceException(e, "Some sort of issue creating parser", XmlRequestReader.class.getName());
        }

        LOGGER.fine("passing filter: " + contentHandler.getFilter());

        return contentHandler.getFilter();
    }
}
