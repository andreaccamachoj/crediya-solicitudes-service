package co.com.pragma.crediya.model.solicitud;

import lombok.Value;

@Value
public class SolicitudPageRequest {
    Integer page;
    Integer size;

    public int offset() { return Math.max(page, 0) * Math.max(size, 1); }
}
