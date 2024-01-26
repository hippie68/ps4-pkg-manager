import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// See https://www.psdevwiki.com/ps4/Package_Files#File_Header
// TODO: change name to "PS4PKGHeader" if this class ever needs to increase its serialVersionUID.
class Header implements Serializable { // TODO: make it "inline class" once future LTS Java versions have that feature.
	private static final long serialVersionUID = 1L;
	static final int MAGIC = 0x7f434e54;
	int type;            		// 0x004
	int file_count;      		// 0x00C
	int entry_count;     		// 0x010
	short sc_entry_count;		// 0x014
	int table_offset;     		// 0x018
	int entry_data_size;  		// 0x01C
	long body_offset;     		// 0x020
	long body_size;       		// 0x028
	long content_offset; 		// 0x030
	long content_size;   		// 0x038
	String content_id;     		// 0x040 (C string, 36 bytes)
	int drm_type;         		// 0x070
	int content_type;     		// 0x074
	int content_flags;    		// 0x078
	int promote_size;     		// 0x07C
	int version_date;     		// 0x080
	int version_hash;   		// 0x084
	int iro_tag;         		// 0x098
	int drm_type_version;       // 0x09C
	String digest_entries_1;    // 0x100
	String digest_entries_2;    // 0x120
	String digest_table_digest; // 0x140
	String digest_body_digest;  // 0x160
	// ...
	int pfs_image_count;        // 0x404
	long pfs_image_flags;       // 0x408
	long pfs_image_offset;      // 0x410
	long pfs_image_size;        // 0x418
	long mount_image_offset;    // 0x420
	long mount_image_size;      // 0x428
	long pkg_size;              // 0x430
	int pfs_signed_size;        // 0x438
	int pfs_cache_size;         // 0x43C
	String pfs_image_digest;    // 0x440
	String pfs_signed_digest;   // 0x460
	long pfs_split_size_nth_0;  // 0x480
	long pfs_split_size_nth_1;  // 0x488
	// ...
	String pkg_digest;          // 0xFE0
}

// See https://www.psdevwiki.com/ps4/Param.sfo
class SFOParameter implements Serializable {
	private static final long serialVersionUID = 1L;
	String name;
	String value;
}

public class PS4PKG implements Serializable {
	private static final long serialVersionUID = 1L;
	// TODO: add access modifiers (doing that is compatible with deserialization).
	String path;
	String directory;
	String filename;
	Header header;
	PS4PKGEntry[] entries;
	SFOParameter[] params;
	String changelog;
	String digests[];

