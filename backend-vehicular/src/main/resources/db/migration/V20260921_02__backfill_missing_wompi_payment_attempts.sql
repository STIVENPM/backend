-- Backfill incremental para bases donde pago_intentos ya existe pero la
-- migracion inicial no copio todos los pagos historicos.
--
-- NO ejecutar a ciegas. Antes, en la misma sesion, definir el ambiente:
--   SET app.wompi_environment = 'test'; -- esta instalacion local usa Sandbox
--   -- o 'prod' unicamente si los datos pertenecen a produccion.
--
-- El script no crea ni altera tablas o indices. Es idempotente por fk_id_pago
-- y aborta antes del INSERT si los datos faltantes no se pueden migrar sin
-- colisiones o perdida de trazabilidad.

BEGIN;

LOCK TABLE public.pagos IN SHARE MODE;
LOCK TABLE public.pago_intentos IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    configured_environment text := current_setting('app.wompi_environment', true);
BEGIN
    IF configured_environment IS NULL OR configured_environment NOT IN ('test', 'prod') THEN
        RAISE EXCEPTION 'Defina app.wompi_environment como test o prod antes de ejecutar el backfill';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagos p
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos i WHERE i.fk_id_pago = p.id_pago
        )
          AND (p.referencia_pago IS NULL OR btrim(p.referencia_pago) = '')
    ) THEN
        RAISE EXCEPTION 'Hay pagos sin intento y sin referencia_pago; deben conciliarse manualmente';
    END IF;

    IF EXISTS (
        SELECT p.referencia_pago
        FROM public.pagos p
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos i WHERE i.fk_id_pago = p.id_pago
        )
        GROUP BY p.referencia_pago
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay referencias duplicadas entre los pagos que requieren backfill';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagos p
        JOIN public.pago_intentos i ON i.referencia = p.referencia_pago
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos own_i WHERE own_i.fk_id_pago = p.id_pago
        )
    ) THEN
        RAISE EXCEPTION 'Una referencia historica ya pertenece a otro intento';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagos p
        JOIN public.pago_intentos i ON i.id_intento = p.id_pago
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos own_i WHERE own_i.fk_id_pago = p.id_pago
        )
    ) THEN
        RAISE EXCEPTION 'Un id_pago historico ya esta usado como id_intento de otro pago';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagos p
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos i WHERE i.fk_id_pago = p.id_pago
        )
          AND p.estado::text NOT IN ('pendiente', 'aprobado', 'rechazado')
    ) THEN
        RAISE EXCEPTION 'Hay estados de pago no soportados por el backfill';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.pagos p
        WHERE NOT EXISTS (
            SELECT 1 FROM public.pago_intentos i WHERE i.fk_id_pago = p.id_pago
        )
          AND p.estado_wompi IS NOT NULL
          AND upper(p.estado_wompi) NOT IN ('PENDING', 'APPROVED', 'DECLINED', 'VOIDED', 'ERROR')
    ) THEN
        RAISE EXCEPTION 'Hay estados Wompi no soportados por el backfill';
    END IF;
END $$;

INSERT INTO public.pago_intentos (
    id_intento,
    fk_id_pago,
    referencia,
    wompi_status,
    wompi_environment,
    estado,
    fecha_confirmacion,
    created_at,
    updated_at
)
SELECT
    p.id_pago,
    p.id_pago,
    p.referencia_pago,
    CASE
        WHEN upper(p.estado_wompi) IN ('PENDING', 'APPROVED', 'DECLINED', 'VOIDED', 'ERROR')
            THEN upper(p.estado_wompi)
        ELSE NULL
    END,
    current_setting('app.wompi_environment'),
    CASE p.estado::text
        WHEN 'aprobado' THEN 'aprobado'
        WHEN 'rechazado' THEN 'rechazado'
        ELSE 'pendiente'
    END,
    CASE WHEN p.estado::text = 'aprobado' THEN p.fecha_pago ELSE NULL END,
    COALESCE(p.fecha_pago, CURRENT_TIMESTAMP),
    COALESCE(p.fecha_pago, CURRENT_TIMESTAMP)
FROM public.pagos p
WHERE NOT EXISTS (
    SELECT 1 FROM public.pago_intentos i WHERE i.fk_id_pago = p.id_pago
);

COMMIT;
