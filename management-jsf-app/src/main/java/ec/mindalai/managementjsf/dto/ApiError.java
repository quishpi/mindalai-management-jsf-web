package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** Error normalizado del API de plataforma ({@code {code, status, message, path, correlationId, details}}). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private int status;
    private String code;
    private String message;
    private String path;
    private String correlationId;
    private List<Map<String, String>> details;

    public boolean hasDetails() {
        return details != null && !details.isEmpty();
    }
}
