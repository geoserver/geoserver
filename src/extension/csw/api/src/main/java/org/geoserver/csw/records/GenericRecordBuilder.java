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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

/**
 * A generic helper that builds CSW records as GeoTools features
 *
 * @author Niels Charlier
 */
public class GenericRecordBuilder implements RecordBuilder {

    /**
     * A user property of the boundingBox attribute containing the original envelopes of the Record.
     */
    public static final String ORIGINAL_BBOXES = "RecordOriginalBounds";

    private static final Pattern PATTERN_ATT_WITH_INDEX = Pattern.compile("([^\\[]*)\\[(.*)\\]");

    protected ComplexFeatureBuilder fb;
    protected List<ReferencedEnvelope> boxes = new ArrayList<ReferencedEnvelope>();
    protected RecordDescriptor recordDescriptor;
    protected Map<Name, Name> substitutionMap = new HashMap<Name, Name>();

    /**
     * A tree structure is built initially before the feature is built, so that all data for the
     * feature can be collected and properly structured to build the feature.
     */
    protected abstract static class TreeNode {
        AttributeDescriptor descriptor = null;

        public abstract TreeNode clone();

        public boolean cleanUp() {
            // by default, do nothing
            return false;
        }
    }

    protected static class TreeLeaf extends TreeNode {
        public Object value;
        public Map<Object, Object> userData;

        @Override
        public TreeLeaf clone() {
            TreeLeaf leaf = new TreeLeaf();
            leaf.value = value;
            if (userData != null) {
                leaf.userData = new HashMap<Object, Object>();
                leaf.userData.putAll(userData);
            }
            leaf.descriptor = descriptor;
            return leaf;
        }
    }

    protected static class TreeBranch extends TreeNode {
        public Map<String, List<TreeNode>> children = new HashMap<String, List<TreeNode>>();

        @Override
        public TreeBranch clone() {
            TreeBranch branch = new TreeBranch();
            for (Map.Entry<String, List<TreeNode>> pair : children.entrySet()) {
                List<TreeNode> list = new ArrayList<TreeNode>();
                for (TreeNode node : pair.getValue()) {
                    list.add(node.clone());
                }
            }
            branch.descriptor = descriptor;
            return branch;
        }

