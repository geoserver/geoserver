/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.feature.type.ProfileImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;

/**
 * A special profile of a pariticular {@link Schema} which maintains a unique mapping of java class
 * to xml schema type.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TypeMappingProfile /*extends ProfileImpl*/ {

    /** Set of profiles to do mappings from. */
    Set<Schema> profiles;

    //    public TypeMappingProfile(Schema schema, Set profile) {
    //        super(schema, profile);
    //    }

    public TypeMappingProfile(Set<Schema> profiles) {
        this.profiles = profiles;
    }

    /**
     * Obtains the {@link AttributeDescriptor} mapped to a particular class.
     *
     * <p>If an exact match cannot be made, then those types which are supertypes of clazz are
     * examined.
     *
     * @param clazz The class.
     * @return The AttributeType, or <code>null</code> if no atttribute type mapped to <code>clazz
     *     </code>
     */
    public AttributeType type(Class<?> clazz) {
        List<AttributeType> assignable = new ArrayList<>();

        for (Object o : profiles) {
            ProfileImpl profile = (ProfileImpl) o;

            for (AttributeType type : profile.values()) {
                if (type.getBinding().isAssignableFrom(clazz)) {
                    assignable.add(type);
                }

                if (clazz.equals(type.getBinding())) {
                    return type;
                }
            }
        }

        if (assignable.isEmpty()) {
            return null;
        }

        if (assignable.size() == 1) {
            return assignable.get(0);
        } else {
            // sort
            Comparator<AttributeType> comparator =
                    (a1, a2) -> {
                        Class<?> c1 = a1.getBinding();
                        Class<?> c2 = a2.getBinding();

                        if (c1.equals(c2)) {
                            return 0;
                        }

                        if (c1.isAssignableFrom(c2)) {
                            return 1;
                        }

                        return -1;
                    };

            Collections.sort(assignable, comparator);

            if (!assignable.get(0).equals(assignable.get(1))) {
                return assignable.get(0);
            }
        }

        return null;
    }

    /**
     * Obtains the {@link Name} of the {@link AttributeDescriptor} mapped to a particular class.
     *
     * @param clazz The class.
     * @return The Name, or <code>null</code> if no atttribute type mapped to <code>clazz</code>
     */
    public Name name(Class<?> clazz) {
        List<Map.Entry> assignable = new ArrayList<>();

        for (Object o : profiles) {
            ProfileImpl profile = (ProfileImpl) o;

            for (Map.Entry<Name, AttributeType> nameAttributeTypeEntry : profile.entrySet()) {
                Map.Entry entry = (Map.Entry) nameAttributeTypeEntry;
                AttributeType type = (AttributeType) entry.getValue();

                if (type.getBinding().isAssignableFrom(clazz)) {
                    assignable.add(entry);
                }

                if (clazz.equals(type.getBinding())) {
                    return (Name) entry.getKey();
                }
            }
        }

        if (assignable.isEmpty()) {
            return null;
        }

        if (assignable.size() == 1) {
            return (Name) assignable.get(0).getKey();
        } else {
            // sort
            Comparator<Map.Entry> comparator =
                    (e1, e2) -> {
                        AttributeType a1 = (AttributeType) e1.getValue();
                        AttributeType a2 = (AttributeType) e2.getValue();

                        Class<?> c1 = a1.getBinding();
                        Class<?> c2 = a2.getBinding();

                        if (c1.equals(c2)) {
                            return 0;
                        }

                        if (c1.isAssignableFrom(c2)) {
                            return 1;
                        }

                        return -1;
                    };

            Collections.sort(assignable, comparator);

            Map.Entry e1 = assignable.get(0);
            Map.Entry e2 = assignable.get(1);
            AttributeType a1 = (AttributeType) e1.getValue();
            AttributeType a2 = (AttributeType) e2.getValue();

            if (!a1.equals(a2)) {
                return (Name) e1.getKey();
            }
        }

        return null;
    }
}
