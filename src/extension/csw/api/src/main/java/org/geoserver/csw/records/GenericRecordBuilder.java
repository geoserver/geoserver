/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.csw.util.PropertyPath;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.data.complex.util.ComplexFeatureConstants;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.feature.type.Types;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * A generic helper that builds CSW records as GeoTools features
 *
 * @author Niels Charlier
 */
public class GenericRecordBuilder implements RecordBuilder {

    /** A user property of the boundingBox attribute containing the original envelopes of the Record. */
    public static final String ORIGINAL_BBOXES = "RecordOriginalBounds";

    /** For wrapping simple content in complex attribute */
    protected static final AttributeType SIMPLE_TYPE =
            new AttributeTypeImpl(ComplexFeatureConstants.SIMPLE_CONTENT, String.class, false, false, null, null, null);

    /** For wrapping simple content in complex attribute */
    protected static final AttributeDescriptor SIMPLE_DESCRIPTOR =
            new AttributeDescriptorImpl(SIMPLE_TYPE, ComplexFeatureConstants.SIMPLE_CONTENT, 1, 1, true, null);

    protected ComplexFeatureBuilder fb;
    protected List<ReferencedEnvelope> boxes = new ArrayList<>();
    protected RecordDescriptor recordDescriptor;
    protected QueryablesMapping queryables;
    protected Map<Name, Name> substitutionMap = new HashMap<>();

    /**
     * A tree structure is built initially before the feature is built, so that all data for the feature can be
     * collected and properly structured to build the feature.
     */
    protected abstract static class TreeNode {
        @Override
        public abstract TreeNode clone();

        public boolean cleanUp() {
            // by default, do nothing
            return false;
        }
    }

    /** Leaf in the tree */
    protected static class TreeLeaf extends TreeNode {

        private Object value;
        private Map<Object, Object> userData;

        public TreeLeaf() {}

        public void setValue(Object value, Map<Object, Object> userData) {
            this.value = value;
            this.userData = userData;
        }

        public Object getValue() {
            return value;
        }

        public Map<Object, Object> getUserData() {
            return userData;
        }

        @Override
        public TreeLeaf clone() {
            TreeLeaf leaf = new TreeLeaf();
            leaf.value = value;
            if (userData != null) {
                leaf.userData = new HashMap<>();
                leaf.userData.putAll(userData);
            }
            return leaf;
        }
    }

    /** Fork in the tree (splitting nodes) */
    protected static class TreeFork {
        private AttributeDescriptor descriptor;
        private List<TreeNode> nodes = new ArrayList<>();
        private boolean multiple = false;

        public TreeFork(AttributeDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public TreeFork(AttributeDescriptor descriptor, TreeNode node) {
            this(descriptor);
            nodes.add(node);
        }

        public AttributeDescriptor getDescriptor() {
            return descriptor;
        }

        private void expand(int size, TreeNode model) {
            if (!multiple && nodes.size() == 1) {
                multiple = true;
                while (nodes.size() < size) {
                    nodes.add(nodes.get(0).clone());
                }
            } else {
                while (nodes.size() < size) {
                    nodes.add(model.clone());
                }
            }
        }

        public List<TreeLeaf> getLeafs() {
            if (nodes.isEmpty()) {
                nodes.add(new TreeLeaf());
            }
            return nodes.stream().map(node -> (TreeLeaf) node).collect(Collectors.toList());
        }

        public TreeLeaf getLeaf() {
            return getLeafs().get(0);
        }

        public List<TreeBranch> getBranches() {
            if (nodes.isEmpty()) {
                nodes.add(new TreeBranch());
            }
            return nodes.stream().map(node -> (TreeBranch) node).collect(Collectors.toList());
        }

        public TreeBranch getBranch() {
            return getBranches().get(0);
        }

        public TreeLeaf getLeaf(int index) {
            expand(index + 1, new TreeLeaf());
            return (TreeLeaf) nodes.get(index);
        }

        public TreeBranch getBranch(int index) {
            expand(index + 1, new TreeBranch());
            return (TreeBranch) nodes.get(index);
        }

        @Override
        public TreeFork clone() {
            TreeFork fork = new TreeFork(descriptor);
            fork.multiple = multiple;
            for (TreeNode node : nodes) {
                fork.nodes.add(node.clone());
            }
            return fork;
        }
    }

    /** Branch in the tree */
    protected static class TreeBranch extends TreeNode {
        private Map<String, TreeFork> children = new HashMap<>();

