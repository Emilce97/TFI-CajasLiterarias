import { renderEdicionesPage } from "./pages/ediciones";
import { renderPagosPage } from "./pages/pagos";
import { renderDemandaPage } from "./pages/demanda";

const app = document.querySelector<HTMLDivElement>("#app")!;

app.innerHTML = `
  <nav>
    <button id="nav-ediciones">Ediciones</button>
    <button id="nav-pagos">Pagos</button>
    <button id="nav-demanda">Demanda</button>
  </nav>
  <div id="contenido"></div>
`;

const contenido = app.querySelector<HTMLDivElement>("#contenido")!;

document.querySelector("#nav-ediciones")!.addEventListener("click", () => {
    renderEdicionesPage(contenido);
});

document.querySelector("#nav-pagos")!.addEventListener("click", () => {
    renderPagosPage(contenido);
});

document.querySelector("#nav-demanda")!.addEventListener("click", () => {
    renderDemandaPage(contenido);
});

// Pagina que se muestra al cargar la app
renderEdicionesPage(contenido);