import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Version {
    public static int currentVersion = 1; // TODO: To be updated automatically by the (private) "make_release" script.
    private static String currentVersionUrl = "https://github.com/hippie68/ps4-pkg-manager/releases/tag/%23"
        + currentVersion;
    private static String newVersionUrl = "https://github.com/hippie68/ps4-pkg-manager/releases/tag/%23"
        + (currentVersion + 1);
    public static String releasesUrl = "https://github.com/hippie68/ps4-pkg-manager/releases";

    // Return values: 0: update found, 1: no update found, 2: unknown error, -1: GitHub broke the update check.
    public static int checkForUpdates() {
        try (Scanner scanner = new Scanner(new URL(newVersionUrl).openStream(), StandardCharsets.UTF_8.toString())) {
            return 0;
        } catch (FileNotFoundException e) {
            // Check if the update mechanism is broken by testing the current release.
            try (Scanner scanner = new Scanner(new URL(currentVersionUrl).openStream(),
                StandardCharsets.UTF_8.toString())) {
                return 1; // Still works, no new update available.
            } catch (IOException e2) {
                return -1; // Broken.
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 2;
        }
    }

    public static String convertReturnValueToString(int ret) {
        return switch (ret) {
            case -1 -> "The update mechanism seems to be broken.\nPlease use the link below to check manually.";
            case 0 -> "A new update is available.";
            case 1 -> "No update found.";
            case 2 -> "Unknown error (see console output).";
            default -> "Unknown return value.";
        };
    }
}
