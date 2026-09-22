# Contrato frontend para pagos Wompi

Contrato vigente desde la migracion `V20260921_01__wompi_payment_attempts.sql`. El frontend abre Wompi Widget con datos calculados por el backend. No envia el monto, no confirma pagos y no limita los metodos: el Widget muestra los habilitados para el comercio.

## Autorizacion y rutas

| Metodo y ruta | Acceso | Resultado |
| --- | --- | --- |
| `POST /api/pagos/reserva/{idReserva}` | Propietario o `ADMIN` con JWT | Crea un intento (`201`) o reutiliza el pendiente (`200`). |
| `GET /api/pagos/reserva/{idReserva}` | Propietario o `ADMIN` con JWT | Devuelve el pago logico y todo su historial de intentos. |
| `POST /api/pagos/reserva/{idReserva}/reconciliar` | Solo `ADMIN` con JWT | Consulta en Wompi los intentos pendientes que ya tengan ID de transaccion. |
| `POST /api/pagos/webhook` | Publica para Wompi; sin JWT | Valida `X-Event-Checksum` o `signature.checksum`. No debe invocarla el frontend. |

En las rutas autenticadas se usa `Authorization: Bearer <JWT>`.

## Iniciar o reutilizar un intento

`POST /api/pagos/reserva/9b431b11-4606-4c29-8926-b5b5768d32af` no recibe body. El monto sale del precio del servicio asociado a la reserva.

Respuesta ficticia `201 Created`:

```json
{
  "idPago": "b0e5d06e-66ce-42e1-a4af-0b7f71a849ac",
  "idIntento": "38ee8b27-6fb8-4fe3-8e2f-b24ee55168aa",
  "idReserva": "9b431b11-4606-4c29-8926-b5b5768d32af",
  "referencia": "PAGO-4f7f4342-f96a-4e10-8a97-a58944559423",
  "montoEnCentavos": 3500000,
  "moneda": "COP",
  "publicKey": "pub_test_ejemplo_no_real",
  "firmaIntegridad": "hash_sha256_ficticio",
  "redirectUrl": "http://localhost:5173/pagos/resultado",
  "reutilizado": false
}
```

Si ya existe un intento `pendiente`, retorna `200 OK`, la misma referencia y `reutilizado: true`. El frontend debe abrir el Widget con esa referencia; no debe pedir referencias nuevas repetidamente.

Conflictos `409`: reserva fuera de `PENDIENTE`, precio cambiado frente al monto fijado o pago ya aprobado. Tambien pueden retornar `403` por acceso cruzado, `404` si no existe la reserva y `500` si la configuracion Wompi es invalida.

## Abrir el Widget

Incluir una vez `https://checkout.wompi.co/widget.js` y usar exactamente los datos de inicio:

```js
const checkout = new WidgetCheckout({
  currency: pago.moneda,
  amountInCents: pago.montoEnCentavos,
  reference: pago.referencia,
  publicKey: pago.publicKey,
  signature: { integrity: pago.firmaIntegridad },
  redirectUrl: pago.redirectUrl
});

checkout.open(() => {
  // Resultado visual informativo: refrescar GET, nunca aprobar localmente.
});
```

No se envia `paymentMethod` ni una lista local. Wompi decide que opciones presenta segun la configuracion real del comercio.

## Consultar el estado

Respuesta ficticia de `GET /api/pagos/reserva/{idReserva}`:

```json
{
  "idPago": "b0e5d06e-66ce-42e1-a4af-0b7f71a849ac",
  "idReserva": "9b431b11-4606-4c29-8926-b5b5768d32af",
  "metodoPago": "online",
  "monto": 35000,
  "estado": "aprobado",
  "fechaPago": "2026-09-21T14:35:20",
  "intentoActual": {
    "idIntento": "38ee8b27-6fb8-4fe3-8e2f-b24ee55168aa",
    "referencia": "PAGO-4f7f4342-f96a-4e10-8a97-a58944559423",
    "wompiTransactionId": "1292-1602113476-10985",
    "wompiPaymentMethodType": "CARD",
    "wompiStatus": "APPROVED",
    "wompiEnvironment": "test",
    "estado": "aprobado",
    "fechaConfirmacion": "2026-09-21T14:35:20",
    "createdAt": "2026-09-21T14:32:10"
  },
  "intentos": []
}
```

`intentos` contiene todos los intentos en orden de creacion, incluido `intentoActual`; se abrevio a `[]` en el ejemplo. El metodo real es texto abierto informado por Wompi, por ejemplo `NEQUI`, `CARD` o `PSE`; el frontend no debe mantener una lista cerrada para validarlo.

Estados del pago logico: `pendiente`, `rechazado` y `aprobado`. Un rechazo terminal permite otro intento; una aprobacion bloquea intentos nuevos.

Estados locales del intento: `pendiente`, `rechazado`, `aprobado` y `aprobado_duplicado`. Este ultimo exige revision por posible doble cobro; no implica reembolso automatico.

Un pago aprobado no cambia por si solo la reserva a `ASIGNADA`. Esa transicion ocurre al crear una asignacion real. Una reserva cancelada tampoco se reactiva si recibe una aprobacion tardia.

## Cambios frente al contrato anterior

- Eliminado: `metodoPagoPermitido`.
- Eliminados de la raiz de consulta: `referenciaPago` y `estadoWompi`.
- Agregados al inicio: `idIntento` y `reutilizado`.
- Agregados a la consulta: `intentoActual` e `intentos`, con referencia, ID de transaccion, metodo real, estado Wompi y ambiente por intento.
- El redirect y el callback del Widget siguen siendo solo informativos; la pantalla consulta el backend hasta un estado final.

## Reconciliacion y limites

La reconciliacion es administrativa y no crea cobros. Retorna `409` cuando no existe un intento pendiente con `wompiTransactionId`, `422` si Wompi devuelve datos que no coinciden y `502` si el proveedor no responde. Wompi solo documenta consulta por ID; si se perdio todo webhook y nunca se persistio ese ID, esta ruta no puede localizar la transaccion por referencia.

Referencias oficiales consultadas el 2026-09-21:

- https://docs.wompi.co/docs/colombia/widget-checkout-web/
- https://docs.wompi.co/docs/colombia/eventos/
- https://docs.wompi.co/docs/colombia/transacciones/

## Pendiente de validacion externa

- Probar en sandbox un pago aprobado y rechazado por cada metodo habilitado realmente para el comercio.
- Confirmar en Dashboard las URLs HTTPS de eventos y redireccion, y que `transaction.updated` este activo.
- Verificar en Dashboard que llaves y secretos correspondan al mismo ambiente; el backend valida sus prefijos.
- Probar reintentos del webhook y reconciliacion con un ID sandbox real, sin datos de produccion.
