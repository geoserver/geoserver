/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.wms.WMS;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the get histogram operation. */
class HistogramTransformer extends TransformerBase {

    public HistogramTransformer(WMS wms) {
        setIndentation(2);
        setEncoding(wms.getCharSet());
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler);
    }

    class TranslatorSupport extends TransformerBase.TranslatorSupport {

        public TranslatorSupport(ContentHandler handler) {
            super(handler, null, null);
        }

        @Override
        public void encode(Object object) throws IllegalArgumentException {
            if (!(object instanceof Domains)) {
                throw new IllegalArgumentException(
                        "Expected domains info but instead got: "
                                + object.getClass().getCanonicalName());
            }
            Domains domains = (Domains) object;
            Tuple<String, List<Integer>> histogram = domains.getHistogramValues();
            Attributes nameSpaces =
                    createAttributes(
                            new String[] {
                                "xmlns",
                                        "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd",
                                "xmlns:ows", "http://www.opengis.net/ows/1.1"
                            });
            start("Histogram", nameSpaces);
            element("ows:Identifier", domains.getHistogramName());
            element("Domain", histogram.first);
            element(
                    "Values",
                    histogram
                            .second
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(",")));
            end("Histogram");
        }
    }
}
