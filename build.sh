#!/bin/bash

swt_dir=~/SWT/4.30
# The SWT .jar files for the different operating systems can be downloaded
# from https://download.eclipse.org/eclipse/downloads/index.html.
# They must be stored in the following subdirectories, below $swt_dir:
#
#   windows/swt.jar
#   macos/swt.jar
#   macos-arm/swt.jar
#   linux/swt.jar
#   linux-arm/swt.jar

# Builds a .jar file that can be executed with "java -jar FILENAME".
# The parameter is one of the subdirectories (see above).
# E.g. "build linux" builds the Linux version.
build() {
  # Clean up and create build directories.
  local build_dir="build/$1"
  rm -r "$build_dir" 2> /dev/null
  mkdir -p "$build_dir" || exit 1
  local class_dir="$build_dir/classes"
  local swt_file="$swt_dir/$1/swt.jar"

  # Compile the project's source code.
  javac -cp "src:$swt_file" src/GUI.java -d "$class_dir" || exit 1

  # Extract required files from the platform's "swt.jar" file.
  local libs
  if [[ $1 == windows ]]; then libs=*.dll; fi
  if [[ $1 == macos* ]]; then libs=*.jnilib; fi
  if [[ $1 == linux* ]]; then libs=*.so; fi
  unzip "$swt_file" 'org/*' "$libs" -d "$class_dir" || exit 1

  # Create the .jar file.
  cd "$class_dir" || exit 1
  jar -cfe "../pkg_manager_$1.jar" GUI *.class $libs org
  cd - > /dev/null
}

case $1 in
  all)
    build windows
    build macos
    build macos-arm
    build linux
    build linux-arm
    ;;
  windows|macos|macos-arm|linux|linux-arm)
    build "$1"
    ;;
  *)
    echo "Invalid argument: \"$1\"" >&2
    ;;
esac
