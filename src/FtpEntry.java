

import java.util.ArrayList;

public class FtpEntry {
    static enum Type {
        DIRECTORY, BLOCK, CHARACTER, SYMBOLIC_LINK, FIFO, SOCKET, REGULAR
    }

    Type type;
    int permissions;
    int links;
    String owner;
    String group; // Can be characters or a number.
    long size;
    String datetime;
    String filename;

    public FtpEntry(Type type, int permissions, int links, String owner, String group, long size, String datetime,
        String filename) {    
        this.type = type; 
        this.permissions = permissions;
        this.links = links;
        this.owner = owner;
        this.group = group;
        this.size = size;
        this.datetime = datetime;
        this.filename = filename;
    }

    public static ArrayList<FtpEntry> getFiles(ArrayList<FtpEntry> list) {
        ArrayList<FtpEntry> newList = new ArrayList<FtpEntry>();
        for (FtpEntry entry : list)
            if (entry.type == Type.REGULAR)
                newList.add(entry);
        return newList;
    }
}
