package co.com.pragma.crediya.model.solicitud;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudPageResponse<T> {
    List<T> content;
    long totalElements;
    int page;
    int size;
}
