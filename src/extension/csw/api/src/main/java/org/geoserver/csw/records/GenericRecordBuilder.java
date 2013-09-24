/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
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
import java.util.Map.Entry;

import org.geoserver.csw.records.RecordBuilder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.geotools.feature.type.Types;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A generic helper that builds CSW records as GeoTools features
 * 
 * @author Niels Charlier
 */
public class GenericRecordBuilder implements RecordBuilder {
    
    /**
     * A user property of the boundingBox attribute containing the original envelopes
     * of the Record. 
     */    
    public static final String ORIGINAL_BBOXES = "RecordOriginalBounds";
        
    protected ComplexFeatureBuilder fb ;
    protected AttributeBuilder ab = new AttributeBuilder(new LenientFeatureFactoryImpl());
    protected List<ReferencedEnvelope> boxes = new ArrayList<ReferencedEnvelope>();
    protected RecordDescriptor recordDescriptor;    
    protected Map<Name, Name> substitutionMap = new HashMap<Name, Name>();
            
    /**
     * A tree structure is built initially before the feature is built, so that all data for the feature can be collected
     * and properly structured to build the feature.
     * 
     *
     */
    protected static abstract class TreeNode {
        AttributeDescriptor descriptor = null;
        
        public abstract TreeNode clone();
    }
    
    protected static abstract class TreeLeaf extends TreeNode {
        public Map<Object, Object> userData;        
    }
    
    protected static class ComplexTreeLeaf extends TreeLeaf {
        public Map<String, Object> value = new HashMap<String, Object>();
        
        @Override
        public ComplexTreeLeaf clone(){
            ComplexTreeLeaf leaf = new ComplexTreeLeaf();        
            leaf.value.putAll(value);
            leaf.userData = userData;
            leaf.descriptor = descriptor;
            return leaf;
        }
    }
    
    protected static class SimpleTreeLeaf extends TreeLeaf {
        public Object value;
        
        @Override
        public SimpleTreeLeaf clone(){
            SimpleTreeLeaf leaf = new SimpleTreeLeaf();        
            leaf.value = value;
            leaf.userData.putAll(userData);
            leaf.descriptor = descriptor;
            return leaf;
        }
    }
    
    protected static class TreeBranch extends TreeNode {
        public Map<String, List<TreeNode>> children = new HashMap<String, List<TreeNode>>();
        
        @Override
        public TreeBranch clone(){
            TreeBranch branch = new TreeBranch();
            for (Map.Entry<String, List<TreeNode>> pair : children.entrySet()) {
                List<TreeNode> list = new ArrayList<TreeNode>();
                for (TreeNode node : pair.getValue()){
                    list.add(node.clone());
                }
            }
            branch.descriptor = descriptor;
            return branch;
        }
    }
    
