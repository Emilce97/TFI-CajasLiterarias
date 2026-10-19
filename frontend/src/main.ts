import { renderEdicionesPage } from "./pages/ediciones";
import { renderPagosPage } from "./pages/pagos";
import { renderDemandaPage } from "./pages/demanda";
import { renderDespachoPage } from "./pages/despacho";
import { renderMiSuscripcionPage } from "./pages/miSuscripcion";
import { cerrarSesion, tieneRol } from "./utils/auth";

const app = document.querySelector<HTMLDivElement>("#app")!;
const esAdmin = tieneRol("ADMIN");
const esSuscriptor = tieneRol("SUSCRIPTOR");

// Sin sesion: Ingresar y Registrarme. Suscriptor: Mi suscripcion. 
// Administradora: Pagos, Demanda y Despacho (solo de ella).
const opcionesPorRol = esAdmin
    ? `<button id="nav-pagos">Pagos</button>
       <button id="nav-demanda">Demanda</button>
       <button id="nav-despacho">Despacho</button>
       <button id="nav-salir">Cerrar sesión</button>`
    : esSuscriptor
        ? `<button id="nav-mi-suscripcion">Mi suscripción</button>
           <button id="nav-salir">Cerrar sesión</button>`
        : `<a href="/login.html">Ingresar</a>
           <a href="/registro.html">Registrarme</a>`;

app.innerHTML = `
  <nav>
    <button id="nav-ediciones">Ediciones</button>
    ${opcionesPorRol}
  </nav>
  <div id="contenido"></div>
`;

const contenido = app.querySelector<HTMLDivElement>("#contenido")!;

document.querySelector("#nav-ediciones")!.addEventListener("click", () => {
    renderEdicionesPage(contenido);
});

document.querySelector("#nav-pagos")?.addEventListener("click", () => {
    renderPagosPage(contenido);
});

document.querySelector("#nav-demanda")?.addEventListener("click", () => {
    renderDemandaPage(contenido);
});

document.querySelector("#nav-despacho")?.addEventListener("click", () => {
    renderDespachoPage(contenido);
});

document.querySelector("#nav-mi-suscripcion")?.addEventListener("click", () => {
    renderMiSuscripcionPage(contenido);
});

document.querySelector("#nav-salir")?.addEventListener("click", cerrarSesion);

// Si el navegador restaura la pagina desde su cache (boton "atras" despues de
// cerrar sesion), se recarga para que no muestre una vista protegida vieja.
window.addEventListener("pageshow", (event) => {
    if (event.persisted) window.location.reload();
});

// Pagina que se muestra al cargar la app
renderEdicionesPage(contenido);