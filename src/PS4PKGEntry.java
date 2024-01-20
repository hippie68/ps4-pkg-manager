import java.io.Serializable;

// See https://www.psdevwiki.com/ps4/Package_Files#Files
public class PS4PKGEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    int id;          // 0x00
    int flags1;      // 0x08
    int flags2;      // 0x0C
    int offset;      // 0x10
    int size;        // 0x14
    String filename;
}
