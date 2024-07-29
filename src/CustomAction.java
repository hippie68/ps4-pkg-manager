import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Display;

/** Class to create and run a single Custom Action. */
public class CustomAction {
	String name;
	String commandPattern;

	public CustomAction(String name, String commandPattern) {
		this.name = name;
		this.commandPattern = commandPattern;
	}

	// Used by the GUI to reject saving command patterns that are invalid.
	public static boolean patternIsValid(String pattern) {
		boolean containsSfoVariables = Arrays.stream(CustomActions.sfoVariables).anyMatch(pattern::contains);
		boolean containsSingularVariables = Arrays.stream(CustomActions.singularVariables).anyMatch(pattern::contains);
		boolean containsPluralVariables = Arrays.stream(CustomActions.pluralVariables).anyMatch(pattern::contains);

		if (containsSingularVariables && containsPluralVariables)
			return false;

		if (containsSfoVariables && (containsSingularVariables || containsPluralVariables))
			return false;

		return true;
	}

	// This is hopefully not needed anymore.
	// private String createEscapedPath(String path) {
	// if (Platform.isWindows) {
	// if (path.length() == 3 && Character.isAlphabetic(path.charAt(0)) && path.charAt(1) == ':'
	// && path.charAt(2) == '\\')
	// return "\"" + path.substring(0, 2) + "\"";
	// else
	// return "\"" + path + "\"";
	// }
	//
	// path = path.replace(" ", "\\ ");
	// path = path.replace("'", "\\'");
	// path = path.replace("&", "\\&");
	// path = path.replace(";", "\\;");
	// path = path.replace("(", "\\(");
	// path = path.replace(")", "\\)");
	// path = path.replace("[", "\\[");
	// path = path.replace("]", "\\]");
	// // TODO: probably many more exotic cases.
	//
	// return path;
	// }

	// Helper function for createTokens().
	/**
	 * Returns the next PKG in a list of PKGs that is of the type specified by <patternVariable>.
	 *
	 * @return Either the PKG or null if there is no PKG of the specified type.
	 */
	private PS4PKG getNextPkg(String patternVariable, List<PS4PKG> pkgs) {
		if (pkgs.isEmpty())
			return null;

		switch (patternVariable) {
			case "%content_id%":
			case "%title_id%":
			case "%dir%":
				return pkgs.get(0);
			case "%app%":
				for (PS4PKG pkg : pkgs) {
					String category = pkg.getSFOValue("CATEGORY");
					if (category == null)
						continue;
					if (category.startsWith("gd"))
						return pkg.exists() ? pkg : null;
				}
				return null;
			case "%patch%":
				for (PS4PKG pkg : pkgs) {
					String category = pkg.getSFOValue("CATEGORY");
					if (category == null)
						continue;
					if (category.startsWith("gp"))
						return pkg.exists() ? pkg : null;
				}
				return null;
			case "%dlc%":
				for (PS4PKG pkg : pkgs) {
					String category = pkg.getSFOValue("CATEGORY");
					if (category == null)
						continue;
					if (category.equals("ac"))
						return pkg.exists() ? pkg : null;
				}
				return null;
			case "%file%":
			case "%files%":
				PS4PKG pkg = pkgs.get(0);
				return pkg.exists() ? pkg : null;
			default:
				return null;
		}
	}