	public PS4PKG(String path) throws Exception {
		Path p = Paths.get(path);
		if (!Files.isRegularFile(p))
			throw new Exception("Not a regular file");

		this.path = path;
		this.directory = Paths.get(path).getParent().toString();
		this.filename = Paths.get(path).getFileName().toString();

		try (var file = new RandomAccessFile(path, "r"); var channel = file.getChannel()) {
			MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_ONLY, 0,
				channel.size() > Integer.MAX_VALUE ? Integer.MAX_VALUE : channel.size());
			load(mbb);
		}
	}

	public PS4PKG(byte[] buffer) throws Exception {
		ByteBuffer bb = ByteBuffer.wrap(buffer);
		load(bb);
	}

	private void load(ByteBuffer bb) throws Exception {
		loadHeader(bb);
		loadEntries(bb);
		loadDigests(bb);
		loadParamSFO(bb);
		loadChangelog(bb);
	}

	private void loadHeader(ByteBuffer bb) throws IOException {
		if (bb.getInt(0) != Header.MAGIC)
			throw new IOException("Invalid magic number");
		this.header = new Header();

		header.type = bb.getInt(0x004);
		header.file_count = bb.getInt(0x00C);
		header.entry_count = bb.getInt(0x010);
		header.sc_entry_count = bb.getShort(0x014);
		header.table_offset = bb.getInt(0x018);
		header.entry_data_size = bb.getInt(0x01C);
		header.body_offset = bb.getLong(0x020);
		header.body_size = bb.getLong(0x028);
		header.content_offset = bb.getLong(0x030);
		header.content_size = bb.getLong(0x038);

		byte[] content_id = new byte[36];
		bb.get(0x040, content_id);
		header.content_id = new String(content_id, "UTF-8");

		header.drm_type = bb.getInt(0x070);
		header.content_type = bb.getInt(0x074);
		header.content_flags = bb.getInt(0x078);
		header.promote_size = bb.getInt(0x07C);
		header.version_date = bb.getInt(0x080);
		header.version_hash = bb.getInt(0x084);
		header.iro_tag = bb.getInt(0x098);
		header.drm_type_version = bb.getInt(0x09C);

		byte[] digest = new byte[32];
		bb.get(0x100, digest);
		header.digest_entries_1 = Hex.byteArrayToHexString(digest);
		bb.get(0x120, digest);
		header.digest_entries_2 = Hex.byteArrayToHexString(digest);
		bb.get(0x140, digest);
		header.digest_table_digest = Hex.byteArrayToHexString(digest);
		bb.get(0x160, digest);
		header.digest_body_digest = Hex.byteArrayToHexString(digest);

		header.pfs_image_count = bb.getInt(0x404);
		header.pfs_image_flags = bb.getLong(0x408);
		header.pfs_image_offset = bb.getLong(0x410);
		header.pfs_image_size = bb.getLong(0x418);
		header.mount_image_offset = bb.getLong(0x420);
		header.mount_image_size = bb.getLong(0x428);
		// ...
		header.pkg_size = bb.getLong(0x430);
		header.pfs_signed_size = bb.getInt(0x438);
		header.pfs_cache_size = bb.getInt(0x43C);

		bb.get(0x440, digest);
		header.pfs_image_digest = Hex.byteArrayToHexString(digest);
		bb.get(0x460, digest);
		header.pfs_image_digest = Hex.byteArrayToHexString(digest);

		header.pfs_split_size_nth_0 = bb.getLong(0x480);
		header.pfs_split_size_nth_1 = bb.getLong(0x488);

		bb.get(0xFE0, digest);
		header.pkg_digest = Hex.byteArrayToHexString(digest);
	}

	private void loadEntries(ByteBuffer bb) throws UnsupportedEncodingException {
		byte[] filename_table = null;

		// Read entries
		entries = new PS4PKGEntry[header.entry_count];
		for (int i = 0; i < header.entry_count; i++) {
			bb.position(header.table_offset + 0x20 * i);

			entries[i] = new PS4PKGEntry();
			entries[i].id = bb.getInt();
			int filename_offset = bb.getInt();
			entries[i].flags1 = bb.getInt();
			entries[i].flags2 = bb.getInt();
			entries[i].offset = bb.getInt();
			entries[i].size = bb.getInt();

			// DEBUG
			// System.out.printf("entry.id: 0x%08x\n", entries[i].id);
			// System.out.printf("entry.filename_offset: 0x%08x\n", filename_offset);
			// System.out.printf("entry.flags1: 0x%08x\n", entries[i].flags1);
			// System.out.printf("entry.flags2: 0x%08x\n", entries[i].flags2);
			// System.out.printf("entry.offset: 0x%08x\n", entries[i].offset);
			// System.out.printf("entry.size: 0x%08x\n", entries[i].size);

			// Read filename table
			if (entries[i].id == 0x200) {
				filename_table = new byte[entries[i].size];
				bb.get(entries[i].offset, filename_table);
			}

			// Read entry's filename
			if (filename_offset > 0) {
				assert filename_table != null;
				int strlen = 0;
				for (int j = filename_offset; filename_table[j] != 0; j++, strlen++)
					;
				entries[i].filename = new String(filename_table, filename_offset, strlen, "UTF-8");
			}
		}
	}

	private void loadDigests(ByteBuffer bb) {
		ByteBuffer file = getFile(0x1, bb);
		int digest_offset = file.getInt(0x10);
		this.digests = new String[this.header.entry_count];

		byte[] bytes = new byte[32];
		for (int i = 0; i < header.entry_count; i++) {
			file.position(digest_offset + i * 32);
			file.get(bytes);
			digests[i] = Hex.byteArrayToHexString(bytes);
		}
	}

	private void loadParamSFO(ByteBuffer bb) throws UnsupportedEncodingException {
		ByteBuffer sfo = getFile(0x1000, bb);
		if (sfo == null)
			return;

		sfo.order(ByteOrder.LITTLE_ENDIAN);
		sfo.position(0x08);
		int keyTableOffset = sfo.getInt();
		int dataTableOffset = sfo.getInt();
		int entryCount = sfo.getInt();
		this.params = new SFOParameter[entryCount];
		byte[] arr = sfo.array();
		for (int i = 0; i < entryCount; i++) {
			short keyOffset = sfo.getShort();
			short fmt = sfo.getShort();
			int len = sfo.getInt();
			int maxLen = sfo.getInt();
			int dataOffset = sfo.getInt();

			this.params[i] = new SFOParameter();

			// TODO: see if this can be reduced.
			int strlen = 0;
			for (int j = keyTableOffset + keyOffset; arr[j] != 0; j++, strlen++)
				;
			params[i].name = new String(arr, keyTableOffset + keyOffset, strlen, "UTF-8");

			if (fmt == 0x404)
				params[i].value = String.format("0x%08X", sfo.getInt(dataTableOffset + dataOffset));
			else
				params[i].value = new String(arr, dataTableOffset + dataOffset, len - 1, "UTF-8");
		}
	}

	private void loadChangelog(ByteBuffer bb) throws IOException {
		ByteBuffer changelog = getFile(0x1260, bb);
		if (changelog == null)
			return;

		this.changelog = new String(changelog.array(), "UTF-8");
	}

	/** Searches for a file that has the specified ID and returns it as a ByteBuffer. */
	private ByteBuffer getFile(int FileID, ByteBuffer bb) {
		for (int i = 0; i < header.entry_count; i++)
			if (entries[i].id == FileID)
				try {
					byte[] ba = new byte[entries[i].size];
					bb.get(entries[i].offset, ba);
					return ByteBuffer.wrap(ba);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
		return null;
	}

	public String getChangelogVersion() {
		String version = null;

		if (this.changelog != null) {
			int index = 0;
			while ((index = changelog.indexOf("app_ver=\"", index)) != -1) {
				index += 9;
				String patchVersion = changelog.substring(index, index + 5);
				if (version == null)
					version = patchVersion;
				else if (patchVersion.compareTo(version) > 0)
					version = patchVersion;
			}
		}

		return version;
	}

	public String getSFOValue(String key) {
		if (this.params == null)
			return "ERROR";
		for (var param : params)
			if (param.name.equals(key))
				return param.value;
		return null;
	}

	public String getCompatibilityChecksum() {
		if (header.content_type == 0x1B)
			return null;

		int target_id;
		switch (this.header.content_flags & 0x0F000000) {
			case 0x0A000000:
				target_id = 0x1001;
				break;
			case 0x02000000:
				target_id = 0x1008;
				break;
			default:
				return null;
		}

		for (int i = 0; i < header.entry_count; i++)
			if (this.entries[i].id == target_id)
				return digests[i];

		return "ERROR";
	}

	/** Returns true if the PKG file still exists. */
	public boolean exists() {
		return Files.exists(Paths.get(this.path));
	}
}
