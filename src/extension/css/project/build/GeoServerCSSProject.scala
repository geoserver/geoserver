import sbt._

class GeoServerCSSProject(info: ProjectInfo) extends DefaultProject(info) {
  val gtVersion="2.7-SNAPSHOT"
  val gsVersion="2.1-SNAPSHOT"

  override def repositories = super.repositories ++ Set(
    "OpenGeo Maven Repository" at "http://repo.opengeo.org/"
  )

  override def libraryDependencies = super.libraryDependencies ++ Set(
    "org.scalatest" % "scalatest" % "1.0" % "test",
    "junit" % "junit" % "4.2" % "test",
    "org.geotools" % "gt-main" % gtVersion,
    "org.geotools" % "gt-cql" % gtVersion,
    "org.geotools" % "gt-epsg-hsql" % gtVersion,
    "org.geotools" % "gt-jdbc" % gtVersion,
    "org.geotools" % "gt-shapefile" % gtVersion,
    "org.geoserver" % "main" % gsVersion % "provided",
    "org.geoserver.web" % "web-core" % gsVersion % "provided",
    "xml-apis" % "xml-apis-xerces" % "2.7.1" from "http://repo.opengeo.org/xml-apis/xml-apis-xerces/2.7.1/xml-apis-xerces-2.7.1.jar"
  )
}
