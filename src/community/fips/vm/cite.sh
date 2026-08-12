#!/bin/bash
# Runs the OGC CITE suites against a GeoServer deployed inside the FIPS machine. The unit tests say
# the code works under a FIPS provider; only a deployment says a released GeoServer does.
#
#   ./cite.sh install          installs Tomcat and PostGIS in the machine, once
#   ./cite.sh run wms13        deploys the war and the suite data directory, then runs the suite
#   ./cite.sh stop             stops Tomcat
#   ./cite.sh log [lines]      tails the GeoServer log
#
# GeoServer runs on the machine's own JDK, under the operating system crypto policy. Teamengine runs
# in docker on the host: it is an HTTP client, its own crypto has nothing to do with the measurement.
# Nothing under build/cite is modified. See README.md for the war to build first.
set -euo pipefail

# shellcheck source=common.sh
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

CITE=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../build/cite" && pwd)
WAR=$SRC_DIR/web/app/target/geoserver.war
GS_URL=http://127.0.0.1:$GS_PORT/geoserver
# a static Wicket resource, used to tell that GeoServer answers: see start_tomcat
LOGO_PATH=/web/wicket/resource/org.geoserver.web.GeoServerBasePage/img/logo.png
TOMCAT_VERSION=${TOMCAT_VERSION:-11.0.13}
TEAMENGINE=fips-cite-teamengine
# where the teamengine container reaches GeoServer, see the relay in run_teamengine
GATEWAY=${GATEWAY:-172.17.0.1}
# the CITE composition uses cite/cite. A FIPS PostgreSQL client refuses a password under 14
# characters, so a longer one is used and the suite data directories are rewritten to match.
DB_USER=cite
DB_PASSWORD=${CITE_DB_PASSWORD:-citefipspassword2026}

# Files under security/ bound to the JCEKS keystore the CITE data directories were written with, or
# to a password encrypted with a key inside it. A FIPS JVM has no JCEKS at all, so that keystore
# cannot be opened. Dropped, GeoServer writes a fresh security directory with its own keystore type,
# and the plain text rule files stay, so the suite keeps its layer, service and REST rules.
CRYPTO_FILES='geoserver.jceks masterpw.xml masterpw.digest masterpw.info version.properties config.xml'
CRYPTO_DIRS='masterpw usergroup role auth pwpolicy filter'

# Installs what a deployment needs and create.sh leaves out: Tomcat, because Rocky ships no package
# for it, and PostgreSQL with PostGIS, because three suites need a database and it has to live on the
# FIPS machine like everything else being measured. Safe to run twice.
install_tools() {
    if $SSH 'test -d /opt/tomcat'; then
        echo "Tomcat is already in /opt/tomcat"
    else
        echo "Installing Tomcat $TOMCAT_VERSION"
        # the mirror keeps the current release only, the archive keeps every one of them
        local path=tomcat/tomcat-11/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz
        vm "curl -sfLo /tmp/tomcat.tgz 'https://dlcdn.apache.org/$path' \
            || curl -sfLo /tmp/tomcat.tgz 'https://archive.apache.org/dist/$path'"
        vm "sudo tar xzf /tmp/tomcat.tgz -C /opt \
            && sudo ln -sfn /opt/apache-tomcat-$TOMCAT_VERSION /opt/tomcat \
            && sudo chown -R $USER: /opt/apache-tomcat-$TOMCAT_VERSION"
    fi

    if $SSH 'systemctl is-active --quiet postgresql'; then
        echo "PostgreSQL is already running"
        return
    fi
    echo "Installing PostgreSQL and PostGIS"
    vm 'sudo dnf install -y -q postgresql-server postgis'
    # SCRAM everywhere on TCP, which is what a FIPS system asks for; the local socket keeps peer
    # authentication so the superuser is reachable without a password
    vm 'sudo postgresql-setup --initdb \
        && sudo sed -i "s/^#*password_encryption.*/password_encryption = scram-sha-256/" \
             /var/lib/pgsql/data/postgresql.conf \
        && sudo sed -i "s|^host\(.*\)all\(.*\)127.0.0.1/32.*|host\1all\2127.0.0.1/32 scram-sha-256|" \
             /var/lib/pgsql/data/pg_hba.conf \
        && sudo systemctl enable --now postgresql'
}

# Deploys the suite and runs it. Everything a suite needs is done here, so one command per suite is
# the whole story: the database when it needs one, the war, its data directory, then teamengine.
run_suite() {
    local suite=$1
    case " wfs10 wfs11 wfs20 " in
    *" $suite "*) load_dataset "$suite" ;;
    esac
    deploy "$suite"
    run_teamengine "$suite"
}

