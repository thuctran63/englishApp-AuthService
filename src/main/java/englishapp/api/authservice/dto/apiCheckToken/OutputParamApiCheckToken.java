package englishapp.api.authservice.dto.apiCheckToken;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutputParamApiCheckToken {
    private String userId;
    private String email;
}
