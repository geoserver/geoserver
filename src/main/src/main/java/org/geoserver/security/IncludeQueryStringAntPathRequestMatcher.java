/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Improved version of Spring Security AntPathRequestMatcher with optional query string regular
 * expression matching in addition to path matching.
 *
 * <p>The original AntPathRequestMatcher was declared final and not easily extendable by
 * composition, so we have wrote our own enhanced version.
 *
 * @author Mauro Bartolomeoli
 */
public final class IncludeQueryStringAntPathRequestMatcher implements RequestMatcher {
    private static final Log logger =
            LogFactory.getLog(IncludeQueryStringAntPathRequestMatcher.class);
    private static final String MATCH_ALL = "/**";
    private static final String QUERYSTRING_SEPARATOR = "|";

    private final Matcher matcher;
    private final Matcher queryStringMatcher;
    private final String pattern;
    private final HttpMethod httpMethod;

    /**
     * Creates a matcher with the specific pattern which will match all HTTP methods.
     *
     * @param pattern the ant pattern to use for matching
     */
    public IncludeQueryStringAntPathRequestMatcher(String pattern) {
        this(pattern, null);
    }

    /**
     * Creates a matcher with the supplied pattern which will match all HTTP methods.
     *
     * @param pattern the ant pattern to use for matching
     * @param httpMethod the HTTP method. The {@code matches} method will return false if the
     *     incoming request doesn't have the same method.
     */
    public IncludeQueryStringAntPathRequestMatcher(String pattern, String httpMethod) {
        Assert.hasText(pattern, "Pattern cannot be null or empty");
        String queryStringPattern = "";
        String originalPattern = pattern;
        // check for querystring pattern existance
        if (pattern.contains(QUERYSTRING_SEPARATOR)) {
            queryStringPattern = pattern.substring(pattern.indexOf(QUERYSTRING_SEPARATOR) + 1);
            pattern = pattern.substring(0, pattern.indexOf(QUERYSTRING_SEPARATOR));
        }
        if (pattern.equals(MATCH_ALL) || pattern.equals("**")) {
            pattern = MATCH_ALL;
            matcher = null;
        } else {
            pattern = pattern.toLowerCase();

            // If the pattern ends with {@code /**} and has no other wildcards, then optimize to a
            // sub-path match
            if (pattern.endsWith(MATCH_ALL)
                    && pattern.indexOf('?') == -1
                    && pattern.indexOf("*") == pattern.length() - 2) {
                matcher = new SubpathMatcher(pattern.substring(0, pattern.length() - 3));
            } else {
                matcher = new SpringAntMatcher(pattern);
            }
        }

        this.pattern = originalPattern;
        // build query string matcher if needed
        if (StringUtils.hasLength(queryStringPattern)) {
            queryStringMatcher = new QueryStringMatcher(queryStringPattern);
        } else {
            queryStringMatcher = null;
        }
        this.httpMethod = StringUtils.hasText(httpMethod) ? HttpMethod.valueOf(httpMethod) : null;
    }

    /**
     * Returns true if the configured pattern(s) (and HTTP-Method) match those of the supplied
     * request.
     *
     * @param request the request to match against. The ant pattern will be matched against the
     *     {@code servletPath} + {@code pathInfo} of the request.
     */
    public boolean matches(HttpServletRequest request) {
        if (httpMethod != null && httpMethod != HttpMethod.valueOf(request.getMethod())) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Request '"
                                + request.getMethod()
                                + " "
                                + getRequestPath(request)
                                + "'"
                                + " doesn't match '"
                                + httpMethod
                                + " "
                                + pattern);
            }

            return false;
        }

        RequestUrlParts url = getRequestPath(request);

        if (logger.isDebugEnabled()) {
            logger.debug("Checking match of request : '" + url + "'; against '" + pattern + "'");
        }
        boolean matched = matchesPath(url) && matchesQueryString(url);
        if (matched) {
            logger.debug("Matched " + url + " with " + pattern);
        }
        return matched;
    }

    private boolean matchesQueryString(RequestUrlParts url) {
        if (queryStringMatcher != null) {
            return queryStringMatcher.matches(url.getQueryString());
        }
        return true;
    }

    private boolean matchesPath(RequestUrlParts url) {
        if (pattern.equals(MATCH_ALL)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Request matched by universal pattern '/**'");
            }

            return true;
        }
        return matcher.matches(url.getPath());
    }

    private RequestUrlParts getRequestPath(HttpServletRequest request) {
        String url = request.getServletPath();

        if (request.getPathInfo() != null) {
            url += request.getPathInfo();
        }

        url = url.toLowerCase();

        String queryString = request.getQueryString();

        return new RequestUrlParts(url, queryString);
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IncludeQueryStringAntPathRequestMatcher)) {
            return false;
        }
        IncludeQueryStringAntPathRequestMatcher other =
                (IncludeQueryStringAntPathRequestMatcher) obj;
        return this.pattern.equals(other.pattern) && this.httpMethod == other.httpMethod;
    }

    @Override
    public int hashCode() {
        int code = 31 ^ pattern.hashCode();
        if (httpMethod != null) {
            code ^= httpMethod.hashCode();
        }
        return code;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ant [pattern='").append(pattern).append("'");

        if (httpMethod != null) {
            sb.append(", ").append(httpMethod);
        }

        sb.append("]");

        return sb.toString();
    }

    private static interface Matcher {
        boolean matches(String path);
    }

    private static class SpringAntMatcher implements Matcher {
        private static final AntPathMatcher antMatcher = new AntPathMatcher();

        private final String pattern;

        private SpringAntMatcher(String pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String path) {
            return antMatcher.match(pattern, path);
        }
    }

    private static class QueryStringMatcher implements Matcher {

        private Pattern pattern = null;

        private QueryStringMatcher(String pattern) {
            try {
                this.pattern = Pattern.compile(parsePattern(pattern), Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                logger.error("Error in filter chain query string pattern", e);
            }
        }

        private String parsePattern(String unparsed) {
            if (!unparsed.startsWith("^")) {
                unparsed = "^" + unparsed;
            }
            if (!unparsed.endsWith("$")) {
                unparsed = unparsed + "$";
            }
            return unparsed;
        }

        public boolean matches(String path) {
            if (pattern != null && path != null) {
                return pattern.matcher(path).matches();
            }
            return false;
        }
    }

    /** Optimized matcher for trailing wildcards */
    private static class SubpathMatcher implements Matcher {
        private final String subpath;
        private final int length;

        private SubpathMatcher(String subpath) {
            assert !subpath.contains("*");
            this.subpath = subpath;
            this.length = subpath.length();
        }

        public boolean matches(String path) {
            return path.startsWith(subpath)
                    && (path.length() == length || path.charAt(length) == '/');
        }
    }

    /** Value object for request parts handled by different matchers. */
    private static class RequestUrlParts {
        private String path;
        private String queryString;

        public RequestUrlParts(String path, String queryString) {
            super();
            this.path = path;
            this.queryString = queryString;
        }

        public String getPath() {
            return path;
        }

        public String getQueryString() {
            return queryString;
        }

        @Override
        public String toString() {
            return "Path: " + path + ", QueryString: " + queryString;
        }
    }
}
