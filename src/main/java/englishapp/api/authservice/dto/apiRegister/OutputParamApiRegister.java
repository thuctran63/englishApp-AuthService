package englishapp.api.authservice.dto.apiRegister;

import lombok.Data;

@Data
public class OutputParamApiRegister {
    private String userName;
    private String accessToken;
    private String refreshToken;
}
