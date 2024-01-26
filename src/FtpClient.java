
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

@SuppressWarnings("serial")
class UnexpectedServerResponse extends IOException {
	public UnexpectedServerResponse(String reply) {
		super(String.format("Server responded with \"%s\"", reply));
	}
}

/** This is a quick, incomplete FTP client implementation for PS4 PKG Manager. */
public class FtpClient {
	private String ctrlAddr;
	int ctrlPort;
	Socket ctrlSocket;
	BufferedReader ctrlReader;
	PrintWriter ctrlWriter;

	String dataAddr;
	int dataPort;
	Socket dataSocket;

	private String lastReply;

	// Return value: 1: Server temporarily unavailable, 0: success
	public int connect(String addr, int port) throws IOException, UnknownHostException, UnexpectedServerResponse {
		if (this.ctrlSocket != null)
			disconnect();
		try {
			this.ctrlAddr = addr;
			this.ctrlPort = port;
			this.ctrlSocket = new Socket(addr, port);
			this.ctrlReader = new BufferedReader(new InputStreamReader(ctrlSocket.getInputStream()));
			this.ctrlWriter = new PrintWriter(new OutputStreamWriter(ctrlSocket.getOutputStream()));
			int ret = getReturnCode();
			if (ret == 120)
				ret = getReturnCode();
			switch (ret) {
				case 421:
					return -1;
				case 220:
					return 0;
				default:
					throw new UnexpectedServerResponse(this.lastReply);
			}
		} catch (IOException e) {
			ctrlSocket.close();
			e.printStackTrace();
			throw e;
		}
	}

	private void write(String s) {
		ctrlWriter.print(s);
	}

	private void send(String... s) {
		for (int i = 0; i < s.length; i++)
			write(s[i]);
		ctrlWriter.print("\r\n");
		ctrlWriter.flush();
	}

	public String getReply() throws IOException {
		String reply;
		while ((reply = ctrlReader.readLine()) != null && reply.length() > 3 && reply.charAt(4) == '-')
			; // Read until the last multiple-line response.
		this.lastReply = reply;
		return reply;
	}

	private int getReturnCode() throws IOException, UnexpectedServerResponse {
		try {
			return Integer.valueOf(getReply().substring(0, 3));
		} catch (StringIndexOutOfBoundsException e) {
			throw new UnexpectedServerResponse(this.lastReply);
		}
	}

	public void login() throws UnexpectedServerResponse, IOException {
		login("anonymous", "anonymous", "noaccount");
	}

	public void login(String userName, String password, String account) throws IOException, UnexpectedServerResponse {
		send(String.format("USER %s", userName));
		int ret = getReturnCode();
		switch (ret) {
			case 230:
				break;
			case 331:
			case 332:
				send(String.format("PASS %s", password));
				ret = getReturnCode();
				switch (ret) {
					case 202:
					case 230:
						break;
					case 332:
						send(String.format("ACCT %s", account));
						ret = getReturnCode();
						switch (ret) {
							case 202:
							case 230:
								break;
							case 530:
							default:
								throw new UnexpectedServerResponse(this.lastReply);
						}
						break;
					default:
						throw new UnexpectedServerResponse(this.lastReply);
				}
				break;
			case 530:
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}
	}

	public void type(boolean binary_mode) throws UnexpectedServerResponse, IOException {
		if (binary_mode == true)
			send("TYPE I");
		else
			send("TYPE A");
		switch (getReturnCode()) {
			case 200:
				return;
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}
	}

	private void pasv() throws UnexpectedServerResponse, IOException {
		send("PASV");
		int ret = getReturnCode();
		switch (ret) {
			case 227:
				int first = 0, last = 0;
				for (int i = 4; i < lastReply.length(); i++)
					if (Character.isDigit(lastReply.charAt(i))) {
						first = i;
						break;
					}
				for (int i = lastReply.length() - 1; i > 3; i--)
					if (Character.isDigit(lastReply.charAt(i))) {
						last = i;
						break;
					}
				try {
					String[] addr = lastReply.substring(first, last + 1).split(",");
					String ip = String.format("%s.%s.%s.%s", addr[0], addr[1], addr[2], addr[3]);
					int port = (Integer.valueOf(addr[4]) << 8) + Integer.valueOf(addr[5]);
					this.dataAddr = ip;
					this.dataPort = port;
				} catch (Exception e) {
					throw new UnexpectedServerResponse(this.lastReply);
				}
				if (this.dataSocket != null && !this.dataSocket.isClosed())
					try {
						dataSocket.close();
					} catch (Exception ignored) {
					}
				this.dataSocket = new Socket(this.dataAddr, this.dataPort);
				return;
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}
	}

	private void retr(String param) throws UnexpectedServerResponse, IOException {
		send("RETR " + param);
		int ret = getReturnCode();
		switch (ret) {
			case 150:
				return;
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}
	}

