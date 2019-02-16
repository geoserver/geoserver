/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S, http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.opengis.feature.type.PropertyDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ListAttributesController.
 *
 * @author kappu
 *     <p>Should get all Attributes related to a featureType we have internal Style add external SLD
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/sldservice")
public class ListAttributesController extends AbstractCatalogController {

    @Autowired
    public ListAttributesController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geoserver.rest.RestBaseController#configurePersister(org.geoserver.config.util.XStreamPersister,
     * org.geoserver.rest.converters.XStreamMessageConverter)
     */
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("Attributes", LayerAttributesList.class);
        xstream.registerConverter(new LayerAttributesListConverter());
        xstream.allowTypes(new Class[] {LayerAttributesList.class});
    }

    @GetMapping(
        path = "/{layerName}/attributes",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public Object attributes(
            @PathVariable String layerName,
            @RequestParam(value = "cache", required = false, defaultValue = "600") long cachingTime,
            final HttpServletResponse response) {
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (cachingTime > 0) {
            response.setHeader(
                    "cache-control",
                    CacheControl.maxAge(cachingTime, TimeUnit.SECONDS)
                            .cachePublic()
                            .getHeaderValue());
        }
        if (layerInfo == null) {
            return wrapObject(new ArrayList(), ArrayList.class);
        }

        if (layerInfo != null && layerInfo.getResource() instanceof FeatureTypeInfo) {
            ResourceInfo obj = layerInfo.getResource();
            Collection<PropertyDescriptor> attributes = null;
            /* Check if it's feature type or coverage */
            if (obj instanceof FeatureTypeInfo) {
                FeatureTypeInfo fTpInfo;
                fTpInfo = (FeatureTypeInfo) obj;

                LayerAttributesList out = new LayerAttributesList(layerName);
                try {
                    attributes = fTpInfo.getFeatureType().getDescriptors();
                    for (PropertyDescriptor attr : attributes) {
                        out.addAttribute(
                                attr.getName().getLocalPart(),
                                attr.getType().getBinding().getSimpleName());
                    }
                } catch (IOException e) {
                    throw new InvalidAttributes();
                }

                return wrapObject(out, LayerAttributesList.class);
            }
        }
        return wrapObject(new ArrayList(), ArrayList.class);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Error generating Attributes List!")
    private class InvalidAttributes extends RuntimeException {
        private static final long serialVersionUID = 7641473348901661113L;
    }

    /** @author Fabiani */
    public class LayerAttributesList {
        private String layerName;

        private Map<String, String> attributes = new HashMap<String, String>();

        public LayerAttributesList(final String layer) {
            layerName = layer;
        }

        public void addAttribute(final String name, final String type) {
            attributes.put(name, type);
        }

        public List<String> getAttributesNames() {
            List<String> out = new ArrayList<String>();

            for (String key : attributes.keySet()) {
                out.add(key);
            }

            return out;
        }

        public int getAttributesCount() {
            return attributes.size();
        }

        public String getAttributeName(final int index) {
            if (index >= getAttributesCount()) return null;

            int cnt = 0;
            for (String key : attributes.keySet()) {
                if (index == cnt) return key;
                cnt++;
            }

            return null;
        }

        public String getAttributeType(final String name) {
            return attributes.get(name);
        }

        /** @return the layerName */
        public String getLayerName() {
            return layerName;
        }
    }

    /** @author Fabiani */
    public class LayerAttributesListConverter implements Converter {

        /**
         * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java .lang.Class)
         */
        public boolean canConvert(Class clazz) {
            return LayerAttributesList.class.isAssignableFrom(clazz);
        }

        /**
         * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object ,
         *     com.thoughtworks.xstream.io.HierarchicalStreamWriter,
         *     com.thoughtworks.xstream.converters.MarshallingContext)
         */
        public void marshal(
                Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            final LayerAttributesList obj = (LayerAttributesList) value;

            writer.addAttribute("layer", obj.getLayerName());

            for (int k = 0; k < obj.getAttributesCount(); k++) {
                writer.startNode("Attribute");
                final String name = obj.getAttributeName(k);
                final String type = obj.getAttributeType(name);

                writer.startNode("name");
                writer.setValue(name);
                writer.endNode();

                writer.startNode("type");
                writer.setValue(type);
                writer.endNode();
                writer.endNode();
            }
        }

        /**
         * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks
         *     .xstream.io.HierarchicalStreamReader,
         *     com.thoughtworks.xstream.converters.UnmarshallingContext)
         */
        public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
