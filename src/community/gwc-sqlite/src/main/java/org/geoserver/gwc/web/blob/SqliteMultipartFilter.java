package org.geoserver.gwc.web.blob;

import org.geoserver.filters.GeoServerFilter;
import org.springframework.web.multipart.support.MultipartFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SqliteMultipartFilter extends MultipartFilter implements GeoServerFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.contains("gwc/rest/sqlite/")) {
            super.doFilterInternal(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
