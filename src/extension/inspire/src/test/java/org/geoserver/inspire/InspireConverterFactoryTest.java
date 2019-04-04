/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.junit.Assert.*;

import org.geotools.util.Converters;
import org.junit.Test;

public class InspireConverterFactoryTest {

    @Test
    public void testCodeOnly() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,,", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }

    @Test
    public void testCodeNamespace() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code", "http://www.geoserver.org"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,http://www.geoserver.org,", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }

    @Test
    public void testCodeMetadata() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(
                new UniqueResourceIdentifier(
                        "code", null, "http://metadata.geoserver.org/id?code"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,,http://metadata.geoserver.org/id?code", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }

    @Test
    public void testCodeNamespaceMetadata() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(
                new UniqueResourceIdentifier(
                        "code", "http://www.geoserver.org", "http://www.geoserver.org/metadata"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,http://www.geoserver.org,http://www.geoserver.org/metadata", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }

    @Test
    public void testMulti() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code1"));
        ids.add(new UniqueResourceIdentifier("code2", "http://www.geoserver.org/1"));
        ids.add(
                new UniqueResourceIdentifier(
                        "code3",
                        "http://www.geoserver.org/2",
                        "http://www.geoserver.org/metadata"));
        String str = Converters.convert(ids, String.class);
        assertEquals(
                "code1,,;code2,http://www.geoserver.org/1,;code3,http://www.geoserver.org/2,http://www.geoserver.org/metadata",
                str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }
}
