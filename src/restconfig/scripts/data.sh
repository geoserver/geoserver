function build {
  mkdir scratch
  cp  ../../../../data/release/$1/$2* scratch/
  if [ "$3" != "" ]; then
    cp  ../../../../data/release/$1/$3* scratch/
  fi
  cd scratch
  zip $2.zip * 
  mv $2.zip ../
  cd ../
  rm -rf scratch
}

build coverages/img_sample Pk50095
build coverages/img_sample usa
build coverages/arc_sample precip30min
build coverages/mosaic_sample mosaic global_mosaic
build data/sf sfdem
build data/sf archsites
build data/sf bugsites
build data/sf restricted
build data/sf roads
build data/sf streams
build data/shapefiles states
build data/nyc giant_polygon
build data/nyc poi
build data/nyc poly_landmarks
build data/nyc tiger_roads
build data/taz_shapes tasmania_cities
build data/taz_shapes tasmania_roads
build data/taz_shapes tasmania_state_boundaries
build data/taz_shapes tasmania_water_bodies

cp  ../../../../data/release/styles/*.sld .
