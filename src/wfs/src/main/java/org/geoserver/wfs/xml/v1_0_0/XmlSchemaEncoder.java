/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import net.opengis.wfs.DescribeFeatureTypeType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSDescribeFeatureTypeOutputFormat;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.gml.producer.FeatureTypeTransformer;
import org.opengis.feature.type.FeatureType;

public class XmlSchemaEncoder extends WFSDescribeFeatureTypeOutputFormat {
    /** Standard logging instance for class */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses");

    // Initialize some generic GML information
    // ABSTRACT OUTSIDE CLASS, IF POSSIBLE
    private static final String SCHEMA_URI = "\"http://www.w3.org/2001/XMLSchema\"";
    private static final String XS_NAMESPACE = "\n  xmlns:xs=" + SCHEMA_URI;
    private static final String GML_URL = "\"http://www.opengis.net/gml\"";
    private static final String GML_NAMESPACE = "\n  xmlns:gml=" + GML_URL;
    private static final String ELEMENT_FORM_DEFAULT = "\n  elementFormDefault=\"qualified\"";
    private static final String ATTR_FORM_DEFAULT =
            "\n  attributeFormDefault=\"unqualified\" version=\"1.0\">";
    private static final String TARGETNS_PREFIX = "\n  targetNamespace=\"";
    private static final String TARGETNS_SUFFIX = "\" ";

    /** Fixed return footer information */
    private static final String FOOTER = "\n</xs:schema>";

    Catalog catalog;

