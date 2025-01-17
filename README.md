# PS4 PKG Manager

![pkg_manager](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/6c9eca5b-4df9-4286-bfb8-3ee3d3c19c69)

This is a desktop application, primarily made for Linux and macOS, to manage PS4 PKG files by storing their metadata in a database. This means that, once scanned, the PKG collection can be viewed without requiring physical access to the PKG files. The user can select list items and operate on them via a context menu. The context menu can be extended with custom commands ("Custom Actions"), to integrate 3rd party tools that accept command line arguments.

Please report any bugs by creating an issue at https://github.com/hippie68/ps4-pkg-manager/issues.

## Some functions in more detail

### PKG Properties
![properties](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/0869344b-c428-469e-8e8c-caf6c7ca17a9)

The properties window contains several tabs that display more detailed information about the PKG. It can be opened from the context menu or by double-clicking a list item.  
In the "PKG Files" tab, files can be extracted by opening a context menu and selecting "Extract File...".

### Custom Actions

![custom_actions](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/38243484-96a1-4599-9f26-acab5af95936)

The PS4 scene has created quite a few tools that operate on PKG files, so that's where Custom Actions come in. They extend the context menu so that external tools can be run on the current PKG selection.  
Custom Actions can be modified without having to run PKG Manager. They are stored in the text file "actions.txt", whose exact location you can find by opening the application menu's "About" dialog. The first '|' character delimits name and command.  
For a list of command suggestions, look here: ![Custom Actions](https://github.com/hippie68/ps4-pkg-manager/discussions/2).

### Tabs

![tabs](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/c0699ced-20f0-4fde-9789-d9f9a3fe845e)

Manage different PKG lists for different purposes: File - New Tab.  
Tabs can be renamed, moved, or closed via a context menu. Each tab can have its own column layout: columns can be moved via drag and drop and each column's visibility can be toogled. Tabs can be used as simple lists and/or to monitor individual directories (see below).

### Synchronized Directories

![synchronized_directories](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/9cf092a8-6526-4f86-8e7f-d77b92267e86)

Found by pressing the "Table Settings" button, this feature monitors the listed directories for PKG files and will add them to the table as soon as they're created, modified, or deleted. If a directory's checkbox is selected, the directory will have all its subdirectories monitored as well.  
External drives that are monitored are automatically detected and scanned for changes as soon as they are mounted.

### FTP Files

![ftp_files](https://github.com/hippie68/ps4-pkg-manager/assets/65259318/e75c3409-e1a6-4c4b-b17b-d3fee139a928)

You can add PKG files that are installed on a PS4: File - Add FTP Files...  
This requires a bug-fixed PS4 FTP server. If you use an old FTP server that is buggy (still found in some homebrew applications or old hosts), the connection speed will most likely drop to zero quickly. The following servers are supported:

- https://github.com/hippie68/ps4-ftp
- GoldHEN 2.3 and GoldHEN 2.4 BETA

After the files are added, the PKGs can be downloaded via the !["Open Directory"](https://github.com/hippie68/ps4-pkg-manager/discussions/2) Custom Action.  
If you ever need to change the files' URLs, you can do so via the context menu.

# How to run

This is a Java application (a .jar file) that requires a Java Runtime Environment (JRE), at least version 17 LTS. You do NOT need a Java Development Kit (JDK). Linux distributions should offer a JRE in the form of "openjdk" JRE packages. If you are using a different operating system and don't already have a JRE installed, I recommend the following one: https://adoptium.net/temurin/releases/?os=any&arch=x64&package=jre.

If you want to be able to start the .jar file with a mouse click, make sure to download the installer (not the .zip or .tar.gz file) and have "Associate .jar" selected during the installation process.

# Contributors wanted!

Feature requests and bug reports: https://github.com/hippie68/ps4-pkg-manager/issues

I would like to make this project a community project where everyone can participate in. You want to write a useful Java class? Let me know and I will add you to the list of collaborators. You should be able to set up the project in your favorite Java IDE by adding the only dependency SWT (https://www.eclipse.org/swt/) as an external library. To make this easier, I have created the script `download_swt.sh` which automatically downloads, extracts, and unsigns swt.jar files for all supported platforms.  
To compile the project into a .jar file, have a look at the file `build.sh`. If it is necessary, we could try to change it from Bash to sh. 

The project is in dire need of supporters that use macOS. I don't have a Mac, so I don't know if the program in its current state works well and looks good on macOS. If you want to provide feedback or to become a maintainer, that would be much appreciated!

Regarding the current code: Basically I am using this project to learn Java. It is my first Java program (an upgrade so to speak from https://github.com/hippie68/ps4-pkg-compatibility-checker), so bear with me if, to put it mildly, large parts of the code are not idiomatic yet.
