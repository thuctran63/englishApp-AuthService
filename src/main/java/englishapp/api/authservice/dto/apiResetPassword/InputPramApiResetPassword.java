package englishapp.api.authservice.dto.apiResetPassword;

import lombok.Data;

@Data
public class InputPramApiResetPassword {
    private String email;
    private String newPassword;
}
