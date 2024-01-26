import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

class SynchronizedDirectory {
	String dir;
	boolean isRecursive;
	boolean isKnownToExist;

	SynchronizedDirectory(String dir, boolean isRecursive) {
		this.dir = dir;
		this.isRecursive = isRecursive;
	}
}

public class WatcherThread extends Thread {
	// Wait up to 30 seconds for (possibly) mounted HDDs to spin up.
	private static final int ATTEMPT_COUNT = 30;
	private static final int ATTEMPT_DELAY = 1000;

	private static final int DIRCHECKER_POLLING_INTERVAL = 3000;

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
		// Periodically check synchronized directories for existence to update WatchKeys, if necessary.
		Thread dirChecker = new Thread(() -> {
			while (true) {
				for (SynchronizedDirectory syncedDir : synchronizedDirectories) {
					boolean dirExists = Files.exists(Paths.get(syncedDir.dir));
					if (dirExists) {
						if (!syncedDir.isKnownToExist) {
							synchronizePkgFiles(syncedDir);
							addWatchKey(syncedDir.dir);
							syncedDir.isKnownToExist = true;
						}
					} else if (syncedDir.isKnownToExist) {
						removeWatchKey(syncedDir.dir);
						syncedDir.isKnownToExist = false;
					}
				}

				try {
					Thread.sleep(DIRCHECKER_POLLING_INTERVAL);
				} catch (InterruptedException e) {
					return;
				}
			}
		});
		dirChecker.setDaemon(true);
		dirChecker.start();