        @Override
        public boolean cleanUp() {
            boolean empty = true;
            for (List<TreeNode> child : children.values()) {
                Iterator<TreeNode> it = child.iterator();
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
    }

    /** The root of the tree */
    protected TreeBranch root = new TreeBranch();

    /**
     * Start Generic Record Builder based on the Record Descriptor
     *
     * @param recordDescriptor The Record Descriptor
     */
    public GenericRecordBuilder(RecordDescriptor recordDescriptor) {
        this.recordDescriptor = recordDescriptor;
        fb = new ComplexFeatureBuilder(recordDescriptor.getFeatureDescriptor());

        for (PropertyDescriptor descriptor : recordDescriptor.getFeatureType().getDescriptors()) {
            @SuppressWarnings("unchecked")
            List<AttributeDescriptor> substitutionGroup =
                    (List<AttributeDescriptor>) descriptor.getUserData().get("substitutionGroup");
            if (substitutionGroup != null) {
                for (Iterator<AttributeDescriptor> it = substitutionGroup.iterator();
                        it.hasNext(); ) {
                    substitutionMap.put(it.next().getName(), descriptor.getName());
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
            String[] path,
            List<Object> value,
            Map<Object, Object> userData,
            int[] splitIndex) {

        AttributeDescriptor descriptor =
                (AttributeDescriptor) Types.findDescriptor(type, attName(path[index]));

        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "Cannot find descriptor for attribute "
                            + path[index]
                            + " in type "
                            + type.getName().toString());
        }

        List<TreeNode> treenodes = branch.children.get(path[index]);

        if (treenodes == null) {
            treenodes = new ArrayList<TreeNode>();
            branch.children.put(path[index], treenodes);
        }

        if (index == path.length - 1) {
            if (descriptor.getType() instanceof ComplexType) {
                if (treenodes.isEmpty()) {
                    for (int i = 0; i < value.size(); i++) {
                        TreeNode child = new TreeBranch();
                        child.descriptor = descriptor;
                        treenodes.add(child);
                    }
                } else if (treenodes.size() == 1) {
                    for (int i = 1; i < value.size(); i++) {
                        treenodes.add(treenodes.get(0).clone());
                    }
                } else if (value.size() != 1 && treenodes.size() != value.size()) {
                    throw new IllegalArgumentException(
                            "Error in mapping: Number of values not matching.");
                }
                // wrap simple content in complex attribute
                AttributeType simpleType =
                        new AttributeTypeImpl(
                                ComplexFeatureConstants.SIMPLE_CONTENT,
                                String.class,
                                false,
                                false,
                                null,
                                null,
                                null);
                AttributeDescriptor simpleDescriptor =
                        new AttributeDescriptorImpl(
                                simpleType,
                                ComplexFeatureConstants.SIMPLE_CONTENT,
                                1,
                                1,
                                true,
                                (Object) null);
                for (int i = 0; i < Math.max(value.size(), treenodes.size()); i++) {
                    Object item = value.size() == 1 ? value.get(0) : value.get(i);
                    if (item != null) {
                        TreeLeaf leaf = new TreeLeaf();
                        leaf.userData = userData;
                        leaf.descriptor = simpleDescriptor;
                        leaf.value = value.size() == 1 ? value.get(0) : value.get(i);
                        leaf.userData = userData;
                        ((TreeBranch) treenodes.get(i))
                                .children.put(
                                        ComplexFeatureConstants.SIMPLE_CONTENT.getLocalPart(),
                                        Collections.singletonList(leaf));
                    }
                }

            } else {
                for (Object item : value) {
                    if (value != null) {
                        TreeLeaf leaf = new TreeLeaf();
                        leaf.userData = userData;

                        leaf.descriptor = descriptor;
                        leaf.value = item;
                        leaf.userData = userData;

                        treenodes.add(leaf);
                    }
                }
            }
        } else {
            if (index < path.length - 2 && Arrays.binarySearch(splitIndex, index) < 0) {
                if (treenodes.isEmpty()) {
                    TreeNode child = new TreeBranch();
                    child.descriptor = descriptor;
                    treenodes.add(child);
                }
                for (int i = 0; i < treenodes.size(); i++) {
                    createAttribute(
                            (TreeBranch) treenodes.get(i),
                            index + 1,
                            (ComplexType) descriptor.getType(),
                            path,
                            value,
                            userData,
                            splitIndex);
                }
            } else {
                if (treenodes.isEmpty()) {
                    for (int i = 0; i < value.size(); i++) {
                        TreeNode child = new TreeBranch();
                        child.descriptor = descriptor;
                        treenodes.add(child);
                    }
                } else if (treenodes.size() == 1) {
                    for (int i = 1; i < value.size(); i++) {
                        treenodes.add(treenodes.get(0).clone());
                    }
                } else if (value.size() != 1 && treenodes.size() != value.size()) {
                    throw new IllegalArgumentException(
                            "Error in mapping: Number of values not matching.");
                }

                for (int i = 0; i < Math.max(value.size(), treenodes.size()); i++) {
                    Object item = value.size() == 1 ? value.get(0) : value.get(i);
                    if (item != null) {
                        createAttribute(
                                (TreeBranch) treenodes.get(i),
                                index + 1,
                                (ComplexType) descriptor.getType(),
                                path,
                                item instanceof List
                                        ? (List<Object>) item
                                        : Collections.singletonList(item),
                                userData,
                                splitIndex);
                    }
                }
            }
        }
    }

    private String attName(String attWithIndex) {
        Matcher matcher = PATTERN_ATT_WITH_INDEX.matcher(attWithIndex);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return attWithIndex;
        }
    }

    /**
     * Adds an element to the current record with user data
     *
     * @param name path of property with dots
     * @param value the value(s) to be inserted
     * @param userData the user data
     */
    public void addElement(
            String name, List<Object> value, Map<Object, Object> userData, int[] splitIndex) {

        createAttribute(
                root,
                0,
                recordDescriptor.getFeatureType(),
                name.split("\\."),
                value,
                userData,
                splitIndex);
    }

    /**
     * Adds an element to the current record
     *
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    public void addElement(String name, Object... values) {
        addElement(name, Arrays.asList((Object[]) values), null, new int[0]);
    }

    /**
     * Adds an element to the current record
     *
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    public void addElement(String name, int[] splitIndex, Object... values) {
        addElement(name, Arrays.asList((Object[]) values), null, splitIndex);
    }

    /**
     * Adds a bounding box to the record. The envelope must be in WGS84
     *
     * @param env the bbox
     */
    public void addBoundingBox(ReferencedEnvelope env) {
        boxes.add(env);
    }

    /**
     * Builds a record and sets up to work on the next one
     *
     * @param id record id
     * @return the Feature
     */
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

        if (recordDescriptor.getBoundingBoxPropertyName() != null) {
            Map<Object, Object> userData =
                    Collections.singletonMap(
                            (Object) ORIGINAL_BBOXES,
                            (Object) new ArrayList<ReferencedEnvelope>(boxes));
            addElement(
                    recordDescriptor.getBoundingBoxPropertyName(),
                    Collections.singletonList((Object) geom),
                    userData,
                    new int[0]);
        }

        root.cleanUp(); // remove empty tags
        for (List<TreeNode> nodes : root.children.values()) {
            for (TreeNode node : nodes) {
                Attribute att = buildNode(node);
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
    private Attribute buildNode(TreeNode node) {

        AttributeBuilder ab = new AttributeBuilder(new LenientFeatureFactoryImpl());

        if (node instanceof TreeLeaf) {

            TreeLeaf leaf = (TreeLeaf) node;

            ab.setDescriptor(node.descriptor);
            Attribute att = ab.buildSimple(null, leaf.value);
            if (leaf.userData != null) {
                att.getUserData().putAll(leaf.userData);
            }
            return att;

        } else if (node instanceof TreeBranch) {

            ab.setDescriptor(node.descriptor);
            List<Attribute> list = new ArrayList<Attribute>();
            for (List<TreeNode> nodes : ((TreeBranch) node).children.values()) {
                for (TreeNode child : nodes) {
                    list.add(buildNode(child));
                }
            }
            return ab.createComplexAttribute(list, null, node.descriptor, null);
        }

        return null;
    }
}
