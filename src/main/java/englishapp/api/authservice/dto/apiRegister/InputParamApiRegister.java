package englishapp.api.authservice.dto.apiRegister;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputParamApiRegister {
    private String userName;
    private String email;
    private String password;
}
