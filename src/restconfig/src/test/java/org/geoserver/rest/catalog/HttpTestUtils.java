/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

public class HttpTestUtils {
    private HttpTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static Matcher<HttpServletResponse> hasStatus(HttpStatus expectedStatus) {
        return new BaseMatcher<HttpServletResponse>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof HttpServletResponse) {
                    HttpStatus value = HttpStatus.valueOf(((HttpServletResponse) item).getStatus());
                    return value == expectedStatus;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("HTTP Response with status ")
                        .appendValue(expectedStatus.value())
                        .appendText(" ")
                        .appendValue(expectedStatus.getReasonPhrase());
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item instanceof HttpServletResponse) {
                    HttpStatus value = HttpStatus.valueOf(((HttpServletResponse) item).getStatus());
                    description
                            .appendText("status was ")
                            .appendValue(value.value())
                            .appendText(" ")
                            .appendValue(value.getReasonPhrase());
                } else {
                    description.appendText("was not an HttpServletResponse");
                }
            }
        };
    }

    public static Matcher<HttpServletResponse> hasHeader(
            String name, Matcher<String> valueMatcher) {
        return new BaseMatcher<HttpServletResponse>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof HttpServletResponse) {
                    String value = ((HttpServletResponse) item).getHeader(name);
                    return !Objects.isNull(value) && valueMatcher.matches(value);
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("HTTP Response with header ")
                        .appendValue(name)
                        .appendText(" with value ")
                        .appendDescriptionOf(valueMatcher);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item instanceof HttpServletResponse) {
                    String value = ((HttpServletResponse) item).getHeader(name);
                    if (Objects.isNull(value)) {
                        description.appendText("did not have header ").appendValue("name");
                    } else {
                        description.appendText("header ").appendValue(name).appendText(" ");
                        valueMatcher.describeMismatch(value, description);
                    }
                } else {
                    description.appendText("was not an HttpServletResponse");
                }
            }
        };
    }

    public static InputStream istream(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        return new ByteArrayInputStream(response.getContentAsString().getBytes());
    }
}
