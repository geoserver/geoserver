from geoserver.format import vector_format

@vector_format('Foo', 'text/plain')
def write(data, output):
  for fc in data:
    for f in fc.features:
      output.write(f.id + ';')