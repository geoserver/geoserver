/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_1.OGC;
import org.opengis.filter.capability.FunctionName;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for the {@code Filter_Capabilities} section.
 * 
 * @author Gabriel Roldan
 * 
 */
class FilterCapabilitiesTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    private String FES_PREFIX;

    public FilterCapabilitiesTransformer(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
        FES_PREFIX = namespaceSupport.getPrefix(OGC.NAMESPACE);
    }

    @Override
    public FilterCapabilitiesTranslator createTranslator(ContentHandler handler) {
        return new FilterCapabilitiesTranslator(handler, namespaceSupport);
    }

    class FilterCapabilitiesTranslator extends AbstractTranslator {

        public FilterCapabilitiesTranslator(ContentHandler handler,
                NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        /**
         * @see org.geotools.xml.transform.Translator#encode(java.lang.Object)
         */
        public void encode(Object o) throws IllegalArgumentException {
            encode();
        }

        /**
         * Encodes the ogc:Filter_Capabilities element.
         * <p>
         * 
         * <pre>
         *  &lt;xsd:element name="Filter_Capabilities"&gt;
         *         &lt;xsd:complexType&gt;
         *           &lt;xsd:sequence&gt;
         *                 &lt;xsd:element name="Spatial_Capabilities" type="ogc:Spatial_CapabilitiesType"/&gt;
         *                 &lt;xsd:element name="Scalar_Capabilities" type="ogc:Scalar_CapabilitiesType"/&gt;
         *           &lt;/xsd:sequence&gt;
         *         &lt;/xsd:complexType&gt;
         * &lt;/xsd:element&gt;
         * </pre>
         * 
         * </p>
         */
        public void encode() throws IllegalArgumentException {

            String fes = FES_PREFIX + ":";

            // REVISIT: for now I"m just prepending ogc onto the name element.
            // Is the proper way to only do that for the qname? I guess it
            // would only really matter if we're going to be producing capabilities
            // documents that aren't qualified, and I don't see any reason to
            // do that.
            start(fes + "Filter_Capabilities");
            start(fes + "Spatial_Capabilities");
            start(fes + "Spatial_Operators");
            element(fes + "Disjoint", null);
            element(fes + "Equals", null);
            element(fes + "DWithin", null);
            element(fes + "Beyond", null);
            element(fes + "Intersect", null);
            element(fes + "Touches", null);
            element(fes + "Crosses", null);
            element(fes + "Within", null);
            element(fes + "Contains", null);
            element(fes + "Overlaps", null);
            element(fes + "BBOX", null);
            end(fes + "Spatial_Operators");
            end(fes + "Spatial_Capabilities");

            start(fes + "Scalar_Capabilities");
            element(fes + "Logical_Operators", null);
            start(fes + "Comparison_Operators");
            element(fes + "Simple_Comparisons", null);
            element(fes + "Between", null);
            element(fes + "Like", null);
            element(fes + "NullCheck", null);
            end(fes + "Comparison_Operators");
            start(fes + "Arithmetic_Operators");
            element(fes + "Simple_Arithmetic", null);

            handleFunctions(fes); // djb: list functions

            end(fes + "Arithmetic_Operators");
            end(fes + "Scalar_Capabilities");
            end(fes + "Filter_Capabilities");
        }

        /**
         * &lt;xsd:complexType name="FunctionsType"&gt; &lt;xsd:sequence&gt; &lt;xsd:element
         * name="Function_Names" type="ogc:Function_NamesType"/&gt; &lt;/xsd:sequence&gt;
         * &lt;/xsd:complexType&gt;
         * 
         */
        private void handleFunctions(String prefix) {
            start(prefix + "Functions");
            start(prefix + "Function_Names");

            Set<FunctionName> functions = getAvailableFunctionNames();

            for (FunctionName fname : functions) {

                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute("", "nArgs", "nArgs", "", fname.getArgumentCount() + "");

                element(prefix + "Function_Name", fname.getName(), atts);
            }

            end(prefix + "Function_Names");
            end(prefix + "Functions");
        }

        Set<FunctionName> getAvailableFunctionNames() {
            // Sort them up for easier visual inspection
            SortedSet<FunctionName> sortedFunctions = new TreeSet<FunctionName>(
                    new Comparator<FunctionName>() {
                        public int compare(FunctionName o1, FunctionName o2) {
                            String n1 = o1.getName();
                            String n2 = o2.getName();

                            return n1.toLowerCase().compareTo(n2.toLowerCase());
                        }
                    });

            Set<FunctionFactory> factories = CommonFactoryFinder.getFunctionFactories(null);
            for (FunctionFactory factory : factories) {
                sortedFunctions.addAll(factory.getFunctionNames());
            }

            return sortedFunctions;
        }

    }
}
