import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class VersionCheckDialog {

    public VersionCheckDialog(Shell parent, boolean updateAlreadyFound) {
        Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Update Check");
        shell.setLayout(new GridLayout(2, true));

        Label messageLabel = new Label(shell, SWT.WRAP);
        messageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        messageLabel.setText("Please wait...");

        Link link = new Link(shell, SWT.NONE);
        link.setText("<a>Show available releases on GitHub</a>");
        link.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        link.addListener(SWT.Selection, e -> Program.launch(Version.releasesUrl));

        Button closeButton = new Button(shell, SWT.PUSH);
        closeButton.setText("Close");
        closeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
        closeButton.addListener(SWT.Selection, e -> shell.close());

        shell.setDefaultButton(closeButton);
        shell.pack();
        GUI.centerShell(shell);
        shell.open();

        while (Display.getCurrent().readAndDispatch())
            ;

        if (updateAlreadyFound == true)
            messageLabel.setText(Version.convertReturnValueToString(0));
        else
            messageLabel.setText(Version.convertReturnValueToString(Version.checkForUpdates()));
        messageLabel.requestLayout();
        shell.pack();
    }

}
