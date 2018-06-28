#!/bin/bash

function ensure_path_writable() {
    local path=$1
    # Ensure input path exist
    if [ ! -d "${path}" ]; then
        mkdir -p ${path}
    fi
    # Check for write permission
    if [ ! -w "${path}" ]; then
        echo "No write permission on directory ${path}"
        exit 1
    fi
}

function parse_yaml() {
    local file=$1
    local version=$2
    local module=$3

    cat $file | tr -d '\n {}'| awk -F',+|:' '''{
        pre="";
        for(i=1; i<=NF; ) {
            if(match($i, /version/)) {
                pre=$i;
                i+=1
            } else {
                result[pre"-"$i] = $(i+1);
                i+=2
            }
        }
    } END {for(e in result) {print e": "result[e]}}''' \
    | grep "$version-$module" | awk -F':' '{print $2}' | tr -d ' ' && echo
}

#read a property from a .properties file
function read_property(){
    # replace "." to "\."
    property_name=`echo $2 | sed 's/\./\\\./g'`
    # file path
    file_name=$1;
    cat $file_name | sed -n -e "s/^[ ]*//g;/^#/d;s/^$property_name=//p" | tail -1
}

function write_property() {
    local file=$1
    local key=$2
    local value=$3

    local os=`uname`
    case $os in
        # Note: in mac os should use sed -i '' "xxx" to replace string,
        # otherwise prompt 'command c expects \ followed by text'.
        # See http://www.cnblogs.com/greedy-day/p/5952899.html
        Darwin) sed -i '' "s!$key=.*!$key=$value!g" "$file" ;;
        *) sed -i "s!$key=.*!$key=$value!g" "$file" ;;
    esac
}

function get_ip() {
    local os=`uname`
    local loopback="127.0.0.1"
    local ip=""
    case $os in
        Linux) ip=`ifconfig | grep 'inet addr:'| grep -v "$loopback" | cut -d: -f2 | awk '{ print $1}'`;;
        FreeBSD|OpenBSD|Darwin) ip=`ifconfig  | grep -E 'inet.[0-9]' | grep -v "$loopback" | awk '{ print $2}'`;;
        SunOS) ip=`ifconfig -a | grep inet | grep -v "$loopback" | awk '{ print $2} '`;;
        *) ip=$loopback;;
    esac
    echo $ip
}

function download() {
    local path=$1
    local link_url=$2

    if command -v wget >/dev/null 2>&1; then
        wget --help | grep -q '\--show-progress' && progress_opt="-q --show-progress" || progress_opt=""
        wget ${link_url} -P ${path} $progress_opt
    elif command -v curl >/dev/null 2>&1; then
        curl ${link_url} -o ${path}/${link_url}
    else
        echo "Required wget or curl but they are not installed"
    fi
}

function ensure_package_exist() {
    local path=$1
    local dir=$2
    local tar=$3
    local link=$4

    if [ ! -d ${path}/${dir} ]; then
        if [ ! -f ${path}/${tar} ]; then
            echo "Downloading the compressed package '${tar}'"
            download ${path} ${link}
            if [ $? -ne 0 ]; then
                echo "Failed to download, please ensure the network is available and link is valid"
                exit 1
            fi
            echo "[OK] Finished download"
        fi
        echo "Unzip the compressed package '$tar'"
        tar -zxvf ${path}/${tar} -C ${path} >/dev/null 2>&1
        if [ $? -ne 0 ]; then
            echo "Failed to unzip, please check the compressed package"
            exit 1
        fi
        echo "[OK] Finished unzip"
    fi
}

function wait_for_startup() {
    local server_name="$1"
    local server_url="$2"
    local timeout_s="$3"

    local now_s=`date '+%s'`
    local stop_s=$(( $now_s + $timeout_s ))

    local status

    echo -n "Connecting to $server_name ($server_url)"
    while [ $now_s -le $stop_s ]; do
        echo -n .
        status=`curl -o /dev/null -s -w %{http_code} $server_url`
        if [ $status -eq 200 ]; then
            echo "OK"
            return 0
        fi
        sleep 2
        now_s=`date '+%s'`
    done

    echo "The operation timed out when attempting to connect to $server_url" >&2
    return 1
}

function wait_for_shutdown() {
    local p_name="$1"
    local timeout_s="$2"

    local now_s=`date '+%s'`
    local stop_s=$(( $now_s + $timeout_s ))

    while [ $now_s -le $stop_s ]; do
        process_status "$p_name" >/dev/null
        if [ $? -eq 1 ]; then
            # Class not found in the jps output. Assume that it stopped.
            return 0
        fi
        sleep 2
        now_s=`date '+%s'`
    done

    echo "$p_name shutdown timeout(exceeded $timeout_s seconds)" >&2
    return 1
}

function process_status() {
    local p=`ps -ef | grep "$1" | grep -v grep | awk '{print $2}'`
    if [ -n "$p" ]; then
        echo "$1 is running with pid $p"
        return 0
    else
        echo "The process $1 does not exist"
        return 1
    fi
}

function kill_process() {
    local pids=`ps -ef | grep "$1" | grep -v grep | awk '{print $2}' | xargs`

    if [ "$pids" = "" ]; then
        echo "There is no $1 process"
    fi

    for pid in ${pids[@]}
    do
        if [ -z "$pid" ]; then
            echo "The process $1 does not exist"
            return
        fi
        echo "Killing $1 (pid $pid)..." >&2
        case "`uname`" in
            CYGWIN*) taskkill /F /PID "$pid" ;;
            *)       kill "$pid" ;;
        esac
    done
}

function remove_with_prompt() {
    local path=$1
    local tips=""

    if [ -d "$path" ]; then
        tips="Remove directory '$path' and all sub files [y/n]?"
    elif [ -f "$path" ]; then
        tips="Remove file '$path' [y/n]?"
    else
        return 0
    fi

    read -p "$tips " yn
    case $yn in
        [Yy]* ) rm -rf "$path";;
        * ) ;;
    esac
}
