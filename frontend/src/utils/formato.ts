// Formatos de fechas y dinero para mostrar en pantalla.
// Las fechas se arman a mano a partir del texto que manda el backend (que ya viene
// en hora de Argentina), para no depender del formato regional de cada navegador.

/** "2026-11-21" → "21/11/2026". Sirve para fechas sin hora (corte, alta, pago). */
export function formatearFecha(iso: string | null): string {
    if (!iso) return "—";
    const [anio, mes, dia] = iso.slice(0, 10).split("-");
    return `${dia}/${mes}/${anio}`;
}

/** "2026-10-17T17:19:57" → "17/10/2026 17:19". Sirve para fechas con hora (modificación, validación). */
export function formatearFechaHora(iso: string | null): string {
    if (!iso) return "—";
    const hora = iso.split("T")[1] ?? "";
    return `${formatearFecha(iso)} ${hora.slice(0, 5)}`.trim();
}

const PESOS = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });

/** 5000 → "$ 5.000,00" */
export function formatearMonto(monto: number | null): string {
    if (monto === null) return "—";
    return PESOS.format(monto);
}