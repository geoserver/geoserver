/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.converters.XStreamJSONMessageConverter;
import org.geoserver.rest.resources.ResourceController.ResourceChildInfo;
import org.geoserver.rest.resources.ResourceController.ResourceDirectoryInfo;
import org.geoserver.rest.resources.ResourceController.ResourceParentInfo;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Message converter to maintain backwards compatibility on JSON encoding of {@link
 * ResourceDirectoryInfo} for single-element {@link ResourceDirectoryInfo#getChildren() children}.
 *
 * <p>The upgrade to XStream 1.4.19/Jettison 1.4.1 changes the deafult Jettison 1.0.1 behavior when
 * encoding single-element collections as JSON. While it used to encode such collections as a JSON
 * object, it now (correctly) encodes them as single-element JSON arrays.
 *
 * <p>This converter preserves the legacy behavior for {@link ResourceController}'s {@link
 * ResourceDirectoryInfo#getChildren()} to avoid any possible breakage in GeoServer REST clients
 * that depend on the legacy behavior.
 */
@Component
public class ResourceDirectoryInfoJSONConverter extends XStreamJSONMessageConverter {

    @Override
    protected boolean supports(Class<?> clazz) {
        return RestWrapper.class.isAssignableFrom(clazz)
                && !RestListWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return supports(clazz) && canWrite(mediaType);
    }

    @Override
    public void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        RestWrapper<?> restWrapper = (RestWrapper<?>) o;
        Object object = restWrapper.getObject();
        if (object instanceof ResourceDirectoryInfo) {
            ResourceDirectoryInfo dirInfo = (ResourceDirectoryInfo) object;
            if (1 == dirInfo.getChildren().size()) {
                boolean alwaysSerializeCollectionsAsArrays = true;
                XStreamPersister xmlPersister =
                        xpf.createJSONPersister(alwaysSerializeCollectionsAsArrays);
                restWrapper.configurePersister(xmlPersister, this);
                writeSingleChildrenDirectoryInfo(dirInfo, xmlPersister, outputMessage.getBody());
                return;
            }
        }
        super.writeInternal(o, outputMessage);
    }

    private void writeSingleChildrenDirectoryInfo(
            ResourceDirectoryInfo dirInfo, XStreamPersister xmlPersister, OutputStream out)
            throws IOException {

        SingleChildDirInfo info = new SingleChildDirInfo(dirInfo);
        XStream xstream = xmlPersister.getXStream();
        xstream.alias("ResourceDirectory", SingleChildDirInfo.class);

        Converter conv =
                new CollectionConverter(xstream.getMapper()) {
                    @Override
                    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
                        return Collection.class.isAssignableFrom(type);
                    }

                    @Override
                    protected void writeCompleteItem(
                            Object item,
                            MarshallingContext context,
                            HierarchicalStreamWriter writer) {

                        super.writeBareItem(item, context, writer);
                    }
                };

        xstream.registerLocalConverter(Children.class, "child", conv);
        xmlPersister.save(info, out);
    }

    static class SingleChildDirInfo {
        String name;
        ResourceParentInfo parent;
        String type;
        Date lastModified;
        Children children;

        public SingleChildDirInfo(ResourceDirectoryInfo info) {
            this.lastModified = info.getLastModified();
            this.name = info.getName();
            this.parent = info.getParent();
            this.type = info.getType();
            this.children = new Children(info.getChildren());
        }
    }

    static class Children {
        List<ResourceChildInfo> child;

        Children(List<ResourceChildInfo> child) {
            this.child = child;
        }
    }
}
