/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.geoserver.opensearch.eo.response.OSEODescriptionResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescriptionTest extends OSEOTestSupport {

    @Test
    public void testGlobalDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("oseo/description");
        assertEquals(OSEODescriptionResponse.OS_DESCRIPTION_MIME, response.getContentType());
        assertEquals(200, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);

        // generic contents check
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:ShortName", equalTo("OSEO")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:LongName",
                equalTo("OpenSearch for Earth Observation")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Description",
                containsString("Earth Observation")));
        assertThat(dom,
                hasXPath("/os:OpenSearchDescription/os:Tags", equalTo("EarthObservation OGC")));
        assertThat(dom,
                hasXPath("/os:OpenSearchDescription/os:LongName", containsString("OpenSearch")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:SyndicationRight", equalTo("open")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:AdultContent", equalTo("false")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Language", equalTo("en-us")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:OutputEncoding", equalTo("UTF-8")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:InputEncoding", equalTo("UTF-8")));

        // check the self link
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Url[@rel='self' "
                + "and @type='application/opensearchdescription+xml']"));
        assertThat(dom,
                hasXPath(
                        "/os:OpenSearchDescription/os:Url[@rel='self' "
                                + "and @type='application/opensearchdescription+xml']/@template",
                        containsString("/oseo/description")));

        // check the result link
        String resultsBase = "/os:OpenSearchDescription/os:Url[@rel='results'and @type='application/atom+xml']";
        assertThat(dom, hasXPath(resultsBase));
        assertThat(dom,
                hasXPath(resultsBase + "/@template",
                        allOf(containsString("searchTerms={searchTerms?}"), //
                                containsString("lat={geo:lat?}"), //
                                containsString("start={time:start?}"))));
        // check some parameters have been described
        String paramBase = resultsBase + "/param:Parameter";
        assertThat(dom, hasXPath(
                paramBase + "[@name='searchTerms' and @value='{searchTerms}' and @minimum='0']"));
        assertThat(dom, hasXPath(paramBase
                + "[@name='count' and @value='{count}' and @minimum='0' and  @minInclusive='0' and @maxInclusive='50']"));
        
        // check some EO parameter
        assertThat(dom, hasXPath(paramBase
                + "[@name='wavelength' and @value='{eo:wavelength}' and @minimum='0']"));
        assertThat(dom, hasXPath(paramBase
                + "[@name='identifier' and @value='{eo:identifier}' and @minimum='0']"));


        // general validation
        checkValidOSDD(dom);
    }

}
