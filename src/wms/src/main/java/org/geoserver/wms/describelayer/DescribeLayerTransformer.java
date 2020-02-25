/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.Iterator;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * <code>org.geotools.xml.transform.TransformerBase</code> specialized in producing a WMS
 * DescribeLayer responses.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerTransformer extends TransformerBase {
    /** The base url upon URLs which point to 'me' should be based. */
    private String baseURL;

    /**
     * Creates a new DescribeLayerTransformer object.
     *
     * @param baseURL the base URL, usually "http://host:port/geoserver"
     */
    public DescribeLayerTransformer(final String baseURL) {
        if (baseURL == null) {
            throw new NullPointerException("serverBaseUrl");
        }
        this.baseURL = baseURL;
        setNamespaceDeclarationEnabled(false);
    }

    /**
     * Creates and returns a Translator specialized in producing a DescribeLayer response document.
     *
     * @param handler the content handler to send sax events to.
     * @return a new <code>DescribeLayerTranslator</code>
     */
    public Translator createTranslator(ContentHandler handler) {
        return new DescribeLayerTranslator(handler);
    }

    /**
     * Gets the <code>Transformer</code> created by the overriden method in the superclass and adds
     * it the DOCTYPE token pointing to the DescribeLayer DTD on this server instance.
     *
     * <p>The DTD is set at the fixed location given by the <code>schemaBaseUrl</code> passed to the
     * constructor <code>+ "wms/1.1.1/WMS_DescribeLayerResponse.dtd</code>.
     *
     * @return a Transformer propoerly configured to produce DescribeLayer responses.
     * @throws TransformerException if it is thrown by <code>super.createTransformer()</code>
     */
    public Transformer createTransformer() throws TransformerException {
        Transformer transformer = super.createTransformer();
        String dtdUrl = buildSchemaURL(baseURL, "wms/1.1.1/WMS_DescribeLayerResponse.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, dtdUrl);

        return transformer;
    }

    /**
     * Sends SAX events to produce a DescribeLayer response document.
     *
     * @author Gabriel Roldan
     * @version $Id$
     */
    private class DescribeLayerTranslator extends TranslatorSupport {
        /** Creates a new DescribeLayerTranslator object. */
        public DescribeLayerTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        /**
         * Encode the object.
         *
         * @param o The {@link DescribeLayerRequest} to encode a DescribeLayer response for
         * @throws IllegalArgumentException if the Object is not encodeable.
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof DescribeLayerRequest)) {
                throw new IllegalArgumentException();
            }

            DescribeLayerRequest req = (DescribeLayerRequest) o;

            AttributesImpl versionAtt = new AttributesImpl();
            final String requestVersion = req.getVersion();
            if (requestVersion == null) {
                throw new NullPointerException("requestVersion");
            }

            versionAtt.addAttribute("", "version", "version", "", requestVersion);

            start("WMS_DescribeLayerResponse", versionAtt);

            handleLayers(req);

            end("WMS_DescribeLayerResponse");
        }

        /**
         * As currently GeoServer does not have support for nested layers, this method declares a
         * <code>LayerDescription</code> element for each featuretype requested.
         */
        private void handleLayers(DescribeLayerRequest req) {
            MapLayerInfo layer;

            final List layers = req.getLayers();

            AttributesImpl queryAtts = new AttributesImpl();
            queryAtts.addAttribute("", "typeName", "typeName", "", "");

            for (Iterator it = layers.iterator(); it.hasNext(); ) {
                layer = (MapLayerInfo) it.next();

                AttributesImpl layerAtts = new AttributesImpl();
                layerAtts.addAttribute("", "name", "name", "", "");
                String owsUrl;
                String owsType;
                if (MapLayerInfo.TYPE_VECTOR == layer.getType()) {
                    owsUrl = buildURL(baseURL, "wfs", null, URLType.SERVICE);
                    owsUrl = appendQueryString(owsUrl, "");
                    owsType = "WFS";
                    layerAtts.addAttribute("", "wfs", "wfs", "", owsUrl);
                } else if (MapLayerInfo.TYPE_RASTER == layer.getType()) {
                    owsUrl = buildURL(baseURL, "wcs", null, URLType.SERVICE);
                    owsUrl = appendQueryString(owsUrl, "");
                    owsType = "WCS";
                } else {
                    // non vector nor raster layer, LayerDescription will not contain these
                    // attributes
                    owsUrl = owsType = null;
                }

                if (owsType != null && owsUrl != null) {
                    // the layer is describable only if its vector or raster based
                    // in our case that meand directly associated to a resourceInfo (ie, no base
                    // map)
                    layerAtts.addAttribute("", "owsURL", "owsURL", "", owsUrl);
                    layerAtts.addAttribute("", "owsType", "owsType", "", owsType);
                }

                layerAtts.setAttribute(
                        0, "", "name", "name", "", layer.getLayerInfo().prefixedName());
                start("LayerDescription", layerAtts);

                queryAtts.setAttribute(0, "", "typeName", "typeName", "", layer.getName());
                element("Query", null, queryAtts);

                end("LayerDescription");
            }
        }
    }
}
