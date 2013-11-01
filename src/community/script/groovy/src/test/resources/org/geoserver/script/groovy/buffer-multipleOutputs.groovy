import geoscript.geom.Geometry

title = 'Buffer'
description = 'Buffers a geometry'

inputs = [
  geom: [title: 'The geometry to buffer', type: Geometry.class], 
  distance: [title: 'The buffer distance', type: Double.class]
]

outputs = [
  geom: [title: 'The buffered geometry',  type: Geometry.class],
  distance: [title: 'The buffer distance', type: Double.class]
]

def run(input) {
  return [geom: input.geom.buffer(input.distance), distance: input.distance]
}