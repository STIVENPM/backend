# Diagnostico local de pagos Wompi

Fecha de verificacion: 2026-09-21 (America/Bogota).

## Revalidacion 2026-09-22 con Dev Tunnel activo

- Backend confirmado en `http://localhost:8081`: `GET /v3/api-docs` responde `200`.
- Frontend confirmado en `http://localhost:5173`: responde `200`.
- Tunel comprobado desde Internet: `https://6z439rzm-8081.use2.devtunnels.ms/v3/api-docs` responde `200`, sin `WWW-Authenticate`. La raiz publica responde `403` por la seguridad normal del backend; el tunel no exige una autenticacion intermedia.
- URL de webhook registrada y confirmada visualmente en Wompi Sandbox: `https://6z439rzm-8081.use2.devtunnels.ms/api/pagos/webhook`.
- Prueba sintetica no destructiva: un `transaction.updated` con firma deliberadamente invalida enviado por el tunel recibio `401`.
- La tabla `pago_intentos` tenia 2 filas antes y despues de esa prueba, con la misma ultima fecha de actualizacion (`2026-09-21 18:34:04.058373`): el evento invalido no modifico pagos.
- Las cuatro credenciales locales estan presentes y sus prefijos corresponden a Sandbox. El backend se inicio despues de la ultima modificacion del archivo local de Wompi.
- La validacion oficial de la llave publica en `GET https://sandbox.wompi.co/v1/merchants/info`, usando `x-merchant-public-key`, sigue respondiendo `404`. Por ello no se inicio una transaccion Sandbox real ni se pidio al usuario introducir datos de pago.
- Pruebas backend reejecutadas: `19` pruebas, `0` fallos, `BUILD SUCCESS`.
- No hubo navegador controlable disponible en esta sesion para inspeccionar Network/Console. La URL guardada en Wompi quedo confirmada por captura; la apertura del Widget y la entrega de un evento real siguen pendientes.

## Resultado ejecutivo

El webhook vacio no impide abrir el Widget y no es el punto donde se corta este flujo. Hay evidencia de que dos clics recientes llegaron al backend y crearon intentos, pero ninguno obtuvo un ID de transaccion Wompi. La causa de configuracion confirmada es que la llave publica local no es reconocida actualmente por Wompi Sandbox:

- la llave tiene el prefijo esperado `pub_test_`;
- `https://checkout.wompi.co/widget.js` responde `200` desde este equipo;
- la consulta oficial `GET https://sandbox.wompi.co/v1/merchants/info`, enviando la llave solo en `x-merchant-public-key`, responde `404 NOT_FOUND_ERROR` con motivo `La entidad solicitada no existe`;
- el endpoint anterior `GET /v1/merchants/{llave}` tambien respondio `404`.

Una llave publica inexistente, copiada de otro entorno o ya rotada deja al Widget sin un comercio Sandbox valido. Debe reemplazarse por la llave Sandbox vigente del comercio. No se modificaron ni se imprimen credenciales en este informe.

La evidencia disponible no permite afirmar que el navegador alcanzo `WidgetCheckout.open()` ni capturar la excepcion exacta: el conector de navegador de esta sesion no expuso Edge ni permitio abrir una pestaña. Network y Console siguen siendo la comprobacion final despues de corregir la llave.

## Cadena observada

| Punto | Resultado |
| --- | --- |
| Boton `Cobrar` | El codigo lo conecta con `cobrar()` y bloquea dobles envios. |
| Servicio HTTP | Usa `POST /pagos/reserva/{idReserva}` sobre `http://localhost:8081/api`; Axios agrega `Authorization: Bearer <JWT>`. |
| Backend | Dos intentos se persistieron a las 18:32:59 y 18:34:04, por lo que al menos esos flujos alcanzaron y confirmaron el POST. El estado HTTP exacto visto por el navegador no esta disponible. |
| Reservas usadas | `7712d781-a674-4d3b-bcdb-163febb884a6` y `ee57f2d5-bbc2-421c-ab11-c80eb40a2b6f`: ambas `PENDIENTE`, con monto igual al precio del servicio. |
| Respuesta esperada | El DTO y OpenAPI del proceso activo exponen `idPago`, `idIntento`, `idReserva`, `referencia`, `montoEnCentavos`, `moneda`, `publicKey`, `firmaIntegridad`, `redirectUrl` y `reutilizado`. |
| SDK | La URL oficial responde `200 text/javascript`; el cargador usa una unica etiqueta, timeout de 15 segundos y permite reintentar. |
| Configuracion del Widget | Coincide con la documentacion oficial: `currency`, `amountInCents`, `reference`, `publicKey`, `signature.integrity` y `redirectUrl`. |
| Comercio Wompi | La llave publica configurada no es reconocida por la API Sandbox (`404 NOT_FOUND_ERROR`). |
| Transaccion | Los dos intentos siguen `pendiente`, ambiente `test` y sin `wompi_transaction_id`; no existe evidencia de transaccion creada. |

