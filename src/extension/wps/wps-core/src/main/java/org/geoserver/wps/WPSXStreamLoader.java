/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geoserver.wps.validator.WPSInputValidator;
import org.geotools.feature.NameImpl;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/**
 * Service loader for the Web Processing Service
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WPSXStreamLoader extends XStreamServiceLoader<WPSInfo> {

    static final Logger LOGGER = Logging.getLogger(WPSXStreamLoader.class);

    static final ProcessGroupInfo PROCESS_GROUP_ERROR = new ProcessGroupInfoImpl();

    public WPSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wps");
    }

    public Class<WPSInfo> getServiceClass() {
        return WPSInfo.class;
    }

    protected WPSInfo createServiceFromScratch(GeoServer gs) {
        WPSInfo wps = new WPSInfoImpl();
        wps.setName("WPS");
        wps.setGeoServer(gs);
        wps.getVersions().add(new Version("1.0.0"));
        wps.setMaxAsynchronousProcesses(Runtime.getRuntime().availableProcessors() * 2);
        wps.setMaxSynchronousProcesses(Runtime.getRuntime().availableProcessors() * 2);
        return wps;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        XStream xs = xp.getXStream();
        // Use custom converter to manage previous wps.xml configuration format
        xs.registerConverter(new ProcessGroupConverter(xs.getMapper(), xs.getReflectionProvider()));
        xs.alias("wps", WPSInfo.class, WPSInfoImpl.class);
        xs.alias("processGroup", ProcessGroupInfoImpl.class);
        xs.alias("name", NameImpl.class);
        xs.alias("name", Name.class, NameImpl.class);
        xs.alias("accessInfo", ProcessInfoImpl.class);
        xs.registerConverter(new NameConverter());
        ClassAliasingMapper mapper = new ClassAliasingMapper(xs.getMapper());
        mapper.addClassAlias("role", String.class);
        xs.registerLocalConverter(
                ProcessGroupInfoImpl.class, "roles", new CollectionConverter(mapper));
        xs.registerLocalConverter(ProcessInfoImpl.class, "roles", new CollectionConverter(mapper));
        xs.registerLocalConverter(
                ProcessInfoImpl.class,
                "validators",
                new XStreamPersister.MultimapConverter(mapper));
        xs.registerLocalConverter(
                WPSInfoImpl.class, "processGroups", new WPSCollectionConverter(mapper));
        xs.alias("maxSizeValidator", MaxSizeValidator.class);
        xs.alias("maxMultiplicityValidator", MultiplicityValidator.class);
        xs.alias("rangeValidator", NumberRangeValidator.class);
        xs.registerLocalConverter(
                NumberRangeValidator.class,
                "range",
                new NumberRangeConverter(xs.getMapper(), xs.getReflectionProvider()));

        xs.allowTypeHierarchy(ProcessGroupInfo.class);
        xs.allowTypeHierarchy(WPSInputValidator.class);
    }

    @Override
    protected WPSInfo initialize(WPSInfo service) {
        // TODO: move this code block to the parent class
        if (service.getKeywords() == null) {
            ((WPSInfoImpl) service).setKeywords(new ArrayList());
        }
        if (service.getExceptionFormats() == null) {
            ((WPSInfoImpl) service).setExceptionFormats(new ArrayList());
        }
        if (service.getMetadata() == null) {
            ((WPSInfoImpl) service).setMetadata(new MetadataMap());
        }
        if (service.getClientProperties() == null) {
            ((WPSInfoImpl) service).setClientProperties(new HashMap());
        }
        if (service.getVersions() == null) {
            ((WPSInfoImpl) service).setVersions(new ArrayList());
        }
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        if (service.getConnectionTimeout() == 0) {
            // timeout has not yet been specified. Use default
            service.setConnectionTimeout(WPSInfoImpl.DEFAULT_CONNECTION_TIMEOUT);
        }
        if (service.getProcessGroups() == null) {
            ((WPSInfoImpl) service).setProcessGroups(new ArrayList());
        } else {
            for (ProcessGroupInfo pg : service.getProcessGroups()) {
                if (pg.getRoles() == null) {
                    pg.setRoles(new ArrayList<String>());
                }
                if (pg.getMetadata() == null) {
                    ((ProcessGroupInfoImpl) pg).setMetadata(new MetadataMap());
                }
                if (pg.getFilteredProcesses() == null) {
                    ((ProcessGroupInfoImpl) pg).setFilteredProcesses(new ArrayList<ProcessInfo>());
                } else {
                    for (ProcessInfo pi : pg.getFilteredProcesses()) {
                        if (pi.getRoles() == null) {
                            ((ProcessInfoImpl) pi).setRoles(new ArrayList<String>());
                        }
                        if (pi.getValidators() == null) {
                            Multimap<String, WPSInputValidator> validators =
                                    ArrayListMultimap.create();
                            ((ProcessInfoImpl) pi).setValidators(validators);
                        }
                        if (pi.getMetadata() == null) {
                            ((ProcessInfoImpl) pi).setMetadata(new MetadataMap());
                        }
                    }
                }
            }
        }
        if (service.getName() == null) {
            service.setName("WPS");
        }

        return service;
    }

    /** Converter for {@link Name} */
    public static class NameConverter extends AbstractSingleValueConverter {

        @Override
        public boolean canConvert(Class type) {
            return Name.class.isAssignableFrom(type);
        }

        @Override
        public String toString(Object obj) {
            Name name = (Name) obj;
            return name.getNamespaceURI() + ":" + name.getLocalPart();
        }

        @Override
        public Object fromString(String str) {
            int idx = str.indexOf(":");
            if (idx == -1) {
                return new NameImpl(str);
            } else {
                String prefix = str.substring(0, idx);
                String local = str.substring(idx + 1);
                return new NameImpl(prefix, local);
            }
        }
    }

    /**
     * Manages unmarshalling of {@link ProcessGroupInfoImpl} taking into account previous wps.xml
     * format in witch {@link ProcessGroupInfoImpl #getFilteredProcesses()} is a collection of
     * {@link NameImpl}
     */
    public static class ProcessGroupConverter extends ReflectionConverter {

        public ProcessGroupConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public boolean canConvert(Class clazz) {
            return ProcessGroupInfoImpl.class == clazz;
        }

        @Override
        public Object doUnmarshal(
                Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
            try {
                ProcessGroupInfo converted =
                        (ProcessGroupInfo) super.doUnmarshal(result, reader, context);

                if (converted.getFilteredProcesses() != null) {
                    List<ProcessInfo> newFilteredProcesses = new ArrayList<ProcessInfo>();
                    for (Object fp : converted.getFilteredProcesses()) {
                        if (fp instanceof NameImpl) {
                            NameImpl ni = (NameImpl) fp;
                            ProcessInfo pi = new ProcessInfoImpl();
                            pi.setName(ni);
                            pi.setEnabled(false);
                            newFilteredProcesses.add(pi);
                        } else {
                            break;
                        }
                    }
                    if (!newFilteredProcesses.isEmpty()) {
                        converted.getFilteredProcesses().clear();
                        converted.getFilteredProcesses().addAll(newFilteredProcesses);
                    }
                }

                return converted;
            } catch (ConversionException e) {
                LOGGER.log(Level.WARNING, "Error unmarshaling WPS Process Group", e);
                List<String> expectedHierarchy =
                        Arrays.asList("wps", "processGroups", "processGroup");
                while (!expectedHierarchy.contains(reader.getNodeName())) {
                    // Abort parsing this process group, and reset the reader to a safe state
                    reader.moveUp();
                }
                return PROCESS_GROUP_ERROR;
            }
        }
    }

    public static class NumberRangeConverter extends ReflectionConverter {

        public NumberRangeConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public boolean canConvert(Class type) {
            return NumberRange.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            NumberRange<?> range = (NumberRange<?>) source;
            writer.startNode("minValue");
            writer.setValue(String.valueOf(range.getMinValue()));
            writer.endNode();
            writer.startNode("maxValue");
            writer.setValue(String.valueOf(range.getMaxValue()));
            writer.endNode();
            if (!range.isMinIncluded()) {
                writer.startNode("isMinIncluded");
                writer.setValue("false");
                writer.endNode();
            }
            if (!range.isMaxIncluded()) {
                writer.startNode("isMaxIncluded");
                writer.setValue("false");
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            reader.moveDown();
            double min = Converters.convert(reader.getValue(), Double.class);
            reader.moveUp();
            reader.moveDown();
            double max = Converters.convert(reader.getValue(), Double.class);
            reader.moveUp();
            NumberRange<Double> range = new NumberRange<>(Double.class, min, max);
            return range;
        }
    }

    /**
     * {@link CollectionConverter} variant used with {@link ProcessGroupConverter} to handle errors
     * thrown by unresolvable process handler classes.
     */
    public static class WPSCollectionConverter extends CollectionConverter {

        public WPSCollectionConverter(Mapper mapper) {
            super(mapper);
        }

        public WPSCollectionConverter(Mapper mapper, Class type) {
            super(mapper, type);
        }

        @Override
        protected void addCurrentElementToCollection(
                HierarchicalStreamReader reader,
                UnmarshallingContext context,
                Collection collection,
                Collection target) {
            Object item = this.readBareItem(reader, context, collection);
            // Remove anything that threw an error upon deserialization
            if (!PROCESS_GROUP_ERROR.equals(item)) {
                target.add(item);
            }
        }
    }
}
