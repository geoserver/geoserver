/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/services/wfs/transforms")
public class TransformController extends AbstractCatalogController {

    @Autowired private TransformRepository repository;

    public TransformController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        path = {"", "{transform}"},
        produces = {
            MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper getTransformsInfo(
            @PathVariable(name = "transform", required = false) String transformInfoName) {
        if (transformInfoName == null) {
            // we need to return all transforms
            try {
                return wrapList(repository.getAllTransforms(), TransformInfo.class);
            } catch (Exception exception) {
                throw new RestException(
                        "Error reading transforms info from repository.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        exception);
            }
        }
        return wrapObject(getTransformInfo(transformInfoName), TransformInfo.class);
    }

    @GetMapping(path = "{transform}", produces = MediaTypeExtensions.APPLICATION_XSLT_VALUE)
    public void getTransforms(
            @PathVariable(name = "transform") String transformInfoName, OutputStream output) {
        InputStream transform = getTransform(transformInfoName);
        try {
            IOUtils.copy(transform, output);
        } catch (Exception exception) {
            throw new RestException(
                    String.format("Error writing transform '%s' XSLT.", transformInfoName),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
    }

    @PostMapping(
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public ResponseEntity<String> postTransformInfo(
            @RequestBody TransformInfo transformInfo, UriComponentsBuilder builder) {
        validate(transformInfo);
        saveTransFormInfo(transformInfo);
        return buildResponse(builder, transformInfo.getName(), HttpStatus.CREATED);
    }

    @PostMapping(consumes = MediaTypeExtensions.APPLICATION_XSLT_VALUE)
    public ResponseEntity<String> postTransform(
            InputStream transform,
            @RequestParam(name = "name", required = false) String transformInfoName,
            @RequestParam(name = "sourceFormat", required = false) String sourceFormat,
            @RequestParam(name = "outputFormat", required = false) String outputFormat,
            @RequestParam(name = "outputMimeType", required = false) String outputMimeType,
            @RequestParam(name = "fileExtension", required = false) String fileExtension,
            UriComponentsBuilder builder) {
        TransformInfo transformInfo;
        try {
            transformInfo = repository.getTransformInfo(transformInfoName);
        } catch (Exception exception) {
            throw new RestException(
                    String.format(
                            "Error reading transform '%s' info from repository.",
                            transformInfoName),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
        if (transformInfo == null) {
            transformInfo = new TransformInfo();
            transformInfo.setName(transformInfoName);
            transformInfo.setSourceFormat(sourceFormat);
            transformInfo.setOutputFormat(outputFormat);
            transformInfo.setOutputMimeType(outputMimeType);
            transformInfo.setFileExtension(fileExtension);
            transformInfo.setXslt(transformInfoName + ".xslt");
            validate(transformInfo);
            saveTransFormInfo(transformInfo);
        }
        saveTransForm(transformInfo, transform);
        return buildResponse(builder, transformInfo.getName(), HttpStatus.CREATED);
    }

    @PutMapping(
        path = "{transform}",
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public void putTransformInfo(
            @RequestBody TransformInfo transformInfo,
            @PathVariable(name = "transform") String transformInfoName) {
        transformInfo.setName(transformInfoName);
        validate(transformInfo);
        saveTransFormInfo(transformInfo);
    }

    @PutMapping(path = "{transform}", consumes = MediaTypeExtensions.APPLICATION_XSLT_VALUE)
    public void putTransform(
            InputStream transform, @PathVariable(name = "transform") String transformInfoName) {
        TransformInfo transformInfo = getTransformInfo(transformInfoName);
        saveTransForm(transformInfo, transform);
    }

    @DeleteMapping(path = "{transform}")
    public void putTransform(@PathVariable(name = "transform") String transformInfoName) {
        TransformInfo transformInfo = getTransformInfo(transformInfoName);
        try {
            repository.removeTransformInfo(transformInfo);
        } catch (Exception exception) {
            throw new RestException(
                    String.format("Error deleting transformation '%s'.", transformInfoName),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
    }

    private void saveTransForm(TransformInfo transformInfo, InputStream transform) {
        try {
            repository.putTransformSheet(transformInfo, transform);
        } catch (Exception exception) {
            throw new RestException(
                    String.format(
                            "Error writing transform '%s' XSLT info to repository.",
                            transformInfo.getName()),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
    }

    private void saveTransFormInfo(TransformInfo transformInfo) {
        try {
            repository.putTransformInfo(transformInfo);
        } catch (Exception exception) {
            throw new RestException(
                    String.format(
                            "Error writing transform '%s' info to repository.",
                            transformInfo.getName()),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
    }

    private ResponseEntity<String> buildResponse(
            UriComponentsBuilder builder, String transformInfoName, HttpStatus status) {
        UriComponents uriComponents =
                builder.path("/services/wfs/transforms/{transform}")
                        .buildAndExpand(transformInfoName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(transformInfoName, headers, status);
    }

    private void validate(TransformInfo info) {
        if (info.getSourceFormat() == null) {
            throw new RestException(
                    "The transformation must have a source format", HttpStatus.BAD_REQUEST);
        }
        if (info.getOutputFormat() == null) {
            throw new RestException(
                    "The transformation must have an output format", HttpStatus.BAD_REQUEST);
        }
    }

    private TransformInfo getTransformInfo(String transformInfoName) {
        TransformInfo transformInfo;
        try {
            transformInfo = repository.getTransformInfo(transformInfoName);
        } catch (Exception exception) {
            throw new RestException(
                    String.format(
                            "Error reading transform '%s' info from repository.",
                            transformInfoName),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
        if (transformInfo == null) {
            throw new ResourceNotFoundException(
                    String.format("Transform '%s' info not found.", transformInfoName));
        }
        return transformInfo;
    }

    private InputStream getTransform(String transformInfoName) {
        TransformInfo transformInfo = getTransformInfo(transformInfoName);
        InputStream transform;
        try {
            transform = repository.getTransformSheet(transformInfo);
        } catch (Exception exception) {
            throw new RestException(
                    String.format(
                            "Error reading transform '%s' XSLT from repository.",
                            transformInfoName),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception);
        }
        if (transform == null) {
            throw new ResourceNotFoundException(
                    String.format("Transform '%s' XSLT not found.", transformInfoName));
        }
        return transform;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return TransformInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.alias("transforms", Collection.class);
        xs.alias("transform", TransformInfo.class);
        xs.registerConverter(new TransformConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.registerLocalConverter(
                TransformInfo.class,
                "featureType",
                new FeatureTypeLinkConverter(catalog, converter));
        xs.addDefaultImplementation(FeatureTypeInfoImpl.class, FeatureTypeInfo.class);

        // setup security
        xs.allowTypes(new Class[] {TransformInfo.class});
    }

    private class TransformConverter extends ReflectionConverter {

        public TransformConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public boolean canConvert(Class type) {
            return TransformInfo.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TransformInfo original = (TransformInfo) source;
            TransformInfo resolved = new TransformInfo(original);
            FeatureTypeInfo ft = resolved.getFeatureType();
            if (ft != null) {
                resolved.setFeatureType(ModificationProxy.unwrap(ft));
            }
            super.marshal(resolved, writer, context);
        }
    }

    private static final class FeatureTypeLinkConverter implements Converter {

        private final XStreamMessageConverter converter;
        private final Catalog catalog;

        private FeatureTypeLinkConverter(Catalog catalog, XStreamMessageConverter converter) {
            this.catalog = catalog;
            this.converter = converter;
        }

        @Override
        public boolean canConvert(Class type) {
            return true;
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            FeatureTypeInfo ft = (FeatureTypeInfo) source;
            writer.startNode("name");
            writer.setValue(ft.prefixedName());
            writer.endNode();
            StoreInfo store = ft.getStore();
            WorkspaceInfo ws = store.getWorkspace();
            converter.encodeLink(
                    "/workspaces/"
                            + converter.encode(ws.getName())
                            + "/datastores/"
                            + converter.encode(store.getName())
                            + "/featuretypes/"
                            + converter.encode(ft.getName()),
                    writer);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String ref = null;
            if (reader.hasMoreChildren()) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    ref = reader.getValue();
                    reader.moveUp();
                }
            } else {
                ref = reader.getValue();
            }
            FeatureTypeInfo result = catalog.getFeatureType(ref);
            if (result == null) {
                result = catalog.getFeatureTypeByName(ref);
            }
            return result;
        }
    }
}
