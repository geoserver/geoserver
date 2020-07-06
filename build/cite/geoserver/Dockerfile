FROM tomcat:jdk11-openjdk
LABEL maintainer="Alessandro Parma<alessandro.parma@geo-solutions.it>"

ENV  DEBIAN_FRONTEND noninteractive

ENV CATALINA_BASE "$CATALINA_HOME"

ENV GEOSERVER_HOME="/var/geoserver"
ENV GEOSERVER_LOG_DIR="${GEOSERVER_HOME}/logs"
ENV GEOSERVER_DATA_DIR="${GEOSERVER_HOME}/datadir"
ENV \
    GEOSERVER_LOG_LOCATION="${GEOSERVER_LOG_DIR}/geoserver.log" \
    GEOWEBCACHE_CONFIG_DIR="${GEOSERVER_DATA_DIR}/gwc"          \
    GEOWEBCACHE_CACHE_DIR="${GEOSERVER_HOME}/gwc_cache_dir"

RUN mkdir -p \
    "${GEOSERVER_DATA_DIR}"     \
    "${GEOSERVER_LOG_DIR}"      \
    "${GEOWEBCACHE_CACHE_DIR}"  \
    "${GEOWEBCACHE_CONFIG_DIR}"

# override at run time as needed JAVA_OPTS
ENV JAVA_OPTS="-Xms1024m -Xmx1024m"

# Optionally remove Tomcat manager, docs, and examples
ARG TOMCAT_EXTRAS=false
ENV TOMCAT_EXTRAS "$TOMCAT_EXTRAS"
RUN \
    if [ "$TOMCAT_EXTRAS" = false ]; then \
        rm -rfv "${CATALINA_HOME}/webapps/*" \
    ; fi

# local dir, tar or remore URLs
ARG GEOSERVER_DATA_DIR_SRC="./.placeholder"
ENV GEOSERVER_DATA_DIR_SRC="${GEOSERVER_DATA_DIR_SRC}"
ADD "${GEOSERVER_DATA_DIR_SRC}" "${GEOSERVER_DATA_DIR}"

# accepts local files and URLs. Tar(s) are automatically extracted
ARG GEOSERVER_WEBAPP_SRC="./.placeholder"
ENV GEOSERVER_WEBAPP_SRC="${GEOSERVER_WEBAPP_SRC}"
ADD "${GEOSERVER_WEBAPP_SRC}" "${CATALINA_BASE}/webapps/"

# zip files require explicit extracion
RUN \
    cd "${CATALINA_BASE}/webapps/"; \
    if [ "${GEOSERVER_WEBAPP_SRC##*.}" = "zip" ]; then \
        unzip "./*zip"; \
        rm ./*zip; \
    fi

WORKDIR "$CATALINA_BASE"

ENV TERM xterm
EXPOSE 8080
