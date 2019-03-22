/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/*
 * Copyright (C) 2004, 2005 Joe Walnes.
 * Copyright (C) 2006, 2007, 2010, 2011, 2013, 2016 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 * Created on 08. May 2004 by Joe Walnes
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.core.util.PresortedMap;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Converts a java.util.TreeMap to XML, and serializes the associated java.util.Comparator. The
 * converter assumes that the entries in the XML are already sorted according the comparator.
 *
 * <p>Cloned from XStream in order to avoid illegal reflective lookup warnings, might introduce
 * loading issues if there are circular references stored in the TreeSet. We don't have those right
 * now, the alternative would be open the java.util package for deep reflection from the command
 * line
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 */
public class TreeMapConverter extends MapConverter {

    private static final class NullComparator extends Mapper.Null implements Comparator {
        public int compare(Object o1, Object o2) {
            Comparable c1 = (Comparable) o1;
            return c1.compareTo(o2);
        }
    }

    private static final Comparator NULL_MARKER = new NullComparator();

    public TreeMapConverter(Mapper mapper) {
        super(mapper, TreeMap.class);
    }

    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        SortedMap sortedMap = (SortedMap) source;
        marshalComparator(mapper(), sortedMap.comparator(), writer, context);
        super.marshal(source, writer, context);
    }

    protected static void marshalComparator(
            Mapper mapper,
            Comparator comparator,
            HierarchicalStreamWriter writer,
            MarshallingContext context) {
        if (comparator != null) {
            writer.startNode("comparator");
            writer.addAttribute(
                    mapper.aliasForSystemAttribute("class"),
                    mapper.serializedClass(comparator.getClass()));
            context.convertAnother(comparator);
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        TreeMap result = null;
        final Comparator comparator = unmarshalComparator(mapper(), reader, context, result);
        if (result == null) {
            result = comparator == null ? new TreeMap() : new TreeMap(comparator);
        }
        populateTreeMap(reader, context, result, comparator);
        return result;
    }

    protected static Comparator unmarshalComparator(
            Mapper mapper,
            HierarchicalStreamReader reader,
            UnmarshallingContext context,
            TreeMap result) {
        final Comparator comparator;
        if (reader.hasMoreChildren()) {
            reader.moveDown();
            if (reader.getNodeName().equals("comparator")) {
                Class comparatorClass = HierarchicalStreams.readClassType(reader, mapper);
                comparator = (Comparator) context.convertAnother(result, comparatorClass);
            } else if (reader.getNodeName().equals("no-comparator")) { // pre 1.4 format
                comparator = null;
            } else {
                // we are already within the first entry
                return NULL_MARKER;
            }
            reader.moveUp();
        } else {
            comparator = null;
        }
        return comparator;
    }

    protected void populateTreeMap(
            HierarchicalStreamReader reader,
            UnmarshallingContext context,
            TreeMap result,
            Comparator comparator) {
        boolean inFirstElement = comparator == NULL_MARKER;
        if (inFirstElement) {
            comparator = null;
        }
        SortedMap sortedMap =
                new PresortedMap(
                        comparator != null && JVM.hasOptimizedTreeMapPutAll() ? comparator : null);
        if (inFirstElement) {
            // we are already within the first entry
            putCurrentEntryIntoMap(reader, context, result, sortedMap);
            reader.moveUp();
        }
        populateMap(reader, context, result, sortedMap);
    }
}
