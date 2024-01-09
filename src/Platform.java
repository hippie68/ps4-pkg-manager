import org.eclipse.swt.SWT;

// This class is used to provide platform-specific constants.
public class Platform {
    static final String PLATFORM = SWT.getPlatform();
    static final String DIRECTORY_TERM;
    static final String DIRECTORIES_TERM;
    static final int SORT_DIRECTION_ASCENDING;
    static final int SORT_DIRECTION_DESCENDING;

    static {
        if (PLATFORM == "gtk" || PLATFORM == "motif") { // Unix
            SORT_DIRECTION_ASCENDING = SWT.DOWN;
            SORT_DIRECTION_DESCENDING = SWT.UP;
            DIRECTORY_TERM = "Directory";
            DIRECTORIES_TERM = "Directories";
        } else { // macOS and Windows
            SORT_DIRECTION_ASCENDING = SWT.UP;
            SORT_DIRECTION_DESCENDING = SWT.DOWN;
            DIRECTORY_TERM = "Folder";
            DIRECTORIES_TERM = "Folders";
        }
    }
}
