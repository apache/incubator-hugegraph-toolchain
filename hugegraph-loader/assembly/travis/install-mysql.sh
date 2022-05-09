#!/bin/bash

set -ev

TRAVIS_DIR=$(dirname $0)

# Need speed up it
CONF=hugegraph-test/src/main/resources/hugegraph.properties
MYSQL_USERNAME=root

# Set MySQL configurations


# Keep for upgrade in future
docker pull mysql:5.7
docker run -p 3306:3306 --name "$1" -e MYSQL_ROOT_PASSWORD="$2" -d mysql:5.7


# Old Version
#MYSQL_DOWNLOAD_ADDRESS="http://dev.MySQL.com/get/Downloads"
#MYSQL_VERSION="MySQL-5.7"
#MYSQL_PACKAGE="mysql-5.7.11-Linux-glibc2.5-x86_64"
#MYSQL_TAR="${MYSQL_PACKAGE}.tar.gz"

#if [[ -d /var/lib/mysql ]]; then
#    # Reference from https://github.com/mozilla/treeherder/blob/master/bin/travis-setup.sh
#    # Using tmpfs for the MySQL data directory reduces travis test runtime by 6x
#    sudo mkdir /mnt/ramdisk
#    sudo mount -t tmpfs -o size=1024m tmpfs /mnt/ramdisk
#    sudo mv /var/lib/mysql /mnt/ramdisk
#    sudo ln -s /mnt/ramdisk/mysql /var/lib/mysql
#    sudo cp "${TRAVIS_DIR}"/mysql.cnf /etc/mysql/conf.d/mysql.cnf
#    sudo service mysql restart
#else
#    echo "Please install mysql firstly."
#    exit 1
#fi