Conclusión: el flujo comprobado llega al backend y se interrumpe antes de crear/completar una transaccion Wompi. La llave publica no reconocida es un bloqueo suficiente y confirmado. Tras reemplazarla, Network/Console debe confirmar si queda un segundo defecto del navegador.

## Base de datos real

La configuracion cargada apunta a PostgreSQL 16.11 en `localhost:5432`, base `LavaRapido_Vehicular`, usuario configurado y esquema `public`. Las consultas se ejecutaron en solo lectura y no mostraron la contraseña.

`public.pago_intentos` coincide con `V20260921_01__wompi_payment_attempts.sql`:

- 11 columnas con tipos, longitudes, nulabilidad y timestamps compatibles con `PagoIntento`;
- PK `pago_intentos_pkey`;
- FK `fk_pago_intentos_pago` a `pagos(id_pago) ON DELETE RESTRICT`;
- UNIQUE `uq_pago_intentos_referencia`;
- checks de ambiente, estado local y estado Wompi;
- indices `idx_pago_intentos_pago`, `idx_pago_intentos_transaccion`, `uq_pago_intentos_transaccion_environment` y `uq_pago_intentos_pendiente_por_pago`;
- sin intentos huerfanos.

`spring.jpa.hibernate.ddl-auto=validate` es compatible: una instancia de comprobacion conecto a esta base e inicializo el `EntityManagerFactory`. No pudo enlazar `8081` porque el backend del IDE ya estaba escuchando alli; no se reemplazo ni se detuvo ese proceso.

Hallazgo de datos: hay 10 pagos y 2 intentos; 8 pagos historicos pendientes no tienen intento. Sus referencias legadas son no vacias y no estan duplicadas. Se agrego el backfill incremental e idempotente [V20260921_02__backfill_missing_wompi_payment_attempts.sql](src/main/resources/db/migration/V20260921_02__backfill_missing_wompi_payment_attempts.sql). No fue aplicado. Antes de ejecutarlo se debe revisar un backup y, en la misma sesion:

```sql
SET app.wompi_environment = 'test';
\i src/main/resources/db/migration/V20260921_02__backfill_missing_wompi_payment_attempts.sql
```

El script solo inserta pagos sin ningun intento, conserva referencias/estados, aborta ante colisiones y no crea ni altera objetos.

## Arranque, seguridad y configuracion

- Backend real: proceso Java del IDE en `8081`, perfil Spring `default`, PostgreSQL `LavaRapido_Vehicular/public`.
- Frontend real: Vite en `http://localhost:5173`; `VITE_API_URL` cae por defecto en `http://localhost:8081/api` si no se define.
- CORS: un preflight real desde `Origin: http://localhost:5173` hacia el POST de pagos respondio `200` con ese origen, metodos requeridos, headers `authorization, content-type` y credenciales habilitadas.
- JWT: el POST sin JWT respondio `403`. Inicio/consulta requieren autenticacion y el servicio valida propietario o `ADMIN`; reconciliacion exige `ADMIN`. No se desactivo ningun control.
- Wompi: `sandbox`; las cuatro credenciales requeridas estan presentes y sus prefijos son coherentes con `test`. La llave publica no es reconocida por Sandbox. La pertenencia mutua de llave privada y secretos al mismo comercio solo puede confirmarse en el Dashboard; los prefijos no la prueban.
- Redirect: el backend genera `http://localhost:5173/pagos/resultado`, que coincide con la ruta React real.
- Webhook: es publico solo respecto de JWT, pero valida checksum. Un evento de diagnostico con firma invalida respondio `401`; no se relajo la validacion.

## Correccion pendiente de configuracion

1. En el Dashboard de Wompi, activar el modo Sandbox del mismo comercio.
2. En Desarrollo/Programadores, copiar nuevamente la llave publica, llave privada, secreto de integridad y secreto de eventos Sandbox del mismo conjunto. Si alguna llave fue rotada, actualizar las cuatro segun corresponda.
3. Sustituirlas solo en `application-wompi-local.properties` o variables de entorno del backend. Nunca llevar la llave privada ni los secretos al frontend.
4. Reiniciar el backend: los properties se cargan al arrancar.
5. Verificar la llave publica sin imprimirla. El resultado esperado es HTTP `200`, no `404`:

```powershell
$p = @{}
Get-Content .\application-wompi-local.properties | ForEach-Object {
  if ($_ -match '^([^#=]+)=(.*)$') { $p[$matches[1].Trim()] = $matches[2].Trim() }
}
curl.exe -sS -o NUL -w "HTTP %{http_code}`n" `
  https://sandbox.wompi.co/v1/merchants/info `
  -H ("x-merchant-public-key: " + $p['wompi.public-key'])
