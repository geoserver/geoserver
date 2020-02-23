/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xml.serializer.TreeWalker;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geoserver.wps.web.InputParameterValues.ParameterValue;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.gml3.GML;
import org.geotools.ows.v1_1.OWS;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPS;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.hsqldb.lib.StringInputStream;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class to turn a {@link ExecuteRequest} into the corresponding WPS 1.0 Execute xml
 *
 * @author Andrea Aime - OpenGeo
 */
class WPSExecuteTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(WPSExecuteTransformer.class);

    /** entity resolver */
    private EntityResolver2 entityResolver;

    public WPSExecuteTransformer() {
        this(null);
    }

    public WPSExecuteTransformer(Catalog catalog) {
        this.catalog = catalog;
    }

    Catalog catalog;

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExecuteRequestTranslator(handler);
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = (EntityResolver2) entityResolver;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public class ExecuteRequestTranslator extends TranslatorSupport {

        /** wfs namespace */
        protected static final String WFS_URI = "http://www.opengis.net/wfs";

        protected static final String WPS_URI = "http://www.opengis.net/wps/1.0.0";

        protected static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

        /** xml schema namespace + prefix */
        protected static final String XSI_PREFIX = "xsi";

        protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

        public ExecuteRequestTranslator(ContentHandler ch) {
            super(ch, null, null);
        }

        public void encode(Object o) throws IllegalArgumentException {
            ExecuteRequest request = (ExecuteRequest) o;
            encode(request, true);
        }

        private void encode(ExecuteRequest request, boolean mainProcess) {
            // add all the usual suspects (we know that we encode the
            // wfs requests as wfs 1.0 and the wcs requests as 1.1, but
            // we really need to move those namespace declaration down to
            // the single request elements so that we can mix them)
            if (mainProcess) {
                AttributesImpl attributes =
                        attributes(
                                "version",
                                "1.0.0",
                                "service",
                                "WPS",
                                "xmlns:xsi",
                                XSI_URI,
                                "xmlns",
                                WPS_URI,
                                "xmlns:wfs",
                                WFS_URI,
                                "xmlns:wps",
                                WPS_URI,
                                "xmlns:ows",
                                OWS.NAMESPACE,
                                "xmlns:gml",
                                GML.NAMESPACE,
                                "xmlns:ogc",
                                OGC.NAMESPACE,
                                "xmlns:wcs",
                                WCS_URI,
                                "xmlns:xlink",
                                XLINK.NAMESPACE,
                                "xsi:schemaLocation",
                                WPS.NAMESPACE
                                        + " "
                                        + "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd");
                start("wps:Execute", attributes);
            } else {
                AttributesImpl attributes = attributes("version", "1.0.0", "service", "WPS");
                start("wps:Execute", attributes);
            }
            element("ows:Identifier", request.processName);
            handleInputs(request.inputs);
            handleOutputs(request.outputs);
            end("wps:Execute");
        }

        /** Helper to build a set of attributes out of a list of key/value pairs */
        AttributesImpl attributes(String... nameValues) {
            AttributesImpl atts = new AttributesImpl();

            for (int i = 0; i < nameValues.length; i += 2) {
                String name = nameValues[i];
                String valu = nameValues[i + 1];

                atts.addAttribute(null, null, name, null, valu);
            }

            return atts;
        }

        public void handleInputs(List<InputParameterValues> inputs) {
            start("wps:DataInputs");
            for (InputParameterValues pv : inputs) {
                for (int i = 0; i < pv.values.size(); i++) {
                    ParameterValue value = pv.values.get(i);
                    if (value == null || value.value == null) {
                        continue;
                    }

                    start("wps:Input");
                    element("ows:Identifier", pv.paramName);
                    if (pv.isBoundingBox()) {
                        ReferencedEnvelope env = (ReferencedEnvelope) value.value;
                        start("wps:Data");
                        String crs = null;
                        if (env.getCoordinateReferenceSystem() != null) {
                            try {
                                crs =
                                        "EPSG:"
                                                + CRS.lookupEpsgCode(
                                                        env.getCoordinateReferenceSystem(), false);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Could not get EPSG code for " + crs);
                            }
                        }
                        if (crs == null) {
                            start("wps:BoundingBoxData", attributes("dimensions", "2"));
                        } else {
                            start("wps:BoundingBoxData", attributes("crs", crs, "dimensions", "2"));
                        }
                        element("ows:LowerCorner", env.getMinX() + " " + env.getMinY());
                        element("ows:UpperCorner", env.getMaxX() + " " + env.getMaxY());
                        end("wps:BoundingBoxData");
                        end("wps:Data");
                    } else if (pv.isComplex()) {
                        if (value.type == ParameterType.TEXT) {
                            handleTextInput(value);
                        } else if (value.type == ParameterType.VECTOR_LAYER) {
                            handleVectorInput(value);
                        } else if (value.type == ParameterType.RASTER_LAYER) {
                            handleRasterLayerInput(value);
                        } else if (value.type == ParameterType.REFERENCE) {
                            handleReferenceInput(value);
                        } else if (value.type == ParameterType.SUBPROCESS) {
                            handleSubprocessInput(value);
                        } else {
                            // write out a warning without blowing pu
                            char[] comment = "Can't handle this data type yet".toCharArray();
                            try {
                                ((LexicalHandler) contentHandler)
                                        .comment(comment, 0, comment.length);
                            } catch (SAXException se) {
                                throw new RuntimeException(se);
                            }
                        }
                    } else if (pv.isCoordinateReferenceSystem()) {
                        handleCoordinateReferenceSystem(value);
                    } else {
                        start("wps:Data");
                        element("wps:LiteralData", Converters.convert(value.value, String.class));
                        end("wps:Data");
                    }
                    end("wps:Input");
                }
            }
            end("wps:DataInputs");
        }

        private void handleCoordinateReferenceSystem(ParameterValue value) {
            try {
                start("wps:Data");
                final CoordinateReferenceSystem crs = (CoordinateReferenceSystem) value.value;
                element("wps:LiteralData", CRS.lookupIdentifier(crs, false));
                end("wps:Data");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void handleRasterLayerInput(ParameterValue value) {
            RasterLayerConfiguration raster = (RasterLayerConfiguration) value.value;
            start(
                    "wps:Reference",
                    attributes(
                            "mimeType",
                            value.mime,
                            "xlink:href",
                            "http://geoserver/wcs",
                            "method",
                            "POST"));
            start("wps:Body");
            if (raster != null && raster.getLayerName() != null) {
                start("wcs:GetCoverage", attributes("service", "WCS", "version", "1.1.1"));
                element("ows:Identifier", raster.getLayerName());
                start("wcs:DomainSubset");

                ReferencedEnvelope bbox = raster.getSpatialDomain();
                String srsUri = GML2EncodingUtils.toURI(bbox.getCoordinateReferenceSystem());
                start("ows:BoundingBox", attributes("crs", srsUri));
                element("ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY());
                element("ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY());
                end("ows:BoundingBox");
                end("wcs:DomainSubset");
                element("wcs:Output", null, attributes("format", "image/tiff"));
                end("wcs:GetCoverage");
            }
            end("wps:Body");
            end("wps:Reference");
        }

        private void handleVectorInput(ParameterValue value) {
            VectorLayerConfiguration vector = (VectorLayerConfiguration) value.value;
            start(
                    "wps:Reference",
                    attributes(
                            "mimeType",
                            value.mime,
                            "xlink:href",
                            "http://geoserver/wfs",
                            "method",
                            "POST"));
            start("wps:Body");

            AttributesImpl atts =
                    attributes("service", "WFS", "version", "1.0.0", "outputFormat", "GML2");

            // if the layer name is qualfiied we should include a namespace mapping on the
            // GetFeature request
            if (catalog != null && vector.layerName != null && vector.layerName.contains(":")) {
                String prefix = vector.layerName.split(":")[0];
                NamespaceInfo ns = catalog.getNamespaceByPrefix(prefix);
                if (ns != null) {
                    atts.addAttribute("", "", "xmlns:" + prefix, null, ns.getURI());
                }
            }

            start("wfs:GetFeature", atts);
            if (vector.layerName == null) {
                start("wfs:Query");
            } else {
                start("wfs:Query", attributes("typeName", vector.layerName));
            }
            // handle attributes and filters here
            end("wfs:Query");
            end("wfs:GetFeature");
            end("wps:Body");
            end("wps:Reference");
        }

        public void handleReferenceInput(ParameterValue value) {
            ReferenceConfiguration reference = (ReferenceConfiguration) value.value;
            if (reference.mime != null) {
                start(
                        "wps:Reference",
                        attributes(
                                "mimeType",
                                reference.mime,
                                "xlink:href",
                                reference.url,
                                "method",
                                reference.method.toString()));
            } else {
                start(
                        "wps:Reference",
                        attributes(
                                "xlink:href",
                                reference.url,
                                "method",
                                reference.method.toString()));
            }
            if (reference.method == ReferenceConfiguration.Method.POST) {
                start("wps:Body");
                cdata(reference.body);
                end("wps:Body");
            }

            end("wps:Reference");
        }

        private void handleSubprocessInput(ParameterValue value) {
            ExecuteRequest request = (ExecuteRequest) value.value;

            start(
                    "wps:Reference",
                    attributes(
                            "mimeType",
                            value.mime,
                            "xlink:href",
                            "http://geoserver/wps",
                            "method",
                            "POST"));
            start("wps:Body");
            encode(request, false);
            end("wps:Body");
            end("wps:Reference");
        }

        private void handleTextInput(ParameterValue value) {
            start("wps:Data");
            start("wps:ComplexData", attributes("mimeType", value.mime));
            String data = Converters.convert(value.value, String.class);
            if (data != null) {
                Document document = parseAsXML(data);
                if (document != null) {
                    dumpAsXML(document);
                } else {
                    try {
                        ((LexicalHandler) contentHandler).startCDATA();
                        chars(data);
                        ((LexicalHandler) contentHandler).endCDATA();
                    } catch (SAXException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            end("wps:ComplexData");
            end("wps:Data");
        }

        private void dumpAsXML(Document document) {
            try {
                TreeWalker tw = new TreeWalker(contentHandler);
                tw.traverse(document);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Document parseAsXML(String data) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setEntityResolver(entityResolver);
                if (!data.startsWith("<?xml")) {
                    data = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n" + data;
                }
                return builder.parse(new StringInputStream(data));
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Failed to parse XML, assuming it's plain text", t);
                return null;
            }
        }

        public void handleOutputs(List<OutputParameter> outputs) {
            start("wps:ResponseForm");
            // if we have a single output we return it in raw form, otherwise
            // go for a full output document
            if (outputs.size() > 1) {
                start("wps:ResponseDocument");
                for (OutputParameter op : outputs) {
                    if (op.isComplex()) {
                        if (op.isComplex()) {
                            start("wps:Output", attributes("mimeType", op.mimeType));
                        } else {
                            start("wps:Output");
                        }
                        element("ows:Identifier", op.paramName);
                        end("wps:Output");
                    }
                }
                end("wps:ResponseDocument");
            } else if (outputs.size() == 1) {
                OutputParameter op = outputs.get(0);
                if (op.isComplex()) {
                    start("wps:RawDataOutput", attributes("mimeType", op.mimeType));
                } else {
                    start("wps:RawDataOutput");
                }
                element("ows:Identifier", op.paramName);
                end("wps:RawDataOutput");
            }
            end("wps:ResponseForm");
        }
    }
}
