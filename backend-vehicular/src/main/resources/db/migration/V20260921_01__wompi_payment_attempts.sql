-- Ejecutar manualmente antes de desplegar esta version.
-- En la misma sesion PostgreSQL, definir primero uno de estos valores:
--   SET app.wompi_environment = 'test'; -- sandbox
--   SET app.wompi_environment = 'prod'; -- produccion
-- El script aborta sin modificar datos si encuentra referencias incompatibles.

BEGIN;

DO $$
DECLARE
    configured_environment text := current_setting('app.wompi_environment', true);
BEGIN
    IF configured_environment IS NULL OR configured_environment NOT IN ('test', 'prod') THEN
        RAISE EXCEPTION 'Defina app.wompi_environment como test o prod antes de ejecutar la migracion';
    END IF;

    IF EXISTS (SELECT 1 FROM pagos WHERE referencia_pago IS NULL OR btrim(referencia_pago) = '') THEN
        RAISE EXCEPTION 'Hay pagos sin referencia_pago; revise esos registros antes de migrar';
    END IF;

    IF EXISTS (
        SELECT referencia_pago FROM pagos GROUP BY referencia_pago HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay referencias de pago duplicadas; concilie los datos antes de migrar';
    END IF;

    IF EXISTS (SELECT 1 FROM pagos WHERE estado::text NOT IN ('pendiente', 'aprobado', 'rechazado')) THEN
        RAISE EXCEPTION 'Hay estados de pago no soportados por la migracion';
    END IF;

    IF EXISTS (
        SELECT 1 FROM pagos
        WHERE estado_wompi IS NOT NULL
          AND upper(estado_wompi) NOT IN ('PENDING', 'APPROVED', 'DECLINED', 'VOIDED', 'ERROR')
    ) THEN
        RAISE EXCEPTION 'Hay estados Wompi no reconocidos; concilie esos registros antes de migrar';
    END IF;
END $$;

CREATE TABLE pago_intentos (
    id_intento uuid PRIMARY KEY,
    fk_id_pago uuid NOT NULL,
    referencia varchar(100) NOT NULL,
    wompi_transaction_id varchar(100),
    wompi_payment_method_type varchar(50),
    wompi_status varchar(20),
    wompi_environment varchar(10) NOT NULL,
    estado varchar(24) NOT NULL,
    fecha_confirmacion timestamp,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_pago_intentos_pago
        FOREIGN KEY (fk_id_pago) REFERENCES pagos(id_pago) ON DELETE RESTRICT,
    CONSTRAINT uq_pago_intentos_referencia UNIQUE (referencia),
    CONSTRAINT ck_pago_intentos_environment CHECK (wompi_environment IN ('test', 'prod')),
    CONSTRAINT ck_pago_intentos_estado
        CHECK (estado IN ('pendiente', 'aprobado', 'rechazado', 'aprobado_duplicado')),
    CONSTRAINT ck_pago_intentos_wompi_status
        CHECK (wompi_status IS NULL OR wompi_status IN ('PENDING', 'APPROVED', 'DECLINED', 'VOIDED', 'ERROR'))
);

INSERT INTO pago_intentos (
    id_intento, fk_id_pago, referencia, wompi_status, wompi_environment,
    estado, fecha_confirmacion, created_at, updated_at
)
SELECT
    id_pago,
    id_pago,
    referencia_pago,
    CASE
        WHEN upper(estado_wompi) IN ('PENDING', 'APPROVED', 'DECLINED', 'VOIDED', 'ERROR')
            THEN upper(estado_wompi)
        ELSE NULL
    END,
    current_setting('app.wompi_environment'),
    CASE estado::text
        WHEN 'aprobado' THEN 'aprobado'
        WHEN 'rechazado' THEN 'rechazado'
        ELSE 'pendiente'
    END,
    CASE WHEN estado::text = 'aprobado' THEN fecha_pago ELSE NULL END,
    COALESCE(fecha_pago, CURRENT_TIMESTAMP),
    COALESCE(fecha_pago, CURRENT_TIMESTAMP)
FROM pagos;

CREATE INDEX idx_pago_intentos_pago ON pago_intentos(fk_id_pago);
CREATE INDEX idx_pago_intentos_transaccion
    ON pago_intentos(wompi_environment, wompi_transaction_id);
CREATE UNIQUE INDEX uq_pago_intentos_transaccion_environment
    ON pago_intentos(wompi_environment, wompi_transaction_id)
    WHERE wompi_transaction_id IS NOT NULL;
CREATE UNIQUE INDEX uq_pago_intentos_pendiente_por_pago
    ON pago_intentos(fk_id_pago)
    WHERE estado = 'pendiente';

COMMENT ON COLUMN pagos.referencia_pago IS
    'Columna legada conservada por trazabilidad; las nuevas referencias viven en pago_intentos.referencia';
COMMENT ON COLUMN pagos.estado_wompi IS
    'Columna legada conservada por trazabilidad; los nuevos estados viven en pago_intentos.wompi_status';

COMMIT;
