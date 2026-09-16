package com.cabin.hondacustom;

import org.junit.Test;
import static org.junit.Assert.*;

public class DirectXpSerialTest {
    @Test public void stockTachPacketMatchesDirectEncoderWithoutMcuRoutingByte() {
        assertEquals("2E C6 02 16 01 20", DirectXpSerial.hex(
            DirectXpSerial.frame(0x4012a, new byte[]{(byte)0xc6, 2, 0x16, 1})));
    }
    @Test public void preservesBodyLengthByteAndWrapsChecksumToByte() {
        assertArrayEquals(new byte[]{0x2e, (byte)0xff, (byte)0xff, 1},
            DirectXpSerial.frame(0x4012a, new byte[]{(byte)0xff, (byte)0xff}));
        assertEquals(66, DirectXpSerial.frame(0x4012a, new byte[64]).length);
        assertThrows(IllegalArgumentException.class, () -> DirectXpSerial.frame(0x4012a, new byte[65]));
    }
    @Test public void rejectsOtherProfilesAndUnspecifiedOrNonSerialPaths() {
        for (int profile : new int[]{0, 0x3012a, 0x5012a, 0x40141})
            assertThrows(IllegalArgumentException.class, () -> DirectXpSerial.frame(profile, new byte[]{1}));
        for (String path : new String[]{null, "", "/dev/ttyS0/../ttyS1", "/tmp/out", "/dev/ttyS0\n", "/dev/ttyMbx3"})
            assertThrows(IllegalArgumentException.class, () -> DirectXpSerial.validateEndpoint(path, 19200));
        assertThrows(IllegalArgumentException.class, () -> DirectXpSerial.validateEndpoint("/dev/ttyS1", 0));
        DirectXpSerial.validateEndpoint("/dev/ttyS1", 19200);
    }
}