        public TreeBranch() {
            super();
        }

        @Override
        public TreeBranch clone() {
            TreeBranch branch = new TreeBranch();
            for (Map.Entry<String, TreeFork> pair : children.entrySet()) {
                branch.children.put(pair.getKey(), pair.getValue().clone());
            }
            return branch;
        }

        @Override
        public boolean cleanUp() {
            boolean empty = true;
            for (TreeFork child : children.values()) {
                Iterator<TreeNode> it = child.nodes.iterator();
                while (it.hasNext()) {
                    if (it.next().cleanUp()) {
                        it.remove();
                    } else {
                        empty = false;
                    }
                }
            }
            return empty;
        }

        public TreeFork getFork(AttributeDescriptor descriptor) {
            return children.computeIfAbsent(descriptor.getName().getLocalPart(), attName -> new TreeFork(descriptor));
        }

        public TreeFork getSimpleFork() {
            return getFork(SIMPLE_DESCRIPTOR);
        }
    }

    /** The root of the tree */
    protected TreeBranch root = new TreeBranch();

    /**
     * Start Generic Record Builder based on the Record Descriptor
     *
     * @param recordDescriptor The Record Descriptor
     */
    public GenericRecordBuilder(RecordDescriptor recordDescriptor) {
        this(recordDescriptor, recordDescriptor);
    }

    /**
     * Start Generic Record Builder based on the Record Descriptor
     *
     * @param recordDescriptor The Record Descriptor
     * @param queryables The queryables
     */
    public GenericRecordBuilder(RecordDescriptor recordDescriptor, QueryablesMapping queryables) {
        this.recordDescriptor = recordDescriptor;
        this.queryables = queryables;
        fb = new ComplexFeatureBuilder(recordDescriptor.getFeatureDescriptor());

        for (PropertyDescriptor descriptor : recordDescriptor.getFeatureType().getDescriptors()) {
            @SuppressWarnings("unchecked")
            List<AttributeDescriptor> substitutionGroup =
                    (List<AttributeDescriptor>) descriptor.getUserData().get("substitutionGroup");
            if (substitutionGroup != null) {
                for (AttributeDescriptor attributeDescriptor : substitutionGroup) {
                    substitutionMap.put(attributeDescriptor.getName(), descriptor.getName());
                }
            }
            substitutionMap.put(descriptor.getName(), descriptor.getName());
        }
    }

    /** Helper method for creating attributes in the tree structure */
    @SuppressWarnings("unchecked")
    private void createAttribute(
            TreeBranch branch,
            int index,
            ComplexType type,
            PropertyPath path,
            List<Object> value,
            Map<Object, Object> userData,
            int[] splitIndex) {

        String attName = path.getName(index);
        Integer attIndex = path.getIndex(index);

        AttributeDescriptor descriptor = (AttributeDescriptor) Types.findDescriptor(type, attName);
        if (descriptor == null) {
            throw new IllegalArgumentException("Cannot find descriptor for attribute "
                    + attName
                    + " in type "
                    + type.getName().toString());
        }

        int fromIndex = attIndex == null ? 0 : attIndex - 1;
        TreeFork fork = branch.getFork(descriptor);
        if (index == path.getSize() - 1) {
            if (descriptor.getType() instanceof ComplexType) {
                if (attIndex != null || value.size() > 1) {
                    for (int i = 0; i < value.size(); i++) {
                        if (value.get(i) != null) {
                            (fork.getBranch(fromIndex + i))
                                    .getSimpleFork()
                                    .getLeaf()
                                    .setValue(value.get(i), userData);
                        }
                    }
                } else if (value.get(0) != null) {
                    for (TreeBranch childBranch : fork.getBranches()) {
                        childBranch.getSimpleFork().getLeaf().setValue(value.get(0), userData);
                    }
                }
            } else {
                if (attIndex != null || value.size() > 1) {
                    for (int i = 0; i < value.size(); i++) {
                        if (value.get(i) != null) {
                            (fork.getLeaf(fromIndex + i)).setValue(value.get(i), userData);
                        }
                    }

                } else if (value.get(0) != null) {
                    for (TreeLeaf leaf : fork.getLeafs()) {
                        leaf.setValue(value.get(0), userData);
                    }
                }
            }
        } else {
            if (index < path.getSize() - 2 && Arrays.binarySearch(splitIndex, index) < 0) {
                if (attIndex != null) {
                    createAttribute(
                            fork.getBranch(fromIndex),
                            index + 1,
                            (ComplexType) descriptor.getType(),
                            path,
                            value,
                            userData,
                            splitIndex);
                } else {
                    for (TreeBranch childBranch : fork.getBranches()) {
                        createAttribute(
                                childBranch,
                                index + 1,
                                (ComplexType) descriptor.getType(),
                                path,
                                value,
                                userData,
                                splitIndex);
                    }
                }
            } else {
                for (int i = 0; i < value.size(); i++) {
                    Object item = value.get(i);
                    if (item != null) {
                        createAttribute(
                                fork.getBranch(fromIndex + i),
                                index + 1,
                                (ComplexType) descriptor.getType(),
                                path,
                                item instanceof List ? (List<Object>) item : Collections.singletonList(item),
                                userData,
                                splitIndex);
                    }
                }
            }
        }
    }