    public XmlSchemaEncoder(GeoServer gs) {
        super(gs, "XMLSCHEMA");

        this.catalog = gs.getCatalog();
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    protected void write(
            FeatureTypeInfo[] featureTypeInfos, OutputStream output, Operation describeFeatureType)
            throws IOException {
        WFSInfo wfs = getInfo();

        // generates response, using general function
        String xmlResponse =
                generateTypes(
                        featureTypeInfos,
                        (DescribeFeatureTypeType) describeFeatureType.getParameters()[0]);

        if (!wfs.getGeoServer().getSettings().isVerbose()) {
            // strip out the formatting.  This is pretty much the only way we
            // can do this, as the user files are going to have newline
            // characters and whatnot, unless we can get rid of formatting
            // when we read the file, which could be worth looking into if
            // this slows things down.
            xmlResponse = xmlResponse.replaceAll(">\n[ \\t\\n]*", ">");
            xmlResponse = xmlResponse.replaceAll("\n[ \\t\\n]*", " ");
        }

        Writer writer =
                new OutputStreamWriter(output, wfs.getGeoServer().getSettings().getCharset());
        writer.write(xmlResponse);
        writer.flush();
    }

    /**
     * Internal method to generate the XML response object, using feature types.
     *
     * @param request The request object.
     * @return The XMLSchema describing the features requested.
     * @throws WFSException For any problems.
     */
    private final String generateTypes(FeatureTypeInfo[] infos, DescribeFeatureTypeType request)
            throws IOException {
        // Initialize return information and intermediate return objects
        StringBuffer tempResponse = new StringBuffer();

        tempResponse.append(
                "<?xml version=\"1.0\" encoding=\""
                        + getInfo().getGeoServer().getSettings().getCharset()
                        + "\"?>"
                        + "\n<xs:schema ");

        // allSameType will throw WFSException if there are types that are not found.
        if (allSameType(infos)) {
            // all the requested have the same namespace prefix, so return their
            // schemas.
            FeatureTypeInfo ftInfo = infos[0];
            String targetNs = ftInfo.getNamespace().getURI();

            // String targetNs = nsInfoType.getXmlns();
            tempResponse.append(TARGETNS_PREFIX + targetNs + TARGETNS_SUFFIX);

            // namespace
            tempResponse.append(
                    "\n  "
                            + "xmlns:"
                            + ftInfo.getNamespace().getPrefix()
                            + "=\""
                            + targetNs
                            + "\"");

            // xmlns:" + nsPrefix + "=\"" + targetNs
            // + "\"");
            tempResponse.append(GML_NAMESPACE);
            tempResponse.append(XS_NAMESPACE);
            tempResponse.append(ELEMENT_FORM_DEFAULT + ATTR_FORM_DEFAULT);

            // request.getBaseUrl should actually be GeoServer.getSchemaBaseUrl()
            // but that method is broken right now.  See the note there.

            // JD: need a good way to publish resources under a web url, at the
            // same time abstracting away the httpness of the service, for
            // now replacing the schemas.opengis.net

            //            tempResponse.append("\n\n<xs:import namespace=" + GML_URL
            //                + " schemaLocation=\"" + request.getSchemaBaseUrl()
            //                + "gml/2.1.2/feature.xsd\"/>\n\n");
            tempResponse.append(
                    "\n\n<xs:import namespace="
                            + GML_URL
                            + " schemaLocation=\""
                            + buildSchemaURL(request.getBaseUrl(), "gml/2.1.2.1/feature.xsd")
                            + "\"/>\n\n");
            tempResponse.append(generateSpecifiedTypes(infos));
        } else {
            // the featureTypes do not have all the same prefixes.
            tempResponse.append(XS_NAMESPACE);
            tempResponse.append(ELEMENT_FORM_DEFAULT + ATTR_FORM_DEFAULT);

            Set prefixes = new HashSet();

            // iterate through the types, and make a set of their prefixes.
            for (int i = 0; i < infos.length; i++) {
                FeatureTypeInfo ftInfo = infos[i];
                prefixes.add(ftInfo.getNamespace().getPrefix());
            }

            Iterator prefixIter = prefixes.iterator();

            while (prefixIter.hasNext()) {
                // iterate through prefixes, and add the types that have that prefix.
                String prefix = prefixIter.next().toString();
                tempResponse.append(
                        getNSImport(
                                prefix,
                                infos,
                                request.getBaseUrl(),
                                request.getService().toLowerCase()));
            }
        }

        tempResponse.append(FOOTER);

        return tempResponse.toString();
    }

    /**
     * Creates a import namespace element, for cases when requests contain multiple namespaces, as
     * you can not have more than one target namespace. See wfs spec. 8.3.1. All the typeNames that
     * have the correct prefix are added to the import statement.
     *
     * @param prefix the namespace prefix, which must be mapped in the main ConfigInfo, for this
     *     import statement.
     * @param infos a list of all requested typeNames, only those that match the prefix will be a
     *     part of this import statement.
     * @return The namespace element.
     */
    private StringBuffer getNSImport(
            String prefix, FeatureTypeInfo[] infos, String baseUrl, String service) {
        LOGGER.finer("prefix is " + prefix);

        StringBuffer retBuffer = new StringBuffer("\n  <xs:import namespace=\"");
        String namespace = catalog.getNamespaceByPrefix(prefix).getURI();
        retBuffer.append(namespace + "\"");

        Map<String, String> params = new HashMap<String, String>();
        params.put("request", "DescribeFeatureType");
        params.put("service", "wfs");
        params.put("version", "1.0.0");

        StringBuilder typeNames = new StringBuilder();
        for (int i = 0; i < infos.length; i++) {
            FeatureTypeInfo info = infos[i];
            String typeName = info.prefixedName();

            if (typeName.startsWith(prefix + ":")) {
                typeNames.append(typeName).append(",");
            }

            // JD: some of this logic should be fixed by poplulating the
            // info objects properly, double check
            //            if (typeName.startsWith(prefix)
            //                    || ((typeName.indexOf(':') == -1)
            //                    && prefix.equals(r.getWFS().getData().getDefaultNameSpace()
            //                                          .getPrefix()))) {
            //                retBuffer.append(typeName + ",");
            //            }
        }
        typeNames.deleteCharAt(retBuffer.length() - 1);
        params.put("typeName", typeNames.toString());

        String ftLocation = buildURL(baseUrl, service, params, URLType.SERVICE);

        retBuffer.append("\n        schemaLocation=\"" + ResponseUtils.encodeXML(ftLocation));
        retBuffer.append("\"/>");

        return retBuffer;
    }

    /**
     * Internal method to print just the requested types. They should all be in the same namespace,
     * that handling should be done before. This will not do any namespace handling, just prints up
     * either what's in the schema file, or if it's not there then generates the types from their
     * FeatureTypes. Also appends the global element so that the types can substitute as features.
     *
     * @param infos The requested table names.
     * @return A string of the types printed.
     * @task REVISIT: We need a way to make sure the extension bases are correct. should likely add
     *     a field to the info.xml in the featureTypes folder, that optionally references an
     *     extension base (should it be same namespace? we could also probably just do an import on
     *     the extension base). This function then would see if the typeInfo has an extension base,
     *     and would add or import the file appropriately, and put the correct substitution group in
     *     this function.
     */
    private String generateSpecifiedTypes(FeatureTypeInfo[] infos) {
        // TypeRepository repository = TypeRepository.getInstance();
        String tempResponse = "";

        String generatedType = "";
        Set validTypes = new HashSet();

        // Loop through requested tables to add element types
        for (int i = 0; i < infos.length; i++) {
            FeatureTypeInfo ftInfo = (FeatureTypeInfo) infos[i];

            if (!validTypes.contains(ftInfo)) {
                // TODO: ressurect this
                File schemaFile = null; /*ftInfo.getSchemaFile();*/

                try {
                    // Hack here, schemaFile should not be null, but it is
                    // when a fType is first created, since we only add the
                    // schemaFile param to dto on a load.  This should be
                    // fixed, maybe even have the schema file persist, or at
                    // the very least be present right after creation.
                    if ((schemaFile != null) && schemaFile.exists() && schemaFile.canRead()) {
                        generatedType = writeFile(schemaFile);
                    } else {
                        FeatureType ft = ftInfo.getFeatureType();
                        String gType = generateFromSchema(ft);
                        if ((gType != null) && (gType != "")) {
                            generatedType = gType;
                        }
                    }
                } catch (IOException e) {
                    generatedType = "";
                }

                if (!generatedType.equals("")) {
                    tempResponse = tempResponse + generatedType;
                    validTypes.add(ftInfo);
                }
            }
        }

        // Loop through requested tables again to add elements
        // NOT VERY EFFICIENT - PERHAPS THE MYSQL ABSTRACTION CAN FIX THIS;
        //  STORE IN HASH?
        for (Iterator i = validTypes.iterator(); i.hasNext(); ) {
            // Print element representation of table
            tempResponse = tempResponse + printElement((FeatureTypeInfo) i.next());
        }

        tempResponse = tempResponse + "\n\n";

        return tempResponse;
    }

    /**
     * Transforms a FeatureTypeInfo into gml, with no headers.
     *
     * @param schema the schema to transform.
     * @task REVISIT: when this class changes to writing directly to out this can just take a writer
     *     and write directly to it.
     */
    private String generateFromSchema(FeatureType schema) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            FeatureTypeTransformer t = new FeatureTypeTransformer();
            t.setIndentation(4);
            t.setOmitXMLDeclaration(true);
            t.transform(schema, writer);

            return writer.getBuffer().toString();
        } catch (TransformerException te) {
            LOGGER.log(Level.WARNING, "Error generating schema from feature type", te);
            throw (IOException) new IOException("problem transforming type").initCause(te);
        }
    }

    /**
     * Internal method to print XML element information for table.
     *
     * @param type The table name.
     * @return The element part of the response.
     */
    private static String printElement(FeatureTypeInfo type) {
        return "\n  <xs:element name=\""
                + type.getName()
                + "\" type=\""
                + type.getNamespace().getPrefix()
                + ":"
                + type.getName()
                + "_Type"
                + "\" substitutionGroup=\"gml:_Feature\"/>";
    }

    /**
     * Adds a feature type object to the final output buffer
     *
     * @param inputFile The name of the feature type.
     * @return The string representation of the file containing the schema.
     * @throws WFSException For io problems reading the file.
     */
    public String writeFile(File inputFile) throws IOException {
        LOGGER.finest("writing file " + inputFile);

        String finalOutput = "";

        try {
            // File inputFile = new File(inputFileName);
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] fileBuffer = new byte[inputStream.available()];

            while (inputStream.read(fileBuffer) != -1) {
                String tempOutput = new String(fileBuffer);
                finalOutput = finalOutput + tempOutput;
            }
        } catch (IOException e) {
            // REVISIT: should things fail if there are featureTypes that
            // don't have schemas in the right place?  Because as it is now
            // a describe all will choke if there is one ft with no schema.xml
            throw (IOException)
                    new IOException(
                                    "problem writing featureType information "
                                            + " from "
                                            + inputFile)
                            .initCause(e);
        }

        return finalOutput;
    }

    /**
     * Checks that the collection of featureTypeNames all have the same prefix. Used to determine if
     * their schemas are all in the same namespace or if imports need to be done.
     *
     * @param infos list of feature type info objects..
     * @return true if all the types in the collection have the same prefix.
     */
    public boolean allSameType(FeatureTypeInfo[] infos) {
        boolean sameType = true;

        if (infos.length == 0) {
            return false;
        }

        FeatureTypeInfo first = infos[0];

        for (int i = 0; i < infos.length; i++) {
            FeatureTypeInfo ftInfo = infos[i];

            if (!first.getNamespace().equals(ftInfo.getNamespace())) {
                return false;
            }
        }

        return sameType;
    }
}
