/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.ParameterType;
import net.opengis.wfs20.QueryExpressionTextType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.StoredQueryType;
import net.opengis.wfs20.TitleType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.wfs.kvp.QNameKvpParser;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xs.XS;
import org.geotools.xsd.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class StoredQuery {

    /** default stored query */
    public static final StoredQuery DEFAULT;

    static {
        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        StoredQueryDescriptionType desc = factory.createStoredQueryDescriptionType();
        desc.setId("urn:ogc:def:query:OGC-WFS::GetFeatureById");

        TitleType title = factory.createTitleType();
        title.setLang("en");
        title.setValue("Get feature by identifier");
        desc.getTitle().add(title);

        ParameterExpressionType param = factory.createParameterExpressionType();
        param.setName("ID");
        param.setType(XS.STRING);
        desc.getParameter().add(param);

        QueryExpressionTextType text = factory.createQueryExpressionTextType();
        text.setIsPrivate(true);
        text.setReturnFeatureTypes(new ArrayList());
        text.setLanguage(StoredQueryProvider.LANGUAGE_20);

        String xml =
                "<wfs:Query xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' xmlns:fes='"
                        + FES.NAMESPACE
                        + "'>"
                        + "<fes:Filter>"
                        + "<fes:ResourceId rid = '${ID}'/>"
                        + "</fes:Filter>"
                        + "</wfs:Query>";
        text.setValue(xml);
        desc.getQueryExpressionText().add(text);

        DEFAULT = new StoredQuery(desc, null);
    }

    StoredQueryDescriptionType queryDef;
    Catalog catalog;

    public StoredQuery(StoredQueryDescriptionType query, Catalog catalog) {
        this.queryDef = query;
        this.catalog = catalog;
    }

    /** Uniquely identifying name of the stored query. */
    public String getName() {
        return queryDef.getId();
    }

    /** Human readable title describing the stored query. */
    public String getTitle() {
        if (!queryDef.getTitle().isEmpty()) {
            return queryDef.getTitle().get(0).getValue();
        }
        return null;
    }

    /** The feature types the stored query returns result for. */
    public List<QName> getFeatureTypes() {
        List<QName> types = new ArrayList();
        for (QueryExpressionTextType qe : queryDef.getQueryExpressionText()) {
            types.addAll(qe.getReturnFeatureTypes());
        }
        return types;
    }

    public StoredQueryDescriptionType getQuery() {
        return queryDef;
    }

    public void validate() throws WFSException, IOException {
        // parse into a dom and check the typeNames.. since we don't have parameter values we can't
        // parse into a QueryType object
        // TODO: use sax
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        // do a non namespace aware parse... this is because we are only parsing part of the
        // document here (the query part), and it is unlikley that any namespace prefixes are
        // declared
        // dbf.setNamespaceAware(true);

        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            db.setEntityResolver(catalog.getResourcePool().getEntityResolver());
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }

        for (QueryExpressionTextType qe : queryDef.getQueryExpressionText()) {
            // verify that the return feature types matched the ones specified by the actual query
            Set<QName> queryTypes = new HashSet();
            try {
                Document doc = db.parse(new InputSource(new StringReader(qe.getValue())));
                NodeList queries = doc.getElementsByTagName("wfs:Query");
                if (queries.getLength() == 0) {
                    queries = doc.getElementsByTagName("Query");
                }
                if (queries.getLength() == 0) {
                    throw new WFSException("StoredQuery does not specify any Query elements");
                }

                for (int i = 0; i < queries.getLength(); i++) {
                    Element query = (Element) queries.item(i);
                    String[] typeNames = query.getAttribute("typeNames").split(" ");
                    for (String typeName : typeNames) {
                        typeName = unmapLocalPrefixes(typeName, query, catalog);
                        queryTypes.addAll((List) new QNameKvpParser(null, catalog).parse(typeName));
                    }
                }
            } catch (Exception e) {
                throw new IOException(e);
            }

            Set<QName> returnTypes = new HashSet(qe.getReturnFeatureTypes());
            boolean allowAnyReturnType = returnTypes.equals(Collections.singleton(new QName("")));
            for (Iterator<QName> it = queryTypes.iterator(); it.hasNext(); ) {
                QName qName = it.next();
                if (!returnTypes.contains(qName)
                        && !allowAnyReturnType
                        && !isParameter(qName.getLocalPart(), queryDef.getParameter())) {
                    throw new WFSException(
                            String.format(
                                    "StoredQuery references typeName %s:%s "
                                            + "not listed in returnFeatureTypes: %s",
                                    qName.getPrefix(),
                                    qName.getLocalPart(),
                                    toString(qe.getReturnFeatureTypes())));
                }
                if (returnTypes.contains(qName)) {
                    returnTypes.remove(qName);
                }
            }

            if (!returnTypes.isEmpty() && !allowAnyReturnType) {
                throw new WFSException(
                        String.format(
                                "StoredQuery declares return feature type(s) not "
                                        + "not referenced in query definition: %s",
                                toString(returnTypes)));
            }
        }
    }

    private String unmapLocalPrefixes(String typeName, Element element, Catalog catalog) {
        String[] split = typeName.trim().split(":");
        if (split.length != 2) {
            return typeName;
        }
        String prefix = split[0];
        String localName = split[1];

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String nodeName = attribute.getNodeName();
            String xmlnsPrefix = null;
            if (nodeName.startsWith("xmlns:")) {
                xmlnsPrefix = nodeName.substring(6);
            } else if (nodeName.equalsIgnoreCase("xmlns")) {
                xmlnsPrefix = "";
            }
            if (prefix.equalsIgnoreCase(xmlnsPrefix)) {
                String uri = attribute.getNodeValue();
                NamespaceInfo ns = catalog.getNamespaceByURI(uri);
                prefix = ns.getName();
            }
        }

        return prefix + ":" + localName;
    }

    private boolean isParameter(
            String parameterCandidate, EList<ParameterExpressionType> parameter) {
        return parameter != null
                && parameter
                        .stream()
                        .map(pet -> "${" + pet.getName() + "}")
                        .anyMatch(name -> parameterCandidate.equals(name));
    }

    String toString(Collection<QName> qNames) {
        StringBuilder sb = new StringBuilder();
        for (QName qName : qNames) {
            sb.append(qName.getPrefix()).append(":").append(qName.getLocalPart()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public List<QueryType> compile(StoredQueryType query) {
        List list = new ArrayList();

        for (QueryExpressionTextType qe : queryDef.getQueryExpressionText()) {

            // do the parameter substitution
            StringBuffer sb = new StringBuffer(qe.getValue());
            for (ParameterType p : query.getParameter()) {
                String name = p.getName();
                String token = "${" + name + "}";
                // stored queries can be used in KVP where the parameter name is case insensitive so
                // do a case
                // insensitive search
                int i = sb.toString().toLowerCase().indexOf(token.toLowerCase());
                while (i > 0) {
                    sb.replace(i, i + token.length(), p.getValue());
                    i = sb.indexOf(token, i + token.length());
                }
            }

            // parse
            Parser p = new Parser(new WFSConfiguration());
            // "inject" namespace mappings
            if (catalog != null) {
                p.getNamespaces().add(new CatalogNamespaceSupport(catalog));
            }
            p.getNamespaces().declarePrefix("gml", GML.NAMESPACE);
            try {
                QueryType compiled =
                        (QueryType) p.parse(new InputSource(new StringReader(sb.toString())));
                list.add(compiled);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }
}
