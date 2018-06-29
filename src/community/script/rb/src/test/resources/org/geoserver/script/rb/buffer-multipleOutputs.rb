require 'java'

$title = 'Buffer'
$description = 'Buffers a geometry'

$inputs = { 
  "geom" => { 
    "title" => "The geometry to buffer", 
    "type" => org.locationtech.jts.geom.Geometry.java_class
   },
  "distance" => { 
    "title" => "The buffer distance", 
    "type" => java.lang.Double.java_class
  }
} 

$outputs = {
  "geom" => { 
    "title" => "The geometry to buffer", 
    "type" => org.locationtech.jts.geom.Geometry.java_class
   },
  "distance" => { 
    "title" => "The buffer distance", 
    "type" => java.lang.Double.java_class
  }
}

def run(inputs)
  return { "geom" => inputs["geom"].buffer(inputs["distance"]), "distance" => inputs["distance"] }
  
end