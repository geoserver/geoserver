# Java Considerations

## Use supported JRE

GeoServer performance depends a lot on the chosen Java Runtime Environment (JRE). The stable and maintenance versions of GeoServer are tested with OpenJDK (LTS) Long Term Support releases. Implementations other than those tested may work correctly, but are generally not recommended.

| GeoServer        | Java 25 | Java 21 | Java 17 | Java 11   | Java 8    | 
| ---------------- | ------- |-------- | ------- | --------- | --------- | 
| GeoServer 3.0.x  | testing | OpenJDK | OpenJDK |           |           | 
| GeoServer 2.28.x |         | OpenJDK | OpenJDK |           |           | 
| GeoServer 2.27.x |         |         |         | OpenJDK   |           | 
| GeoServer 2.22.x |         |         |         | OpenJDK   | OpenJDK <br/> OracleJRE |
| GeoServer 2.15.x |         |         |         | OpenJDK   | OpenJDK <br/> OracleJRE |
| GeoServer 2.9.x  |         |         |         |           | OpenJDK <br/> OracleJRE |

Please see GeoServer 2 manual compatibility guidance prior to Java 8.

Reference:

* [Downloads](https://geoserver.org/download/) (Archive tab lists Java compatibility)

## Running on Java 17

GeoServer 2.28.x and above requires Java 17 as the minimum version.

Java 17 deployment on Tomcat 11 and Jetty 12.1 is subject to automated testing.

GeoServer code depends on a variety of libraries trying to access the JDK internals. It does not seem to matter when running as a web application. However, in case of need, here is the full list of opens used by the build process:

```
--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.text=ALL-UNNAMED \
--add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
--add-opens=java.desktop/sun.awt.image=ALL-UNNAMED \
--add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED \
--add-opens=java.desktop/sun.java2d.pipe=ALL-UNNAMED
```

## Running on Java 21

GeoServer 2.28.x and above supports Java 21 with no additional configuration.

Java 21 deployment on Tomcat 11 and Jetty 12.1 is subject to automated testing.

Running GeoServer under Java 21 on other Application Servers may require some additional configuration.
Most Application Servers now support Java 21.

## Running on Java 25

Java 25 deployment on Tomcat 11 and Jetty 12.1 is subject to automated testing.

We welcome community testing in this environment, and will upgrade status to recommended based on positive [user forum](https://discourse.osgeo.org/c/geoserver/user/51) feedback.
