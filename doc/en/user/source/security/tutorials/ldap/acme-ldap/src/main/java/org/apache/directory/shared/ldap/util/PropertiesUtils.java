/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.util;



import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.directory.shared.ldap.NotImplementedException;


/**
 * A utility class used for accessing, finding, merging and macro expanding
 * properties, on disk, via URLS or as resources.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 686082 $
 */
public class PropertiesUtils
{
    /** default properties file extension */
    private static final String DOTPROPERTIES = ".properties";


    // ------------------------------------------------------------------------
    // Utilities for discovering Properties
    // ------------------------------------------------------------------------

    /**
     * Loads a properties object in a properties file if it exists relative to
     * the filename ${user.home}. If the file ${user.home}/[filename] does not
     * exist then one last attempt to find the file is made if filename does not
     * have a .properties extension. If so and
     * ${user.home}/[filename].properties exists then it is loaded.
     * 
     * @param filename
     *            the properties file name with or without an extension
     * @return the user properties object
     */
    public static Properties findUserProperties( String filename )
    {
        return findProperties( new File( System.getProperty( "user.home" ) ), filename );
    }


    /**
     * Create a new properties object and load the properties file if it exists
     * relative to [dir]/[filename] or [dir]/[filename].properties.
     * 
     * @param dir
     *            the base directory
     * @param filename
     *            the full fine name or the base name w/o the extension
     * @return the loaded properties object
     */
    public static Properties findProperties( File dir, String filename )
    {
        final File asis = new File( dir, filename );

        if ( asis.exists() )
        {
            return getProperties( asis );
        }

        if ( filename.endsWith( DOTPROPERTIES ) )
        {
            String noExt = filename.substring( 0, filename.length() - 11 );
            if ( new File( dir, noExt ).exists() )
            {
                return getProperties( new File( dir, noExt ) );
            }

            return new Properties();
        }

        File withExt = new File( dir, filename + DOTPROPERTIES );
        if ( withExt.exists() )
        {
            return getProperties( withExt );
        }

        return new Properties();
    }


    /**
     * Load a properties from a resource relative to a supplied class. First an
     * attempt is made to locate a property file colocated with the class with
     * the name [class].properties. If this cannot be found or errors result an
     * empty Properties file is returned.
     * 
     * @param ref
     *            a class to use for relative path references
     * @return the static properties
     */
    public static Properties getStaticProperties( Class<?> ref )
    {
        final Properties properties = new Properties();
        final String address = ref.toString().replace( '.', '/' );
        final String path = address + ".properties";
        InputStream input = ref.getResourceAsStream( path );

        if ( null != input )
        {
            try
            {
                properties.load( input );
            }
            catch ( IOException e )
            {
                return properties;
            }
        }

        return properties;
    }


    /**
     * Load properties from a resource relative to a supplied class and path.
     * 
     * @param ref
     *            a class to use for relative path references
     * @param path
     *            the relative path to the resoruce
     * @return the static properties
     */
    public static Properties getStaticProperties( Class<?> ref, String path )
    {
        Properties properties = new Properties();
        InputStream input = ref.getResourceAsStream( path );

        if ( input == null )
        {
            return properties;
        }

        try
        {
            properties.load( input );
        }
        catch ( IOException e )
        {
            return properties;
        }

        return properties;
    }


    /**
     * Creates a properties object and loads the properties in the file
     * otherwise and empty property object will be returned.
     * 
     * @param file
     *            the properties file
     * @return the properties object
     */
    public static Properties getProperties( File file )
    {
        Properties properties = new Properties();

        if ( null == file )
        {
            return properties;
        }

        if ( file.exists() )
        {
            try
            {
                final FileInputStream fis = new FileInputStream( file );
                try
                {
                    properties.load( fis );
                }
                finally
                {
                    fis.close();
                }
            }
            catch ( IOException e )
            {
                return properties;
            }
        }

        return properties;
    }


