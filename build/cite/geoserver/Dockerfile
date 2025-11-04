ARG JAVA_VERSION=17
FROM tomcat:11.0-jdk${JAVA_VERSION}-temurin-jammy

# Set environment variables for Java and GeoServer options
ENV EXTRA_JAVA_OPTS="-Xms256m -Xmx1g"
ENV GEOSERVER_DATA_DIR=/opt/geoserver_data
ENV GEOWEBCACHE_CACHE_DIR=/opt/geowebcache_data

# Define GeoServer-related options
ENV CATALINA_OPTS="$EXTRA_JAVA_OPTS \
    --add-exports=java.desktop/sun.awt.image=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.text=ALL-UNNAMED \
    --add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
    --add-opens=java.desktop/sun.awt.image=ALL-UNNAMED \
    --add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED \
    --add-opens=java.desktop/sun.java2d.pipe=ALL-UNNAMED \
    -Djava.awt.headless=true -server \
    -Dfile.encoding=UTF-8 \
    -Djavax.servlet.request.encoding=UTF-8 \
    -Djavax.servlet.response.encoding=UTF-8 \
    -D-XX:SoftRefLRUPolicyMSPerMB=36000 \
    -Xbootclasspath/a:$CATALINA_HOME/lib/marlin.jar \
    -Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine \
    -Dorg.geotools.coverage.jaiext.enabled=true"

# Create a non-root user and group for running Tomcat
RUN groupadd -r tomcat && useradd -r -g tomcat -d /usr/local/tomcat -s /sbin/nologin tomcat

# Expose Tomcat's default port
EXPOSE 8080

# Define arguments for source file and data directory source
ARG SOURCE_FILE
ARG GEOSERVER_DATA_DIR_SRC

# Download or copy the GeoServer WAR or ZIP file, or extract if it’s a .zip
ADD ${SOURCE_FILE} "$CATALINA_HOME/webapps/"

# zip files require explicit extracion
RUN \
    cd "$CATALINA_HOME/webapps/"; \
    if [ "${SOURCE_FILE##*.}" = "zip" ]; then \
        unzip "./*zip"; \
    fi;

# Copy the GeoServer data directory from the specified location
ADD ${GEOSERVER_DATA_DIR_SRC} ${GEOSERVER_DATA_DIR}

# Set ownership and permissions for Tomcat and GeoServer data directories
RUN chown -R tomcat:tomcat /usr/local/tomcat \
	&& mkdir -p $GEOSERVER_DATA_DIR \
	&& chown -R tomcat:tomcat $GEOSERVER_DATA_DIR \
	&& mkdir -p $GEOWEBCACHE_CACHE_DIR \
	&& chown -R tomcat:tomcat $GEOWEBCACHE_CACHE_DIR

# Switch to the unprivileged user
USER tomcat
WORKDIR "$CATALINA_HOME"

# Define health check for GeoServer
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/geoserver/web/wicket/resource/org.geoserver.web.GeoServerBasePage/img/logo.png || exit 1

# Start Tomcat as the unprivileged user
CMD ["catalina.sh", "run"]