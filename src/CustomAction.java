import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomAction {
    String name;
    String commandPattern;

    // Known pattern variables; the order matters: singular, more specific variables come first.
    private static String[] patternVariables = new String[] { "%dir%", "%app%", "%patch%", "%dlc%", "%file%",
        "%files%" };

    public CustomAction(String name, String commandPattern) {
        this.name = name;
        this.commandPattern = commandPattern;
    }

    // Used by the GUI to reject saving command patterns that are invalid.
    public static boolean patternIsValid(String pattern) {
        if ((pattern.contains("%file%") || pattern.contains("%app%") || pattern.contains("%patch%")
            || pattern.contains("%dlc%"))
            && (pattern.contains("%files%") || pattern.contains("%apps%") || pattern.contains("%patches%")
                || pattern.contains("%dlcs%")))
            return false;

        return true;
    }

    private String createEscapedPath(String path) {
        path = path.replace(" ", "\\ ");
        path = path.replace("'", "\\'");
        path = path.replace("&", "\\&");
        path = path.replace(";", "\\;");
        path = path.replace("(", "\\(");
        path = path.replace(")", "\\)");
        path = path.replace("[", "\\[");
        path = path.replace("]", "\\]");
        // TODO: probably many more exotic cases.

        return path;
    }

    // Helper function for createTokens().
    // Returns the next PKG in a list of PKGs that is of the type specified by <patternVariable>.
    private PS4PKG getNextPkg(String patternVariable, List<PS4PKG> pkgs) {
        if (pkgs.isEmpty())
            return null;

        switch (patternVariable) {
            case "%dir%":
                return pkgs.get(0);
            case "%app%":
                for (PS4PKG pkg : pkgs) {
                    String category = pkg.getSFOValue("CATEGORY");
                    if (category == null)
                        continue;
                    if (category.startsWith("gd"))
                        return pkg;
                }
                return null;
            case "%patch%":
                for (PS4PKG pkg : pkgs) {
                    String category = pkg.getSFOValue("CATEGORY");
                    if (category == null)
                        continue;
                    if (category.startsWith("gp"))
                        return pkg;
                }
                return null;
            case "%dlc%":
                for (PS4PKG pkg : pkgs) {
                    String category = pkg.getSFOValue("CATEGORY");
                    if (category == null)
                        continue;
                    if (category.equals("ac"))
                        return pkg;
                }
                return null;
            case "%file%":
            case "%files%":
                return pkgs.get(0);
            default:
                return null;
        }
    }

    // Helper function for run().
    // Splits the user-provided command line into tokens that can be properly parsed by the operating system.
    private String[] createTokens(PS4PKG pkgs[]) {
        List<String> tokens = new ArrayList<>();
        List<PS4PKG> pkgList = new ArrayList<PS4PKG>(List.of(pkgs));

        // Create tokens.
        for (int i = 0; i < commandPattern.length(); i++) {
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
                    break;
                // Substrings delimited by spaces.
                default:
                    int start2 = i;
                    do {
                        i++;
                    } while (i < commandPattern.length() && commandPattern.charAt(i) != ' ');
                    tokens.add(commandPattern.substring(start2, i));
            }
        }

        // Replace all PKG variables inside all tokens.
        for (int i = 0; i < tokens.size(); i++) {
            String token;
            for (String variable : patternVariables) {
                while ((token = tokens.get(i)).contains(variable)) {
                    PS4PKG pkg = getNextPkg(variable, pkgList);
                    if (pkg == null) {
                        // Allow plural variables to exhaust the list.
                        if (variable.equals("%files%")) {
                            tokens.set(i, token.replace(variable, ""));
                            continue;
                        }

                        return null; // For singular variables, not having a matching PKG is an error.
                    }

                    if (variable.equals("%dir%"))
                        tokens.set(i, token.replace(variable, createEscapedPath(pkg.directory)));
                    else if (variable.equals("%files%"))
                        tokens.set(i, token.replace(variable, createEscapedPath(pkg.path) + " %files%"));
                    else
                        tokens.set(i, token.replace(variable, createEscapedPath(pkg.path)));
                    pkgList.remove(pkg);
                }
            }
        }

        return tokens.toArray(String[]::new);
    }

    public void run(PS4PKG pkgs[]) {
        String[] tokens = createTokens(pkgs);
        // System.out.println("Created tokens:\n" + Arrays.toString(tokens)); // DEBUG
        if (tokens == null)
            return;

        Process process; // To make use of the return value, if ever necessary.
        try {
            process = Runtime.getRuntime().exec(tokens);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
