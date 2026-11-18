import { HttpErrorResponse } from "@angular/common/http";
import { Component, OnInit, inject, signal } from "@angular/core";
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { CategoryApiService } from "../data-access/category-api.service";
import { ProductApiService } from "../data-access/product-api.service";
import { Category, PageResponse, ProductWriteRequest } from "../models/catalog.models";
import { IdentityApiService } from "../../auth/identity-api.service";
import { canonicalMoneyInput } from "../money-decimal.utils";

const moneyValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const result = canonicalMoneyInput(String(control.value ?? ""));
  return result.error ? { [result.error]: true } : null;
};

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a routerLink="/catalog/products">← Voltar para produtos</a>
    <section>
      <h2>{{ editing ? "Editar produto" : "Novo produto" }}</h2>
      @if (loading()) { <p role="status">Carregando produto…</p> }
      @if (error()) { <p role="alert">{{ error() }}</p> }
      @if (canWrite) {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label for="sku">SKU *</label><input id="sku" formControlName="sku" maxlength="64" required />
          @if (showError("sku")) { <small class="field-error">Informe um SKU.</small> }
          <label for="name">Nome *</label><input id="name" formControlName="name" maxlength="160" required />
          @if (showError("name")) { <small class="field-error">Informe o nome do produto.</small> }
          <label for="description">Descrição</label><textarea id="description" formControlName="description" maxlength="2000" rows="3"></textarea>
          <label for="categoryId">Categoria *</label>
          <select id="categoryId" formControlName="categoryId" required>
            <option value="">Selecione uma categoria</option>
            @for (category of availableCategories(); track category.id) {
              <option [value]="category.id">{{ category.name }}{{ category.active ? "" : " (inativa)" }}</option>
            }
          </select>
          @if (showError("categoryId")) { <small class="field-error">Selecione uma categoria.</small> }
          <label for="price">Preço (R$) *</label>
          <input id="price" type="text" inputmode="decimal" autocomplete="off" formControlName="price" placeholder="0,00" required aria-describedby="price-hint price-error" />
          <small id="price-hint">Use vírgula ou ponto decimal, sem separador de milhar. Até duas casas decimais.</small>
          @if (showError("price")) { <small id="price-error" class="field-error">{{ priceError() }}</small> }
          <div class="actions"><button type="submit" [disabled]="form.invalid || saving() || loading()">{{ saving() ? "Salvando…" : editing ? "Salvar alterações" : "Cadastrar produto" }}</button><a routerLink="/catalog/products">Cancelar</a></div>
        </form>
      } @else if (!loading()) { <p role="alert">Sua conta não tem permissão para alterar produtos.</p> }
    </section>
  `,
  styles: [`:host{display:block}section{margin-top:1rem;background:#fff;border:1px solid #d9e0ea;border-radius:.5rem;padding:1.25rem}form{display:grid;gap:.45rem;max-width:38rem}input,textarea,select{font:inherit;padding:.6rem;border:1px solid #718096;border-radius:.25rem}label{margin-top:.65rem;font-weight:600}small{color:#46556b}.field-error{color:#a32736}.actions{display:flex;align-items:center;gap:1rem;margin-top:1rem}button{background:#174ea6;color:#fff;border:0;border-radius:.25rem;padding:.65rem .85rem;cursor:pointer}button:disabled{opacity:.6;cursor:wait}`],
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly products = inject(ProductApiService);
  private readonly categories = inject(CategoryApiService);
  private readonly identity = inject(IdentityApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly id = this.route.snapshot.paramMap.get("id");
  readonly editing = !!this.id;
  readonly form = this.fb.nonNullable.group({
    sku: ["", [Validators.required, Validators.maxLength(64), Validators.pattern(/\S/)]],
    name: ["", [Validators.required, Validators.maxLength(160), Validators.pattern(/\S/)]],
    description: ["", [Validators.maxLength(2000)]],
    categoryId: ["", Validators.required],
    price: ["", [Validators.required, moneyValidator]],
  });
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal("");
  readonly availableCategories = signal<Category[]>([]);
  readonly canWrite = this.identity.identity()?.authorities.some((role) => ["ROLE_ADMIN", "ROLE_MANAGER"].includes(role)) ?? false;
  private formSubmitted = false;
  private categoryResults: Category[] = [];
  private currentCategoryId = "";

  ngOnInit(): void {
    this.loading.set(true);
    this.loadCategories(0, []);
    if (this.id) this.products.get(this.id).subscribe({
      next: (product) => {
        this.currentCategoryId = product.categoryId;
        this.updateAvailableCategories();
        this.form.setValue({ sku: product.sku, name: product.name, description: product.description ?? "", categoryId: product.categoryId, price: product.price });
        this.loading.set(false);
      },
      error: (error: unknown) => { this.error.set(this.messageFor(error, true)); this.loading.set(false); },
    });
    else this.loading.set(false);
  }

  showError(field: "sku" | "name" | "categoryId" | "price"): boolean {
    const control = this.form.controls[field];
    return control.invalid && (control.touched || this.formSubmitted);
  }

  priceError(): string {
    const errors = this.form.controls.price.errors;
    if (errors?.["required"] || errors?.["negative"]) return "Informe um preço igual ou maior que zero.";
    if (errors?.["precision"]) return "O preço pode ter no máximo duas casas decimais.";
    if (errors?.["max"]) return "O preço máximo é R$ 999.999.999.999,99.";
    return "Informe um valor decimal válido, sem separador de milhar.";
  }

  submit(): void {
    this.formSubmitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving() || !this.canWrite) return;
    const raw = this.form.getRawValue();
    const price = canonicalMoneyInput(raw.price);
    if (!price.value) return;
    const body: ProductWriteRequest = { sku: raw.sku.trim(), name: raw.name.trim(), description: raw.description === "" ? null : raw.description, categoryId: raw.categoryId, price: price.value };
    this.saving.set(true);
    this.error.set("");
    const request = this.id ? this.products.update(this.id, body) : this.products.create(body);
    request.subscribe({
      next: () => { void this.router.navigateByUrl("/catalog/products", { state: { productSuccess: this.id ? "Produto atualizado." : "Produto cadastrado." } }); },
      error: (error: unknown) => { this.saving.set(false); this.error.set(this.messageFor(error)); },
    });
  }

  private loadCategories(page: number, collected: Category[]): void {
    this.categories.list({ page, size: 100, sort: "name,asc" }).subscribe({
      next: (response: PageResponse<Category>) => {
        this.categoryResults = [...collected, ...response.content];
        this.updateAvailableCategories();
        if (!response.last) this.loadCategories(page + 1, this.categoryResults);
      },
      error: (error: unknown) => this.error.set(this.messageFor(error)),
    });
  }

  private updateAvailableCategories(): void {
    this.availableCategories.set(this.categoryResults.filter((category) => category.active || (this.editing && category.id === this.currentCategoryId)));
  }

  private messageFor(error: unknown, loading = false): string {
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    if (status === 400) return "Revise os campos informados. O servidor rejeitou os dados enviados.";
    if (status === 401) return "Sua sessão expirou. Entre novamente para continuar.";
    if (status === 403) return "Você não tem permissão para esta operação.";
    if (status === 404) return loading ? "Produto não encontrado." : "O produto ou a categoria não foi encontrado.";
    if (status === 409) return "Já existe um produto com esse SKU ou há uma restrição de catálogo.";
    return loading ? "Não foi possível carregar o produto." : "Não foi possível carregar ou salvar o produto. Tente novamente.";
  }
}