    /**
     * Loads a properties file as a CL resource if it exists and returns an
     * empty Properties object otherwise.
     * 
     * @param classloader
     *            the loader to use for the resources
     * @param path
     *            the path to the resource
     * @return the loaded or new Properties
     */
    public static Properties getProperties( ClassLoader classloader, String path )
    {
        Properties properties = new Properties();
        InputStream input = classloader.getResourceAsStream( path );

        if ( input != null )
        {
            try
            {
                properties.load( input );
            }
            catch ( IOException e )
            {
                return properties;
            }
        }

        return properties;
    }


    /**
     * Loads a properties file as a class resource if it exists and returns an
     * empty Properties object otherwise.
     * 
     * @param clazz
     *            the class to use for resolving the resources
     * @param path
     *            the relative path to the resource
     * @return the loaded or new Properties
     */
    public static Properties getProperties( Class<?> clazz, String path )
    {
        Properties properties = new Properties();
        InputStream input = clazz.getResourceAsStream( path );

        if ( input != null )
        {
            try
            {
                properties.load( input );
            }
            catch ( IOException e )
            {
                return properties;
            }
        }

        return properties;
    }


    // ------------------------------------------------------------------------
    // Utilities for operating on or setting Properties values
    // ------------------------------------------------------------------------

    /**
     * Expands out a set of property key macros in the following format
     * ${foo.bar} where foo.bar is a property key, by dereferencing the value of
     * the key using the original source Properties and other optional
     * Properties. If the original expanded Properties contain the value for the
     * macro key, foo.bar, then dereferencing stops by using the value in the
     * expanded Properties: the other optional Properties are NOT used at all.
     * If the original expanded Properties do NOT contain the value for the
     * macro key, then the optional Properties are used in order. The first of
     * the optionals to contain the value for the macro key (foo.bar) shorts the
     * search. Hence the first optional Properties in the array to contain a
     * value for the macro key (foo.bar) is used to set the expanded value. If a
     * macro cannot be expanded because it's key was not defined within the
     * expanded Properties or one of the optional Properties then it is left as
     * is.
     * 
     * @param expanded
     *            the Properties to perform the macro expansion upon
     * @param optionals
     *            null or an optional set of Properties to use for dereferencing
     *            macro keys (foo.bar)
     */
    public static void macroExpand( Properties expanded, Properties[] optionals )
    {
        // Handle null optionals
        if ( null == optionals )
        {
            optionals = new Properties[0];
        }

        Enumeration<?> list = expanded.propertyNames();
        
        while ( list.hasMoreElements() )
        {
            String key = ( String ) list.nextElement();
            String macro = expanded.getProperty( key );

            int n = macro.indexOf( "${" );
            if ( n < 0 )
            {
                continue;
            }

            int m = macro.indexOf( "}", n + 2 );
            if ( m < 0 )
            {
                continue;
            }

            final String symbol = macro.substring( n + 2, m );

            if ( expanded.containsKey( symbol ) )
            {
                final String value = expanded.getProperty( symbol );
                final String head = macro.substring( 0, n );
                final String tail = macro.substring( m + 1 );
                final String resolved = head + value + tail;
                expanded.put( key, resolved );
                continue;
            }

            /*
             * Check if the macro key exists within the array of optional
             * Properties. Set expanded value to first Properties with the key
             * and break out of the loop.
             */
            for ( int ii = 0; ii < optionals.length; ii++ )
            {
                if ( optionals[ii].containsKey( symbol ) )
                {
                    final String value = optionals[ii].getProperty( symbol );
                    final String head = macro.substring( 0, n );
                    final String tail = macro.substring( m + 1 );
                    final String resolved = head + value + tail;
                    expanded.put( key, resolved );
                    break;
                }
            }
        }
    }


    /**
     * Discovers a value within a set of Properties either halting on the first
     * time the property is discovered or continuing on to take the last value
     * found for the property key.
     * 
     * @param key
     *            a property key
     * @param sources
     *            a set of source Properties
     * @param haltOnDiscovery
     *            true if we stop on finding a value, false otherwise
     * @return the value found or null
     */
    public static String discover( String key, Properties[] sources, boolean haltOnDiscovery )
    {
        String retval = null;

        for ( int ii = 0; ii < sources.length; ii++ )
        {
            if ( sources[ii].containsKey( key ) )
            {
                retval = sources[ii].getProperty( key );

                if ( haltOnDiscovery )
                {
                    break;
                }
            }
        }

        return retval;
    }


