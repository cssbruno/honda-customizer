package com.cabin.hondacustom;

import java.io.IOException;

/** Direct UART variant of g0/r.f -> g0/a.c. No MCU wrapper or Binder call. */
final class DirectXpSerial {
    static byte[] frame(int profile, byte[] body) {
        if (profile != 0x4012a) throw new IllegalArgumentException("Expected XP 0x4012A");
        int[] bytes = XpPacket.unsigned(body);
        byte[] framed = new byte[bytes.length + 2];
        framed[0] = 0x2e;
        int sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            framed[i + 1] = (byte) bytes[i];
            sum += bytes[i];
        }
        framed[framed.length - 1] = (byte) (sum ^ 0xff);
        return framed;
    }

    static void validateEndpoint(String path, int baud) {
        if (path == null || !path.matches("/dev/tty(?:S|USB|ACM)[0-9]+"))
            throw new IllegalArgumentException("Expected explicit /dev/ttyS…, ttyUSB… or ttyACM…");
        if (baud != 9600 && baud != 19200 && baud != 38400 && baud != 57600 && baud != 115200)
            throw new IllegalArgumentException("Expected explicit baud: 9600, 19200, 38400, 57600 or 115200");
    }

    static int send(int profile, String path, int baud, byte[] body) throws IOException {
        validateEndpoint(path, baud);
        byte[] packet = frame(profile, body);
        System.loadLibrary("honda_serial");
        return writeOnce(path, baud, packet);
    }

    private static native int writeOnce(String path, int baud, byte[] frame) throws IOException;

    static String hex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte b : data) {
            if (out.length() > 0) out.append(' ');
            out.append(String.format(java.util.Locale.ROOT, "%02X", b & 255));
        }
        return out.toString();
    }
}
