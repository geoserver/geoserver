/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;

public class EncoderXStreamInitializer implements NotificationXStreamInitializer {

    /** Alias for encoder tag of in xml configuration */
    public String name;

    /** Class to use for encoder with filed 'name' */
    public Class<? extends NotificationEncoder> clazz;

    /**
     * Define an alias for the {@link DefaultNotificationProcessor#encoder encoder}<br>
     * Define a class for the {@link NotificationEncoder}<br>
     * An example of encoder configuration section in notifier.xml is:
     *
     * <pre>{@code
     *  <genericProcessor>
     *           <geonodeEncoder>
     *           ...
     *           </geonodeEncoder>
     * </genericProcessor>
     *
     * }</pre>
     *
     * @param xs XStream object
     */
    public EncoderXStreamInitializer(String name, Class<? extends NotificationEncoder> clazz) {
        super();
        this.name = name;
        this.clazz = clazz;
    }

    @Override
    public void init(XStream xs) {
        xs.aliasAttribute(DefaultNotificationProcessor.class, "encoder", name);
        xs.registerLocalConverter(
                DefaultNotificationProcessor.class,
                "encoder",
                new EncoderConverter(xs.getMapper(), xs.getReflectionProvider(), this));
    }

    /** @author Alessio Fabiani, GeoSolutions S.A.S. */
    public static class EncoderConverter extends ReflectionConverter {

        private EncoderXStreamInitializer encoderXStreamInitializer;

        /** */
        public EncoderConverter(
                Mapper mapper,
                ReflectionProvider reflectionProvider,
                EncoderXStreamInitializer encoderXStreamInitializer) {
            super(mapper, reflectionProvider);
            this.encoderXStreamInitializer = encoderXStreamInitializer;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public boolean canConvert(Class clazz) {
            return clazz.isAssignableFrom(encoderXStreamInitializer.clazz);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            NotificationEncoder encoder = null;
            String nodeName = reader.getNodeName();

            List<EncoderXStreamInitializer> serializers =
                    GeoServerExtensions.extensions(EncoderXStreamInitializer.class);

            for (EncoderXStreamInitializer serializer : serializers) {
                if (serializer.name.equals(nodeName)) {
                    try {
                        encoder = serializer.clazz.getDeclaredConstructor().newInstance();
                        encoder =
                                (NotificationEncoder)
                                        context.convertAnother(encoder, serializer.clazz);
                        break;
                    } catch (InstantiationException
                            | IllegalAccessException
                            | NoSuchMethodException
                            | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return encoder;
        }
    }
}
