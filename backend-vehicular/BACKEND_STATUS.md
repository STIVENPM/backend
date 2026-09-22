# Estado del backend

Revision posterior a la correccion Wompi: 2026-09-21. Alcance verificado: backend Java 17, Spring Boot 4.0.5, PostgreSQL, pruebas aisladas y documentacion del contrato frontend. No se modifico el frontend ni se hicieron cobros o cambios sobre una base real.

## Resultado

La integracion conserva Wompi Widget/Checkout como unico canal de pago y ya no esta limitada a Nequi. El backend genera monto, referencia y firma; el Widget decide los metodos disponibles para el comercio. La confirmacion sigue dependiendo de un webhook firmado o de una consulta administrativa por un ID de transaccion previamente verificado.

| Hallazgo de la auditoria | Estado actual | Evidencia principal |
| --- | --- | --- |
| Filtro exclusivo de `NEQUI` | Corregido | `PagoService` acepta el `payment_method_type` informado y las pruebas cubren `NEQUI`, `CARD` y `PSE`. |
| `metodoPagoPermitido="NEQUI"` | Corregido | El campo se elimino de `PagoWidgetResponseDTO`; no se sustituyo por un valor artificial. |
| Pago aprobado cambiaba reserva a `ASIGNADA` | Corregido | Pagos no modifica estados operativos. `ASIGNADA` queda bajo el flujo real de asignaciones. |
| Sin moneda, ambiente o ID | Corregido | Se exige `COP`, ambiente esperado, ID, referencia, monto, metodo y estado reconocido antes de persistir. |
| Sin metodo real ni ID persistido | Corregido | `PagoIntento` guarda `wompiTransactionId`, `wompiPaymentMethodType`, ambiente y estado. |
| Reintentos perdian historial | Corregido | Un `Pago` logico por reserva tiene multiples `PagoIntento`, cada uno con referencia unica. |
| Eventos tardios degradaban aprobaciones | Corregido | Los estados aprobados son monotónicos; fecha de aprobacion no se reemplaza. |
| Posible doble cobro no detectado | Corregido | Una aprobacion adicional queda como `aprobado_duplicado`, se registra como incidencia y no repite efectos. |
| Sin recuperacion de confirmaciones | Parcialmente corregido | Ruta `ADMIN` consulta `GET /v1/transactions/{id}`. Sin ID local no existe busqueda oficial por referencia implementable. |

## Modelo y concurrencia

- `pagos` conserva el pago logico unico por reserva, el canal `online`, monto y estado financiero agregado.
- `pago_intentos` conserva referencia, ID de transaccion, metodo real, ambiente, estado Wompi y fechas por intento.
- El inicio bloquea pesimisticamente la reserva y el pago. Un intento pendiente valido se reutiliza; un pago aprobado retorna conflicto.
- El webhook bloquea el intento y el pago. La BD refuerza unicidad de referencia, transaccion por ambiente y un solo intento pendiente por pago.
- Una transaccion no puede asociarse a otro intento. Las aprobaciones tardias de reservas canceladas se conservan y registran sin reactivar la reserva.
- No hay reembolsos automaticos ni cobros por API directa.

## Webhook

`POST /api/pagos/webhook` conserva el checksum SHA-256 construido con las rutas y el orden dinamico de `signature.properties`, seguido de `timestamp` y el secreto de eventos.

Antes de actualizar valida:

- evento `transaction.updated` y objeto `data.transaction`;
- referencia registrada;
- monto exacto en centavos y moneda `COP`;
- ambiente `test`/`prod` coherente con configuracion;
- ID de transaccion, metodo no vacio y estado reconocido;
- identidad previa del intento y ausencia de asociacion cruzada.

Eventos firmados irrelevantes, duplicados o de otro ambiente reciben `200`. Firma invalida recibe `401`; estructura/datos incompatibles `422`; conflictos de identidad `409`; errores de BD `503`. Un fallo interno no se convierte deliberadamente en exito, por lo que Wompi puede reintentar.

