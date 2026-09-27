package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/** Operador de plataforma. El selector de responsables de tickets usa id + fullName. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUserDto {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private List<String> roles;

    public String displayName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return username;
    }
}
