from geoserver.format import vector_format

@vector_format('Foo', 'text/plain')
def write(data, output):
    for f in data.features:
      output.write(f.id + ';')