/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
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
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CapabilitiesController. */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/sldservice")
public class CapabilitiesController extends BaseSLDServiceController {

    public CapabilitiesController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
            path = "/capabilities",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Object classificationCapabilities() {
        return wrapObject(new SldServiceCapabilities(), SldServiceCapabilities.class);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("capabilities", SldServiceCapabilities.class);
        xstream.registerConverter(new CapabilitiesConverter());
        xstream.allowTypes(new Class[] {SldServiceCapabilities.class});
    }

    class CapabilitiesConverter implements Converter {

        @Override
        public void marshal(
                Object o,
                HierarchicalStreamWriter hierarchicalStreamWriter,
                MarshallingContext marshallingContext) {
            SldServiceCapabilities caps = (SldServiceCapabilities) o;
            List<String> vectorMethods = caps.getVectorClassifications();
            hierarchicalStreamWriter.startNode("vector");
            encodeMethods(vectorMethods, hierarchicalStreamWriter);
            hierarchicalStreamWriter.endNode();
            List<String> rasterMethods = caps.getRasterClassifications();
            hierarchicalStreamWriter.startNode("raster");

            encodeMethods(rasterMethods, hierarchicalStreamWriter);
            hierarchicalStreamWriter.endNode();
        }

        private void encodeMethods(List<String> methods, HierarchicalStreamWriter writer) {
            for (String method : methods) {
                writer.startNode("classifications");
                writer.setValue(method);
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(
                HierarchicalStreamReader hierarchicalStreamReader,
                UnmarshallingContext unmarshallingContext) {
            return null;
        }

        @Override
        public boolean canConvert(Class aClass) {
            return SldServiceCapabilities.class.isAssignableFrom(aClass);
        }
    }
}
