package org.geoserver.spatialite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sqlite.SQLiteConfig;

public class SpatialiTest
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
          //enabling Spatial Metadata
	  //String sql = "";
	  String sql = "SELECT InitSpatialMetaData();";
	  stmt.execute(sql);
	  
	  // Load the files and execute the querys.

	      File file = new File("querys.txt");
	      FileInputStream fis = null;
	      BufferedInputStream bis = null;
	      DataInputStream dis = null;

	      try {
	        fis = new FileInputStream(file);

	        // Here BufferedInputStream is added for fast reading.
	        bis = new BufferedInputStream(fis);
	        dis = new DataInputStream(bis);

	        // dis.available() returns 0 if the file does not have more lines
	        
	        try
	        {
	            conn.setAutoCommit(false); 
	            String consulta = null;
	            while (dis.available() != 0) {
	                    consulta = dis.readLine();
	                    stmt.execute(consulta);
	                    System.out.println(consulta);
	                }
	            conn.commit();
	        
	        }
	        
	        
	        catch(SQLException e)
	        {
	          System.err.println(e.getMessage());
	        }
	        
	        // dispose all the resources after using them.
	        fis.close();
	        bis.close();
	        dis.close();

	      } catch (FileNotFoundException e) {
	        e.printStackTrace();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
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