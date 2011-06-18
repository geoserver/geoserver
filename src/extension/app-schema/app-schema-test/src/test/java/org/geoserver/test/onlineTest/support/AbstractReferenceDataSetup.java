/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.support;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;

import com.sun.rowset.CachedRowSetImpl;

/**
 * Base class that initialise and provides the methods for online test to take place. Other tests
 * that intends to run their unit test online should extend from this class and implement the
 * abstract methods.
 * 
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 * 
 */
public abstract class AbstractReferenceDataSetup extends JDBCTestSetup {
    // The type of database to use.
    public abstract JDBCDataStoreFactory createDataStoreFactory();

    // Setup the data.
    public abstract void setUp() throws Exception;
    
    protected abstract Properties createExampleFixture();

    public void setUpData() throws Exception {
        super.setUpData();
    }

    public void initializeDatabase() throws Exception {
        super.initializeDatabase();
    }

    // retrieve the id of the database.
    public abstract String getDatabaseID();

    /**
     * This method doesn't not handle paging therefore care must be taken when dealing with large
     * dataset.
     * 
     * @param sql
     * @return CachedRowSetImpl the result from the execution of the sql
     * @throws Exception
     */
    public CachedRowSetImpl runWithResult(String sql) throws Exception {
        // connect
        Connection conn = getConnection();
        Statement st = null;
        try {
            st = conn.createStatement();
            CachedRowSetImpl crs = new CachedRowSetImpl();
            crs.populate(st.executeQuery(sql));
            return crs;
        } finally {
            st.close();
            conn.close();
        }

    }

    public void runOracleStoreProcedure(String sql) throws Exception {
        Connection conn = getConnection();
        CallableStatement cs = null;
        try {
            cs = conn.prepareCall(sql);
            cs.execute();
        } finally {
            cs.close();
            conn.close();
        }
    }

    @Override
    public void run(String input) throws Exception {
        super.run(input.replaceAll(DatabaseUtil.NEWLINE, " "));
    }

}