# Creates the database the suite needs and loads its dataset. The composition runs PostgreSQL as a
# container on the host; here it is in the machine, so the connection stays on the FIPS system.
load_dataset() {
    local suite=$1 name=cite_$1 sql
    sql=$(echo "$CITE/$suite"/*/*postgis*.sql)
    [ -f "$sql" ] || { echo "No PostGIS dataset under $CITE/$suite" >&2; exit 1; }

    vm "sudo -u postgres psql -tAc \"select 1 from pg_roles where rolname='$DB_USER'\"" | grep -q 1 ||
        vm "sudo -u postgres psql -q -c 'CREATE ROLE $DB_USER LOGIN'"
    vm "sudo -u postgres psql -q -v ON_ERROR_STOP=1 \
        -c \"ALTER ROLE $DB_USER PASSWORD '$DB_PASSWORD'\" \
        -c 'DROP DATABASE IF EXISTS $name' \
        -c 'CREATE DATABASE $name OWNER $DB_USER'"
    vm "sudo -u postgres psql -q -v ON_ERROR_STOP=1 -d $name -c 'CREATE EXTENSION postgis' \
        -c 'GRANT ALL PRIVILEGES ON SCHEMA public TO $DB_USER'"

    # the dataset script has its own "\connect <db> cite" line, so it has to run as cite over
    # TCP: the local socket is peer authenticated and the machine has no cite account
    copy_in "$sql" /tmp/cite-dataset.sql
    vm "PGPASSWORD='$DB_PASSWORD' psql -q -h 127.0.0.1 -U $DB_USER -d $name \
        -f /tmp/cite-dataset.sql > /dev/null"
    vm "sudo -u postgres psql -q -d $name -c \
        'GRANT ALL ON ALL TABLES IN SCHEMA public TO $DB_USER'"
    echo "Database $name is ready"
}

# Copies the war and the suite data directory into the machine and starts Tomcat on them. There is
# one Tomcat, so each suite replaces the one before it.
deploy() {
    local suite=$1 src
    [ -f "$WAR" ] || { echo "No war at $WAR, see README.md" >&2; exit 1; }
    # four suites have no data directory and borrow another suite's, which the shipped composition
    # states as GEOSERVER_DATA_DIR_SRC; read it there rather than repeating the mapping
    src=$(awk -F'"' '/GEOSERVER_DATA_DIR_SRC:/{print $2}' "$CITE/$suite/compose.override.yml")
    src=${src:+$CITE/${src#./}/}
    src=${src:-$(echo "$CITE/$suite"/*/)}
    [ -d "$src" ] || { echo "No data directory under $CITE/$suite" >&2; exit 1; }

    stop_tomcat
    echo "Copying the war and the $suite data directory"
    vm 'rm -rf /opt/tomcat/webapps/geoserver /opt/tomcat/webapps/geoserver.war /opt/tomcat/work/*'
    copy_in "$WAR" /opt/tomcat/webapps/geoserver.war
    vm "rm -rf ~/cite-data/$suite; mkdir -p ~/cite-data"
    copy_in "$src" "cite-data/$suite/"
    vm "cd ~/cite-data/$suite/security 2>/dev/null && rm -rf $CRYPTO_FILES $CRYPTO_DIRS || true"

    # the data directories point at the "postgres" container of the composition, and use the short
    # password it uses; GeoServer rewrites the password as crypt3: the first time it reads the store
    vm "find ~/cite-data/$suite/workspaces -name datastore.xml -exec sed -i \
        -e 's|<entry key=\"host\">postgres</entry>|<entry key=\"host\">127.0.0.1</entry>|' \
        -e 's|<entry key=\"passwd\">cite</entry>|<entry key=\"passwd\">$DB_PASSWORD</entry>|' {} +"

    write_setenv "\$HOME/cite-data/$suite"
    start_tomcat "$suite"
}

# Writes the Tomcat environment; $1 is the data directory as a path the machine's shell expands.
# Written on the host and copied: the ssh in $SSH passes -n, which would eat a heredoc.
write_setenv() {
    local setenv
    setenv=$(mktemp)
    cat > "$setenv" <<EOF
export GEOSERVER_DATA_DIR=$1
export GEOWEBCACHE_CACHE_DIR=$1/gwc
export CATALINA_OPTS="-Xms1g -Xmx2g -Djava.awt.headless=true -server \
 -Dfile.encoding=UTF-8 \
 --add-exports=java.desktop/sun.awt.image=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
 --add-opens=java.base/java.text=ALL-UNNAMED \
 --add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
 --add-opens=java.desktop/sun.java2d.pipe=ALL-UNNAMED \
 --add-opens=java.naming/com.sun.jndi.ldap=ALL-UNNAMED \
 -Dorg.geotools.coverage.jaiext.enabled=true"
EOF
    copy_in "$setenv" /opt/tomcat/bin/setenv.sh F755
    rm -f "$setenv"
}

# Waits on a static Wicket resource rather than on the home page: the gpkg12 catalog holds a layer
# with no resource behind it, and the page that lists layers answers 500 while every service works.
start_tomcat() {
    echo "Starting Tomcat"
    vm 'CATALINA_PID=/tmp/tomcat.pid /opt/tomcat/bin/startup.sh'
    local code logo=$GS_URL$LOGO_PATH
    for _ in $(seq 1 60); do
        code=$(curl -s -o /dev/null -w '%{http_code}' "$logo" || true)
        case "$code" in 200 | 302) echo "GeoServer is up on $1"; return 0 ;; esac
        sleep 5
    done
    echo "GeoServer did not come up, see: ./cite.sh log" >&2
    exit 1
}

