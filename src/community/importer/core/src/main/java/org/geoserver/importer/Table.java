package org.geoserver.importer;

public class Table extends ImportData {

    /** table name */
    String name;

    /** the database */
    Database db;

    public Table(String name, Database db) {
        this.name = name;
        this.db = db;
    }

    public void setDatabase(Database db) {
        this.db = db;
    }

    public Database getDatabase() {
        return db;
    }

    @Override
    public String getName() {
        return name;
    }
}
