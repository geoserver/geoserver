import geoscript.geom.Geometry

title = 'Buffer'
description = 'Buffers a geometry'

inputs = [
  geom: [title: 'The geometry to buffer', type: Geometry.class], 
  distance: [title: 'The buffer distance', type: Double.class]
]

outputs = [
  result: [title: 'The buffered geometry',  type: Geometry.class]
]

def run(input) {
  return [result: input.geom.buffer(input.distance)]
}