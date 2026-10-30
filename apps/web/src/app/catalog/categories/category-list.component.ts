import { Component, OnInit, inject, signal } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { CategoryApiService } from "../data-access/category-api.service";
import { Category, PageResponse } from "../models/catalog.models";
import { IdentityApiService } from "../../auth/identity-api.service";

const PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <nav aria-label="Catálogo">
      <a routerLink="/catalog/categories" aria-current="page">Categorias</a>
    </nav>
    <section>
      <div class="title-row">
        <div>
          <h2>Categorias</h2>
          <p>Gerencie as categorias do catálogo.</p>
        </div>
        @if (canWrite()) {
          <a class="button" routerLink="/catalog/categories/new"
            >Nova categoria</a
          >
        }
      </div>
      @if (success()) {
        <p class="success" role="status">{{ success() }}</p>
      }
      @if (loading()) {
        <p role="status">Carregando categorias…</p>
      }
      @if (error()) {
        <p role="alert">{{ error() }}</p>
        <button type="button" (click)="load(page())">Tentar novamente</button>
      }
      @if (!loading() && !error() && response()?.content?.length === 0) {
        <p>Não há categorias cadastradas.</p>
      }
      @if (!loading() && response()?.content?.length) {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">Descrição</th>
                <th scope="col">Situação</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (category of response()?.content; track category.id) {
                <tr>
                  <td>{{ category.name }}</td>
                  <td>{{ category.description || "—" }}</td>
                  <td>
                    <span [class.inactive]="!category.active">{{
                      category.active ? "Ativa" : "Inativa"
                    }}</span>
                  </td>
                  <td class="actions">
                    @if (canWrite()) {
                      <a
                        [routerLink]="[
                          '/catalog/categories',
                          category.id,
                          'edit',
                        ]"
                        >Editar</a
                      >
                    }
                    @if (canWrite() && category.active) {
                      <button type="button" (click)="deactivate(category)">
                        Desativar
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <nav class="pagination" aria-label="Paginação de categorias">
          <button
            type="button"
            (click)="load(page() - 1)"
            [disabled]="page() <= 0 || loading()"
          >
            Anterior
          </button>
          <span
            >Página {{ page() + 1 }} de {{ response()?.totalPages || 1 }} ·
            {{ response()?.totalElements }} categorias</span
          >
          <button
            type="button"
            (click)="load(page() + 1)"
            [disabled]="response()?.last || loading()"
          >
            Próxima
          </button>
        </nav>
      }
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      nav[aria-label="Catálogo"] {
        margin: 1rem 0;
      }
      section {
        background: #fff;
        border: 1px solid #d9e0ea;
        border-radius: 0.5rem;
        padding: 1.25rem;
      }
      .title-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        margin-bottom: 1rem;
      }
      h2 {
        margin: 0.25rem 0;
      }
      p {
        color: #46556b;
      }
      .button {
        background: #174ea6;
        color: white;
        border-radius: 0.25rem;
        padding: 0.65rem 0.85rem;
        text-decoration: none;
      }
      .table-wrap {
        overflow-x: auto;
      }
      table {
        width: 100%;
        border-collapse: collapse;
        text-align: left;
      }
      th,
      td {
        border-bottom: 1px solid #d9e0ea;
        padding: 0.75rem 0.5rem;
        vertical-align: top;
      }
      .inactive {
        color: #8b2635;
        font-weight: 600;
      }
      .actions {
        white-space: nowrap;
        display: flex;
        gap: 0.75rem;
      }
      .pagination {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.75rem;
        margin-top: 1rem;
      }
      .success {
        color: #176b3a;
      }
      button {
        cursor: pointer;
      }
      @media (max-width: 600px) {
        section {
          padding: 0.75rem;
        }
        .title-row {
          align-items: flex-start;
          flex-direction: column;
        }
        .pagination {
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class CategoryListComponent implements OnInit {
  private readonly api = inject(CategoryApiService);
  private readonly identity = inject(IdentityApiService);
  private readonly router = inject(Router);
  readonly response = signal<PageResponse<Category> | null>(null);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly success = signal("");
  readonly page = signal(0);
  readonly canWrite = () =>
    this.identity
      .identity()
      ?.authorities.some((role) =>
        ["ROLE_ADMIN", "ROLE_MANAGER"].includes(role),
      ) ?? false;

  ngOnInit(): void {
    const message = this.router.getCurrentNavigation()?.extras.state?.[
      "categorySuccess"
    ] as string | undefined;
    if (message) this.success.set(message);
    this.load(0);
  }

  load(page: number, preserveSuccess = false): void {
    this.loading.set(true);
    this.error.set("");
    if (!preserveSuccess) this.success.set("");
    const requestedPage = Math.max(0, page);
    this.api
      .list({
        page: requestedPage,
        size: Math.min(PAGE_SIZE, MAX_PAGE_SIZE),
        sort: "name,asc",
      })
      .subscribe({
        next: (response) => {
          this.response.set(response);
          this.page.set(response.page);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(this.messageFor(error));
          this.loading.set(false);
        },
      });
  }

  deactivate(category: Category): void {
    if (
      !window.confirm(
        `Desativar a categoria “${category.name}”? Ela permanecerá no histórico.`,
      )
    )
      return;
    this.loading.set(true);
    this.error.set("");
    this.api.deactivate(category.id).subscribe({
      next: () => {
        this.success.set("Categoria desativada.");
        this.load(this.page(), true);
      },
      error: (error: unknown) => {
        this.error.set(this.messageFor(error, true));
        this.loading.set(false);
      },
    });
  }

  private messageFor(error: unknown, writing = false): string {
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    if (status === 401)
      return "Sua sessão expirou. Entre novamente para continuar.";
    if (status === 403) return "Você não tem permissão para esta operação.";
    if (status === 409 && writing)
      return "A categoria não pode ser desativada devido a uma restrição existente.";
    return "Não foi possível carregar as categorias. Tente novamente.";
  }
}