    /**
     * Adds an element to the current record with user data
     *
     * @param name path of property with dots
     * @param value the value(s) to be inserted
     * @param userData the user data
     */
    public void addElement(PropertyPath name, List<Object> value, Map<Object, Object> userData, int[] splitIndex) {

        createAttribute(root, 0, recordDescriptor.getFeatureType(), name, value, userData, splitIndex);
    }

    /**
     * Adds an element to the current record
     *
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    @Override
    public void addElement(PropertyPath name, Object... values) {
        addElement(name, Arrays.asList(values), null, new int[0]);
    }

    /**
     * Adds an element to the current record
     *
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    @Override
    public void addElement(PropertyPath name, int[] splitIndex, Object... values) {
        addElement(name, Arrays.asList(values), null, splitIndex);
    }

    /**
     * Adds a bounding box to the record. The envelope must be in WGS84
     *
     * @param env the bbox
     */
    @Override
    public void addBoundingBox(ReferencedEnvelope env) {
        boxes.add(env);
    }

    /**
     * Builds a record and sets up to work on the next one
     *
     * @param id record id
     * @return the Feature
     */
    @Override
    public Feature build(String id) {
        // gather all the bounding boxes in a single geometry
        Geometry geom = null;
        for (ReferencedEnvelope env : boxes) {
            try {
                env = env.transform(AbstractRecordDescriptor.DEFAULT_CRS, true);

                Polygon poly = JTS.toGeometry(env);
                poly.setUserData(AbstractRecordDescriptor.DEFAULT_CRS);
                if (geom == null) {
                    geom = poly;
                } else {
                    geom = geom.union(poly);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to reproject one of the bounding boxes to WGS84, "
                                + "this should never happen with valid coordinates",
                        e);
            }
        }
        if (geom instanceof Polygon) {
            geom = geom.getFactory().createMultiPolygon(new Polygon[] {(Polygon) geom});
        }

        if (queryables.getBoundingBoxPropertyName() != null) {
            Map<Object, Object> userData = Collections.singletonMap(ORIGINAL_BBOXES, new ArrayList<>(boxes));
            addElement(
                    PropertyPath.fromDotPath(queryables.getBoundingBoxPropertyName()),
                    Collections.singletonList(geom),
                    userData,
                    new int[0]);
        }

        root.cleanUp(); // remove empty tags
        for (TreeFork fork : root.children.values()) {
            for (TreeNode node : fork.nodes) {
                Attribute att = buildNode(fork, node);
                fb.append(substitutionMap.get(att.getName()), att);
            }
        }

        boxes.clear();
        root = new TreeBranch();
        return fb.buildFeature(id);
    }

    /**
     * Helper method for building feature from tree node
     *
     * @param node the node
     * @return list of attributes to be added to feature
     */
    private Attribute buildNode(TreeFork fork, TreeNode node) {

        AttributeBuilder ab = new AttributeBuilder(new LenientFeatureFactoryImpl());

        if (node instanceof TreeLeaf) {

            TreeLeaf leaf = (TreeLeaf) node;

            ab.setDescriptor(fork.descriptor);
            Attribute att = ab.buildSimple(null, leaf.value);
            if (leaf.userData != null) {
                att.getUserData().putAll(leaf.userData);
            }
            return att;

        } else if (node instanceof TreeBranch) {

            ab.setDescriptor(fork.descriptor);
            List<Attribute> list = new ArrayList<>();
            for (TreeFork childFork : ((TreeBranch) node).children.values()) {
                for (TreeNode childNode : childFork.nodes) {
                    list.add(buildNode(childFork, childNode));
                }
            }
            return ab.createComplexAttribute(list, null, fork.descriptor, null);
        }

        return null;
    }
}
