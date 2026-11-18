import { HttpErrorResponse } from "@angular/common/http";
import { Component, OnInit, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { IdentityApiService } from "../../auth/identity-api.service";
import { CategoryApiService } from "../data-access/category-api.service";
import { ProductApiService } from "../data-access/product-api.service";
import { Category, PageResponse, Product, ProductListQuery } from "../models/catalog.models";
import { formatMoneyBRL } from "../money-decimal.utils";

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <nav aria-label="Catálogo"><a routerLink="/catalog/products" aria-current="page">Produtos</a> · <a routerLink="/catalog/categories">Categorias</a></nav>
    <section>
      <div class="title-row"><div><h2>Produtos</h2><p>Consulte e gerencie os produtos do catálogo.</p></div>@if (canWrite()) { <a class="button" routerLink="/catalog/products/new">Novo produto</a> }</div>
      <form class="filters" [formGroup]="filters" (ngSubmit)="search()">
        <label>Nome <input formControlName="name" /></label>
        <label>SKU <input formControlName="sku" /></label>
        <label>Categoria <select formControlName="categoryId"><option value="">Todas</option>@for (category of categories(); track category.id) { <option [value]="category.id">{{ category.name }}</option> }</select></label>
        <label>Status <select formControlName="active"><option value="">Todos</option><option value="true">Ativos</option><option value="false">Inativos</option></select></label>
        <button type="submit" [disabled]="loading()">Pesquisar</button>
      </form>
      @if (success()) { <p class="success" role="status">{{ success() }}</p> }
      @if (loading()) { <p role="status">Carregando produtos…</p> }
      @if (error()) { <p role="alert">{{ error() }}</p><button type="button" (click)="load(page())">Tentar novamente</button> }
      @if (!loading() && !error() && response()?.content?.length === 0) { <p>Não há produtos para os filtros informados.</p> }
      @if (!loading() && response()?.content?.length) {
        <div class="table-wrap"><table><thead><tr><th>SKU</th><th>Nome</th><th>Categoria</th><th>Preço</th><th>Situação</th><th>Ações</th></tr></thead><tbody>
          @for (product of response()?.content; track product.id) { <tr><td>{{ product.sku }}</td><td>{{ product.name }}</td><td>{{ categoryName(product.categoryId) }}</td><td>{{ formatPrice(product.price) }}</td><td><span [class.inactive]="!product.active">{{ product.active ? "Ativo" : "Inativo" }}</span></td><td class="actions">@if (canWrite()) { <a [routerLink]="['/catalog/products', product.id, 'edit']">Editar</a>@if (product.active) { <button type="button" (click)="deactivate(product)">Desativar</button> } }</td></tr> }
        </tbody></table></div>
        <nav class="pagination" aria-label="Paginação de produtos"><button type="button" (click)="load(page() - 1)" [disabled]="page() <= 0 || loading()">Anterior</button><span>Página {{ page() + 1 }} de {{ response()?.totalPages || 1 }} · {{ response()?.totalElements }} produtos</span><button type="button" (click)="load(page() + 1)" [disabled]="response()?.last || loading()">Próxima</button></nav>
      }
    </section>
  `,
  styles: [`:host{display:block}nav[aria-label="Catálogo"]{margin:1rem 0}section{background:#fff;border:1px solid #d9e0ea;border-radius:.5rem;padding:1.25rem}.title-row{display:flex;align-items:center;justify-content:space-between;gap:1rem;margin-bottom:1rem}h2{margin:.25rem 0}p{color:#46556b}.button{background:#174ea6;color:white;border-radius:.25rem;padding:.65rem .85rem;text-decoration:none}.filters{display:flex;align-items:end;flex-wrap:wrap;gap:.75rem;margin:1rem 0 1.5rem}.filters label{display:grid;gap:.25rem;font-weight:600}.filters input,.filters select{font:inherit;padding:.45rem;border:1px solid #718096;border-radius:.25rem}.table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse;text-align:left}th,td{border-bottom:1px solid #d9e0ea;padding:.75rem .5rem;vertical-align:top}.inactive{color:#8b2635;font-weight:600}.actions{white-space:nowrap;display:flex;gap:.75rem}.pagination{display:flex;justify-content:space-between;align-items:center;gap:.75rem;margin-top:1rem}.success{color:#176b3a}button{cursor:pointer}@media(max-width:600px){section{padding:.75rem}.title-row{align-items:flex-start;flex-direction:column}.pagination{flex-wrap:wrap}}`],
})
export class ProductListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductApiService);
  private readonly categoryApi = inject(CategoryApiService);
  private readonly identity = inject(IdentityApiService);
  private readonly router = inject(Router);
  readonly filters = this.fb.nonNullable.group({ name: "", sku: "", categoryId: "", active: "" });
  readonly response = signal<PageResponse<Product> | null>(null);
  readonly loading = signal(false);
  readonly error = signal("");
  readonly success = signal("");
  readonly page = signal(0);
  readonly categories = signal<Category[]>([]);
  readonly canWrite = () => this.identity.identity()?.authorities.some((role) => ["ROLE_ADMIN", "ROLE_MANAGER"].includes(role)) ?? false;
  private query: ProductListQuery = {};

  ngOnInit(): void {
    const message = this.router.getCurrentNavigation()?.extras.state?.["productSuccess"] as string | undefined;
    if (message) this.success.set(message);
    this.loadCategories(0, []);
    this.load(0);
  }

  search(): void {
    const raw = this.filters.getRawValue();
    this.query = { name: raw.name.trim() || undefined, sku: raw.sku.trim() || undefined, categoryId: raw.categoryId || undefined, active: raw.active === "" ? undefined : raw.active === "true" };
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true); this.error.set("");
    this.api.list({ ...this.query, page: Math.max(0, page), size: 20, sort: "name,asc" }).subscribe({
      next: (response) => { this.response.set(response); this.page.set(response.page); this.loading.set(false); },
      error: (error: unknown) => { this.error.set(this.messageFor(error)); this.loading.set(false); },
    });
  }

  categoryName(id: string): string { return this.categories().find((category) => category.id === id)?.name ?? "—"; }
  formatPrice(price: string): string { return formatMoneyBRL(price); }

  deactivate(product: Product): void {
    if (!window.confirm(`Desativar o produto “${product.name}”? Ele permanecerá no histórico.`)) return;
    this.loading.set(true); this.error.set("");
    this.api.deactivate(product.id).subscribe({
      next: () => { this.success.set("Produto desativado."); this.load(this.page()); },
      error: (error: unknown) => { this.error.set(this.messageFor(error, true)); this.loading.set(false); },
    });
  }

  private loadCategories(page: number, previous: Category[]): void {
    this.categoryApi.list({ page, size: 100, sort: "name,asc" }).subscribe({
      next: (response) => { const all = [...previous, ...response.content]; this.categories.set(all); if (!response.last) this.loadCategories(page + 1, all); },
      error: () => this.categories.set([]),
    });
  }

  private messageFor(error: unknown, writing = false): string {
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    if (status === 401) return "Sua sessão expirou. Entre novamente para continuar.";
    if (status === 403) return "Você não tem permissão para esta operação.";
    if (status === 409 && writing) return "O produto não pode ser desativado devido a uma restrição existente.";
    return "Não foi possível carregar os produtos. Tente novamente.";
  }
}