    /**
     * Merges a set of properties from source Properties into a target
     * properties instance containing keys. This method does not allow null
     * overrides.
     * 
     * @param keys
     *            the keys to discover values for
     * @param sources
     *            the sources to search
     * @param haltOnDiscovery
     *            true to halt on first find or false to continue to last find
     */
    public static void discover( Properties keys, Properties[] sources, boolean haltOnDiscovery )
    {
        if ( null == sources || null == keys )
        {
            return;
        }

        /*
         * H A N D L E S I N G L E V A L U E D K E Y S
         */
        for ( Object key:keys.keySet() )
        {
            String value = discover( (String)key, sources, haltOnDiscovery );

            if ( value != null )
            {
                keys.setProperty( (String)key, value );
            }
        }
    }


    // ------------------------------------------------------------------------
    // Various Property Accessors
    // ------------------------------------------------------------------------

    /**
     * Gets a String property as a boolean returning a defualt if the key is not
     * present. In any case, true, on, 1 and yes strings return true and
     * everything else returns
     * 
     * @param props
     *            the properties to get the value from
     * @param key
     *            the property key
     * @param defaultValue
     *            the default value to return if key is not present
     * @return true defaultValue if property does not exist, else return true if
     *         the String value is one of 'true', 'on', '1', 'yes', otherwise
     *         false is returned
     */
    public static boolean get( Properties props, String key, boolean defaultValue )
    {
        if ( props == null || !props.containsKey( key ) || props.getProperty( key ) == null )
        {
            return defaultValue;
        }

        String val = props.getProperty( key ).trim().toLowerCase();
        return val.equals( "true" ) || val.equals( "on" ) || val.equals( "1" ) || val.equals( "yes" );
    }


    /**
     * Gets a property or entry value from a hashtable and tries to transform
     * whatever the value may be to an primitive integer.
     * 
     * @param ht
     *            the hashtable to access for the value
     * @param key
     *            the key to use when accessing the ht
     * @param defval
     *            the default value to use if the key is not contained in ht or
     *            if the value cannot be represented as a primitive integer.
     * @return the primitive integer representation of a hashtable value
     */
    public static int get( Hashtable<String, Object> ht, Object key, int defval )
    {
        if ( ht == null || !ht.containsKey( key ) || ht.get( key ) == null )
        {
            return defval;
        }

        Object obj = ht.get( key );

        if ( obj instanceof Byte )
        {
            return ( ( Byte ) obj ).intValue();
        }
        if ( obj instanceof Short )
        {
            return ( ( Short ) obj ).intValue();
        }
        if ( obj instanceof Integer )
        {
            return ( ( Integer ) obj ).intValue();
        }
        if ( obj instanceof Long )
        {
            return ( ( Long ) obj ).intValue();
        }
        if ( obj instanceof String )
        {
            try
            {
                return Integer.parseInt( ( String ) obj );
            }
            catch ( NumberFormatException ne )
            {
                ne.printStackTrace();
                return defval;
            }
        }

        return defval;
    }


    public static long get( Properties props, String key, long defaultValue )
    {
        if ( props == null || !props.containsKey( key ) || props.getProperty( key ) == null )
        {
            return defaultValue;
        }

        throw new NotImplementedException();
    }


    public static byte get( Properties props, String key, byte defaultValue )
    {
        if ( props == null || !props.containsKey( key ) || props.getProperty( key ) == null )
        {
            return defaultValue;
        }

        throw new NotImplementedException();
    }


    public static char get( Properties props, String key, char defaultValue )
    {
        if ( props == null || !props.containsKey( key ) || props.getProperty( key ) == null )
        {
            return defaultValue;
        }

        throw new NotImplementedException();
    }
}