```

No se puede escribir automaticamente la correccion porque el valor valido debe obtenerse del Dashboard del comercio y es secreto de configuracion del usuario.

## Prueba local completa

1. Mantener backend en `8081` y frontend en `5173`. Iniciar sesion como `ADMIN` con las credenciales existentes.
2. Crear o elegir una reserva `PENDIENTE` cuyo precio sea un entero positivo. No aprobar ni editar pagos directamente en PostgreSQL.
3. Abrir DevTools antes del clic. En Network conservar el log y filtrar por `pagos` y `widget.js`; en Console conservar errores.
4. Pulsar `Cobrar` una sola vez. Debe observarse:
   - `POST http://localhost:8081/api/pagos/reserva/{idReserva}` con JWT;
   - `201 Created` para intento nuevo o `200 OK` para reutilizado;
   - los diez campos del contrato, sin valores vacios;
   - `widget.js` con `200`;
   - apertura del modal por `WidgetCheckout.open()`.
5. Si el POST falla, conservar status y body: `401` sesion, `403` acceso, `404` reserva, `409` estado/precio/pago, `500` configuracion. Si el POST funciona pero no abre, copiar la primera excepcion de Console y la peticion Wompi fallida; no repetir el POST.
6. Completar una prueba Sandbox con los datos oficiales. No confirmar el pago localmente desde el callback; esperar al webhook y consultar `GET /api/pagos/reserva/{idReserva}`.
7. Verificar que el GET cambie a `aprobado` o `rechazado`, incluya el ID/metodo/estado real y mantenga separado el estado operativo de la reserva.

## Webhook local con tunel HTTPS

No hay `ngrok` ni `cloudflared` disponible en `PATH`; no se instalo ni se abrio un tunel y no se recibio un evento real. Tras instalar `cloudflared` por decision del usuario, el comando oficial de Quick Tunnel para este backend es:

```powershell
cloudflared tunnel --url http://localhost:8081
```

Si el ejecutable esta en el directorio actual y no en `PATH`, usar `.\cloudflared.exe tunnel --url http://localhost:8081`. Debe permanecer ejecutandose durante toda la prueba. Copiar el dominio aleatorio que imprime y configurar en Wompi Sandbox:

```text
https://<dominio-del-tunel>/api/pagos/webhook
```

Si cambia el dominio temporal, actualizar la URL de Eventos en Wompi. Configurar el evento `transaction.updated`. El tunel expone solamente HTTP del backend; no exponer PostgreSQL.

El webhook y el redirect cumplen funciones distintas:

- Wompi servidor -> URL publica HTTPS del webhook: confirma el estado financiero con firma.
- Navegador del mismo PC -> `http://localhost:5173/pagos/resultado`: vuelve a la interfaz y consulta el backend; no confirma el pago.

Para una prueba en el mismo PC, el redirect local es viable mientras Vite siga activo en `5173`. Wompi no necesita resolver ese `localhost`; lo abre el navegador del usuario.

Fuentes oficiales: [Widget de Wompi](https://docs.wompi.co/docs/colombia/widget-checkout-web/), [ambientes y llaves](https://docs.wompi.co/docs/colombia/ambientes-y-llaves/), [tokens y endpoint de comercio](https://docs.wompi.co/docs/colombia/tokens-de-aceptacion/), [eventos](https://docs.wompi.co/docs/colombia/eventos/) y [Quick Tunnels de Cloudflare](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/).

## Verificaciones ejecutadas

| Verificacion | Resultado |
| --- | --- |
| Tests backend con mocks (`.\mvnw.cmd test`) | 19/19 correctos. |
| Tests frontend con mocks | 14/14 correctos. |
| TypeScript frontend | Correcto. |
| Build frontend | Correcto; advertencia no bloqueante por chunk principal mayor a 500 kB. |
| PostgreSQL real | Conexion, esquema, constraints, indices y conteos comprobados en solo lectura. |
| `ddl-auto=validate` real | Entidades validadas contra PostgreSQL; la segunda instancia no tomo `8081` porque ya estaba ocupado. |
| Backend/frontend locales | `GET /v3/api-docs` y raiz Vite respondieron `200`. |
| CORS y rechazo sin JWT | Preflight `200`; POST sin token `403`. |
| Firma webhook invalida | `401`. |
| SDK desde este equipo | `200 text/javascript`. |
| Llave publica contra Sandbox | `404 NOT_FOUND_ERROR`; requiere correccion en configuracion. |
| Apertura del Widget | No observada: falta Network/Console de una pestaña accesible tras corregir la llave. |
| Pago Sandbox y webhook real | No probados; no se abrio tunel ni se creo una transaccion. |

No se modifico el repositorio frontend, no se aplico SQL y no se creo, aprobo ni altero artificialmente ningun pago.
