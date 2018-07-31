/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.model.domain.CodedValue;
import com.boundlessgeo.gsr.model.domain.CodedValueDomain;
import com.boundlessgeo.gsr.model.domain.InheritedDomain;
import com.boundlessgeo.gsr.model.domain.RangeDomain;

public class DomainSchemaTest extends JsonSchemaTest {

    @Test
    public void testRangeDomainJsonSchema() throws Exception {
        int[] range = { 1, 1000 };
        RangeDomain domain = new RangeDomain("Measured Length", range);
        String json = getJson(domain);
        assertTrue(validateJSON(json, "gsr/1.0/rangeDomain.json"));
    }

    @Test
    public void testCodedValueDomainJsonSchema() throws Exception {
        CodedValue v1 = new CodedValue("Aluminum", "AL");
        CodedValue v2 = new CodedValue("Copper", "CU");
        CodedValue v3 = new CodedValue("Steel", "STEEL");
        CodedValue v4 = new CodedValue("Not Applicable", "NA");
        Set<CodedValue> codedValues = new HashSet<>();
        codedValues.add(v1);
        codedValues.add(v2);
        codedValues.add(v3);
        codedValues.add(v4);
        CodedValueDomain codedValueDomain = new CodedValueDomain("Material", codedValues);
        String json = getJson(codedValueDomain);
        assertTrue(validateJSON(json, "gsr/1.0/codedValueDomain.json"));
    }

    public void testInheritedDomainJsonSchema() throws Exception {
        InheritedDomain domain = new InheritedDomain();
        String json = getJson(domain);
        assertTrue(validateJSON(json, "gsr/1.0/inheritedDomain.json"));
    }
}