    /**
     * The root of the tree
     */
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
            List<AttributeDescriptor> substitutionGroup = (List<AttributeDescriptor>) descriptor.getUserData().get("substitutionGroup");
            if (substitutionGroup != null) {
                for (Iterator<AttributeDescriptor> it = substitutionGroup.iterator(); it.hasNext();) {
                    substitutionMap.put(it.next().getName(), descriptor.getName());
                }
            }
            substitutionMap.put(descriptor.getName(), descriptor.getName());
        }
    }
    
    /**
     * Helper method for creating attributes in the tree structure
     * 
     * @param branch
     * @param index
     * @param type
     * @param path
     * @param value
     * @param userData
     */
    private void createAttribute(TreeBranch branch, int index, ComplexType type, String[] path, List<Object> value, Map<Object, Object> userData, int splitIndex) {
        
        AttributeDescriptor descriptor =  (AttributeDescriptor) Types.findDescriptor(type, path[index]);
        
        if (descriptor == null) {
            throw new IllegalArgumentException("Cannot find descriptor for attribute " + path[index] + " in type " + type.getName().toString());
        }
        
        List<TreeNode> treenodes = branch.children.get(path[index]);
        
        if (treenodes == null) {
            treenodes = new ArrayList<TreeNode>();
            branch.children.put(path[index], treenodes);
        }
                
        if (index == path.length - 1) { //can only happen if there is a path with size 1
            
            for (Object item : value) {
                SimpleTreeLeaf leaf = new SimpleTreeLeaf();
                leaf.userData = userData;
                
                leaf.descriptor = descriptor;
                leaf.value = item;
                leaf.userData = userData;
                
                treenodes.add(leaf);
            }
        
        } else if (index == path.length - 2) {
            
            if (treenodes.isEmpty()) {
                for (int i = 0; i<value.size(); i++) {
                    ComplexTreeLeaf leaf = new ComplexTreeLeaf();
                    treenodes.add(leaf);
                    leaf.descriptor = descriptor;
                }
            } else if (treenodes.size() == 1) {
                for (int i = 1; i<value.size(); i++) {
                    treenodes.add(treenodes.get(0).clone());
                }
            } else if (value.size()!=1 && treenodes.size() != value.size()) {
                throw new IllegalArgumentException("Error in mapping: Number of values not matching.");
            }
            
            for (int i = 0; i<value.size(); i++) {
                ComplexTreeLeaf leaf = (ComplexTreeLeaf) treenodes.get(i); 
                
                leaf.value.put(path[index+1], value.size()==1? value.get(0) : value.get(i));                           
                leaf.userData = userData;
            }
            
        } else {
            
            if (index != splitIndex) {            
                if (treenodes.isEmpty()) {
                    TreeNode child = new TreeBranch();
                    child.descriptor = descriptor;                                    
                    treenodes.add(child);
                }     
                for (int i = 0; i < treenodes.size(); i++) {          
                    createAttribute((TreeBranch) treenodes.get(i), index+1, (ComplexType) descriptor.getType(), path, value, userData, splitIndex);
                }
            } else {
                if (treenodes.isEmpty()) {
                    for (int i = 0; i<value.size(); i++) {
                        TreeNode child = new TreeBranch();
                        child.descriptor = descriptor;  
                        treenodes.add(child);
                    }
                } else if (treenodes.size() == 1) {
                    for (int i = 1; i < value.size(); i++) {
                        treenodes.add(treenodes.get(0).clone());
                    }
                } else if (treenodes.size() != value.size()) {
                    throw new IllegalArgumentException("Error in mapping: Number of values not matching.");
                }
                
                for (int i = 0; i < value.size(); i++) {                    
                    createAttribute((TreeBranch) treenodes.get(i), index+1, (ComplexType) descriptor.getType(), path, Collections.singletonList(value.get(i)), userData, splitIndex);                    
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
    public void addElement(String name, List<Object> value, Map<Object, Object> userData, int splitIndex) {
        
        createAttribute(root, 0, recordDescriptor.getFeatureType(), name.split("\\."), value, userData, splitIndex);
                
    }
        
    /**
     * Adds an element to the current record
     * 
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    public void addElement(String name, String... values) {
        addElement(name, Arrays.asList((Object[]) values), null, -1);
    }
    
    /**
     * Adds an element to the current record
     * 
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    public void addElement(String name, int splitIndex, String... values) {
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
     * @param id
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
                                + "this should never happen with valid coordinates", e);
            }
        }
        if (geom instanceof Polygon) {
            geom = geom.getFactory().createMultiPolygon(new Polygon[] { (Polygon) geom });
        }

        Map<Object, Object> userData = Collections.singletonMap((Object) ORIGINAL_BBOXES, (Object) new ArrayList(boxes));
        addElement( recordDescriptor.getBoundingBoxPropertyName(), Collections.singletonList((Object)geom), userData, -1);
        
                   
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
        
        if (node instanceof TreeLeaf) {

            if (node instanceof ComplexTreeLeaf) {                
               
                ComplexTreeLeaf leaf = (ComplexTreeLeaf) node;

                ComplexType type = (ComplexType) node.descriptor.getType();
                    
                ab.setDescriptor(node.descriptor);

                for (Entry<String, Object> entry : leaf.value.entrySet()) {

                    PropertyDescriptor descriptor = Types.findDescriptor(type, entry.getKey());

                    if (descriptor == null) {
                        throw new IllegalArgumentException("Cannot find descriptor for attribute "
                                + entry.getKey() + " in type " + type.getName().toString());
                    }

                    ab.add(null, entry.getValue(), descriptor.getName());

                }

                Attribute att = ab.build();

                if (leaf.userData != null) {
                    for (Entry<String, Object> entry : leaf.value.entrySet()) {
                        ((ComplexAttribute) att).getProperty(entry.getKey()).getUserData()
                                .putAll(leaf.userData);
                    }
                }

                return att;
            }
            else {
                
                SimpleTreeLeaf leaf = (SimpleTreeLeaf)node;       
                
                ab.setDescriptor(node.descriptor);
                Attribute att = ab.buildSimple(null, leaf.value);

                if (leaf.userData != null) {
                    att.getUserData().putAll(leaf.userData);
                }

                return att;
            }                            
            
        } else if (node instanceof TreeBranch) {

            List<Attribute> list = new ArrayList<Attribute>();
            
            for (List<TreeNode> nodes : ((TreeBranch)node).children.values()) {                                  
                for (TreeNode child : nodes) {  
                    list.add(buildNode(child));
                }                
            }

            return ab.createComplexAttribute(list, null, node.descriptor, null);
                 
        }
        
        return null;        
    }

}