stop_tomcat() {
    $SSH 'test -f /tmp/tomcat.pid' || return 0
    vm 'CATALINA_PID=/tmp/tomcat.pid /opt/tomcat/bin/shutdown.sh 30 -force' || true
}

# Runs the suite in the teamengine container and prints its report. Only teamengine is started: the
# composition would build its own GeoServer container from a stock Ubuntu image with no FIPS policy,
# which is exactly what this machine exists to avoid.
run_teamengine() {
    local suite=$1 image forms=() code
    # $1 has to be image:, or a commented out line wins, as ogcapi-features10 has two of them
    image=${IMAGE:-$(awk '/^  teamengine:/{f=1;next} /^  [a-z]/{f=0} f&&$1=="image:"{print $2;exit}' \
        "$CITE/$suite/compose.override.yml")}
    image=${image:-geoserver-docker.osgeo.org/geoserver-cite:teamengine_latest}
    # the suites that cannot be driven over the REST API need their form file in te_base/forms
    for form in "$CITE/$suite"/*.xml; do
        case "$form" in *compose* | *'*'*) continue ;; esac
        forms+=(-v "$form:/home/teamengine/te_base/forms/$(basename "$form"):ro")
    done

    # a connection from a container to a qemu user mode forward is accepted and then reset, though
    # the same forward answers the host, so the loopback port is relayed onto the bridge address
    if ! curl -sf -o /dev/null --max-time 5 "http://$GATEWAY:8080/geoserver$LOGO_PATH"; then
        echo "Starting the relay on $GATEWAY:8080"
        setsid socat "TCP-LISTEN:8080,bind=$GATEWAY,fork,reuseaddr" "TCP:127.0.0.1:$GS_PORT" \
            > /dev/null 2>&1 < /dev/null &
        sleep 2
        # a relay left behind by an earlier machine holds the port and answers nothing: socat then
        # fails to bind without a word, and every suite talks to whatever the old one points at
        curl -sf -o /dev/null --max-time 5 "http://$GATEWAY:8080/geoserver$LOGO_PATH" || {
            echo "The relay on $GATEWAY:8080 does not reach GeoServer. Another socat may hold" >&2
            echo "the port: pkill -f 'socat TCP-LISTEN:8080' and run this again." >&2
            exit 1
        }
    fi

    mkdir -p "$CITE/logs"
    rm -rf "$CITE"/logs/*
    docker rm -f "$TEAMENGINE" > /dev/null 2>&1 || true

    # run-test.sh asks for the host name geoserver on port 8080, so the name is mapped to the bridge
    # rather than anything under build/cite being edited
    local args=(--name "$TEAMENGINE" --hostname teamengine.local
        --add-host "geoserver:$GATEWAY" -p 18080:8080
        -v "$CITE/logs:/logs:rw"
        -v "$CITE/logs:/home/teamengine/te_base/users/teamengine:rw"
        -v "$CITE/logs:/usr/local/tomcat/te_base/users/ogctest/rest:rw"
        -v "$CITE/run-test.sh:/run-test.sh:ro"
        -v "$CITE/forms/yes.xml:/home/teamengine/te_base/forms/yes.xml:ro"
        "${forms[@]}")

    echo "Running the $suite suite, this takes a while"
    case " wfs11 wcs10 " in
    *" $suite "*)
        # these two have no REST API support, so teamengine runs from the command line and exits.
        # It writes no results file either, it reports through its exit code and its own log.
        docker run --rm "${args[@]}" "$image" /bin/bash -c "/run-test.sh $suite"
        echo "$suite passed"
        return 0
        ;;
    esac

    docker run -d "${args[@]}" "$image" > /dev/null
    echo "Waiting for teamengine"
    for _ in $(seq 1 60); do
        code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:18080/teamengine/site/logo.png || true)
        [ "$code" = 200 ] && break
        sleep 5
    done
    [ "$code" = 200 ] || { echo "teamengine did not come up" >&2; docker logs --tail 40 "$TEAMENGINE"; exit 1; }
    docker exec "$TEAMENGINE" /bin/bash -c "/run-test.sh $suite" || true

    cd "$CITE"
    for f in logs/*-results.xml; do
        [ -f "$f" ] || { echo "No result file produced" >&2; exit 1; }
        case "$f" in
        *testng-results.xml) ./testng-results-report.sh "$f"; ./testng-results-validate.sh "$f" ;;
        *) ./xml-results-report.sh "$f"; ./xml-results-validate.sh "$f" ;;
        esac
    done
}

# $1 is a host path, $2 a path in the machine, $3 an optional rsync --chmod value.
copy_in() {
    rsync -a ${3:+--chmod="$3"} -e "ssh $SSH_OPTS" "$1" "$USER@localhost:$2"
}

case "${1:-}" in
install) install_tools ;;
run) run_suite "${2:?suite name, for example wms13}" ;;
stop) stop_tomcat ;;
log) vm "tail -n ${2:-200} /opt/tomcat/logs/catalina.out" ;;
*)
    sed -n '2,9p' "${BASH_SOURCE[0]}"
    exit 1
    ;;
esac
