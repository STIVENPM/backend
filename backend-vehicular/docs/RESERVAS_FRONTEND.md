# Módulo de Reservas: referencia para frontend

Base URL local: `http://localhost:8081`. Todas las rutas requieren JWT. La app móvil usa creación, detalle, historial propio y cancelación propia; el panel web ADMIN usa listado, detalle, creación presencial, cambio de estado y cancelación.

## 1. Endpoints disponibles

| Método y ruta | Acceso efectivo | Uso |
|---|---|---|
| `POST /api/reservas` | USER autenticado para su vehículo; ADMIN para cualquier vehículo | Ambos. El ADMIN registra reservas presenciales para el dueño real del vehículo. |
| `GET /api/reservas` | ADMIN | Panel web: supervisión completa de turnos. |
| `GET /api/reservas/{id}` | ADMIN o USER dueño de la reserva | Ambos. |
| `GET /api/reservas/usuario/{id}` | ADMIN o USER cuyo id coincide con `{id}` | Web para historial de cliente; móvil para historial propio. |
| `PATCH /api/reservas/{id}/estado?estado={ESTADO}` | ADMIN | Panel web: asignar, iniciar o finalizar. |
| `PATCH /api/reservas/{id}/cancelar` | ADMIN o USER dueño, si el estado permite cancelar | Ambos; no elimina la fila. |

No existe rol `OPERATOR` en las reglas actuales: solo `ADMIN` puede cambiar estado. Un usuario autenticado que no sea ADMIN recibe 403 para el listado y para `PATCH .../estado`.

## 2. Contratos

### Crear — `POST /api/reservas`

| Campo | Tipo | Requerido | Regla |
|---|---|---:|---|
| `fkIdVehiculo` | UUID | Sí | Debe existir. USER debe ser dueño; ADMIN puede usar cualquier vehículo. |
| `fkIdServicio` | UUID | Sí | Debe existir. |
| `fechaReserva` | `YYYY-MM-DD` | Sí | No hay validación de fecha pasada en el código actual. |
| `horaReserva` | `HH:mm` o `HH:mm:ss` | Sí | Debe estar entre 06:00 y 20:00; el fin estimado por duración del servicio no puede superar las 20:00. |

```json
{
  "fkIdVehiculo": "37ee5630-748c-4d96-af2a-7dd6cd2fc097",
  "fkIdServicio": "2c0a099b-f3f3-4d68-a4f0-ed8623d7c88f",
  "fechaReserva": "2026-09-10",
  "horaReserva": "10:00:00"
}
```

Respuesta: `201 Created` con `ReservaResponseDTO`. Aunque un ADMIN cree la reserva, `idUsuario` y `nombreUsuario` corresponden al dueño del vehículo, no al ADMIN.

### Listar, detalle e historial

- `GET /api/reservas` devuelve `200 OK` con un arreglo de `ReservaResponseDTO`. No tiene filtros, paginación ni orden garantizado.
- `GET /api/reservas/{id}` devuelve `200 OK` con una reserva. El servicio valida dueño o ADMIN.
- `GET /api/reservas/usuario/{id}` devuelve `200 OK` con un arreglo ordenado por fecha y hora descendentes. El USER solo puede solicitar su UUID; ADMIN puede solicitar cualquiera.

### Cambiar estado — `PATCH /api/reservas/{id}/estado?estado={ESTADO}`

Solo ADMIN. No lleva body. El parámetro `estado` es obligatorio y debe ser exacto, por ejemplo:

```http
PATCH /api/reservas/b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8/estado?estado=EN_PROCESO
```

Devuelve `200 OK` con `ReservaResponseDTO`. Al cambiar a `EN_PROCESO` se fija `fechaHoraInicio`; al cambiar a `FINALIZADA`, `fechaHoraFin`, usando la hora del servidor.

### Cancelar — `PATCH /api/reservas/{id}/cancelar`

No lleva body. USER dueño o ADMIN pueden llamarlo. Devuelve `200 OK` con `ReservaResponseDTO` y `estado: "CANCELADA"`. No hay borrado físico. Solo es válida desde `PENDIENTE` o `ASIGNADA`.

### Respuesta exitosa — `ReservaResponseDTO`

| Campo | Tipo |
|---|---|
| `idReserva`, `idUsuario`, `idVehiculo`, `idServicio` | UUID |
| `nombreUsuario`, `placaVehiculo`, `tipoVehiculo`, `nombreServicio`, `descripcionServicio` | texto; `tipoVehiculo` y `descripcionServicio` pueden ser `null` |
| `precioServicio` | decimal JSON |
| `duracionServicio` | entero en minutos |
| `fechaReserva` | `YYYY-MM-DD` |
| `horaReserva` | hora ISO |
| `fechaHoraInicio`, `fechaHoraFin`, `createdAt`, `updatedAt` | fecha-hora ISO-8601; inicio/fin pueden ser `null` |
| `estado` | valor de `EstadoReserva` |

