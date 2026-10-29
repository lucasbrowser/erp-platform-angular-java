import { Component, inject } from "@angular/core";
import { RouterLink } from "@angular/router";
import { AuthService } from "../auth/auth.service";

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (auth.status() === "loading") {
      <p role="status">Verificando sessão…</p>
    } @else if (auth.status() === "error") {
      <p role="alert">
        Não foi possível inicializar a conexão com o provedor de identidade.
      </p>
    } @else if (auth.status() === "authenticated") {
      <p>Sessão autenticada.</p>
      <a routerLink="/home">Abrir início</a>
    } @else {
      <p>Entre com sua conta corporativa para continuar.</p>
      <button type="button" (click)="auth.login('/home')">Entrar</button>
    }
  `,
})
export class LandingComponent {
  readonly auth = inject(AuthService);
}
