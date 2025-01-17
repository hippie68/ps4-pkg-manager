#!/bin/bash

output_dir=swt
eclipse_download_prefix='https://download.eclipse.org/eclipse/downloads/'
eclipse_archive_prefix='https://archive.eclipse.org/eclipse/downloads/'
file_request_prefix='https://www.eclipse.org/downloads/download.php?file='
file_request_suffix='&mirror_id=1' # 1: Canada (global main mirror)
signature_files=(META-INF/ECLIPSE_.RSA META-INF/ECLIPSE_.SF)
temp_filename=swt_download_temp.jar

error() {
    echo "$1" >&2
    exit 1
}

# $1: question string
confirm() {
    local input
    while [[ $input != n && $input != y ]]; do
        echo -n "$1 (y/n) "
        read -rn1 input
        echo
    done
    [[ $input == y ]] && return 0 || return 1
}

# Prints either a specific or the latest Eclipse version directory.
# $1 (optional): Eclipse build name (e.g. "4.30")
get_eclipse_dir() {
    local dir
    if [[ $1 ]]; then
        dir=$(curl -s "$eclipse_archive_prefix" \
            | grep -Po '(?<=a href=")[^"]+(?=">'"$1"'<)') \
            || error "Could not find Eclipse version directory for build name \"$1\"."
        echo "$eclipse_archive_prefix$dir"
    else
        dir=$(curl -s "$eclipse_download_prefix" \
            | grep -Po '(?<=a href=")[^"]+(?=" title="Latest Release")') \
            || error "Could not find Eclipse version directory."
        echo "$eclipse_download_prefix$dir"
    fi
}

# Extracts the build name from an Eclipse version directory.
# $1: the directory.
get_eclipse_version() {
    echo "$1" | grep -Po '(?<=-).+?(?=-)'
}

# Converts and prints all SWT download links from an Eclipse download page.
# $1: an SWT download page.
get_swt_downloads() {
    while read -r line; do
        local file_name=${line#*=}
        local file_dir=${1#*.org}
        echo "${file_request_prefix}${file_dir}${file_name}${file_request_suffix}"
    done < <(curl -s "$1" | grep -Po '(?<=a href=")[^"]+(?=">swt-)')
}

# Prints script usage information.
# $1: output stream number.
print_usage() {
    echo -e "Usage: $(basename "$0") PLATFORM [BUILD_NAME]\n\nPLATFORM: all|windows[-arm]|macos[-arm]|linux[-arm]\nBUILD_NAME: 4.30|4.31|..." >&"$1"
}

################################################################################

# Parse command line arguments.
if [[ $# -lt 1 ]]; then
    echo "Missing argument: PLATFORM" >&2
    print_usage 2
    exit 1
fi
if [[ $* == *-h* || $* == *--help* ]]; then
    print_usage 1
    exit 0
fi
platform=$1
swt_version=$2
case $platform in
    all|windows|windows-arm|linux|linux-arm|macos|macos-arm) ;;
    *) echo "Invalid platform: $platform" >&2; print_usage 2; exit 1
esac

# Check for dependencies.
if hash zip; then
    unsigner=zip
elif hash 7z; then
    unsigner=7z
else
    error "This script requires either 7z or zip."
fi

# Collect all SWT download URLs.
echo "Searching online for available SWT downloads..."
eclipse_dir=$(get_eclipse_dir "$swt_version")
[[ $swt_version == "" ]] && swt_version=$(get_eclipse_version "$eclipse_dir")
readarray -t urls < <(get_swt_downloads "$eclipse_dir")

# Download SWT files for known/supported operating systems.
mkdir -p "$output_dir" && cd "$output_dir" || exit 1
for url in "${urls[@]}"; do
    case $url in
        *win32-x86*) os_dir=windows ;;
        *win32-aarch64*) os_dir=windows-arm ;;
        *linux-x86_64*) os_dir=linux ;;
        *linux-aarch64*) os_dir=linux-arm ;;
        *linux-ppc64le*) continue ;;
        *linux-riscv64*) continue ;;
        *macosx-x86_64*) os_dir=macos ;;
        *macosx-aarch64*) os_dir=macos-arm ;;
        *) echo "Skipping unknown platform: $url"; continue
    esac
    [[ $platform != all && $platform != "$os_dir" ]] && continue;

    output_filename="$output_dir/$swt_version/$os_dir/swt.jar"
    if [[ -f "../$output_filename" ]]; then
        confirm "File \"$output_filename\" already exists. Download anyway?" \
            || continue
    fi

    # Download temporary SWT archive.
    mkdir -p "$swt_version/$os_dir" && cd "$swt_version/$os_dir" || exit 1
    echo "Downloading \"$url\"..."
    if ! curl -L'#' "$url" > "$temp_filename"; then
        echo "Could not download \"$url\"." >&2
        continue
    fi

    # Extract and unsign swt.jar from downloaded archive.
    echo "Extracting to \"$output_filename\"..."
    jar --extract --file="$temp_filename" swt.jar || exit 1
    echo "Unsigning \"$output_filename\"..."
    if [[ $unsigner == 7z ]]; then
        7z d -tzip swt.jar "${signature_files[@]}" > /dev/null || exit 1
    else
        zip -d swt.jar "${signature_files[@]}" > /dev/null || exit 1
    fi
    rm "$temp_filename"

    cd ../.. || exit 1
done
