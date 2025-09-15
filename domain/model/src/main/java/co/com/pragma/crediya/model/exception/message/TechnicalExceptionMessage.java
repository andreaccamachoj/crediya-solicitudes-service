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
    ),
    USER_SERVICE_ERROR(
        "TEC0002",
                "Error consultando el microservicio de usuarios",
                "502",
                "Hubo un problema al comunicarse con el servicio de autenticación."
    ),
    UNEXPECTED_ERROR(
        "TEC0003",
                "Error inesperado en el sistema",
                "500",
                "Ocurrió un error inesperado procesando la solicitud."
    );

    private final String code;
    private final String description;
    private final String itcCode;
    private final String message;
}
