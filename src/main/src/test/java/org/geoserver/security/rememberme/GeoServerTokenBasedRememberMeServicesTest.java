/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.rememberme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetailsService;

public class GeoServerTokenBasedRememberMeServicesTest {

    /**
     * this does a test of the remember-me service for ensuring that the logout:
     *
     * <p>1. send the cancel token to the browser<br>
     * 2. doesn't allow the token to be used in the future
     *
     * <p>Method: <br>
     * 1. we use a real (decode-able token) - `realToken` <br>
     * 2. The token hasn't been cancelled (yet) so it should be decode-able <br>
     * 3. we execute the logout <br>
     * 4. ensure that the cancel-remember-me cookie is attached to the response <br>
     * 5. ensure that the cache has the cancelled cookie in it <br>
     * 6. ensure that you cannot decode that token anymore
     */
    @Test
    public void testLogout() {
        GeoServerTokenBasedRememberMeServices sut =
                new GeoServerTokenBasedRememberMeServices("key", mock(UserDetailsService.class));
        GeoServerTokenBasedRememberMeServices spy = Mockito.spy(sut);

        // this token can be decoded.
        String realToken = "YWRtaW4lNDBkZWZhdWx0OjE3NjExNTQzMzI0NDA6TUQ1OmY4ZWZmYzMyZTllMTlmYTEzZTdjMWQyY2ZhZDczNTQ0";

        // create a request with a remember-me cookie token
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("remember-me", realToken);
        Cookie[] cookies = {cookie};
        when(request.getCookies()).thenReturn(cookies);
        when(request.getContextPath()).thenReturn("/");

        // response and capture the addCookie(cookie) cookie argument
        // this will be the cancel remember-me cookie sent to the browser
        HttpServletResponse response = mock(HttpServletResponse.class);
        ArgumentCaptor<Cookie> argumentCaptor = ArgumentCaptor.forClass(Cookie.class);

        // we havent logged out yet, so we should be able to decode the token
        assertEquals("admin@default", spy.decodeCookie(realToken)[0]);

        // actual logout
        spy.logout(request, response, null);

        // the remember-me cookie token value should be in the disallow cache
        assertEquals(1, GeoServerTokenBasedRememberMeServices.unauthorizedRememberMeCookieCache.size());
        assertNotNull(GeoServerTokenBasedRememberMeServices.unauthorizedRememberMeCookieCache.getIfPresent(realToken));

        // verify that the cancel remember-me token is sent to browser
        verify(response).addCookie(argumentCaptor.capture());
        assertNotNull(argumentCaptor.getValue());
        assertNull(argumentCaptor.getValue().getValue());
        assertEquals("remember-me", argumentCaptor.getValue().getName());

        // the cookie should no longer be able to be decoded because its in the not-allowed cache
        String[] decodedCookieTokens = spy.decodeCookie(realToken);

        // decoded cookie has no tokens
        assertNotNull(decodedCookieTokens);
        assertEquals(0, decodedCookieTokens.length);
    }
}
