import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/** Describes a button's properties.
 *
 * @param name  The button's text.
 * @param listener  Code (e.g. a lambda expression) that is run when the button gets clicked.
 */
record DialogButton(String name, Listener listener) {}

/** Collection of static methods to help simplify repetitive GUI creation tasks. */
final class ShellHelpers {
    private ShellHelpers() {}

    // TODO: implement support for other Layout types, too.
    /** Sets Shell margins that look better than the default 5 points. Currently, this method must be called after the
     * shell's layout has been set to {@code GridLayout}. */
    public static void setShellMargin(Shell shell) {
        if (! (shell.getLayout() instanceof GridLayout layout))
            return;
        layout.marginWidth = 8;
        layout.marginHeight = 8;
    }

    /**
     * Creates a row of Buttons that do something when clicked. To be used as the bottom element of a shell's layout.
     *
     * @param parent              The Composite to insert the buttons into. Its layout type must be `Gridlayout`.
     * @param horizontalAlignment The way the buttons should be horizontally aligned (SWT.LEFT, SWT.CENTER, SWT.RIGHT).
     * @param buttons             The buttons.
     */
    public static void createDialogButtons(Shell parent, int horizontalAlignment, DialogButton... buttons) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(horizontalAlignment, SWT.CENTER, true, true));
        GridLayout gridLayout = new GridLayout(buttons.length, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginTop = 5;
        gridLayout.marginHeight = 0;
        composite.setLayout(gridLayout);

        // TODO: use Display.getDismissalAlignment() to order the buttons using the platform's style.
        for (int i = 0; i < buttons.length; i++) {
            Button button = new Button(composite, SWT.PUSH);
            GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            gridData.minimumWidth = 80;
            button.setLayoutData(gridData);
            button.setText(buttons[i].name());
            button.addListener(SWT.Selection, buttons[i].listener());

            if ((horizontalAlignment == SWT.LEFT && i == 0)
                || (horizontalAlignment == SWT.RIGHT && i == buttons.length - 1))
                parent.getShell().setDefaultButton(button);
        }
    }

    /** Centers a shell on its parent. Must be called after shell.pack() and before shell.open(). */
    public static void centerShell(Shell shell) {
        Composite parent = shell.getParent();
        if (parent == null)
            return;

        Rectangle parentSize = parent.getBounds();
        Rectangle shellSize = shell.getBounds();
        int x = (parentSize.width - shellSize.width) / 2 + parentSize.x;
        int y = (parentSize.height - shellSize.height) / 2 + parentSize.y;
        shell.setLocation(new Point(x, y));
    }

    /** Runs an event loop in a shell until the shell's window is closed. The current thread must be a user-interface
     * thread. */
    public static void runEventLoop(Shell shell) {
        Display display = Display.getCurrent();
        if (display == null)
            return;

        while (!shell.isDisposed())
            if (!display.readAndDispatch())
                display.sleep();
    }
}