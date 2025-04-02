package englishapp.api.authservice.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Mã hóa mật khẩu
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // 12 là độ mạnh của salt
    }

    // Kiểm tra mật khẩu
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
