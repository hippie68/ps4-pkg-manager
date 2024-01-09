import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Display;

record SynchronizedDirectory(String dir, boolean recursion) {
}

public class WatcherThread extends Thread {
    private TabContent tabContent;
    private ArrayList<WatchKey> watchKeys = new ArrayList<>();
    private ArrayList<SynchronizedDirectory> synchronizedDirectories = new ArrayList<>(); // User-provided list of
                                                                                          // directories.

    // Files matching these extensions are watched.
    public static Set<String> fileNameExtensions = new HashSet<String>(
        List.of(".pkg", ".part", ".crdownload", ".download", ".opdownload", ".!ut", ".!bt", ".!qB")); // TODO: make
                                                                                                      // the list
                                                                                                      // customizable.
    WatchService watchService;

    public WatcherThread(TabContent tabContent) {
        this.tabContent = tabContent;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean hasValidFileNameExtension(String path) {
        String lowerCasePath = path.toLowerCase();
        for (String ext : fileNameExtensions)
            if (lowerCasePath.endsWith(ext))
                return true;
        return false;
    }

    @Override
    public void run() {
        WatchKey watchKey;
        while (true) {
            try {
                watchKey = watchService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                // Only check for the events ENTRY_DELETE, ENTRY_CREATE, and ENTRY_MODIFY...
                if (!(event.context() instanceof Path)) // ...one of which the event is, if it is a Path.
                    continue;

                Path path = ((Path) watchKey.watchable()).resolve((Path) event.context());
                String name = ((Path) event.context()).toString();
                System.out.printf("Event: %s, Path: %s\n", event.kind(), path.toString()); // DEBUG

                // If something was deleted...
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    if (hasValidFileNameExtension(name)) { // ...it could have been a PKG file from the tab's table...
                        Display.getDefault().asyncExec(() -> tabContent.removeFile(path.toString()));
                    } else if (removeWatchKey(path.toString()) == true) { // ...or a watched directory...
                        // ...in which case remove all PKG files from the table that have that directory as a parent.
                        Display.getDefault().asyncExec(() -> tabContent.removeFiles(path.toString()));
                    }
                    continue; // ...or something else that doesn't matter.
                    // TODO: corner case where the user has made one of his directories end with a file extension.
                }

                // Add newly created or modified PKG files to the tab's table.
                if (hasValidFileNameExtension(name) && Files.isRegularFile(path)) {
                    // Get file size.
                    long fileSize;
                    try {
                        fileSize = Files.size(path);

                        // Make sure at least the PKG header is readable.
                        if (fileSize < 0x1000)
                            continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }

                    tabContent.addFile(path.toString());
                    // TODO: adding the file each time it is modified is kind of inefficient.
                    continue;
                }

                // If a directory was created and one of its parent directories is being recursively watched, watch
                // it.
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                    && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    System.out.println("Adding new directory (found in a recursive dir)");
                    for (SynchronizedDirectory syncedDir : synchronizedDirectories) {
                        if (path.startsWith(syncedDir.dir())) {
                            addWatchKey(path.toString());
                            continue;
                        }
                    }

                    continue; // Don't watch it.
                }
            }

            // TODO: overflow handling?

            // printWatchedDirectories(); // DEBUG

            watchKey.reset();

//            try {
//                sleep(2000); // TODO: let user adjust this value?
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    // TODO
    private Set<String> getPkgFiles(String directory) {
        try (Stream<Path> stream = Files.list(Paths.get(directory))) {
            return stream.filter(file -> Files.isRegularFile(file) && hasValidFileNameExtension(file.toString()))
                .map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Set<String> getDirectoriesRecursively(String directory) {
        try (Stream<Path> stream = Files.walk(Paths.get(directory))) {
            return stream.filter(file -> Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
                .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Adds a user-provided directory.
    public synchronized void addDirectory(String directory, boolean recursion) {
        // Don't add the same directory twice...
        for (int i = 0; i < synchronizedDirectories.size(); i++) {
            SynchronizedDirectory syncedDir = synchronizedDirectories.get(i);
            if (syncedDir.dir().equals(directory)) {
                // ...but update the recursion, if necessary.
                if (syncedDir.recursion() != recursion)
                    synchronizedDirectories.set(i, new SynchronizedDirectory(directory, recursion));
                return;
            }
        }

        synchronizedDirectories.add(new SynchronizedDirectory(directory, recursion));

        // printSynchronizedDirectories(); // DEBUG
    }

    // Internal use only.
    private synchronized void addWatchKey(String directory) {
        // Don't watch the same directory twice.
        boolean osIsWindows = System.getProperty("os.name").startsWith("Windows");
        for (WatchKey watchKey : watchKeys) {
            if (osIsWindows) {
                if (watchKey.watchable().toString().equalsIgnoreCase(directory))
                    return;
            } else {
                if (watchKey.watchable().toString().equals(directory))
                    return;
            }
        }

        WatchKey watchKey;
        try {
            watchKey = Paths.get(directory).register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        watchKeys.add(watchKey);

        // printWatchedDirectories(); // DEBUG
    }

    // Deletes all current WatchKeys and creates them from scratch, using the user-provided directories.
    public synchronized void generateWatchKeys() {
        for (WatchKey watchKey : watchKeys)
            watchKey.cancel();
        watchKeys.clear();

        for (SynchronizedDirectory syncedDir : synchronizedDirectories) {
            if (syncedDir.recursion() == true) {
                Set<String> directories = getDirectoriesRecursively(syncedDir.dir());
                if (directories == null)
                    addWatchKey(syncedDir.dir());
                else
                    for (String dir : directories)
                        addWatchKey(dir);
            } else {
                addWatchKey(syncedDir.dir());
            }
        }
    }

    // Scans all watched directories for PKG files and adds them to the table.
    public synchronized void addPkgFilesFromWatchedDirectories() {
        Set<String> pkgFiles = new HashSet<>();
        for (WatchKey watchKey : watchKeys) {
            String dir = watchKey.watchable().toString();
            Set<String> pkgs = getPkgFiles(dir);
            if (pkgs != null)
                pkgFiles.addAll(pkgs);
        }
        for (String pkgFile : pkgFiles)
            tabContent.addFile(pkgFile);
    }

    // Removes a user-provided directory.
    public synchronized void removeDirectory(String directory) {
        synchronizedDirectories.removeIf(synchedDir -> synchedDir.dir().equals(directory));
        removeWatchKey(directory);

        // printSynchronizedDirectories(); // DEBUG
    }

    // If the specified directory is found in the known WatchKeys, it is removed and true is returned (otherwise false).
    private synchronized boolean removeWatchKey(String directory) {
        for (WatchKey watchKey : watchKeys) {
            String watchDirectory = watchKey.watchable().toString();
            if (watchDirectory.equals(directory)) {
                watchKey.cancel();
                watchKeys.remove(watchKey);
                return true;
            }
        }

        return false;
    }

    public synchronized String[] getSynchedDirs() {
        String[] dirs = new String[synchronizedDirectories.size()];
        for (int i = 0; i < synchronizedDirectories.size(); i++)
            dirs[i] = synchronizedDirectories.get(i).dir();
        return dirs;
    }

    public synchronized int[] getSynchedDirsRecursionState() {
        int[] recursions = new int[synchronizedDirectories.size()];
        for (int i = 0; i < synchronizedDirectories.size(); i++)
            recursions[i] = synchronizedDirectories.get(i).recursion() ? 1 : 0;
        return recursions;
    }

    private String[] getWatchedDirectories() {
        String[] watchedDirectories = new String[watchKeys.size()];
        for (int i = 0; i < watchKeys.size(); i++)
            watchedDirectories[i] = watchKeys.get(i).watchable().toString();
        return watchedDirectories;
    }

    // Debug function.
    public void printSynchronizedDirectories() {
        System.out.println("Synchronized (user-provided) directories for tab " + tabContent.getName());
        for (SynchronizedDirectory syncedDir : synchronizedDirectories)
            System.out.println(syncedDir.dir() + ", " + syncedDir.recursion());
    }

    // Debug function.
    public void printWatchedDirectories() {
        System.out.println("Watched directories:");
        for (WatchKey watchKey : watchKeys)
            System.out.println(watchKey.watchable().toString());
    }
}
