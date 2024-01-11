import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.eclipse.swt.widgets.Display;

// TODO: Display N/A only if the PKG is not complete yet (compare header's size with real file's size).
public class TableThread extends Thread {
    private TabContent tabContent;
    private PkgQueue<Object> queue;
    private static final String DATA_MISSING = "[N/A]";

    TableThread(TabContent tabContent, PkgQueue<Object> queue) {
        this.tabContent = tabContent;
        this.queue = queue;
    }

    public String[] Ps4PkgToTableItemText(PS4PKG pkg) {
        // Get Title
        String title = (title = pkg.getSFOValue("TITLE")) == null ? DATA_MISSING : title;

        // Get Title ID
        String titleID = (titleID = pkg.getSFOValue("TITLE_ID")) == null ? DATA_MISSING : titleID;

        // Get Region
        String region = switch (pkg.header.content_id.charAt(0)) {
            case 'E' -> "Europe";
            case 'H' -> "Asia";
            case 'I' -> "World";
            case 'J' -> "Japan";
            case 'U' -> "USA";
            default -> "Unknown";
        };

        // Get Version
        String version = pkg.getChangelogVersion();
        if ((version = pkg.getChangelogVersion()) == null && (version = pkg.getSFOValue("APP_VER")) == null
            && (version = pkg.getSFOValue("VERSION")) == null)
            version = DATA_MISSING;
        else if (version.length() > 1 && version.charAt(0) == '0')
            version = version.substring(1);

        // Get Type
        String type;
        String category = pkg.getSFOValue("CATEGORY");
        if (category == null) {
            type = DATA_MISSING;
        } else {
            if (category.startsWith("gd"))
                type = "App";
            else if (category.startsWith("gp"))
                type = "Patch";
            else if (category.equals("ac"))
                type = "DLC";
            else
                type = "Other";
        }

        // Get SDK and FW
        String sdk;
        String fw;
        if (type.equals("DLC")) {
            sdk = "";
            fw = "";
        } else {
            // SDK
            String pubtoolinfo = pkg.getSFOValue("PUBTOOLINFO");
            if (pubtoolinfo == null) {
                sdk = "?";
            } else {
                int index = pubtoolinfo.indexOf("sdk_ver=");
                if (index == -1) {
                    sdk = "?";
                } else {
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

        // Get Backport (requires sdk to be set)
        String backport;
        if (sdk.compareTo("5.05") == 0 || pkg.filename.toLowerCase().indexOf("bp") != -1
            || pkg.filename.toLowerCase().indexOf("backport") != -1
            || (pkg.changelog != null && pkg.changelog.toLowerCase().indexOf("backport") != -1))
            backport = "\u2713";
        else
            backport = "";

        // Get release tags
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
                || (changelogLowerCase != null && changelogLowerCase.contains(tag.toLowerCase()))) {
                if (!releaseTags.isEmpty())
                    releaseTags += ", ";
                releaseTags += tag;
            }

        // Get Compatibility Checksum
        String checksum = (checksum = pkg.getCompatibilityChecksum()) == null ? "" : checksum;

        // Create TableItem texts.
        String[] texts = new String[Column.length];
        for (int i = 0; i < texts.length; i++) {
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
                case BACKPORT -> backport;
                case SDK -> sdk;
                case FIRMWARE -> fw;
                case SIZE -> {
                    if (pkg.header.pkg_size > 1000000000)
                        yield String.format("%.02f GB", (double) pkg.header.pkg_size / 1000000000);
                    else if (pkg.header.pkg_size > 1000000)
                        yield String.format("%d MB", Math.round(pkg.header.pkg_size / 1000000));
                    else
                        yield String.format("%d KB", Math.round(pkg.header.pkg_size / 1000));
                }
                case RELEASE_TAGS -> releaseTags;
                case COMPATIBILITY_CHECKSUM -> checksum;
                default -> "[COLUMN NOT IMPLEMENTED]";
            };
        }
        return texts;
    }

    private void insertPkgIntoTable(PS4PKG pkg) {
        if (pkg == null) // Happened during testing, probably due to corrupted/empty database. May or may not happen
                         // in final code.
            return;

        // Insert data into table buffer and table
        Display.getDefault()
            .asyncExec(() -> tabContent.processNewTableItemData(new TableItemData(pkg, Ps4PkgToTableItemText(pkg))));
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

    @Override
    public void run() {
        while (true) {
            Object o = queue.pop(); // Either of type String (path to a directory or to a PKG file) or PS4PKG.
            PS4PKG pkg;

            if (o instanceof String) {
                String path = (String) o;

                if (Files.isDirectory(Paths.get(path))) {
                    Set<String> pkgFilenames = WatcherThread.getPkgFiles(path);
                    if (pkgFilenames == null)
                        continue;

                    for (String pkgFilename : pkgFilenames) {
                        pkg = getPS4PKG(pkgFilename);
                        if (pkg != null)
                            insertPkgIntoTable(pkg);
                    }
                } else {
                    pkg = getPS4PKG(path);
                    if (pkg != null)
                        insertPkgIntoTable(pkg);
                }
            } else {
                pkg = (PS4PKG) o;
                insertPkgIntoTable(pkg);
            }
        }
    }
}
