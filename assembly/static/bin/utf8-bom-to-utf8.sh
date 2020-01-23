#!/usr/bin/env bash

if [[ -z "$1" ]]; then
    echo "Usage：$0 [folder | file]"
    echo "Convert UTF-8 encoded files to UTF-8 without BOM format"
    exit 1
fi

path=$1
find ${path} -type f -name "*" -print | xargs -i sed -i '1 s/^\xef\xbb\xbf//' {}
echo "Conversion completed"