```json
{
  "idReserva": "b6a7ebd5-a2f1-49fe-9c12-8c68842d52e8",
  "idUsuario": "e8a71d0f-f65b-441c-9f33-88113fd5a9f2",
  "nombreUsuario": "Ana Martínez",
  "idVehiculo": "37ee5630-748c-4d96-af2a-7dd6cd2fc097",
  "placaVehiculo": "ABC123",
  "tipoVehiculo": "AUTOMOVIL",
  "idServicio": "2c0a099b-f3f3-4d68-a4f0-ed8623d7c88f",
  "nombreServicio": "Lavado general",
  "descripcionServicio": "Lavado exterior e interior del vehículo",
  "precioServicio": 35000.00,
  "duracionServicio": 60,
  "fechaReserva": "2026-09-10",
  "horaReserva": "10:00:00",
  "fechaHoraInicio": "2026-09-10T10:02:11",
  "fechaHoraFin": null,
  "estado": "EN_PROCESO",
  "createdAt": "2026-09-01T14:20:11",
  "updatedAt": "2026-09-10T10:02:11"
}
```

### Errores

Las excepciones atendidas por `GlobalExceptionHandler` usan este formato:

```json
{
  "error": "Reserva no encontrada"
}
```

| Situación | HTTP | Ejemplos de mensaje |
|---|---:|---|
| Recurso inexistente (`RecursoNoEncontradoException`) | 404 | `Reserva no encontrada`, `Vehículo no encontrado`, `Servicio no encontrado`, `Usuario no encontrado` |
| Request inválido, horario, solapamiento o transición inválida | 400 | `El vehículo es obligatorio`, `La hora de la reserva debe estar entre 06:00 y 20:00`, `Ya existe una reserva solapada para ese vehículo en ese horario` |
| Vehículo de otro usuario al crear como USER | 400 | `El vehículo no pertenece al usuario autenticado` |
| Acceso a reserva/historial ajeno desde el servicio | 403 | `No tienes permiso para acceder a esta reserva` o `No tienes permiso para consultar las reservas de este usuario` |
| JWT inválido o expirado | 401 | Texto plano: `Token invalido` |
| USER intentando ruta exclusiva ADMIN | 403 | Respuesta del filtro de Spring Security; no tiene formato `{ "error": ... }` personalizado. |

## 3. Estados y transiciones

Los valores deben enviarse en mayúsculas:

| Estado actual | Transiciones válidas |
|---|---|
| `PENDIENTE` | `PENDIENTE`, `ASIGNADA`, `CANCELADA` |
| `ASIGNADA` | `ASIGNADA`, `EN_PROCESO`, `CANCELADA` |
| `EN_PROCESO` | `EN_PROCESO`, `FINALIZADA` |
| `FINALIZADA` | Ninguna |
| `CANCELADA` | Ninguna |

La web debe mostrar asignar/cancelar para `PENDIENTE`, iniciar/cancelar para `ASIGNADA`, finalizar para `EN_PROCESO` y ninguna acción para estados finales. Repetir el mismo estado es aceptado.

## 4. Autenticación

Incluye el JWT obtenido en login en cada llamada:

```http
Authorization: Bearer <jwt>
```

El frontend debe tratar `401` como sesión expirada o token inválido y limpiar/redirigir a login. Para `403`, mostrar falta de permisos y no reintentar.

## 5. Flujo típico del panel ADMIN

1. Cargar turnos: `GET /api/reservas`; mostrar estado, fecha/hora, placa, cliente, servicio, duración y precio.
2. Abrir detalle: `GET /api/reservas/{id}`; mostrar todos los campos del DTO y las marcas de inicio/fin.
3. Ejecutar acción: `PATCH /api/reservas/{id}/estado?estado=ASIGNADA`, luego `EN_PROCESO` y finalmente `FINALIZADA`. Sustituir en la UI la respuesta recibida.
4. Para registrar atención presencial: elegir un vehículo y servicio existentes y llamar `POST /api/reservas`. La reserva queda en el historial del dueño del vehículo.
5. Si se anula antes de iniciar: `PATCH /api/reservas/{id}/cancelar`.

No hay actualización en tiempo real: tras un cambio, actualiza el elemento con la respuesta o vuelve a pedir el listado.

## 6. Pendientes y limitaciones

- No hay filtros por fecha, estado, vehículo o usuario en el listado ADMIN.
- No hay paginación, orden configurable, WebSocket ni SSE.
- No hay validación de que `fechaReserva` sea futura.
- El solapamiento compara intervalos `[inicio, fin)` usando la duración configurada del servicio. Las reservas canceladas no bloquean horario.
- No hay endpoint para editar fecha, hora, vehículo o servicio de una reserva existente.
- Los errores de autorización generados por Spring Security no tienen body JSON personalizado.
