import { Component } from "@angular/core";
import { RouterLink } from "@angular/router";

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `<h2>Acesso indisponível</h2><p>Não foi possível autenticar sua sessão.</p><a routerLink="/">Voltar ao início</a>`,
})
export class AccessDeniedComponent {}
