import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class ShowInternalReleaseTags {
    public ShowInternalReleaseTags(Shell parent) {
        Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Hard-Coded Release Tags");
        shell.setLayout(new GridLayout(2, false));

        // Release group tags
        Composite c1 = new Composite(shell, SWT.NONE);
        c1.setLayout(new RowLayout(SWT.VERTICAL));
        c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        createBoldLabel(c1, "Release groups:\n");
        for (String group : ReleaseTags.getHardCodedReleaseGroups()) {
            Label label = new Label(c1, SWT.NONE);
            label.setText(group);
        }

        // Release tags
        Composite c2 = new Composite(shell, SWT.NONE);
        c2.setLayout(new RowLayout(SWT.VERTICAL));
        c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        createBoldLabel(c2, "Releases:\n");
        for (String release : ReleaseTags.getHardCodedReleases()) {
            Label label = new Label(c2, SWT.NONE);
            label.setText(release);
        }

        new Label(c2, SWT.NONE);

        // File name expansions
        createBoldLabel(c2, "File name expansions:\n");
        List<String> sortedList = new ArrayList<String>(ReleaseTags.releaseGroupsMap.keySet());
        for (String key : sortedList) {
            Label label = new Label(c2, SWT.NONE);
            label.setText(key + " -> " + ReleaseTags.releaseGroupsMap.get(key));
        }
        new Label(c2, SWT.NONE);
        sortedList = new ArrayList<>(ReleaseTags.releasesMap.keySet());
        for (String key : sortedList) {
            Label label = new Label(c2, SWT.NONE);
            label.setText(key + " -> " + ReleaseTags.releasesMap.get(key));
        }

        new Label(c1, SWT.NONE);

        Link link = new Link(shell, SWT.NONE);
        link.setText("Please report missing tags and errors <a>here</a>.");
        GridData linkLayoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        link.setLayoutData(linkLayoutData);
        String url = "https://github.com/hippie68/ps4-pkg-manager/discussions/1";
        link.addListener(SWT.Selection, event -> Program.launch(url));

        shell.pack();
        ShellHelpers.centerShell(shell);
        shell.open();
    }

    private void createBoldLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        FontData fontData = label.getFont().getFontData()[0];
        Font font = new Font(parent.getShell().getDisplay(),
            new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
        label.setFont(font);
    }
}