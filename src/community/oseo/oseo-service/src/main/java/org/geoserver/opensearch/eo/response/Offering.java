/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.util.List;

/** This class is used for passing custom Offering object to freemarker templates. */
public class Offering {

    String offeringCode;
    List<OfferingDetail> offeringDetailList;

    public Offering(String offeringCode, List<OfferingDetail> offeringDetailList) {
        this.offeringCode = offeringCode;
        this.offeringDetailList = offeringDetailList;
    }

    public String getOfferingCode() {
        return offeringCode;
    }

    public List<OfferingDetail> getOfferingDetailList() {
        return offeringDetailList;
    }
}
