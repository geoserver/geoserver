/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.util.Date;
import net.opengis.cat.csw20.ElementSetType;
import org.geotools.feature.FeatureCollection;

/**
 * The full response to a GetRecords request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordsResult {

    ElementSetType elementSet;

    String recordSchema;

    int numberOfRecordsMatched;

    int numberOfRecordsReturned;

    int nextRecord;

    FeatureCollection records;

    Date timestamp;

    public CSWRecordsResult(
            ElementSetType elementSet,
            String recordSchema,
            int numberOfRecordsMatched,
            int numberOfRecordsReturned,
            int nextRecord,
            Date timestamp,
            FeatureCollection records) {
        super();
        this.elementSet = elementSet;
        this.recordSchema = recordSchema;
        this.numberOfRecordsMatched = numberOfRecordsMatched;
        this.numberOfRecordsReturned = numberOfRecordsReturned;
        this.nextRecord = nextRecord;
        this.records = records;
        this.timestamp = timestamp;
    }

    public ElementSetType getElementSet() {
        return elementSet;
    }

    public String getRecordSchema() {
        return recordSchema;
    }

    public int getNumberOfRecordsMatched() {
        return numberOfRecordsMatched;
    }

    public int getNumberOfRecordsReturned() {
        return numberOfRecordsReturned;
    }

    public int getNextRecord() {
        return nextRecord;
    }

    public FeatureCollection getRecords() {
        return records;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setElementSet(ElementSetType elementSet) {
        this.elementSet = elementSet;
    }

    public void setRecordSchema(String recordSchema) {
        this.recordSchema = recordSchema;
    }

    public void setNumberOfRecordsMatched(int numberOfRecordsMatched) {
        this.numberOfRecordsMatched = numberOfRecordsMatched;
    }

    public void setNumberOfRecordsReturned(int numberOfRecordsReturned) {
        this.numberOfRecordsReturned = numberOfRecordsReturned;
    }

    public void setNextRecord(int nextRecord) {
        this.nextRecord = nextRecord;
    }

    public void setRecords(FeatureCollection records) {
        this.records = records;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
