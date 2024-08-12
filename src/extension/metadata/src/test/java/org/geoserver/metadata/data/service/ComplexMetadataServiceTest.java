/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test the ComplexMetadataService. We use the data from the template service
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ComplexMetadataServiceTest extends AbstractMetadataTest {

    @Autowired private MetadataTemplateService templateService;

    @Autowired private ComplexMetadataService service;

    @Test
    public void testMergeSimpleFields() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("simple fields").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);

        // simple fields original values
        Assert.assertEquals(
                "the-identifier-single", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("99", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals("Select me", parent.get(String.class, "dropdown-field").getValue());

        HashMap<String, List<Integer>> descriptionMap = new HashMap<>();
        service.merge(parent, children, descriptionMap);

        // Should be updated simple fields
        Assert.assertEquals(
                "template-identifier", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("77", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals(
                "Or select this row", parent.get(String.class, "dropdown-field").getValue());

        Assert.assertEquals(0, descriptionMap.get("identifier-single").get(0).intValue());
        Assert.assertEquals(0, descriptionMap.get("number-field").get(0).intValue());
        Assert.assertEquals(0, descriptionMap.get("dropdown-field").get(0).intValue());
        // list simple fields
        Assert.assertEquals(3, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 2).getValue());
        // Object fields
        ComplexMetadataMap submap = parent.subMap("referencesystem-object");
        Assert.assertEquals("object-code", submap.get(String.class, "code").getValue());
        Assert.assertEquals("object-codeSpace", submap.get(String.class, "code-space").getValue());
        // list of objects
        Assert.assertEquals(2, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = parent.subMap("referencesystem-object-list", 1);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        Assert.assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
        // list of nested objects
        Assert.assertEquals(2, parent.size("feature-catalog"));
        ComplexMetadataMap submapNested01 = parent.subMap("feature-catalog/feature-attribute", 0);
        Assert.assertEquals(
                "First object catalog object", submapNested01.get(String.class, "name").getValue());
        Assert.assertEquals("String", submapNested01.get(String.class, "type").getValue());
        Assert.assertEquals(1, submapNested01.size("domain"));
        ComplexMetadataMap submapdomain = submapNested01.subMap("domain", 0);
        Assert.assertEquals(
                "a domain for first catalog object",
                submapdomain.get(String.class, "code").getValue());
        Assert.assertEquals("15", submapdomain.get(String.class, "value").getValue());
    }

    @Test
    public void testMergeObject() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("object-field").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);
        HashMap<String, List<Integer>> descriptionMap = new HashMap<>();
        service.merge(parent, children, descriptionMap);

        // simple fields
        Assert.assertEquals(
                "the-identifier-single", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("99", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals("Select me", parent.get(String.class, "dropdown-field").getValue());
        // list simple fields
        Assert.assertEquals(3, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 2).getValue());
        // Should be updated Object fields
        ComplexMetadataMap submap = parent.subMap("referencesystem-object");
        Assert.assertEquals("templateValue-code", submap.get(String.class, "code").getValue());
        Assert.assertEquals(
                "templateValue-codeSpace", submap.get(String.class, "code-space").getValue());

        Assert.assertEquals(0, descriptionMap.get("referencesystem-object").get(0).intValue());

        // list of objects
        Assert.assertEquals(2, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = parent.subMap("referencesystem-object-list", 1);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        Assert.assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
    }

    @Test
    public void testMergeListFields() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-list-simple").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);
        HashMap<String, List<Integer>> descriptionMap = new HashMap<>();
        service.merge(parent, children, descriptionMap);

        // simple fields
        Assert.assertEquals(
                "the-identifier-single", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("99", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals("Select me", parent.get(String.class, "dropdown-field").getValue());
        // Should be updated list simple fields
        Assert.assertEquals(5, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "template-value01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "template--value02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 2).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 3).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 4).getValue());

        Assert.assertEquals(0, descriptionMap.get("refsystem-as-list").get(0).intValue());
        Assert.assertEquals(1, descriptionMap.get("refsystem-as-list").get(1).intValue());
        // Object fields
        Assert.assertEquals(
                "object-code", parent.get(String.class, "referencesystem-object/code").getValue());
        Assert.assertEquals(
                "object-codeSpace",
                parent.get(String.class, "referencesystem-object/code-space").getValue());
        // list of objects
        Assert.assertEquals(2, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = parent.subMap("referencesystem-object-list", 1);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        Assert.assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
    }

    @Test
    public void testMergeListObjects() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-object list").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);
        HashMap<String, List<Integer>> descriptionMap = new HashMap<>();
        service.merge(parent, children, descriptionMap);

        // simple fields
        Assert.assertEquals(
                "the-identifier-single", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("99", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals("Select me", parent.get(String.class, "dropdown-field").getValue());
        // list simple fields
        Assert.assertEquals(3, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 2).getValue());
        // Object fields
        Assert.assertEquals(
                "object-code", parent.get(String.class, "referencesystem-object/code").getValue());
        Assert.assertEquals(
                "object-codeSpace",
                parent.get(String.class, "referencesystem-object/code-space").getValue());
        // Should be updated list of objects
        Assert.assertEquals(4, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submapTemplateOne = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submapTemplateTwo = parent.subMap("referencesystem-object-list", 1);
        ComplexMetadataMap submapUserInputOne = parent.subMap("referencesystem-object-list", 2);
        ComplexMetadataMap submapUserInputTwo = parent.subMap("referencesystem-object-list", 3);
        Assert.assertEquals(
                "list-objectcode01", submapUserInputOne.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01",
                submapUserInputOne.get(String.class, "code-space").getValue());
        Assert.assertEquals(
                "list-objectcode02", submapUserInputTwo.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02",
                submapUserInputTwo.get(String.class, "code-space").getValue());
        Assert.assertEquals(
                "template-code01", submapTemplateOne.get(String.class, "code").getValue());
        Assert.assertEquals(
                "template-codespace01",
                submapTemplateOne.get(String.class, "code-space").getValue());
        Assert.assertEquals(
                "template-code02", submapTemplateTwo.get(String.class, "code").getValue());
        Assert.assertEquals(
                "template-codespace02",
                submapTemplateTwo.get(String.class, "code-space").getValue());

        Assert.assertEquals(0, descriptionMap.get("referencesystem-object-list").get(0).intValue());
        Assert.assertEquals(1, descriptionMap.get("referencesystem-object-list").get(1).intValue());
    }

    @Test
    public void testMergeListNestedObjects() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-nested-object").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);
        service.merge(parent, children, new HashMap<>());

        // list of objects
        Assert.assertEquals(2, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = parent.subMap("referencesystem-object-list", 1);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        Assert.assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
        // Should be updated list of nested objects
        Assert.assertEquals(1, parent.size("feature-catalog/feature-attribute"));
        ComplexMetadataMap submapTemplate = parent.subMap("feature-catalog/feature-attribute", 0);
        Assert.assertEquals(
                "template-identifier", submapTemplate.get(String.class, "name").getValue());
        Assert.assertEquals("Geometry", submapTemplate.get(String.class, "type").getValue());
        Assert.assertEquals(2, submapTemplate.size("domain"));
        ComplexMetadataMap submapdomain01 = submapTemplate.subMap("domain", 0);
        ComplexMetadataMap submapdomain02 = submapTemplate.subMap("domain", 1);
        Assert.assertEquals(
                "template-domain-code01", submapdomain01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "template-domain-code01", submapdomain01.get(String.class, "value").getValue());
        Assert.assertEquals(
                "template-domain-code02", submapdomain02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "template-domain-code02", submapdomain02.get(String.class, "value").getValue());
    }

    @Test
    public void testUnlink() throws IOException {

        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());
        ComplexMetadataMap child =
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-list-simple").getMetadata());
        ComplexMetadataMap child01 =
                new ComplexMetadataMapImpl(
                        templateService.findByName("simple fields").getMetadata());

        ArrayList<ComplexMetadataMap> children = new ArrayList<>();
        children.add(child);
        children.add(child01);
        HashMap<String, List<Integer>> derivedAtts = new HashMap<>();

        // LINK
        service.merge(parent, children, derivedAtts);

        Assert.assertEquals(
                "template-identifier", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("77", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals(
                "Or select this row", parent.get(String.class, "dropdown-field").getValue());
        Assert.assertEquals(0, derivedAtts.get("identifier-single").get(0).intValue());
        Assert.assertEquals(0, derivedAtts.get("number-field").get(0).intValue());
        Assert.assertEquals(0, derivedAtts.get("dropdown-field").get(0).intValue());

        Assert.assertEquals(5, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "template-value01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "template--value02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 2).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 3).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 4).getValue());
        Assert.assertEquals(0, derivedAtts.get("refsystem-as-list").get(0).intValue());
        Assert.assertEquals(1, derivedAtts.get("refsystem-as-list").get(1).intValue());

        // UNLINK
        service.merge(parent, new ArrayList<ComplexMetadataMap>(), derivedAtts);

        Assert.assertEquals(null, parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals(null, parent.get(String.class, "number-field").getValue());
        Assert.assertEquals(null, parent.get(String.class, "dropdown-field").getValue());
        Assert.assertNull(derivedAtts.get("identifier-single"));
        Assert.assertNull(derivedAtts.get("number-field"));
        Assert.assertNull(derivedAtts.get("dropdown-field"));

        Assert.assertEquals(3, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 2).getValue());

        Assert.assertNull(derivedAtts.get("refsystem-as-list"));
    }

    @Test
    public void testDuplicatedRemoved() {
        ComplexMetadataMap map = new ComplexMetadataMapImpl(new HashMap<>());

        map.get(String.class, "refsystem-as-list", 0).setValue("value-1");
        map.get(String.class, "refsystem-as-list", 1).setValue("value-2");
        map.get(String.class, "refsystem-as-list", 2).setValue("template-value01");
        map.get(String.class, "refsystem-as-list", 3).setValue("value-3");

        map.subMap("referencesystem-object-list", 0)
                .get(String.class, "code")
                .setValue("template-code02");
        map.subMap("referencesystem-object-list", 0)
                .get(String.class, "code-space")
                .setValue("template-codespace02");
        map.subMap("referencesystem-object-list", 1)
                .get(String.class, "code")
                .setValue("template-code01");
        map.subMap("referencesystem-object-list", 1)
                .get(String.class, "code-space")
                .setValue("template-codespace03");
        map.subMap("referencesystem-object-list", 2)
                .get(String.class, "code")
                .setValue("template-code04");
        map.subMap("referencesystem-object-list", 2)
                .get(String.class, "code-space")
                .setValue("template-codespace01");

        ArrayList<ComplexMetadataMap> templates = new ArrayList<>();
        templates.add(
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-list-simple").getMetadata()));
        templates.add(
                new ComplexMetadataMapImpl(
                        templateService.findByName("template-object list").getMetadata()));

        service.merge(map, templates, new HashMap<>());

        assertEquals(5, map.size("refsystem-as-list"));
        assertEquals("template-value01", map.get(String.class, "refsystem-as-list", 0).getValue());
        assertEquals("template--value02", map.get(String.class, "refsystem-as-list", 1).getValue());
        assertEquals("value-1", map.get(String.class, "refsystem-as-list", 2).getValue());
        assertEquals("value-2", map.get(String.class, "refsystem-as-list", 3).getValue());
        assertEquals("value-3", map.get(String.class, "refsystem-as-list", 4).getValue());

        assertEquals(4, map.size("referencesystem-object-list"));
        assertEquals(
                "template-code01",
                map.subMap("referencesystem-object-list", 0).get(String.class, "code").getValue());
        assertEquals(
                "template-codespace01",
                map.subMap("referencesystem-object-list", 0)
                        .get(String.class, "code-space")
                        .getValue());
        assertEquals(
                "template-code02",
                map.subMap("referencesystem-object-list", 1).get(String.class, "code").getValue());
        assertEquals(
                "template-codespace02",
                map.subMap("referencesystem-object-list", 1)
                        .get(String.class, "code-space")
                        .getValue());
        assertEquals(
                "template-code01",
                map.subMap("referencesystem-object-list", 2).get(String.class, "code").getValue());
        assertEquals(
                "template-codespace03",
                map.subMap("referencesystem-object-list", 2)
                        .get(String.class, "code-space")
                        .getValue());
        assertEquals(
                "template-code04",
                map.subMap("referencesystem-object-list", 3).get(String.class, "code").getValue());
        assertEquals(
                "template-codespace01",
                map.subMap("referencesystem-object-list", 3)
                        .get(String.class, "code-space")
                        .getValue());
    }

    @Test
    public void testDerive() {
        ComplexMetadataMap map = new ComplexMetadataMapImpl(new HashMap<>());

        map.get(String.class, "source", 0).setValue("sourceb");
        map.get(String.class, "source", 1).setValue("sourcex");
        map.get(String.class, "source", 2).setValue("sourcec");
        service.derive(map);

        assertEquals("targetb", map.get(String.class, "target", 0).getValue());
        assertEquals(null, map.get(String.class, "target", 1).getValue());
        assertEquals("targetc", map.get(String.class, "target", 2).getValue());

        // check removal
        map.delete("source", 2);
        service.derive(map);
        assertEquals(2, map.size("target"));
    }

    @Test
    public void testClean() throws IOException {
        ComplexMetadataMap parent =
                new ComplexMetadataMapImpl(templateService.findByName("allData").getMetadata());

        parent.get(String.class, "doesn't exist").setValue("blah");
        parent.subMap("doesn't exist either").get(String.class, "duh").setValue("bleh");

        service.clean(parent);

        assertNull(parent.get(String.class, "doesn't exist").getValue());
        assertNull(parent.subMap("doesn't exist either").get(String.class, "duh").getValue());

        // simple fields original values
        Assert.assertEquals(
                "the-identifier-single", parent.get(String.class, "identifier-single").getValue());
        Assert.assertEquals("99", parent.get(String.class, "number-field").getValue());
        Assert.assertEquals("Select me", parent.get(String.class, "dropdown-field").getValue());

        // list simple fields
        Assert.assertEquals(3, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        Assert.assertEquals(
                "list-refsystem-02", parent.get(String.class, "refsystem-as-list", 1).getValue());
        Assert.assertEquals(
                "list-refsystem-03", parent.get(String.class, "refsystem-as-list", 2).getValue());
        // Object fields
        ComplexMetadataMap submap = parent.subMap("referencesystem-object");
        Assert.assertEquals("object-code", submap.get(String.class, "code").getValue());
        Assert.assertEquals("object-codeSpace", submap.get(String.class, "code-space").getValue());
        // list of objects
        Assert.assertEquals(2, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        ComplexMetadataMap submap02 = parent.subMap("referencesystem-object-list", 1);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        Assert.assertEquals("list-objectcode02", submap02.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace02", submap02.get(String.class, "code-space").getValue());
        // list of nested objects
        Assert.assertEquals(2, parent.size("feature-catalog"));
        ComplexMetadataMap submapNested01 = parent.subMap("feature-catalog/feature-attribute", 0);
        Assert.assertEquals(
                "First object catalog object", submapNested01.get(String.class, "name").getValue());
        Assert.assertEquals("String", submapNested01.get(String.class, "type").getValue());
        Assert.assertEquals(1, submapNested01.size("domain"));
        ComplexMetadataMap submapdomain = submapNested01.subMap("domain", 0);
        Assert.assertEquals(
                "a domain for first catalog object",
                submapdomain.get(String.class, "code").getValue());
        Assert.assertEquals("15", submapdomain.get(String.class, "value").getValue());
    }

    @Test
    public void testFixRepeat() throws IOException {
        Map<String, Serializable> parentRaw = templateService.findByName("norepeat").getMetadata();
        ComplexMetadataMap parent = new ComplexMetadataMapImpl(parentRaw);

        assertFixRepeat(parent);

        service.init(parent);

        assertFixRepeat(parent);

        assertTrue(parentRaw.get("refsystem-as-list") instanceof List);
        assertTrue(parentRaw.get("referencesystem-object-list/code-space") instanceof List);
        assertTrue(parentRaw.get("feature-catalog/feature-attribute/definition") instanceof List);
        assertTrue(parentRaw.get("feature-catalog/feature-attribute/type") instanceof List);
        assertTrue(parentRaw.get("feature-catalog/feature-attribute/name") instanceof List);
        assertTrue(
                ((List<?>) parentRaw.get("feature-catalog/feature-attribute/domain/code")).get(0)
                        instanceof List);
        assertTrue(
                ((List<?>) parentRaw.get("feature-catalog/feature-attribute/domain/value")).get(0)
                        instanceof List);
    }

    public void assertFixRepeat(ComplexMetadataMap parent) {
        // list simple fields
        Assert.assertEquals(1, parent.size("refsystem-as-list"));
        Assert.assertEquals(
                "list-refsystem-01", parent.get(String.class, "refsystem-as-list", 0).getValue());
        // list of objects
        Assert.assertEquals(1, parent.size("referencesystem-object-list"));
        ComplexMetadataMap submap01 = parent.subMap("referencesystem-object-list", 0);
        Assert.assertEquals("list-objectcode01", submap01.get(String.class, "code").getValue());
        Assert.assertEquals(
                "list-objectcodeSpace01", submap01.get(String.class, "code-space").getValue());
        // list of nested objects
        Assert.assertEquals(1, parent.size("feature-catalog"));
        ComplexMetadataMap submapNested01 = parent.subMap("feature-catalog/feature-attribute", 0);
        Assert.assertEquals(
                "First object catalog object", submapNested01.get(String.class, "name").getValue());
        Assert.assertEquals("String", submapNested01.get(String.class, "type").getValue());
        Assert.assertEquals(1, submapNested01.size("domain"));
        ComplexMetadataMap submapdomain = submapNested01.subMap("domain", 0);
        Assert.assertEquals(
                "a domain for first catalog object",
                submapdomain.get(String.class, "code").getValue());
        Assert.assertEquals("15", submapdomain.get(String.class, "value").getValue());
    }
}
