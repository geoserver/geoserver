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
    protected static class TreeNode {
        AttributeDescriptor descriptor = null;
    }
    
    protected static class TreeLeaf extends TreeNode {
        public Map<Object, Object> userData;
    }
    
    protected static class ComplexTreeLeaf extends TreeLeaf {
        public List<Map<String, Object>> values = new ArrayList<Map<String, Object>> ();
    }
    
    protected static class SimpleTreeLeaf extends TreeLeaf {
        public List<Object> values;
    }
    
    protected static class TreeBranch extends TreeNode {
        public Map<String, TreeNode> children = new HashMap<String, TreeNode>();
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
    private void createAttribute(TreeBranch branch, int index, ComplexType type, String[] path, List<Object> value, Map<Object, Object> userData) {
        
        AttributeDescriptor descriptor =  (AttributeDescriptor) Types.findDescriptor(type, path[index]);
        
        if (descriptor == null) {
            throw new IllegalArgumentException("Cannot find descriptor for attribute " + path[index] + " in type " + type.getName().toString());
        }
                
        if (index == path.length - 1) { //can only happen if there is a path with size 1
            
            SimpleTreeLeaf leaf = new SimpleTreeLeaf();
            
            leaf.descriptor = descriptor;
            leaf.values = new ArrayList( value);
            leaf.userData = userData;
            
            branch.children.put(path[index], leaf);
            
            leaf.userData = userData;
        
        } else if (index == path.length - 2) {
            
            ComplexTreeLeaf leaf = (ComplexTreeLeaf) branch.children.get(path[index]); 
            
            if (leaf == null) {
                leaf = new ComplexTreeLeaf();
                branch.children.put(path[index], leaf);
                leaf.descriptor = descriptor;
            }
            
            //matching the sizes
            if (leaf.values.size() == 0) {
                for (int i = 0; i<value.size(); i++) {
                    leaf.values.add(new HashMap<String,Object>());
                }
            } else if (leaf.values.size() == 1) {
                for (int i = 1; i < value.size(); i++) {
                        leaf.values.add(new HashMap<String,Object>(leaf.values.get(0)));
                }
            } else if (value.size()!=1 && leaf.values.size() != value.size()) {
                throw new IllegalArgumentException("Error in mapping: Number of values not matching.");
            }
            
            for (int i=0; i < leaf.values.size(); i++) {
                leaf.values.get(i).put(path[index+1], value.size()==1? value.get(0) : value.get(i));
            }
                       
            leaf.userData = userData;
            
        } else {
            
            TreeNode child = branch.children.get(path[index]);
            
            if (child == null) {
                child = new TreeBranch();
                child.descriptor = descriptor;
                                
                branch.children.put(path[index], child);
            }
                        
            createAttribute((TreeBranch) child, index+1, (ComplexType) descriptor.getType(), path, value, userData);
                        
        }
                        
    } 
    
    /**
     * Adds an element to the current record with user data
     * 
     * @param name path of property with dots
     * @param value the value(s) to be inserted
     * @param userData the user data
     */
    public void addElement(String name, List<Object> value, Map<Object, Object> userData) {
        
        createAttribute(root, 0, recordDescriptor.getFeatureType(), name.split("\\."), value, userData);
                
    }
        
    /**
     * Adds an element to the current record
     * 
     * @param name path of property with dots
     * @param values the value(s) to be inserted
     */
    public void addElement(String name, String... values) {
        addElement(name, Arrays.asList((Object[]) values), null);
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
        addElement( recordDescriptor.getBoundingBoxPropertyName(), Collections.singletonList((Object)geom), userData);
        
                       
        for (TreeNode node : root.children.values()) {  
            for (Attribute att : buildNode(node)) {
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
    private List<Attribute> buildNode(TreeNode node) {
        if (node instanceof TreeLeaf) {
                      
            List<Attribute> atts = new ArrayList<Attribute>();

            if (node instanceof ComplexTreeLeaf) {                
               
                ComplexTreeLeaf leaf = (ComplexTreeLeaf) node;

                ComplexType type = (ComplexType) node.descriptor.getType();

                for (Map<String, Object> item : leaf.values) {
                    
                    ab.setDescriptor(node.descriptor);
                                                            
                    for (Entry<String, Object> entry : item.entrySet()) {

                        PropertyDescriptor descriptor = Types.findDescriptor(type, entry.getKey());
    
                        if (descriptor == null) {
                            throw new IllegalArgumentException("Cannot find descriptor for attribute "
                                    + entry.getKey() + " in type " + type.getName().toString());
                        }
    
                        ab.add(null, entry.getValue(), descriptor.getName());
                                                      
                    }
                    
                    Attribute att = ab.build();
                    
                    if (leaf.userData != null) {
                        for (Entry<String, Object> entry : item.entrySet()) {
                            ((ComplexAttribute) att).getProperty(entry.getKey()).getUserData().putAll(leaf.userData);
                        }
                    }  
                                                            
                    atts.add(att);
                }
            }
            else {
                
                SimpleTreeLeaf leaf = (SimpleTreeLeaf)node;       
                
                for (Object item: leaf.values) {
                    ab.setDescriptor(node.descriptor);   
                    Attribute att = ab.buildSimple(null, item);

                    if (leaf.userData != null) {
                        att.getUserData().putAll(leaf.userData);
                    }
                    
                    atts.add( att );                    
                }
                
            }                            
            
            return atts;     
            
        } else if (node instanceof TreeBranch) {
            
            List<Attribute> list = new ArrayList<Attribute>();
            
            for (TreeNode child :( (TreeBranch)node).children.values()) {
                for (Attribute att : buildNode(child)) {
                    list.add(att);
                }
            }
            
            return Collections.<Attribute>singletonList(ab.createComplexAttribute(list, null, node.descriptor, null));
                 
        }
        
        return null;
        
    }

}
