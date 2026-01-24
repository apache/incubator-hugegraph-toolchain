#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to You under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
set -ev

TRAVIS_DIR=$(dirname $0)

# Parameters validation
if [ $# -lt 2 ]; then
    echo "Usage: $0 <database_name> <root_password>"
    exit 1
fi

DB_NAME="$1"
DB_PASS="$2"
MYSQL_USERNAME=root

echo "Attempting to start MySQL service with Docker..."

# Check if Docker is available
if command -v docker &> /dev/null; then
    echo "Docker found, using Docker container for MySQL..."
    
    # Pull and run MySQL container
    docker pull mysql:5.7 || {
        echo "Failed to pull MySQL Docker image, will try native MySQL"
    } && {
        docker run -p 3306:3306 --name "${DB_NAME}" -e MYSQL_ROOT_PASSWORD="${DB_PASS}" -d mysql:5.7 || {
            echo "Failed to start MySQL Docker container"
            exit 1
        }
        
        echo "Waiting for MySQL to be ready..."
        sleep 15
        
        # Verify MySQL is accessible
        until mysql -h 127.0.0.1 -u "${MYSQL_USERNAME}" -p"${DB_PASS}" -e "SELECT 1" > /dev/null 2>&1; do
            echo "Waiting for MySQL connection..."
            sleep 2
        done
        
        echo "MySQL is ready"
        exit 0
    }
fi

echo "Docker not available, checking for native MySQL installation..."

# Fallback to native MySQL if Docker fails
if command -v mysqld &> /dev/null; then
    echo "Native MySQL found, starting service..."
    sudo service mysql start || {
        echo "Failed to start native MySQL service"
        exit 1
    }
    
    sleep 5
    
    # Set root password and create database
    mysql -u ${MYSQL_USERNAME} -e "ALTER USER '${MYSQL_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASS}';" || true
    mysql -u ${MYSQL_USERNAME} -p"${DB_PASS}" -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME};" || {
        echo "Failed to create database"
        exit 1
    }
    
    echo "MySQL native setup completed"
else
    echo "Neither Docker nor native MySQL found"
    exit 1
fi

