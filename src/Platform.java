import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;

// This class is used to provide platform-specific constants.
public class Platform {
    public static final String PLATFORM = SWT.getPlatform();
    public static final String DIRECTORY_TERM;
    public static final String DIRECTORIES_TERM;
    public static final String SUBDIRECTORY_TERM;
    public static final String SUBDIRECTORIES_TERM;
    public static final String RIGHT_CLICK_TERM;
    public static final int SORT_DIRECTION_ASCENDING;
    public static final int SORT_DIRECTION_DESCENDING;
    public static final boolean isWindows = PLATFORM == "win32";
    public static final boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac");

    public static final String MOD1;
    private static final String MOD2 = "Shift";
    private static final String MOD3 = "Alt";

    static {
        if (PLATFORM == "gtk" || PLATFORM == "motif") { // Unix
            SORT_DIRECTION_ASCENDING = SWT.DOWN;
            SORT_DIRECTION_DESCENDING = SWT.UP;
            DIRECTORY_TERM = "Directory";
            DIRECTORIES_TERM = "Directories";
            SUBDIRECTORY_TERM = "Subdirectory";
            SUBDIRECTORIES_TERM = "Subdirectories";
        } else { // macOS and Windows
            SORT_DIRECTION_ASCENDING = SWT.UP;
            SORT_DIRECTION_DESCENDING = SWT.DOWN;
            DIRECTORY_TERM = "Folder";
            DIRECTORIES_TERM = "Folders";
            SUBDIRECTORY_TERM = "Subfolder";
            SUBDIRECTORIES_TERM = "Subfolders";
        }

        if (isMac) {
            MOD1 = "Cmd";
            RIGHT_CLICK_TERM = "Control-click";
        } else {
            MOD1 = "Ctrl";
            RIGHT_CLICK_TERM = "Right-click";
        }
    }

    /**
     * Sets a global keyboard shortcut for a particular MenuItem.
     *
     * @param item the MenuItem
     * @param keys a logical OR ('|') combination of SWT.MOD1, SWT.MOD2, SWT.MOD3, and one regular keyboard character
     */
    public static void setAccelerator(MenuItem item, int keys) {
        StringBuilder sb = new StringBuilder();
        if ((keys & SWT.MOD3) == SWT.MOD3) {
            sb.append(MOD3);
            sb.append('+');
        }
        if ((keys & SWT.MOD2) == SWT.MOD2) {
            sb.append(MOD2);
            sb.append('+');
        }
        if ((keys & SWT.MOD1) == SWT.MOD1) {
            sb.append(MOD1);
            sb.append('+');
        }
        sb.append(keys ^ SWT.MOD1 ^ SWT.MOD2 ^ SWT.MOD3);

        item.setText(item.getText() + "\t" + sb);
        item.setAccelerator(keys);
    }
}
