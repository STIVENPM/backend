# Módulo de Pagos: Wompi Widget (Nequi)

Base URL local: `http://localhost:8081`. Los endpoints de inicio y consulta requieren `Authorization: Bearer <JWT>`; el webhook es exclusivo de Wompi y no lleva JWT.

## Endpoints

| Método y ruta | Acceso | Uso |
|---|---|---|
| `POST /api/pagos/reserva/{idReserva}` | USER dueño o ADMIN | Crea el pago pendiente y devuelve la configuración segura del Widget. |
| `GET /api/pagos/reserva/{idReserva}` | USER dueño o ADMIN | Consulta el estado persistido del pago. |
| `POST /api/pagos/webhook` | Wompi | URL configurada en Dashboard; el backend valida su checksum. |

No se envía body ni monto al iniciar un pago: el backend obtiene el precio del servicio de la reserva. Solo se admite una reserva `PENDIENTE` y un único pago por reserva. ADMIN puede iniciar el pago presencialmente, pero el pago queda asociado a la reserva y al cliente propietario, nunca al ADMIN.

## Abrir el Widget

Incluye una vez `https://checkout.wompi.co/widget.js`. Tras un `201 Created`, usa la respuesta así:

```js
const config = await api.post(`/api/pagos/reserva/${idReserva}`);
const checkout = new WidgetCheckout({
  currency: config.moneda,
  amountInCents: config.montoEnCentavos,
  reference: config.referencia,
  publicKey: config.publicKey,
  signature: { integrity: config.firmaIntegridad },
  redirectUrl: config.redirectUrl
});
checkout.open(() => {
  // La respuesta del Widget solo es informativa. Consultar el backend o refrescar la vista.
});
```

La respuesta contiene únicamente datos públicos/seguros para el Widget. No contiene secreto de integridad, secreto de eventos ni llave privada. `montoEnCentavos` es el precio COP multiplicado por 100, como exige Wompi.

## Estado y UX

El resultado definitivo siempre llega por webhook. En la pantalla de resultado, consulta `GET /api/pagos/reserva/{idReserva}` hasta que `estado` sea `aprobado` o `rechazado`; no confirmes una reserva usando el callback ni el `id` de la redirección. Al aprobarse, el backend pasa la reserva de `PENDIENTE` a `ASIGNADA`.

Ejemplo de consulta:

```json
{
  "idPago": "b0e5d06e-66ce-42e1-a4af-0b7f71a849ac",
  "idReserva": "9b431b11-4606-4c29-8926-b5b5768d32af",
  "referenciaPago": "PAGO-...",
  "metodoPago": "online",
  "monto": 35000,
  "estadoWompi": "APPROVED",
  "estado": "aprobado",
  "fechaPago": "2026-09-03T14:00:00"
}
```

## Nequi y Sandbox

Este módulo procesa exclusivamente eventos cuyo `payment_method_type` sea `NEQUI`. Si Wompi envía un evento con firma válida pero otro método, el backend lo ignora sin modificar pago ni reserva y responde `2xx`, evitando reintentos. Habilita solamente Nequi para el comercio en el Dashboard de Wompi: la documentación vigente del Widget no publica una opción de configuración por instancia para ocultar los otros medios. En sandbox usa una llave `pub_test_`; los números Nequi `3991111111` y `3992222222` simulan aprobado y rechazado, respectivamente.

Configura en Wompi la URL pública `POST https://<tu-dominio>/api/pagos/webhook`. No apuntes el Dashboard a `localhost`; usa un túnel HTTPS durante desarrollo. La validación de checksum ya se realiza en backend.
