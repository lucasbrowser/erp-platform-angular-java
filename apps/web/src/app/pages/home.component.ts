import { Component, inject, OnInit } from "@angular/core";
import { RouterLink } from "@angular/router";
import { AuthService } from "../auth/auth.service";
import { IdentityApiService } from "../auth/identity-api.service";

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <h2>Início</h2>
    @if (identity.state() === "loading") {
      <p role="status">Consultando identidade na API…</p>
    } @else if (identity.state() === "loaded") {
      <p>Sessão autenticada no ERP Core.</p>
      <p>Identificador: {{ identity.identity()?.userId }}</p>
      <p>
        Authorities aceitas pela API:
        {{ identity.identity()?.authorities?.join(", ") || "nenhuma" }}
      </p>
    } @else if (identity.state() === "unauthenticated") {
      <p role="alert">
        A API não reconheceu a sessão. Entre novamente para renovar o acesso.
      </p>
      <button type="button" (click)="auth.login('/home')">
        Entrar novamente
      </button>
    } @else if (identity.state() === "forbidden") {
      <p role="alert">
        Sua conta não tem permissão para consultar a identidade atual.
      </p>
    } @else {
      <p role="alert">
        Não foi possível consultar a API. Tente novamente mais tarde.
      </p>
    }
    <button type="button" (click)="auth.logout()">Sair</button>
    <p><a routerLink="/">Voltar</a></p>
  `,
})
export class HomeComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly identity = inject(IdentityApiService);

  ngOnInit(): void {
    this.identity.load();
  }
}
