import java.util.ArrayList;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

record QueuedDir(String path, boolean recursive) {
}

public class TableThread extends Thread {
	private TabContent tabContent;
	private PkgQueue<Object> queue;
	private static final String DATA_MISSING = "[N/A]";
	private boolean isProcessing;

	TableThread(TabContent tabContent, PkgQueue<Object> queue) {
		this.tabContent = tabContent;
		this.queue = queue;
	}

	public String[] Ps4PkgToTableItemText(PS4PKG pkg) {
		// Get Title.
		String title;
		int titleLanguage = Settings.getTitleLanguage();
		if (titleLanguage == -1 || (title = pkg.getSFOValue(String.format("TITLE_%02d", titleLanguage))) == null)
			title = (title = pkg.getSFOValue("TITLE")) == null ? DATA_MISSING : title;

		// Get Title ID.
		String titleID = (titleID = pkg.getSFOValue("TITLE_ID")) == null ? DATA_MISSING : titleID;

		// Get Region.
		String region = switch (pkg.header.content_id.charAt(0)) {
			case 'E' -> "Europe";
			case 'H' -> "Asia";
			case 'I' -> "World";
			case 'J' -> "Japan";
			case 'U' -> "USA";
			default -> "Unknown";
		};

		// Get Version.
		String version;
		if ((version = pkg.getChangelogVersion()) == null && (version = pkg.getSFOValue("APP_VER")) == null
			&& (version = pkg.getSFOValue("VERSION")) == null)
			version = DATA_MISSING;
		else if (version.length() > 1 && version.charAt(0) == '0')
			version = version.substring(1);

		// Get Type.
		String type;
		String category = pkg.getSFOValue("CATEGORY");
		if (category == null)
			type = DATA_MISSING;
		else if (category.startsWith("gd"))
			type = "App";
		else if (category.startsWith("gp"))
			type = "Patch";
		else if (category.equals("ac"))
			type = "DLC";
		else
			type = "Other";

		// Get SDK and FW.
		String sdk;
		String fw;
		if (type.equals("DLC")) {
			sdk = "";
			fw = "";
		} else {
			// SDK
			String pubtoolinfo = pkg.getSFOValue("PUBTOOLINFO");
			if (pubtoolinfo == null)
				sdk = "?";
			else {
				int index = pubtoolinfo.indexOf("sdk_ver=");
				if (index == -1)
					sdk = "?";
				else {
					String sdk1, sdk2, result;
					try {
						index += 8;
						sdk1 = pubtoolinfo.substring(index + (pubtoolinfo.charAt(index) == '0' ? 1 : 0), index + 2);
						sdk2 = pubtoolinfo.substring(index + 2, index + 4);
						result = sdk1 + '.' + sdk2;
					} catch (StringIndexOutOfBoundsException e) {
						result = "?";
					}
					sdk = result;
				}
			}

			// FW
			String system_ver = pkg.getSFOValue("SYSTEM_VER");
			if (system_ver == null)
				fw = "?";
			else
				fw = (system_ver.charAt(2) == '0' ? system_ver.substring(3, 4) : system_ver.substring(2, 4)) + '.'
					+ system_ver.substring(4, 6);
		}

		// Get fake status.
		String fake = switch(pkg.isFake) {
			case -1 -> "";
			case 1 -> "\u2713";
			default -> DATA_MISSING;
		};

		// Get Backport (requires sdk to be set).
		String backport;
		if (sdk.compareTo("5.05") == 0 || pkg.filename.toLowerCase().indexOf("bp") != -1
			|| pkg.filename.toLowerCase().indexOf("backport") != -1
			|| pkg.changelog != null && pkg.changelog.toLowerCase().indexOf("backport") != -1)
			backport = "\u2713";
		else
			backport = "";

		// Get release tags.
		String releaseTags = "";
		String filenameLowerCase = pkg.filename.toLowerCase();
		String changelogLowerCase = pkg.changelog == null ? null : pkg.changelog.toLowerCase();
		// TODO: too many toLowerCase() calls here.
		for (String group : ReleaseTags.getJoinedReleaseGroups())
			if (filenameLowerCase.contains(group.toLowerCase())) {
				if (!releaseTags.isEmpty())
					releaseTags += ", ";
				releaseTags += group;
			}
		for (String tag : ReleaseTags.getJoinedReleases())
			if (filenameLowerCase.contains(tag.toLowerCase())
				|| changelogLowerCase != null && changelogLowerCase.contains(tag.toLowerCase())) {
				if (!releaseTags.isEmpty())
					releaseTags += ", ";
				releaseTags += tag;
			}

		// Get Compatibility Checksum.
		String checksum = (checksum = pkg.getCompatibilityChecksum()) == null ? "" : checksum;

		// Create TableItem texts.
		String[] texts = new String[Column.length];
		for (int i = 0; i < texts.length; i++)
			texts[i] = switch (Column.values()[i]) {
				case INDEX -> String.valueOf(tabContent.stamp++);
				case PATH -> pkg.path;
				case DIRECTORY -> pkg.directory;
				case FILENAME -> pkg.filename;
				case TITLE -> title;
				case TITLE_ID -> titleID;
				case REGION -> region;
				case TYPE -> type;
				case VERSION -> version;
				case FAKE -> fake;
				case BACKPORT -> backport;
				case SDK -> sdk;
				case FIRMWARE -> fw;
				case SIZE -> {
					if (pkg.header.pkg_size > 1000000000)
						yield String.format("%.02f GB", (double) pkg.header.pkg_size / 1000000000);
					else if (pkg.header.pkg_size > 1000000)
						yield String.format("%d MB", Math.round((double) pkg.header.pkg_size / 1000000));
					else
						yield String.format("%d KB", Math.round((double) pkg.header.pkg_size / 1000));
				}
				case RELEASE_TAGS -> releaseTags;
				case COMPATIBILITY_CHECKSUM -> checksum;
				default -> "[COLUMN NOT IMPLEMENTED]";
			};
		return texts;
	}

