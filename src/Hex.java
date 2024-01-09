public class Hex {
    static final char[] hexTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            hexChars[i * 2] = hexTable[(bytes[i] & 0xF0) >> 4];
            hexChars[i * 2 + 1] = hexTable[bytes[i] & 0x0F];
        }
        return new String(hexChars);
    }
}