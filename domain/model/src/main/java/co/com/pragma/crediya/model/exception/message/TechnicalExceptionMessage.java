package co.com.pragma.crediya.model.exception.message;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalExceptionMessage {
    ERROR_LIST(
            "TEC0001",
                    "Ocurrio un error al moment de lista solicitudes",
                    "500",
                    "Error interno al consultar solicitudes."
    );

    private final String code;
    private final String description;
    private final String itcCode;
    private final String message;
}
