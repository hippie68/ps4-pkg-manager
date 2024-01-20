import java.text.DecimalFormat;
import java.util.Comparator;

import org.eclipse.swt.SWT;

public enum Column {
    // To maintain backward compatibility, this order must never be changed. New columns must be added at the end.
    // Once a new program version is released, new columns must never be removed. If a column ever becomes obsolete, it
    // must be replaced with a placeholder containing obsolescence information.
    INDEX("Index", "Represents the ascending order in which PKGs were added in the current session", false, SWT.LEFT,
        Comparators.longComparator),
    PATH("Path", Platform.DIRECTORY_TERM + " and file name combined", false, SWT.LEFT, Comparators.stringComparator),
    DIRECTORY(Platform.DIRECTORY_TERM, Platform.DIRECTORY_TERM, false, SWT.LEFT, Comparators.stringComparator),
    FILENAME("File name", "File name", false, SWT.LEFT, Comparators.stringComparator),
    TITLE("Title", "The content's official name", true, SWT.LEFT, Comparators.stringComparator),
    TITLE_ID("Title ID", "The content's international identifier", true, SWT.LEFT, Comparators.stringComparator),
    REGION("Region", "Asia/Europe/Japan/USA/World", true, SWT.LEFT, Comparators.stringComparator),
    TYPE("Type", "App/Patch/DLC", true, SWT.LEFT, Comparators.stringComparator),
    VERSION("Version", "The content's true version number", true, SWT.RIGHT, Comparators.numberComparator),
    BACKPORT("Backport", "A check mark means the PKG is a backport", true, SWT.CENTER, Comparators.boolComparator),
    SDK("SDK", "Software development kit version", true, SWT.RIGHT, Comparators.numberComparator),
    FIRMWARE("Firmware", "Required PS4 firmware version", true, SWT.RIGHT, Comparators.numberComparator),
    SIZE("Size", "File size", false, SWT.RIGHT, Comparators.sizeComparator),
    RELEASE_TAGS("Release", "Release tags found in the file name and/or changelog data", false, SWT.LEFT,
        Comparators.stringComparator),
    COMPATIBILITY_CHECKSUM("Compatibility checksum",
        "Indicates whether app and patch PKGs are compatible with each other (\"married\")", true, SWT.LEFT,
        Comparators.stringComparator);

    public final String name;
    public final String tooltip;
    public boolean enabledByDefault;
    public final int style;
    public final Comparator<String> comparator;
    public static final int length = values().length;

    // The order in which the current program version displays columns by default. May be changed arbitrarily.
    public static final int[] order = { INDEX.ordinal(), PATH.ordinal(), DIRECTORY.ordinal(), FILENAME.ordinal(),
        TITLE.ordinal(), TITLE_ID.ordinal(), REGION.ordinal(), TYPE.ordinal(), VERSION.ordinal(), BACKPORT.ordinal(),
        SDK.ordinal(), FIRMWARE.ordinal(), SIZE.ordinal(), RELEASE_TAGS.ordinal(), COMPATIBILITY_CHECKSUM.ordinal() };

    Column(String name, String tooltip, boolean enabledByDefault, int style, Comparator<String> comparator) {
        this.name = name;
        this.tooltip = tooltip;
        this.enabledByDefault = enabledByDefault;
        this.style = style;
        this.comparator = comparator;
    }

    // Used to recreate stored column order.
    public static Column get(int ordinal) {
        for (Column column : values())
            if (column.ordinal() == ordinal)
                return column;
        return null;
    }
}

class Comparators {
    static final Comparator<String> longComparator = (a, b) -> Long.signum(Long.valueOf(a) - Long.valueOf(b));

    static final Comparator<String> stringComparator = (a, b) -> a.compareToIgnoreCase(b);

    static final Comparator<String> boolComparator = (a, b) -> a.length() == b.length() ? 0
        : a.length() > b.length() ? -1 : 1;

    static final Comparator<String> numberComparator = (a, b) -> {
        double fa, fb;
        try {
            fa = Double.valueOf(a);
        } catch (NumberFormatException e) {
            fa = Double.MAX_VALUE;
        }
        try {
            fb = Double.valueOf(b);
        } catch (NumberFormatException e) {
            fb = Double.MAX_VALUE;
            if (fa == fb)
                return a.compareTo(b);
        }
        double result = fa - fb;
        return (int) Math.signum(result);
    };

    private static final String decimalSeparator = String
        .valueOf(new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator());

    static private String toKB(String size) {
        String[] split = size.split(" ");
        split[0] = split[0].replace(decimalSeparator, "");
        switch (split[1]) {
            case "GB":
                return split[0] + "000000";
            case "MB":
                return split[0] + "000";
            default:
                return split[0];
        }
    }

    static final Comparator<String> sizeComparator = (a, b) -> longComparator.compare(toKB(a), toKB(b));
}