	// Helper function for run().
	// Splits the user-provided command line into tokens that can be properly parsed by the operating system.
	private String[] createTokens(PS4PKG pkgs[], String commandPattern) {
		List<String> tokens = new ArrayList<>();
		List<PS4PKG> pkgList = new ArrayList<PS4PKG>(List.of(pkgs));

		// Create tokens.
		for (int i = 0; i < commandPattern.length(); i++)
			switch (commandPattern.charAt(i)) {
				// Substrings surrounded by single quotes.
				case '\'':
					int start = i + 1;
					do {
						i++;
						if (i == commandPattern.length())
							return null; // Pattern invalid.
					} while (commandPattern.charAt(i) != '\'');
					tokens.add(commandPattern.substring(start, i));
					// Note: to make more complex commands possible, like -> terminal -e "bash -c 'pkgrename %files%'",
					// replace all file names inside this token by (reactivating and) using createEscapedPath() on each.
					break;
				// Substrings delimited by spaces.
				default:
					int start2 = i;
					do
						i++;
					while (i < commandPattern.length() && commandPattern.charAt(i) != ' ');
					tokens.add(commandPattern.substring(start2, i));
			}

		if (Arrays.stream(CustomActions.sfoVariables).anyMatch(commandPattern::contains)) {
			// Replace all SFO variables inside all tokens.
			PS4PKG pkg = pkgList.get(0); // Patterns that have SFO variables only allow a single PKG.
			for (int i = 0; i < tokens.size(); i++) {
				String token;

				for (String variable : CustomActions.sfoVariables)
					while ((token = tokens.get(i)).contains(variable)) {
						String value = switch (variable) {
							case "%content_id%" -> pkg.getSFOValue("CONTENT_ID");
							case "%title_id%" -> pkg.getSFOValue("TITLE_ID");
							default -> null;
						};
						if (value == null)
							return null;

						tokens.set(i, token.replace(variable, value));
					}
			}
		} else {
			// Replace all PKG variables inside all tokens.
			for (int i = 0; i < tokens.size(); i++) {
				String token;
				boolean filesVariableSuccessfullyUsed = false;

				for (String variable : CustomActions.patternVariables)
					while ((token = tokens.get(i)).contains(variable)) {
						PS4PKG pkg = getNextPkg(variable, pkgList);
						if (pkg == null) {
							// Allow plural variables to exhaust the list, if they have been used successfully once.
							if (variable.equals("%files%") && filesVariableSuccessfullyUsed) {
								tokens.remove(i--);
								continue;
							}

							return null; // Otherwise, or for singular variables, not having a matching PKG is an
										 // error.
						}

						if (variable.equals("%dir%"))
							tokens.set(i, token.replace(variable, pkg.directory));
						else if (variable.equals("%files%")) {
							filesVariableSuccessfullyUsed = true;
							tokens.set(i, "%files%");
							tokens.add(i, token.replace(variable, pkg.path));
							i++;
						} else
							tokens.set(i, token.replace(variable, pkg.path));
						pkgList.remove(pkg);
					}
			}
		}

		return tokens.toArray(String[]::new);
	}

	public void run(TabContent tabContent, PS4PKG pkgs[]) {
		String pattern = this.commandPattern;

		// Handle known command prefixes (if more are added, the logic needs to be rewritten).
		if (this.commandPattern.startsWith("CONFIRM"))
			if (new YesNoDialog("Confirmation Required", "Run \"" + this.name + "\" on the selected PKG files?")
				.open() == true)
				pattern = this.commandPattern.substring(7).trim();
			else
				return;

		String[] tokens = createTokens(pkgs, pattern);
		// System.out.println("Created tokens:\n" + Arrays.toString(tokens)); // DEBUG
		if (tokens == null) {
			new ErrorMessage(Display.getCurrent().getActiveShell(), "Could not find required PKGs.");
			// Note: the message means either a required PKG type or files are missing.
			return;
		}

		// Create a list of existing PKG files to later check if they still exist after running the process.
		ArrayList<PS4PKG> existingPkgs = new ArrayList<>();
		for (PS4PKG pkg : pkgs)
			if (!pkg.path.startsWith("ftp://") && Files.exists(Paths.get(pkg.path)))
				existingPkgs.add(pkg);

		Process process; // To make use of the return value, if ever necessary.
		ProcessBuilder pb = new ProcessBuilder(tokens);
		try {
			process = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Remove PKGs from the tab if they don't exist anymore after the process has finished running.
		Thread itemRemover = new Thread(() -> {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}

			for (PS4PKG pkg : existingPkgs)
				if (!Files.exists(Paths.get(pkg.path)))
					tabContent.getDisplay().asyncExec(() -> {
						if (!tabContent.isDisposed())
							tabContent.removeFile(pkg.path);
					});
		});
		itemRemover.setDaemon(true);
		itemRemover.start();
	}
}
