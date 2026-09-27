package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Error normalizado del API de plataforma ({@code {code, status, message, path, correlationId, details}}).
 *
 * <p>{@code details} es deliberadamente permisivo: el backend lo usa como lista de
 * {@code {field, message}} en errores de validacion y como texto con el codigo del conflicto
 * (por ejemplo {@code TENANT_DUPLICATED_RUC}). Tiparlo como lista descartaba el mensaje completo
 * del error y el operador acababa viendo un "Error 409" generico.</p>
 */
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
    private Object details;

    public boolean hasDetails() {
        return details != null;
    }

    /** Codigo de detalle cuando el backend lo envia como texto, no como lista. */
    public String detailText() {
        return details instanceof String text ? text : null;
    }
}
