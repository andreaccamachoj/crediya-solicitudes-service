package co.com.pragma.crediya.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessExceptionMessage {
    // Usuario
    SALARY_OUT_OF_RANGE      ("BUS0001", "Salario fuera de rango",                "409", "El salario_base está fuera del rango permitido."),
    EMAIL_ALREADY_REGISTERED ("BUS0002", "Correo ya registrado",                  "409", "El correo electrónico ya se encuentra registrado."),
    ROLE_NOT_FOUND           ("BUS0003", "Rol no encontrado",                     "404", "El rol indicado no existe."),

    // Solicitud
    LOAN_TYPE_NOT_FOUND      ("BUS0101", "Tipo de préstamo no encontrado",        "404", "El tipo de préstamo seleccionado no existe."),
    STATE_NOT_FOUND          ("BUS0102", "Estado no encontrado",                  "404", "El estado requerido no existe en el sistema."),
    USER_NOT_FOUND           ("BUS0103", "Usuario no encontrado",                 "404", "No existe un usuario asociado a los datos suministrados."),
    USER_ID_REQUIRED          ("BUS0104", "idUsuario no recibido",                  "404", "El idUsuario no fue recibido"),
    USER_SERVICE_ERROR           ("BUS0105", "Error al consumir microservicio",                 "404", "Se produjo un error al consumir el microservicio de usuarios"),
    UNAUTHORIZED("BUS0008","Unauthorized","404",
            "Usuario inautorizado."),
    ROL_NOT_FOUND("BUS0009","Rol not found","404",
            "Este rol no tiene permitido crear solicitudes."),
    USER_IDENTITY_MISMATCH(
            "AST0010",
            "Usuario autenticado no coincide",
            "403",
            "No puede crear una solicitud en nombre de otro usuario. Debe usar su propia identidad."
    ),
    ROL_NOT_ASESOR("BUS0009","Rol not found","404",
            "Este rol no tiene permitido listar las solicitudes.");

    private final String code;
    private final String description;
    private final String itcCode;
    private final String message;
}
