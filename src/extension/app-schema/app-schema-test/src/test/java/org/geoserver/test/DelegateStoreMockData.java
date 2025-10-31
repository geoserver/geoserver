/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

public class DelegateStoreMockData extends FeatureChainingMockData {

    public static String DB_PARAMS = "<parameters>\n" + "        <Parameter>\n"
            + "          <name>delegateStoreName</name>\n"
            + "          <value>stations:delegateStore</value>\n"
            + "        </Parameter>\n"
            + "      </parameters>"; //

    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX, "MappedFeature", "MappedFeaturePropertyfile.xml", "MappedFeaturePropertyfile.properties");
    }

    @Override
    protected String providePostgisParams() {
        return DB_PARAMS;
    }
}
