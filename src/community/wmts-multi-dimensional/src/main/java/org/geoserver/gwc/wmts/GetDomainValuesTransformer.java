/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.wms.WMS;
import org.geotools.data.Query;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.filter.sort.SortOrder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/** XML transformer for the describe domains operation. */
class GetDomainValuesTransformer extends TransformerBase {

    public GetDomainValuesTransformer(WMS wms) {
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
            Attributes nameSpaces =
                    createAttributes(
                            new String[] {
                                "xmlns",
                                        "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd",
                                "xmlns:ows", "http://www.opengis.net/ows/1.1"
                            });
            start("DomainValues", nameSpaces);
            Dimension dimension = domains.getDimensions().get(0);
            element("ows:Identifier", dimension.getDimensionName());
            element("Limit", String.valueOf(domains.getMaxReturnedValues()));
            element("Sort", domains.getSortOrder() == SortOrder.ASCENDING ? "asc" : "desc");
            if (domains.getFromValue() != null) {
                element("FromValue", domains.getFromValue());
            }
            Query query = new Query(null, domains.getFilter());
            List<String> values =
                    dimension.getPagedDomainValuesAsStrings(
                                    query, domains.getMaxReturnedValues(), domains.getSortOrder())
                            .second;
            element("Domain", values.stream().collect(Collectors.joining(",")));
            element("Size", String.valueOf(values.size()));

            end("DomainValues");
        }
    }
}
