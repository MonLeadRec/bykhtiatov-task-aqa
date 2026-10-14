package support;

import java.security.SecureRandom;

public final class Tokens {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    private Tokens() {
    }

    // 32 символа из [0-9A-F] — то, что валидатор пропускает.
    public static String random() {
        char[] buf = new char[32];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = HEX[RND.nextInt(HEX.length)];
        }
        return new String(buf);
    }
}
