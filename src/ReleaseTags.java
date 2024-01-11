import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReleaseTags {
    // Hard-coded lists of release tags.
    private static final String[] hardCodedReleaseGroups = { "AUGETY", "BigBlueBox", "BlaZe", "CAF", "DarKmooN",
        "DUPLEX", "GCMR", "HOODLUM", "iNTERNAL", "JRP", "KOTF", "LeveLUp", "LiGHTFORCE", "MarvTM", "MOEMOE", "PiKMiN",
        "Playable", "PRELUDE", "PROTOCOL", "RESPAWN", "SharpHD", "TCD", "UNLiMiTED", "WaLMaRT", "WaYsTeD" };
    private static final String[] hardCodedReleases = { "Arczi", "CyB1K", "Fugazi", "Golemnight", "High Speed",
        "OPOISSO893", "RetroGamer_74", "SeanP2500", "TKJ13", "VikaCaptive", "Whitehawkx" };

    // This maps strings found in original release group file names to full release group names.
    public static final Map<String, String> releaseGroupsMap = Map.of("blz", "BlaZe", "lfc", "LiGHTFORCE", "moe-",
        "MOEMOE");
    // Same, but for regular releases.
    public static final Map<String, String> releasesMap = Map.of("fxd", "Fugazi");

    // Combined list of hard-coded and user-provided release tags that can be extended at run-time (by users via the
    // Settings dialog).
    private static ArrayList<String> combinedReleaseGroups = new ArrayList<>();
    private static ArrayList<String> combinedReleases = new ArrayList<>();
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    private static Lock readLock = lock.readLock();
    private static Lock writeLock = lock.writeLock();

    public static String[] getHardCodedReleaseGroups() {
        return hardCodedReleaseGroups;
    }

    public static String[] getHardCodedReleases() {
        return hardCodedReleases;
    }

    // Combines hard-coded tags with user-provided tags.
    // TODO: any simpler way?
    public static void addReleaseTags(String[] releaseGroups, String[] releases) {
        writeLock.lock();

        combinedReleaseGroups = new ArrayList<>(List.of(hardCodedReleaseGroups));
        loop: for (String userGroup : releaseGroups) {
            if (userGroup.isEmpty()) // Happens in GUI:loadGUIState while splitting if the string is empty.
                continue;
            // Before adding, check if the user made a capitalization correction.
            for (int i = 0; i < hardCodedReleaseGroups.length; i++) {
                if (hardCodedReleaseGroups[i].toLowerCase().equals(userGroup.toLowerCase())) {
                    combinedReleaseGroups.set(i, userGroup); // Override with user tag.
                    continue loop;
                }
            }

            combinedReleaseGroups.add(userGroup);
        }

        combinedReleases = new ArrayList<>(List.of(hardCodedReleases));
        loop: for (String userRelease : releases) {
            if (userRelease.isEmpty())
                continue;
            for (int i = 0; i < hardCodedReleases.length; i++) {
                if (hardCodedReleases[i].toLowerCase().equals(userRelease.toLowerCase())) {
                    combinedReleases.set(i, userRelease);
                    continue loop;
                }
            }

            combinedReleases.add(userRelease);
        }

        writeLock.unlock();
    }

    public static ArrayList<String> getJoinedReleaseGroups() {
        try {
            readLock.lock();
            return combinedReleaseGroups;
        } finally {
            readLock.unlock();
        }
    }

    public static ArrayList<String> getJoinedReleases() {
        try {
            readLock.lock();
            return combinedReleases;
        } finally {
            readLock.unlock();
        }
    }
}
