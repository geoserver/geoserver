/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.rest.CatalogResourceBase;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.wfs.xslt.config.TransformInfo;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * The resource representing the transformation itself
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class TransformResource extends CatalogResourceBase {

    /**
     * media type for XSLT
     */
    public static final MediaType MEDIATYPE_XSLT = new MediaType("application/xslt+xml");
    static {
        MediaTypes.registerExtension("xslt", MEDIATYPE_XSLT);
    }

    private TransformRepository repository;

    public TransformResource(Context context, Request request, Response response,
            TransformRepository repository, Catalog catalog) {
        super(context, request, response, TransformInfo.class, catalog);
        this.repository = repository;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = super.createSupportedFormats(request, response);
        formats.add(new XSLTDataFormat(MEDIATYPE_XSLT));
        return formats;
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new TransformHTMLDataFormat(clazz, request, response, this);
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String transform = RESTUtils.getAttribute(getRequest(), "transform");
        if (transform == null) {
            throw new RestletException("Failed to locate transformation " + transform,
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        TransformInfo info = repository.getTransformInfo(transform);
        DataFormat format = getFormatGet();
        if (format instanceof XSLTDataFormat) {
            return repository.getTransformSheet(info);
        } else {
            return info;
        }
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    @Override
    protected void handleObjectDelete() throws Exception {
        String transform = RESTUtils.getAttribute(getRequest(), "transform");
        TransformInfo info = repository.getTransformInfo(transform);
        repository.removeTransformInfo(info);
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String transform = RESTUtils.getAttribute(getRequest(), "transform");
        if (object instanceof TransformInfo) {
            TransformInfo info = (TransformInfo) object;
            // force the right name
            info.setName(transform);
            repository.putTransformInfo(info);
        } else {
            TransformInfo info = repository.getTransformInfo(transform);
            repository.putTransformSheet(info, IOUtils.toInputStream((String) object));
        }
    }

    @Override
    public boolean allowPost() {
        return RESTUtils.getAttribute(getRequest(), "transform") == null;
    }

    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String transform = getQueryStringValue("name");
        String sourceFormat = getQueryStringValue("sourceFormat");
        String outputFormat = getQueryStringValue("outputFormat");
        String outputMimeType = getQueryStringValue("outputMimeType");
        String fileExtension = getQueryStringValue("fileExtension");

        TransformInfo info;
        if (object instanceof TransformInfo) {
            info = (TransformInfo) object;
            if(transform != null) {
                info.setName(transform);
            }
            validate(info);
            repository.putTransformInfo(info);
            return info.getName();
        } else {
            // the format has turned the user provided stream into a string for validation purposes
            String xslt = (String) object;
            info = repository.getTransformInfo(transform);
            if (info == null) {
                info = new TransformInfo();
                info.setName(transform);
                info.setSourceFormat(sourceFormat);
                info.setOutputFormat(outputFormat);
                info.setOutputMimeType(outputMimeType);
                info.setFileExtension(fileExtension);
                info.setXslt(transform + ".xslt");
                validate(info);
                repository.putTransformInfo(info);
            }

            repository.putTransformSheet(info, new ByteArrayInputStream(xslt.getBytes()));
        }

        return info.getName();
    }

    private void validate(TransformInfo info) {
        if (info.getSourceFormat() == null) {
            throw new RestletException("The transformation must have a source format",
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
        if (info.getOutputFormat() == null) {
            throw new RestletException("The transformation must have an output format",
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    @Override
    protected void configureXStream(XStream xs) {
        super.configureXStream(xs);
        xs.alias("transform", TransformInfo.class);
        xs.registerConverter(new TransformConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.registerLocalConverter(TransformInfo.class, "featureType",
                new FeatureTypeLinkConverter());
        xs.addDefaultImplementation(FeatureTypeInfoImpl.class, FeatureTypeInfo.class);
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
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            TransformInfo original = (TransformInfo) source;
            TransformInfo resolved = new TransformInfo(original);
            FeatureTypeInfo ft = resolved.getFeatureType();
            if (ft != null) {
                resolved.setFeatureType((FeatureTypeInfo) ModificationProxy.unwrap(ft));
            }
            super.marshal(resolved, writer, context);
        }

    }

    private class FeatureTypeLinkConverter implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return true;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            FeatureTypeInfo ft = (FeatureTypeInfo) source;
            writer.startNode("name");
            writer.setValue(ft.prefixedName());
            writer.endNode();
            StoreInfo store = ft.getStore();
            WorkspaceInfo ws = store.getWorkspace();
            encodeLink(
                    "/workspaces/" + encode(ws.getName()) + "/datastores/"
                            + encode(store.getName()) + "/featuretypes/" + encode(ft.getName()),
                    writer, getFormatGet());
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
