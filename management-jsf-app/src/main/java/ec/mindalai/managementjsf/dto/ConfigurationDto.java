package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationDto {

    private UUID id;
    private String configKey;
    private String configValue;
    private String valueType;
    private String category;
    private String description;
    private boolean editable;
    private UUID updatedBy;
    private Instant updatedAt;
}
