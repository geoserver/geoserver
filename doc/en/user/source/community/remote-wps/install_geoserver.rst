.. _extensions_wps_remote_install_geoserver:

Deployment And Setup Of GeoServer With WPS Remote Plugin
========================================================

The following commands will prepare a CentOS 7 Minimal ISO machine for the deployment of:

* GeoServer with the following plugins:
    
    * GeoServer WPS
    * GeoServer Remote WPS Orchestrator
    * GeoServer Importer
    
The OS iso has been downloaded from:::

    http://isoredirect.centos.org/centos/7/isos/x86_64/CentOS-7-x86_64-Minimal-1503-01.iso

Preparation of the system: standard and basic OS packages
---------------------------------------------------------

Hostname and other useful packages
++++++++++++++++++++++++++++++++++

Update the file ``/etc/hosts`` making sure that the ip addresses matches the name of the machine.

.. code-block:: bash

  # as root

  $> yum -y install man vim openssh-clients mc zip unzip wget net-tools
    
Configure the Java Virtual Environment
++++++++++++++++++++++++++++++++++++++

.. code-block:: bash

  # as root
  
  $> # Install OpenJDK 17 using the package manager
  $> export SDKMAN_DIR="/usr/local/sdkman" && curl -s "https://get.sdkman.io" | bash
  $> source "$HOME/.sdkman/bin/sdkman-init.sh"
  $> sdk list java|grep "17.*\-tem"
  $>             |     | 17.0.15      | tem     |            | 17.0.15-tem         
  $>             |     | 17.0.14      | tem     |            | 17.0.14-tem
  $> sdk install java 17.0.15-tem -y
  $> # Make Java available to all users
  $> echo 'export SDKMAN_DIR="/usr/local/sdkman"' >> /etc/bashrc
  $> echo '[[ -s "/usr/local/sdkman/bin/sdkman-init.sh" ]] && source "/usr/local/sdkman/bin/sdkman-init.sh"' >> /etc/bashrc
  
  # Verify the proper installation on the JDK
  
  $> java -version
    openjdk version "17.0.15" 2025-04-15
    OpenJDK Runtime Environment Temurin-17.0.15+6 (build 17.0.15+6)
    OpenJDK 64-Bit Server VM Temurin-17.0.15+6 (build 17.0.15+6, mixed mode, sharing)
  
  $> javac -version
    javac 17.0.15
    
Installing Apache Tomcat
++++++++++++++++++++++++

.. code-block:: bash

  # as root
  
  $> yum -y install tomcat-webapps
  $> systemctl disable tomcat.service
  $> # Set JAVA_HOME for Tomcat service. Edit /etc/tomcat/tomcat.conf
  $> # and replace JAVA_HOME="/usr/lib/jvm/jre" with JAVA_HOME="/usr/local/sdkman/candidates/java/current"
  $> cp /etc/sysconfig/tomcat /etc/sysconfig/geoserver
  $> ln -s /usr/share/tomcat/ /opt/tomcat

**Creating apache tomcat HOME context**

Creating base template directory

