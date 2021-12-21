/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.util.List;

/** This class is used for passing custom object to freemarker template. */
public class Offering {

    String offeringCode;
    List<String> offeringDetailList;

    public Offering(String offeringCode, List<String> offeringDetailList) {
        this.offeringCode = offeringCode;
        this.offeringDetailList = offeringDetailList;
    }

    public String getOfferingCode() {
        return offeringCode;
    }

    public List<String> getOfferingDetailList() {
        return offeringDetailList;
    }
}