	private void insertPkgIntoTable(PS4PKG pkg) {
		if (pkg == null)
			return;

		// Insert data into table buffer and table.
		Display.getDefault().asyncExec(() -> {
			if (!tabContent.isDisposed())
				tabContent.processNewTableItemData(new TableItemData(pkg, Ps4PkgToTableItemText(pkg)));
		});
	}

	private PS4PKG getPS4PKG(String filename) {
		PS4PKG pkg;
		try {
			pkg = new PS4PKG(filename);
		} catch (Exception e) {
			System.err.println("File name: " + filename);
			e.printStackTrace();
			return null; // TODO: handle "not a PS4 PKG" and I/O errors differently; ignore non-PKG
						 // drops, output some error message for I/O errors.
		}
		return pkg;
	}

	/** Checks if the thread is currently processing data. */
	public synchronized boolean isProcessingData(boolean ignoreNullObjects) {
		return queue.getLength() > (ignoreNullObjects ? 1 : 0) || isProcessing;
	}

	public synchronized int getQueueLength() {
		return queue.getLength();
	}

	private synchronized void setIsProcessing(boolean value) {
		isProcessing = value;
	}

	@Override
	public void run() {
		while (true)
			try {
				setIsProcessing(false);
				Object obj = queue.pop(); // Either of type String (path to a PKG file), QueuedDir, or PS4PKG.
				if (obj == null) // Sent by GUI.java on exiting.
					return;
				setIsProcessing(true);

				if (obj instanceof String path)
					insertPkgIntoTable(getPS4PKG(path));
				else if (obj instanceof QueuedDir queuedDir) {
					if (queuedDir.recursive() == true) {
						ArrayList<String> dirs = new ArrayList<>();
						dirs.add(queuedDir.path());
						dirs.addAll(WatcherThread.getSubdirectories(queuedDir.path()));
						for (String dir : dirs) {
							Set<String> files = WatcherThread.getPkgFiles(dir);
							if (files == null)
								continue;
							for (String file : files) {
								insertPkgIntoTable(getPS4PKG(file));
								if (Thread.interrupted())
									throw new InterruptedException();
							}
						}
					} else {
						Set<String> files = WatcherThread.getPkgFiles(queuedDir.path());
						if (files == null)
							continue;
						for (String file : files) {
							insertPkgIntoTable(getPS4PKG(file));
							if (Thread.interrupted())
								throw new InterruptedException();
						}
					}
				} else if (obj instanceof PS4PKG pkg) {
                    insertPkgIntoTable(pkg);
				}
			} catch (InterruptedException e) {
				setIsProcessing(false);
				return;
			}
	}
}