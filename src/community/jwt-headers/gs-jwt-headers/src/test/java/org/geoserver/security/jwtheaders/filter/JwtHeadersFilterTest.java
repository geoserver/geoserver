/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import java.io.IOException;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthenticationDetails;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.Assert;

public class JwtHeadersFilterTest {

    @Test
    public void testExistingAuthIsFromThisConfig() throws IOException {
        GeoServerJwtHeadersFilterConfig config1 = new GeoServerJwtHeadersFilterConfig();
        config1.setId("abc123");
        GeoServerJwtHeadersFilter filter1 = new GeoServerJwtHeadersFilter();
        filter1.initializeFromConfig(config1);

        GeoServerJwtHeadersFilterConfig config2 = new GeoServerJwtHeadersFilterConfig();
        config2.setId("aaaaaaaaaaa111111111111111");
        GeoServerJwtHeadersFilter filter2 = new GeoServerJwtHeadersFilter();
        filter2.initializeFromConfig(config1);

        // trivial cases

        // no existing auth
        Assert.isTrue(!filter1.existingAuthIsFromThisConfig(null), "must be true");
        // not from us
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null, null);
        Assert.isTrue(!filter1.existingAuthIsFromThisConfig(auth), "must be true");

        // details, but wrong type
        auth.setDetails(new WebAuthenticationDetails(null, null));
        Assert.isTrue(!filter1.existingAuthIsFromThisConfig(auth), "must be true");

        // more complex cases
        // details is JwtHeaders, but wrong id
        auth.setDetails(new JwtHeadersWebAuthenticationDetails(
                config2.getId(), new GeoServerAbstractTestSupport.GeoServerMockHttpServletRequest("", "")));
        Assert.isTrue(!filter1.existingAuthIsFromThisConfig(auth), "must be true");

        // details is JwtHeaders,right id
        auth.setDetails(new JwtHeadersWebAuthenticationDetails(
                config1.getId(), new GeoServerAbstractTestSupport.GeoServerMockHttpServletRequest("", "")));
        Assert.isTrue(filter1.existingAuthIsFromThisConfig(auth), "must be true");
    }

    @Test
    public void testPrincipleHasChanged() throws IOException {
        GeoServerJwtHeadersFilterConfig config1 = new GeoServerJwtHeadersFilterConfig();
        config1.setId("abc123");
        GeoServerJwtHeadersFilter filter1 = new GeoServerJwtHeadersFilter();
        filter1.initializeFromConfig(config1);

        // trivial cases
        Assert.isTrue(!filter1.principleHasChanged(null, null), "must be true");
        Assert.isTrue(!filter1.principleHasChanged(null, "aaa"), "must be true");

        UsernamePasswordAuthenticationToken auth_aaa = new UsernamePasswordAuthenticationToken("aaa", null, null);
        UsernamePasswordAuthenticationToken auth_bbb = new UsernamePasswordAuthenticationToken("bbb", null, null);

        // aaa->aaa
        Assert.isTrue(!filter1.principleHasChanged(auth_aaa, "aaa"), "must be true");

        /// bbb->aaa
        Assert.isTrue(filter1.principleHasChanged(auth_bbb, "aaa"), "must be true");
    }
}