		WatchKey watchKey;
		while (true) {
			try {
				watchKey = watchService.take();
			} catch (InterruptedException e) {
				dirChecker.interrupt();
				return;
			}
			for (WatchEvent<?> event : watchKey.pollEvents()) {
				// Only check for the events ENTRY_DELETE, ENTRY_CREATE, and ENTRY_MODIFY...
				if (!(event.context() instanceof Path)) // ...one of which the event is, if it is a Path.
					continue;

				Path path = ((Path) watchKey.watchable()).resolve((Path) event.context());
				String name = ((Path) event.context()).toString();
				// System.out.printf("Event: %s, Path: %s\n", event.kind(), path.toString()); // DEBUG

				// If something was deleted...
				if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
					if (hasValidFileNameExtension(name))
						Display.getDefault().asyncExec(() -> {
							if (!tabContent.isDisposed())
								tabContent.removeFile(path.toString());
						});
					else if (removeWatchKey(path.toString()) == true)
						// ...in which case remove all PKG files from the table that have that directory as a parent.
						Display.getDefault().asyncExec(() -> {
							if (!tabContent.isDisposed())
								tabContent.removeFiles(path.toString());
						});
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

					if (!tabContent.isDisposed())
						tabContent.addFile(path.toString());
					// TODO: adding the file each time it is modified is kind of inefficient.
					continue;
				}

				// If a directory was created and one of its parent directories is being recursively watched, watch it.
				if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
					&& Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
					for (SynchronizedDirectory syncedDir : synchronizedDirectories)
						if (path.startsWith(syncedDir.dir)) {
							addWatchKey(path.toString());
							continue;
						}

					continue; // Don't watch it.
				}
			}
			// printWatchedDirectories(); // DEBUG

			watchKey.reset();
		}
	}

	/** Returns the paths of all PKG files found in the provided directory. */
	public static Set<String> getPkgFiles(String directory) {
		try (Stream<Path> stream = Files.list(Paths.get(directory))) {
			return stream.filter(file -> Files.isRegularFile(file) && hasValidFileNameExtension(file.toString()))
				.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toSet());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Recursively gathers the specified directory's subdirectories, ignoring directories that can't be accessed. */
	public static Set<String> getSubdirectories(String dir) {
		Set<String> dirs = new HashSet<>();

		try {
			Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (Files.isDirectory(file))
						dirs.add(file.toAbsolutePath().toString());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
					if (e instanceof AccessDeniedException)
						return FileVisitResult.CONTINUE;
					return super.visitFileFailed(file, e);
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dirs;
	}

	/** Adds a user-provided directory to the list of known synchronized directories. */
	public synchronized void addDirectory(String directory, boolean recursion) {
		// Don't add the same directory twice...
		for (int i = 0; i < synchronizedDirectories.size(); i++) {
			SynchronizedDirectory syncedDir = synchronizedDirectories.get(i);
			if (syncedDir.dir.equals(directory)) {
				// ...but update the recursion, if necessary.
				if (syncedDir.isRecursive != recursion)
					synchronizedDirectories.set(i, new SynchronizedDirectory(directory, recursion));
				return;
			}
		}

		synchronizedDirectories.add(new SynchronizedDirectory(directory, recursion));

		// printSynchronizedDirectories(); // DEBUG
	}

	/** Makes a directory being watched by the WatchService and adds it to the WatchKey list. */
	private synchronized void addWatchKey(String directory) {
		// Don't watch the same directory twice.
		for (WatchKey watchKey : watchKeys)
			if (Platform.isWindows) {
				if (watchKey.watchable().toString().equalsIgnoreCase(directory))
					return;
			} else if (watchKey.watchable().toString().equals(directory))
				return;

		WatchKey watchKey;
		try {
			watchKey = Paths.get(directory).register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (NoSuchFileException e) { // Happens when the directory is not found.
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		watchKeys.add(watchKey);

		// printWatchedDirectories(); // DEBUG
	}

	/**
	 * Deletes all current WatchKeys and creates them from scratch, using the user-provided synchronized directories.
	 */
	public synchronized void generateWatchKeys() {
		for (WatchKey watchKey : watchKeys)
			watchKey.cancel();
		watchKeys.clear();

		for (SynchronizedDirectory syncedDir : synchronizedDirectories) {
			addWatchKey(syncedDir.dir);
			if (syncedDir.isRecursive == true) {
				Set<String> directories = getSubdirectories(syncedDir.dir);
				for (String dir : directories)
					addWatchKey(dir);
			}
		}
	}

	/**
	 * Synchronizes the table's list items with the PKG files found (or not found) on disk in the specified
	 * SynchronizedDirectory. The directory must exist. This method is supposed to run one time before the WatchService
	 * starts, or later when a SynchronizedDirectory that was initially not existing (i.e. because it was not mounted)
	 * is seen by the DirChecker thread in run().
	 */
	private void synchronizePkgFiles(SynchronizedDirectory syncedDir) {
		ArrayList<String> files = new ArrayList<>();
		ArrayList<String> dirs = new ArrayList<>();

		Path path = Paths.get(syncedDir.dir);
		if (!Files.exists(path))
			return;

		// The outer loop is supposed to wait for mounted HDDs to spin up.
		for (int attempts = 0; attempts < ATTEMPT_COUNT; attempts++) {
			if (!Files.isReadable(path) || !Files.isExecutable(path)) {
				try {
					Thread.sleep(ATTEMPT_DELAY);
				} catch (InterruptedException e) {
					return;
				}
				continue;
			}

			Set<String> topFiles = getPkgFiles(syncedDir.dir);
			if (topFiles != null)
				files.addAll(topFiles);
			dirs.add(syncedDir.dir);

			if (syncedDir.isRecursive) {
				Set<String> subdirs = getSubdirectories(syncedDir.dir);
				for (String dir : subdirs) {
					Set<String> subdirFiles = getPkgFiles(dir);
					if (subdirFiles == null)
						continue;

					files.addAll(subdirFiles);
				}
				dirs.addAll(subdirs);
			}

			// Send the GUI work to the GUI thread.
			Display.getDefault().asyncExec(() -> {
				if (tabContent.isDisposed())
					return;

				// Add all found PKG files to the list.
				for (String file : files)
					tabContent.addFile(file);

				// Remove PKGs that don't exist anymore from the list.
				TableItem[] items = tabContent.getTable().getItems();
				PS4PKG[] pkgs = tabContent.getPkgs(items);
				for (int i = 0; i < items.length; i++)
					for (String dir : dirs)
						if (pkgs[i].directory.equals(dir) && !pkgs[i].exists())
							tabContent.removeTableItem(items[i]);
			});

			return;
		}
	}

	/** Removes a user-provided directory. */
	public synchronized void removeDirectory(String directory) {
		synchronizedDirectories.removeIf(synchedDir -> synchedDir.dir.equals(directory));
		removeWatchKey(directory);

		// printSynchronizedDirectories(); // DEBUG
	}

	/**
	 * If the specified directory is found in the known WatchKeys, it is removed and true is returned (otherwise false).
	 */
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

	/** Returns the currently synchronized directories. */
	public synchronized String[] getSynchedDirs() {
		String[] dirs = new String[synchronizedDirectories.size()];
		for (int i = 0; i < synchronizedDirectories.size(); i++)
			dirs[i] = synchronizedDirectories.get(i).dir;
		return dirs;
	}

	/**
	 * Returns the currently synchronized directories's recursion state, which is supposed to be iterated over together
	 * with getSynchedDirs().
	 */
	public synchronized int[] getSynchedDirsRecursionState() {
		int[] recursions = new int[synchronizedDirectories.size()];
		for (int i = 0; i < synchronizedDirectories.size(); i++)
			recursions[i] = synchronizedDirectories.get(i).isRecursive ? 1 : 0;
		return recursions;
	}

	private synchronized String[] getWatchedDirectories() {
		String[] watchedDirectories = new String[watchKeys.size()];
		for (int i = 0; i < watchKeys.size(); i++)
			watchedDirectories[i] = watchKeys.get(i).watchable().toString();
		return watchedDirectories;
	}

	// Debug function.
	public synchronized void printSynchronizedDirectories() {
		System.out.println("Synchronized (user-provided) directories for tab " + tabContent.getName());
		for (SynchronizedDirectory syncedDir : synchronizedDirectories)
			System.out.println(syncedDir.dir + ", " + syncedDir.isRecursive);
	}

	// Debug function.
	public synchronized void printWatchedDirectories() {
		System.out.println("Watched directories:");
		for (WatchKey watchKey : watchKeys)
			System.out.println(watchKey.watchable().toString());
	}
}
