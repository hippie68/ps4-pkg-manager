#!/bin/bash

swt_dir=swt
#swt_version=

# SWT libraries in the form of swt.jar files for different operating systems can
# be extracted from the "SWT Binary and Source" downloads of specific Eclipse
# releases at https://download.eclipse.org/eclipse/downloads/index.html.
# They must be stored in the following subdirectories, below
# "$swt_dir/$swt_version":
#
#   windows/swt.jar
#   windows-arm/swt.jar
#   macos/swt.jar
#   macos-arm/swt.jar
#   linux/swt.jar
#   linux-arm/swt.jar
#
# If you download a more recent SWT version (> 4.30) manually, you must make
# sure to remove Eclipse signature files from the swt.jar file to be able to
# build the project. You can download and unsign swt.jar files automatically
# with the script "download_swt.sh".

################################################################################

error() {
    echo "$1" >&2
    exit 1
}

# Checks the subdirectories of $swt_dir and returns the one that stands for the
# highest SWT version.
get_highest_version_dir() {
    local major=0
    local minor=0
    for dir in "$swt_dir/"*.*; do
        dir=${dir##*/}
        [[ $dir == "*.*" ]] && break
        major_cur=${dir%.*}
        minor_cur=${dir#*.}
        if [[ ($major_cur -gt "$major")
            || ($major_cur -eq "$major" && $minor_cur -gt "$minor") ]]; then
            major=$major_cur
            minor=$minor_cur
        fi
    done
    [[ $major -ne 0 && $minor -ne 0 ]] && echo "$major.$minor"
}

# Builds a .jar file that can be executed with "java -jar FILENAME".
# $1: one of the supported OS identifiers
# E.g. "build linux" builds the Linux version.
build() {
  local swt_file="$swt_dir/$swt_version/$1/swt.jar"
  if [[ ! -f "$swt_file" ]]; then
      echo "Not building for platform \"$1\" (file \"$swt_file\" does not exist)."
      return 1
  fi
  echo "Building for platform \"$1\"..."

  # Clean up and create build directories.
  local build_dir="build/$1"
  rm -r "$build_dir" 2> /dev/null
  mkdir -p "$build_dir" || exit 1
  local class_dir="$build_dir/classes"

  # Compile the project's source code.
  javac -cp "src:$swt_file" src/GUI.java -d "$class_dir" || exit 1

  # Extract required files from the platform's "swt.jar" file.
  local libs
  if [[ $1 == windows* ]]; then libs='*.dll'; fi
  if [[ $1 == macos* ]]; then libs='*.jnilib'; fi
  if [[ $1 == linux* ]]; then libs='*.so'; fi
  unzip "$swt_file" 'org/*' "$libs" -d "$class_dir" || exit 1

  # Create the .jar file.
  cd "$class_dir" || exit 1
  jar -cfe "../pkg_manager_$1.jar" GUI ./*.class $libs org
  cd - > /dev/null || exit 1
}

################################################################################

if [[ $# -lt 1 ]]; then
    echo -e "Usage: $(basename "$0") PLATFORM [SWT_VERSION]\n\nPLATFORM: all|windows|windows-arm|macos|macos-arm|linux|linux-arm" >&2
    exit 1
fi

[[ -d "$swt_dir" ]] || error "Required SWT directory does not exist: \"$swt_dir\"."
[[ -v 2 ]] && swt_version=$2
[[ $swt_version == "" ]] && swt_version=$(get_highest_version_dir)
[[ $swt_version == "" ]] && error "Could not find an SWT version directory in SWT directory \"$swt_dir\"."

case $1 in
  all)
    build windows
    build windows-arm
    build macos
    build macos-arm
    build linux
    build linux-arm
    ;;
  windows|windows-arm|macos|macos-arm|linux|linux-arm)
    build "$1"
    ;;
  *)
    echo "Invalid argument: \"$1\"" >&2
    ;;
esac