.. code-block:: bash

  # as root

  $> mkdir -p /var/lib/tomcat/geoserver/{bin,conf,logs,temp,webapps,work}

  $> cp -Rf /opt/tomcat/conf/* /var/lib/tomcat/geoserver/conf/
  
**Creating geoserver apache tomcat BASE context**

Make sure you already:

* installed tomcat (Installing apache tomcat)

* created the base catalina template (Creating apache tomcat HOME context)

Edit ``server.xml`` file

GeoServer is the first tomcat instance we are installing in this VM, so we can keep the default ports:

  - 8005 for commands to catalina instance

  - 8009 for the AJP connection port

  - 8080 for the HTTP connection port
  
Remember that you may change these ports in the file ``/var/lib/tomcat/geoserver/conf/server.xml``

**Final configurations**

Set the ownership of the ``geoserver/`` related directories to user tomcat

.. code-block:: bash

  # as root
  
  $> chown tomcat: -R /var/lib/tomcat/geoserver
  $> cp /etc/tomcat/tomcat.conf /etc/tomcat/geoserver.conf
  
  $> vi /etc/tomcat/geoserver.conf
  
    # This variable is used to figure out if config is loaded or not.
    TOMCAT_CFG_LOADED="1"

    # In new-style instances, if CATALINA_BASE isn't specified, it will
    # be constructed by joining TOMCATS_BASE and NAME.
    TOMCATS_BASE="/var/lib/tomcats/"

    # Where your java installation lives
    #JAVA_HOME="/usr/lib/jvm/jre"
    JAVA_HOME="/usr/local/sdkman/candidates/java/current"

    # Where your tomcat installation lives
    CATALINA_HOME="/usr/share/tomcat"
    CATALINA_BASE="/var/lib/tomcat/geoserver"
    CATALINA_PID=$CATALINA_BASE/work/pidfile.pid

    # System-wide tmp
    CATALINA_TMPDIR="/var/cache/tomcat/temp"

    # You can pass some parameters to java here if you wish to
    #JAVA_OPTS="-Xminf0.1 -Xmaxf0.3"
    # Use JAVA_OPTS to set java.library.path for libtcnative.so
    #JAVA_OPTS="-Djava.library.path=/usr/lib"
    JAVA_OPTS="-server -XX:SoftRefLRUPolicyMSPerMB=36000 -Xms1024m -Xmx2048m
    -XX:PermSize=64m -XX:+UseConcMarkSweepGC -XX:NewSize=48m -DGEOSERVER_DATA_DIR=/storage/data/
    -DENABLE_ADVANCED_PROJECTION=false -Dorg.geotools.shapefile.datetime=true -Duser.timezone=GMT
    -Dorg.geotools.filter.function.simplify=true -DGEOMETRY_COLLECT_MAX_COORDINATES=50000"

    # You can change your tomcat locale here
    #LANG="en_US"
    # Run tomcat under the Java Security Manager
    SECURITY_MANAGER="false"
    
  $> cp /usr/lib/systemd/system/tomcat.service /usr/lib/systemd/system/geoserver.service
  
  $> vi /usr/lib/systemd/system/geoserver.service
  
    EnvironmentFile=/etc/tomcat/geoserver.conf
  
  $> systemctl enable geoserver.service
  $> systemctl restart geoserver.service
  
  # Follow the server startup procedure and make sure everything goes smoothly through the following command
  
  $> tail -F /var/lib/tomcat/geoserver/logs/catalina.YYYY-MM-DD.log

Deploy And Configure GeoServer
++++++++++++++++++++++++++++++

**First deployment**

.. code-block:: bash

  # as root
  
  # Git and Maven must be installed on the system
  $> yum -y install git
  $> yum -y install maven
  $> # Remove the JDK 8 installed with tomcat/maven
  $> rpm -e --nodeps java-1.8.0-openjdk java-1.8.0-openjdk-devel java-1.8.0-openjdk-headless
  $> source ~/.bashrc
  
  # Verify the Maven installation and double check that the JDK recognized is OpenJDK 17+
  $> mvn -version
    Apache Maven 3.0.5 (Red Hat 3.0.5-17)
    Maven home: /usr/share/maven
    Java version: 17.0.15, vendor: Eclipse Adoptium
    Java home: /usr/local/sdkman/candidates/java/17.0.15-tem
    Default locale: en_US, platform encoding: ANSI_X3.4-1968
    OS name: "linux", version: "6.10.14-linuxkit", arch: "aarch64", family: "unix"
  
  # The following procedures allow to collect and compile the source code from the GIT repository.
  $> cd
  
  $> git clone https://github.com/geosolutions-it/geoserver.git geoserver.src
  
  $> cd geoserver.src/src
  
  $> git checkout wps-remote
  $> git pull
  
  $> mvn clean install -Pwps,wps-remote,importer,security,rest-ext -DskipTests
  
  $> mv web/app/target/geoserver.war /var/lib/tomcat/geoserver/webapps/
  
  $> chown -Rf tomcat: /var/lib/tomcat/geoserver
  
  $> mv /var/lib/tomcat/geoserver/webapps/geoserver/data/ /storage/
  
  $> chown -Rf tomcat: /storage
  
  $> vim /storage/data/remoteProcess.properties
  
	# Default Properties
	remoteProcessStubCycleSleepTime = 100

	# Base path where uploaded files are stored
	# . This is used only when a remote uploader is enabled on the Python
	# . WPS Agent. This property represents the local base path (on the filesystem
	# . of GeoServer) where to search for uploaded files.
	# . If not file has been found here (or this option is not enabled), GeoServer
	# . looks for absolute path and/or paths relative to the GEOSERVER DATA DIR.
	#uploadedFilesBasePath = /tmp

	# Full path to the template used to generate the OWS WMC Json output
	# . This property is used only when a "application/owc" output type on
	# . the Python WPS Agent.
	#owc_wms_json_template = absolute_path/to/wmc_template.json

	# Specific kvps for {@link RemoteProcessClient) implementations
	xmpp_server = localhost
	xmpp_server_embedded = false
	xmpp_server_embedded_secure = true
	xmpp_server_embedded_certificate_file = bogus_mina_tls.cert
	xmpp_server_embedded_certificate_password = boguspw
	xmpp_port = 5222

	xmpp_manager_username = admin
	xmpp_manager_password = R3m0T3wP5

	# domain and MUC service name of the XMPP Server
	xmpp_domain = geoserver.org
	xmpp_bus = conference

	# name, user and password of the management room
	xmpp_management_channel = management
	xmpp_management_channel_user = admin
	xmpp_management_channel_pwd = R3m0T3wP5

	# comma separated list of available rooms for services. Those rooms'names will be equal to the service and WPS Process namespace
	# Avoid spaces
	xmpp_service_channels = default,geosolutions

	# millis
	xmpp_packet_reply_timeout = 500

	# connection keep alive
	xmpp_connection_ping_interval = 30000
	xmpp_connection_ping_timeout = 10000
	xmpp_connection_ping_initial_delay = 20000

	# Thresholds indicating overloaded resources
	xmpp_cpu_perc_threshold = 82.5
	xmpp_mem_perc_threshold = 84.6
    
  # Restart GeoServer
  
  $> service geoserver restart
  
.. warning:: GeoServer won't connect to XMPP Server until it has been correctly configured and started as explained in the next section :ref:`extensions_wps_remote_install_xmpp`.
