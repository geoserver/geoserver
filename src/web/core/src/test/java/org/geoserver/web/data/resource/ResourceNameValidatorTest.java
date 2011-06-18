package org.geoserver.web.data.resource;


import junit.framework.TestCase;

import org.geoserver.web.StringValidatable;
import org.geoserver.web.data.resource.BasicResourceConfig.ResourceNameValidator;

public class ResourceNameValidatorTest extends TestCase {

    public void testValidUnderscoreMiddle() {
        StringValidatable validatable = new StringValidatable("abc_def");
        new ResourceNameValidator().validate(validatable);
        assertTrue(validatable.isValid());
    }
    
    public void testValidUnderscoreStart() {
        StringValidatable validatable = new StringValidatable("_def");
        new ResourceNameValidator().validate(validatable);
        assertTrue(validatable.isValid());
    }
    
    public void testValidPoint() {
        StringValidatable validatable = new StringValidatable("abc.def");
        new ResourceNameValidator().validate(validatable);
        assertTrue(validatable.isValid());
    }
    
    public void testEmpty() {
        StringValidatable validatable = new StringValidatable("");
        new ResourceNameValidator().validate(validatable);
        assertFalse(validatable.isValid());
    }
    
    public void testSpace() {
        StringValidatable validatable = new StringValidatable("abc def");
        new ResourceNameValidator().validate(validatable);
        assertFalse(validatable.isValid());
    }
}
