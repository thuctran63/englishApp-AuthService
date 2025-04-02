package englishapp.api.authservice.dto.apiLogin;

import lombok.Data;

@Data
public class OutputParamApiLogin {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String userName;
    private String email;
    private int typeUser;
}