## Rutas de pagos

| Metodo | Ruta | Acceso | Respuesta principal |
| --- | --- | --- | --- |
| `POST` | `/api/pagos/reserva/{idReserva}` | Propietario o `ADMIN` | `201` intento nuevo, `200` intento reutilizado, `409` conflicto. |
| `GET` | `/api/pagos/reserva/{idReserva}` | Propietario o `ADMIN` | Pago e historial de intentos. |
| `POST` | `/api/pagos/reserva/{idReserva}/reconciliar` | `ADMIN` | Consulta Wompi por IDs locales; no crea transacciones. |
| `POST` | `/api/pagos/webhook` | Wompi, sin JWT | Procesamiento autenticado por checksum. |

Contrato completo y ejemplos: `WOMPI_FRONTEND_CONTRACT.md`.

## Base de datos

Script: `src/main/resources/db/migration/V20260921_01__wompi_payment_attempts.sql`.

Debe aplicarse antes de arrancar esta version porque `spring.jpa.hibernate.ddl-auto=validate` se mantiene. En la misma sesion PostgreSQL:

```sql
SET app.wompi_environment = 'test'; -- usar 'prod' solo para datos de produccion
\i src/main/resources/db/migration/V20260921_01__wompi_payment_attempts.sql
```

El script corre en una transaccion, detecta referencias nulas/duplicadas y estados incompatibles antes del DDL, migra cada pago existente y conserva las columnas legadas `referencia_pago` y `estado_wompi`. No fue ejecutado sobre ninguna BD.

## Configuracion

Variables requeridas, con ejemplos ficticios en `application-example.properties`:

- `WOMPI_ENVIRONMENT=sandbox` o `production`;
- `WOMPI_PUBLIC_KEY=pub_test_...`;
- `WOMPI_PRIVATE_KEY=prv_test_...`, solo para consulta backend;
- `WOMPI_INTEGRITY_SECRET=test_integrity_...`;
- `WOMPI_EVENTS_SECRET=test_events_...`.

El backend valida que los prefijos correspondan al ambiente y no registra secretos. Los valores `prod_*` se exigen en produccion.

## Pruebas

Comando ejecutado:

```powershell
.\mvnw.cmd test
```

Resultado: `BUILD SUCCESS`; 19 pruebas, 0 fallos, 0 errores, 0 omitidas.

Cobertura agregada:

- aprobacion `NEQUI`, `CARD` y `PSE` sin asignacion automatica;
- monto, moneda y ambiente incorrectos;
- duplicado y evento tardio tras aprobacion;
- reintento tras rechazo con nueva referencia;
- reutilizacion del intento pendiente bajo lock y bloqueo tras aprobacion;
- acceso de un usuario al pago ajeno;
- aprobacion tardia de reserva cancelada y deteccion de posible cobro duplicado;
- reconciliacion verificada y error del proveedor;
- checksum valido/invalido, header/body y secreto ausente.

Las pruebas usan mocks; no acceden a Wompi ni a PostgreSQL. Falta una prueba de integracion real de constraints/locks contra PostgreSQL aislado.

## Pendientes externos

1. Aplicar y revisar la migracion en una copia/backup de la BD antes del despliegue.
2. Verificar en sandbox los metodos realmente habilitados para el comercio; el codigo no los supone.
3. Configurar en Dashboard una URL HTTPS publica para `transaction.updated` y la URL de retorno.
4. Validar con transacciones sandbox el ID recibido, reintentos de webhook y reconciliacion por llave privada.
5. Definir el procedimiento humano para `aprobado_duplicado` y pagos aprobados sobre reservas canceladas. No se implementaron reembolsos automaticos.

## Fuentes oficiales

- Widget/Checkout: https://docs.wompi.co/docs/colombia/widget-checkout-web/
- Eventos y checksum: https://docs.wompi.co/docs/colombia/eventos/
- Consulta por ID y estados: https://docs.wompi.co/docs/colombia/transacciones/
