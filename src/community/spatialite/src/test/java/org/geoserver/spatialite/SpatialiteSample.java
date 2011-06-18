package org.geoserver.spatialite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.File;
import org.sqlite.SQLiteConfig;

public class SpatialiteSample
{
  public static void main(String[] args) throws ClassNotFoundException
  {
    try
	{
	// load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");
        Connection conn = null;
        String curr_dir = "";
	try
        {
          // enabling dynamic extension loading
          // absolutely required by SpatiaLite
          SQLiteConfig config = new SQLiteConfig();
          config.enableLoadExtension(true);
    	  
          // create a database connection
          conn = DriverManager.getConnection("jdbc:sqlite:spatialiteSample.sqlite",config.toProperties());
          Statement stmt = conn.createStatement();
          stmt.setQueryTimeout(30); // set timeout to 30 sec.
        	  File dir1 = new File (".");
        	  try {
        	       curr_dir = dir1.getCanonicalPath();
    	          }
        	  catch(Exception e) {
        	      e.printStackTrace();
    	           }
	  stmt.execute("SELECT load_extension('"+curr_dir+"/lib/native/Windows/amd64/libspatialite-2-4.dll')");
       // enabling Spatial Metadata
	  String sql = "SELECT InitSpatialMetaData();";
	  stmt.execute(sql);
// ################# BEGIN EXAMPLE QUERYS ###############################
	  // creating a POINT table
      sql = "CREATE TABLE test_pt (";
      sql += "id INTEGER NOT NULL PRIMARY KEY,";
      sql += "name TEXT NOT NULL)";
      stmt.execute(sql);
      // creating a POINT Geometry column
      sql = "SELECT AddGeometryColumn('test_pt', ";
      sql += "'geom', 4326, 'POINT', 'XY')";
      stmt.execute(sql);

      // creating a LINESTRING table
      sql = "CREATE TABLE test_ln (";
      sql += "id INTEGER NOT NULL PRIMARY KEY,";
      sql += "name TEXT NOT NULL)";
      stmt.execute(sql);
      // creating a LINESTRING Geometry column
      sql = "SELECT AddGeometryColumn('test_ln', ";
      sql += "'geom', 4326, 'LINESTRING', 'XY')"; 
      stmt.execute(sql);

      // creating a POLYGON table
      sql = "CREATE TABLE test_pg (";
      sql += "id INTEGER NOT NULL PRIMARY KEY,";
      sql += "name TEXT NOT NULL)";
      stmt.execute(sql);
      // creating a POLYGON Geometry column
      sql = "SELECT AddGeometryColumn('test_pg', ";
      sql += "'geom', 4326, 'POLYGON', 'XY')";
      stmt.execute(sql);
      // inserting some POINTs
      conn.setAutoCommit(false);
      int i;
      for (i = 0; i < 100; i++)
      {
        // for POINTs we'll use full text sql statements
        sql = "INSERT INTO test_pt (id, name, geom) VALUES (";
        sql += i + 1;
        sql += ", 'test POINT #";
        sql += i + 1;
        sql += "', GeomFromText('POINT(";
        sql += i / 1000.0;
        sql += " ";
        sql += i / 1000.0;
        sql += ")', 4326))";
        stmt.executeUpdate(sql);
      }
      conn.commit();

      // checking POINTs
      sql = "SELECT DISTINCT Count(*), GeometryType(geom), ";
      sql += "Srid(geom) FROM test_pt";
      ResultSet rs = stmt.executeQuery(sql);
      while(rs.next())
      {
        // read the result set
        String msg = "> Inserted ";
        msg += rs.getInt(1);
        msg += " entities of type ";
        msg += rs.getString(2);
        msg += " SRID=";
        msg += rs.getInt(3);
        System.out.println(msg);
      }

      // inserting some LINESTRINGs
      // this time we'll use a Prepared Statement
      sql = "INSERT INTO test_ln (id, name, geom) ";
      sql += "VALUES (?, ?, GeomFromText(?, 4326))";
      PreparedStatement ins_stmt = conn.prepareStatement(sql);
      conn.setAutoCommit(false);
      for (i = 0; i < 100000; i++)
      {
        // setting up values / binding
        String name = "test LINESTRING #";
        name += i + 1;
        String geom = "LINESTRING (";
        if ((i%2) == 1)
        {
          // odd row: five points
          geom += "-180.0 -90.0, ";
          geom += -10.0 - (i / 1000.0);
          geom += " ";
          geom += -10.0 - (i / 1000.0);
          geom += ", ";
          geom += -10.0 - (i / 1000.0);
          geom += " ";
          geom += 10.0 + (i / 1000.0);
          geom += ", ";
          geom += 10.0 + (i / 1000.0);
          geom += " ";
          geom += 10.0 + (i / 1000.0);
          geom += ", 180.0 90.0";
        }
        else
        {
          // even row: two points
          geom += -10.0 - (i / 1000.0);
          geom += " ";
          geom += -10.0 - (i / 1000.0);
          geom += ", ";
          geom += 10.0 + (i / 1000.0);
          geom += " ";
          geom += 10.0 + (i / 1000.0);
        }
        geom += ")";
        ins_stmt.setInt(1, i+1);
        ins_stmt.setString(2, name);
        ins_stmt.setString(3, geom);
        ins_stmt.executeUpdate();
      }
      conn.commit();

      // checking LINESTRINGs
      sql = "SELECT DISTINCT Count(*), GeometryType(geom), ";
      sql += "Srid(geom) FROM test_ln";
      rs = stmt.executeQuery(sql);
      while(rs.next())
      {
        // read the result set
        String msg = "> Inserted ";
        msg += rs.getInt(1);
        msg += " entities of type ";
        msg += rs.getString(2);
        msg += " SRID=";
        msg += rs.getInt(3);
        System.out.println(msg);
      }

      // inserting some POLYGONs
      // this time too we'll use a Prepared Statement
      sql = "INSERT INTO test_pg (id, name, geom) ";
      sql += "VALUES (?, ?, GeomFromText(?, 4326))";
      ins_stmt = conn.prepareStatement(sql);
      conn.setAutoCommit(false);
      for (i = 0; i < 100000; i++)
      {
        // setting up values / binding
        String name = "test POLYGON #";
        name += i + 1;
        ins_stmt.setInt(1, i+1);
        ins_stmt.setString(2, name);
        String geom = "POLYGON((";
        geom += -10.0 - (i / 1000.0);
        geom += " ";
        geom += -10.0 - (i / 1000.0);
        geom += ", ";
        geom += 10.0 + (i / 1000.0);
        geom += " ";
        geom += -10.0 - (i / 1000.0);
        geom += ", ";
        geom += 10.0 + (i / 1000.0);
        geom += " ";
        geom += 10.0 + (i / 1000.0);
        geom += ", ";
        geom += -10.0 - (i / 1000.0);
        geom += " ";
        geom += 10.0 + (i / 1000.0);
        geom += ", ";
        geom += -10.0 - (i / 1000.0);
        geom += " ";
        geom += -10.0 - (i / 1000.0);
        geom += "))";
        ins_stmt.setInt(1, i+1);
        ins_stmt.setString(2, name);
        ins_stmt.setString(3, geom);
        ins_stmt.executeUpdate();
      }
      conn.commit();

      // checking POLYGONs
      sql = "SELECT DISTINCT Count(*), GeometryType(geom), ";
      sql += "Srid(geom) FROM test_pg";
      rs = stmt.executeQuery(sql);
      while(rs.next())
      {
        // read the result set
        String msg = "> Inserted ";
        msg += rs.getInt(1);
        msg += " entities of type ";
        msg += rs.getString(2);
        msg += " SRID=";
        msg += rs.getInt(3);
        System.out.println(msg);
      }
	  
// ################# FINISH EXAMPLE QUERYS ###############################
	}
    catch(SQLException e)
    {
      // if the error message is "out of memory",
      // it probably means no database file is found
      System.err.println(e.getMessage());
    }
    finally
    {
      try
      {
        if(conn != null)
          conn.close();
      }
      catch(SQLException e)
      {
        // connection close failed.
        System.err.println(e);
      }
    }
	}
	catch(RuntimeException e)
	{
		System.err.println(e.getMessage());
	}
  }
}