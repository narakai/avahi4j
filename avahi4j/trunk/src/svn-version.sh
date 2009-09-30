#!/bin/bash

branch="$(svn info | grep URL | awk -F/ '{print $6}')"
branch=${branch##*/}
revision="$(svn status -v | awk '$1 ~ /[0-9]/ {print $1}' | sort | uniq | tail -1)"

version="${branch}_r${revision}"


if [ "x${version}" != "x_r" ]; then
        echo "#define VER_REV \"${version}\"" > version.h
else
        echo "#define VER_REV \"UNKNOWN\"" > version.h
fi

