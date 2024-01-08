# PS4 PKG Manager

This is an application, primarily made for Linux and MacOS, to manage PS4 PKG files by storing their metadata in a database. This means that, once scanned, the PKG collection can be viewed without requiring physical access to the PKG files. The user can select list items and operate on them via the context menu. The context menu can be extended with custom commands ("Custom Actions"), to integrate 3rd party tools that accept command line arguments.

The application is not finished yet, but it is already in a usable state. Don't let the unfinished state discourage you from trying it out: there is no functionality included to move, change, or delete PKG files. I'm putting it online now because as the project becomes more complex I believe it could greatly profit from bug reports, different opinions/ideas, and from code contributions. Ideally, it becomes a community project with multiple collaborators.

## Some functions in more detail

### PKG Properties

The properties window contains several tabs that display more detailed information about the PKG. It can be opened from the context menu or by double-clicking a list item.

### Custom Actions

The PS4 scene has created quite a few tools that operate on PKG files, so that's where Custom Actions come in. They extend the context menu so that 3rd party tools can be run with a single click, using the current PKG selection.

### Synchronized Directories

Found by pressing the "Table Settings" button, this feature monitors the listed directories for PKG files and will add them to the table as soon as they're created, modified, or deleted. If a directory's checkbox is selected, the directory will have all its subdirectories monitored as well.

# Contributors wanted!

I would like to make this project a community project where everyone can participate in. You want to write a useful Java class? Let me know and I will add you to the list of collaborators. To compile the project, have a look at the file "build.sh". If it is necessary, we could try to change it from Bash to sh. You should be able to set up the project in your favorite Java IDE. The only dependency is SWT: https://www.eclipse.org/swt/.

The project is in dire need of a maintainer for macOS. I don't have a Mac, so I don't know if the program in its current state works well and looks good on macOS. If you want to help or to become a maintainer, that would be much appreciated!  
On Windows, the program is currently broken. At least in my Windows VM it does not look as good as native widgets should, and some functionality like adding directories does not work. I don't have a real Windows PC, so if anyone is interested in providing fixes, that'd be awesome.  

Regarding the current code: Basically I am using this project to learn Java. It is my first Java program (an upgrade so to speak from https://github.com/hippie68/ps4-pkg-compatibility-checker), so bear with me if, to put it mildly, large parts of the code are not idiomatic yet.
