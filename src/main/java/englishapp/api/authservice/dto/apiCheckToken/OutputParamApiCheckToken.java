package englishapp.api.authservice.dto.apiCheckToken;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutputParamApiCheckToken {
    private String userId;
    private String email;
    private String role;
    private List<String> listBlockEndpoint;
}
