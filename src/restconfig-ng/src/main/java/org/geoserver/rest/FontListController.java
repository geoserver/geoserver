package org.geoserver.rest;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.map.HashedMap;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geotools.renderer.style.FontCache;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import freemarker.template.Template;

@RestController
@RequestMapping(path = "/restng", produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
public class FontListController extends RestBaseController {

    @GetMapping(value = "/fonts", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public Map<String, Set<String>> getFonts() {
        FontCache cache = FontCache.getDefaultInstance();

        Map<String, Set<String>> fonts = new HashedMap();

        fonts.put("fonts", new TreeSet<>(cache.getAvailableFonts()));

        return fonts;
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        // TODO Auto-generated method stub
        super.configureFreemarker(converter, template);
    }

}
