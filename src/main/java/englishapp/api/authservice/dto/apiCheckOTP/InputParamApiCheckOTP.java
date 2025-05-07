package englishapp.api.authservice.dto.apiCheckOTP;

import lombok.Data;

@Data
public class InputParamApiCheckOTP {
    private String email;
    private String otpCode;
}
