import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

// This class is used to provide platform-specific constants.
public class Platform {
    static final String PLATFORM = SWT.getPlatform();
    static final String DIRECTORY_TERM;
    static final String DIRECTORIES_TERM;
    static final String SUBDIRECTORY_TERM;
    static final String SUBDIRECTORIES_TERM;
    static final int SORT_DIRECTION_ASCENDING;
    static final int SORT_DIRECTION_DESCENDING;
    static final boolean isWindows = PLATFORM == "win32";

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
    }

    /** Must be called after shell.pack() and before shell.open() to fix platform-dependant shell centering issues. */
    static void centerShell(Shell shell) {
        if (Platform.isWindows) {
            Rectangle parentSize;
            Composite parent = shell.getParent();
            if (parent == null) { // For shells which have no parent shell (i.e. Properties).
                return;
                // TODO
                // parentSize = shell.getDisplay().getBounds();
            } else
                parentSize = shell.getParent().getBounds();
            Rectangle shellSize = shell.getBounds();
            int x = (parentSize.width - shellSize.width) / 2 + parentSize.x;
            int y = (parentSize.height - shellSize.height) / 2 + parentSize.y;
            shell.setLocation(new Point(x, y));

        }
    }
}
