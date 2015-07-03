/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

public class Foo {

    String prop1;
    Integer prop2;
    Double prop3;
    
    public Foo(String prop1, Integer prop2, Double prop3) {
        super();
        this.prop1 = prop1;
        this.prop2 = prop2;
        this.prop3 = prop3;
    }
    public String getProp1() {
        return prop1;
    }
    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }
    public Integer getProp2() {
        return prop2;
    }
    public void setProp2(Integer prop2) {
        this.prop2 = prop2;
    }
    public Double getProp3() {
        return prop3;
    }
    public void setProp3(Double prop3) {
        this.prop3 = prop3;
    }
}
