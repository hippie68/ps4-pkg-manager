import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CustomActions {
    private Shell shell;
    public static CustomAction[] actions;
    private Composite customActionsGroup;

    public CustomActions(Shell parent) {
        this.shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        shell.setText("Custom Actions");
        shell.setLayout(new GridLayout(1, false));

        Composite infoGroup = new Composite(shell, SWT.NONE);
        infoGroup.setLayout(new GridLayout(1, false));
        Label info = new Label(infoGroup, SWT.WRAP);
        info.setText(
            """
                Custom Actions are user-defined context menu entries that can be used to run platform-dependant commands on selected PKGs. The commands can contain placeholder variables which are evaluated in the following order:

                   1. %dir% -> The first PKG file's directory.
                   2. %app%, %patch%, and %dlc% -> The next PKG file that is of the specified type.
                   3. %file% -> The next PKG file.
                   4. %files% -> All remaining PKG files, delimited by space characters.
                """);

        this.customActionsGroup = new Composite(shell, SWT.NONE);
        RowLayout customActionsGroupLayout = new RowLayout(SWT.VERTICAL);
        customActionsGroupLayout.marginLeft = 0;
        customActionsGroupLayout.marginRight = 0;
        customActionsGroupLayout.marginTop = 0;
        customActionsGroupLayout.marginBottom = 0;
        customActionsGroupLayout.spacing = 0;
        customActionsGroup.setLayout(customActionsGroupLayout);
        customActionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Make info label wrap.
        addCustomAction(customActionsGroup, "", ""); // Dummy Custom Action to calculate info label's width.
        customActionsGroup.layout();
        GridData infoLayoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        infoLayoutData.widthHint = customActionsGroup.getChildren()[1].getSize().x - 12; // 12: 4 * 3 pixel margins.
        info.setLayoutData(infoLayoutData);
        info.requestLayout();
        // Remove dummy Custom Action.
        Control[] controls = customActionsGroup.getChildren();
        for (Control control : controls)
            control.dispose();
        while (Display.getCurrent().readAndDispatch())
            ;

        loadActionsFromBuffer(customActionsGroup);

        Composite buttonArea = new Composite(shell, SWT.NONE);
        buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        GridLayout buttonAreaLayout = new GridLayout(4, false);
        buttonArea.setLayout(buttonAreaLayout);

        Button add = new Button(buttonArea, SWT.PUSH);
        add.setText("Create New Custom Action...");
        add.addListener(SWT.Selection, e -> {
            addCustomAction(customActionsGroup, "", "");

            // Set focus on the new "Name" Text.
            Control[] children = customActionsGroup.getChildren();
            ((Composite) children[children.length - 1]).getChildren()[2].setFocus();
        });

        Link link = new Link(buttonArea, SWT.LEFT);
        link.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        link.setText("<a>Share Custom Actions on GitHub</a> (external link)");
        link.addListener(SWT.Selection,
            e -> Program.launch("https://github.com/hippie68/ps4-pkg-manager/discussions/2"));

        Button cancel = new Button(buttonArea, SWT.PUSH);
        cancel.setText("Cancel");
        cancel.addListener(SWT.Selection, e -> shell.close());

        Button close = new Button(buttonArea, SWT.PUSH);
        close.setText("Apply and Close");
        close.addListener(SWT.Selection, e -> {
            saveActionsToBuffer();
            shell.close();
        });

        shell.setDefaultButton(close);
        shell.pack();
        Platform.centerShell(shell);
        shell.open();
    }

    // Helper function for isValidPkgSelection().
    private static int countSubstrings(String string, String substring) {
        int count = 0;
        int next = 0;
        while ((next = string.indexOf(substring)) != -1) {
            count++;
            string = string.substring(next + substring.length());
        }
        return count;
    }

    /** Checks if a PKG selection matches a Custom Action's variable requirements. */
    public static boolean isValidPkgSelection(CustomAction action, PS4PKG[] pkgs) {
        int requiredApps = countSubstrings(action.commandPattern, "%app%");
        int requiredPatches = countSubstrings(action.commandPattern, "%patch%");
        int requiredDlcs = countSubstrings(action.commandPattern, "%dlc%");
        int requiredFiles = countSubstrings(action.commandPattern, "%file%");

        if (requiredApps + requiredPatches + requiredDlcs + requiredFiles == 0)
            return true;

        int apps = 0;
        int patches = 0;
        int dlcs = 0;
        int files = 0;
        for (PS4PKG pkg : pkgs) {
            String category = pkg.getSFOValue("CATEGORY");
            if (category == null) {
                files++;
                continue;
            }

            if (category.startsWith("gd"))
                apps++;
            else if (category.startsWith("gp"))
                patches++;
            else if (category.startsWith("ac"))
                dlcs++;
            else
                files++;
        }

        if (apps < requiredApps || patches < requiredPatches || dlcs < requiredDlcs) {
            return false;
        } else if (apps == requiredApps && patches == requiredPatches && dlcs == requiredDlcs
            && files == requiredFiles) {
            if (action.commandPattern.contains("%files%")) // Too little PKGs.
                return false;
            else
                return true;
        } else {
            int remainingFiles = apps - requiredApps + patches - requiredPatches + dlcs - requiredDlcs + files
                - requiredFiles;
            return remainingFiles == 0 ^ action.commandPattern.contains("%files%");
        }
    }

    // Reads all user-provided action definitions from the user interface and stores them in the static array "actions".
    private void saveActionsToBuffer() {
        Control[] controls = customActionsGroup.getChildren();
        if (controls.length < 2)
            return;

        List<CustomAction> actions = new ArrayList<>();
        for (int i = 1; i < controls.length; i++) {
            Control[] children = ((Composite) controls[i]).getChildren();

            String name = ((Text) children[2]).getText().replace("|", "").strip(); // No vertical bars allowed.
            String command = ((Text) children[3]).getText().strip();
            if (name.isEmpty() && command.isEmpty())
                continue;

            actions.add(new CustomAction(name, command));
        }
        CustomActions.actions = actions.toArray(CustomAction[]::new);
    }

    private void loadActionsFromBuffer(Composite composite) {
        if (actions == null)
            return;

        for (CustomAction action : actions)
            addCustomAction(composite, action.name, action.commandPattern);
    }

    public static void saveActionsToFile(String filename) {
        if (actions == null)
            return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (CustomAction action : actions)
                writer.write(action.name + " | " + action.commandPattern + '\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadActionsFromFile(String filename) {
        List<CustomAction> actions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            while (true) {
                String line = reader.readLine().trim();
                int barIndex = line.indexOf('|');
                String name = line.substring(0, barIndex).trim();
                String command = line.substring(barIndex + 1, line.length()).trim();

                actions.add(new CustomAction(name, command));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
        }

        CustomActions.actions = actions.toArray(new CustomAction[0]);
    }

    private void addCustomAction(Composite parent, String name, String command) {
        Composite composite = new Composite(parent, SWT.NONE);

        if (parent.getChildren().length == 1) {
            Label nameHeader = new Label(composite, SWT.NONE);
            nameHeader.setText("Name");
            nameHeader.pack();

            Label commandHeader = new Label(composite, SWT.NONE);
            commandHeader.setText("Command");
            commandHeader.pack();

            addCustomAction(parent, name, command);
            return;
        }

        composite.setLayout(new GridLayout(5, false));

        Button up = new Button(composite, SWT.ARROW | SWT.UP);
        up.addListener(SWT.Selection, e -> {
            Control[] controls = parent.getChildren();
            Control currentControl = ((Button) e.widget).getParent();
            for (int i = 1; i < controls.length; i++) {
                if (controls[i] == currentControl) {
                    if (i != 1) {
                        controls[i].moveAbove(controls[i - 1]);
                        parent.layout();
                    }
                    return;
                }
            }
        });

        Button down = new Button(composite, SWT.ARROW | SWT.DOWN);
        down.addListener(SWT.Selection, e -> {
            Control[] controls = parent.getChildren();
            Control currentControl = ((Button) e.widget).getParent();
            for (int i = 1; i < controls.length; i++) {
                if (controls[i] == currentControl) {
                    if (i != controls.length - 1) {
                        controls[i].moveBelow(controls[i + 1]);
                        parent.layout();
                    }
                    return;
                }
            }
        });

        Text nameText = new Text(composite, SWT.BORDER);
        nameText.setText(name);
        GridData nameTextLayoutData = new GridData(SWT.LEFT, SWT.FILL, false, false);
        nameTextLayoutData.widthHint = (Platform.isWindows ? 8 : 6) * up.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        nameText.setLayoutData(nameTextLayoutData);

        Text commandText = new Text(composite, SWT.BORDER);
        commandText.setText(command);
        GridData commandTextLayoutData = new GridData(SWT.LEFT, SWT.FILL, false, false);
        commandTextLayoutData.widthHint = (Platform.isWindows ? 22 : 18) * up.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        commandText.setLayoutData(commandTextLayoutData);

        Button remove = new Button(composite, SWT.PUSH);
        remove.setText("Delete");
        remove.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        remove.addListener(SWT.Selection, e -> {
            if (parent.getChildren().length == 2) {
                for (Control control : parent.getChildren())
                    control.dispose();
            } else {
                composite.dispose();
            }
            shell.pack();
        });

        shell.pack();

        // Set correct header positions.
        if (parent.getChildren().length == 2) {
            Control[] controls = parent.getChildren();
            Composite headerComposite = (Composite) controls[0];
            Composite firstActionComposite = (Composite) controls[1];
            Label nameHeader = (Label) headerComposite.getChildren()[0];
            Label commandHeader = (Label) headerComposite.getChildren()[1];
            int nameHeaderX = firstActionComposite.getChildren()[2].getLocation().x;
            int commandHeaderX = firstActionComposite.getChildren()[3].getLocation().x;
            Point headerCompositeSize = headerComposite.getSize();
            Point nameHeaderSize = nameHeader.getSize();
            Point commandHeaderSize = commandHeader.getSize();

            nameHeader.setLocation(nameHeaderX,
                nameHeader.getLocation().y + (headerCompositeSize.y - nameHeaderSize.y));
            commandHeader.setLocation(commandHeaderX, (headerCompositeSize.y - commandHeaderSize.y));
            shell.layout(true, true);
        }

        while (Display.getCurrent().readAndDispatch())
            ;
    }
}
