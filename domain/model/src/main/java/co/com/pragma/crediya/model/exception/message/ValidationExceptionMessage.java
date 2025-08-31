package co.com.pragma.crediya.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValidationExceptionMessage {

    // Usuario
    NAMES_REQUIRED      ("VAL0001", "Nombres obligatorios",                 "400", "El campo 'nombres' es obligatorio."),
    LASTNAMES_REQUIRED  ("VAL0002", "Apellidos obligatorios",               "400", "El campo 'apellidos' es obligatorio."),
    EMAIL_REQUIRED      ("VAL0003", "Correo electrónico obligatorio",       "400", "El campo 'correo_electronico' no puede estar vacío."),
    EMAIL_INVALID       ("VAL0004", "Correo electrónico inválido",          "400", "El correo electrónico no tiene un formato válido."),
    SALARY_REQUIRED     ("VAL0005", "Salario base obligatorio",             "400", "El salario_base es obligatorio."),
    ROL_REQUIRED        ("VAL0006", "Rol obligatorio",                      "400", "Debe indicar el id_rol."),

    // Solicitud
    AMOUNT_REQUIRED     ("VAL0101", "Monto obligatorio",                    "400", "El monto es obligatorio."),
    AMOUNT_INVALID      ("VAL0102", "Monto inválido",                       "400", "El monto debe ser mayor que cero."),
    TERM_INVALID        ("VAL0103", "Plazo inválido",                       "400", "El plazo debe ser mayor que cero."),
    LOAN_TYPE_REQUIRED  ("VAL0104", "Tipo de préstamo obligatorio",         "400", "Debe seleccionar un tipo de préstamo."),
    DOCUMENT_REQUIRED   ("VAL0105", "Documento de identidad obligatorio",   "400", "El documento de identidad es obligatorio.");

    private final String code;
    private final String description;
    private final String itcCode;
    private final String message;
}