	// TODO
	public ArrayList<FtpEntry> list(String path) throws UnexpectedServerResponse, IOException {
		pasv();
		send(String.format("LIST %s", path));
		int ret;
		while ((ret = getReturnCode()) == 450)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		ArrayList<FtpEntry> entries = new ArrayList<FtpEntry>();

		switch (ret) {
			// TODO: handle these cases properly and add remaining cases.
			case 150:
				break;
			case 550:
				return entries;
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.dataSocket.getInputStream()))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// System.out.println(line); // DEBUG

				String[] tokens = line.split(" +", 9);
				if (tokens.length != 9)
					continue; // TODO: handle the error properly.

				FtpEntry.Type type = switch (line.charAt(0)) {
					case 'd' -> FtpEntry.Type.DIRECTORY;
					case 'b' -> FtpEntry.Type.BLOCK;
					case 'c' -> FtpEntry.Type.CHARACTER;
					case 'l' -> FtpEntry.Type.SYMBOLIC_LINK;
					case 'p' -> FtpEntry.Type.FIFO;
					case 's' -> FtpEntry.Type.SOCKET;
					case '-' -> FtpEntry.Type.REGULAR;
					default -> throw new UnexpectedServerResponse(line);
				};

				int permissions = 0; // TODO

				int links;
				try {
					links = Integer.parseInt(tokens[1]);
				} catch (NumberFormatException e) {
					links = 0;
				}

				long size;
				try {
					size = Long.parseLong(tokens[4]);
				} catch (NumberFormatException e) {
					size = 0;
				}

				String datetime = String.join(" ", tokens[5], tokens[6], tokens[7]);

				entries.add(new FtpEntry(type, permissions, links, tokens[2] /* owner */, tokens[3] /* group */, size,
					datetime, tokens[8] /* filename */));
			}
		}

		ret = getReturnCode();
		switch (ret) {
			// TODO: remaining cases
			case 226:
				break;
			default:
				throw new UnexpectedServerResponse(this.lastReply);
		}

		return entries;
	}

	/**
	 * Prints the full paths of subdirectories found in the specified directory. For the full paths to be actually full,
	 * currently the directory itself must be an absolute path.
	 */
	public ArrayList<String> getDirectories(String directory, String regex)
		throws UnexpectedServerResponse, IOException {
		ArrayList<FtpEntry> entries = list(directory);
		entries.removeIf(entry -> entry.type != FtpEntry.Type.DIRECTORY || entry.filename.equals(".")
			|| entry.filename.equals("..") || regex != null && !entry.filename.matches(regex));

		ArrayList<String> directories = new ArrayList<>();
		for (FtpEntry entry : entries)
			directories.add(directory + (directory.equals("/") ? "" : "/") + entry.filename);
		return directories;
	}

	public ArrayList<String> getFiles(String directory, String fileExtension)
		throws UnexpectedServerResponse, IOException {
		ArrayList<FtpEntry> entries = list(directory);
		entries.removeIf(entry -> entry.type != FtpEntry.Type.REGULAR || !entry.filename.endsWith(fileExtension));

		ArrayList<String> files = new ArrayList<>();
		for (FtpEntry entry : entries)
			files.add(directory + (directory.equals("/") ? "" : "/") + entry.filename);
		return files;
	}

	/** Downloads a file, saving it to a target. The file's path must be absolute. */
	public void download(String file, int len, String target) throws UnexpectedServerResponse, IOException {
		pasv();
		retr(file);
		try (FileOutputStream output = new FileOutputStream(target)) {
			InputStream input = dataSocket.getInputStream();
			byte[] buffer = new byte[8192];
			int byteCount;

			if (len > 0) {
				int total = 0;
				while ((byteCount = input.read(buffer)) != -1) {
					output.write(buffer, 0, byteCount);
					total += byteCount;
					if (total >= len) {
						dataSocket.close();
						break;
					}
				}
			} else
				while ((byteCount = input.read(buffer)) != -1)
					output.write(buffer, 0, byteCount);
		}

		int ret = getReturnCode();
		switch (ret) {
			case 226:
				return;
			default:
				// System.out.println("Return code after download: " + ret);
		}
	}

	public void download(String file, String target) throws UnexpectedServerResponse, IOException {
		download(file, 0, target);
	}

	/** Downloads the first n bytes of a remote file to a buffer. */
	public byte[] downloadByteArray(String file, int n) throws UnexpectedServerResponse, IOException {
		pasv();
		retr(file);

		byte[] buffer = null;
		try (InputStream input = dataSocket.getInputStream()) {
			buffer = input.readNBytes(n);
			// System.out.println("downloaded size: " + buffer.length);
		} catch (Exception ignore) {
			ignore.printStackTrace();
		} finally {
			try {
				dataSocket.close();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}

		int ret = getReturnCode();

		return buffer;
	}

	public void disconnect() {
		try {
			if (this.ctrlSocket != null)
				ctrlSocket.close();
			if (this.dataSocket != null)
				dataSocket.close();
		} catch (IOException e) {
		}
	}
}
