/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.kvp;

import java.net.URLDecoder;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.PropertyName;

/**
 * Tests for {@link Filter_2_0_0_KvpParser}, the parser for Filter 2.0 in KVP requests.
 *
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class Filter_2_0_0_KvpParserTest {

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} can be parsed from percent-encoded form into
     * a {@link PropertyIsLike} object.
     *
     * @param expectedLiteral expected decoded filter literal
     * @param encodedLiteral percent-encoded filter literal
     * @param matchCase value of {@code matchCase} filter attribute or {@code null} if none
     */
    private static void parsePropertyIsLike(
            String expectedLiteral, String encodedLiteral, Boolean matchCase) throws Exception {
        String encodedXml =
                "%3Cfes:Filter" //
                        + "%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%3E" //
                        + "%3Cfes:PropertyIsLike" //
                        + "%20wildCard=%22*%22" //
                        + "%20singleChar=%22%25%22" //
                        + "%20escapeChar=%22!%22" //
                        + (matchCase == null ? "" : "%20matchCase=%22" + matchCase + "%22") //
                        + "%3E" //
                        + "%3Cfes:ValueReference%3E" //
                        + "topp:STATE_NAME" //
                        + "%3C/fes:ValueReference%3E" //
                        + "%3Cfes:Literal%3E" //
                        + encodedLiteral //
                        + "%3C/fes:Literal%3E%" //
                        + "3C/fes:PropertyIsLike%3E" //
                        + "%3C/fes:Filter%3E";
        String xml = URLDecoder.decode(encodedXml, "UTF-8");
        @SuppressWarnings("unchecked")
        List<Filter> filters = (List<Filter>) new Filter_2_0_0_KvpParser(null).parse(xml);
        Assert.assertEquals(1, filters.size());
        PropertyIsLike propertyIsLike = (PropertyIsLike) filters.get(0);
        Assert.assertEquals("*", propertyIsLike.getWildCard());
        Assert.assertEquals("%", propertyIsLike.getSingleChar());
        Assert.assertEquals("!", propertyIsLike.getEscape());
        Assert.assertEquals(matchCase == null ? true : matchCase, propertyIsLike.isMatchingCase());
        Assert.assertEquals(
                "topp:STATE_NAME",
                ((PropertyName) propertyIsLike.getExpression()).getPropertyName());
        Assert.assertEquals(expectedLiteral, propertyIsLike.getLiteral());
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with an ASCII literal can be parsed from
     * percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeAsciiLiteral() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", null);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with a non-ASCII literal can be parsed from
     * percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeNonAsciiLiteral() throws Exception {
        parsePropertyIsLike("ü*", "%C3%BC*", null);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with {@code matchCase="true"} can be parsed
     * from percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeMatchCaseTrue() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", true);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with {@code matchCase="false"} can be parsed
     * from percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeMatchCaseFalse() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", false);
    }
}
