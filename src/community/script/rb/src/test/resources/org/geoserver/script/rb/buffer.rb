require 'java'

$title = 'Buffer'
$description = 'Buffers a geometry'

$inputs = { 
  "geom" => { 
    "title" => "The geometry to buffer", 
    "type" => com.vividsolutions.jts.geom.Geometry.java_class
   },
  "distance" => { 
    "title" => "The buffer distance", 
    "type" => java.lang.Double.java_class
  }
} 

$outputs = {
  "result" => {
    "title" => "The buffered geometry", 
    "type" => com.vividsolutions.jts.geom.Geometry.java_class
  }
}

def run(inputs)
  return { "result" => inputs["geom"].buffer(inputs["distance"]) }
  
end