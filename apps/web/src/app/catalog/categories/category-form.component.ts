import { HttpErrorResponse } from "@angular/common/http";
import { Component, OnInit, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { ActivatedRoute } from "@angular/router";
import { CategoryApiService } from "../data-access/category-api.service";
import { IdentityApiService } from "../../auth/identity-api.service";

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a routerLink="/catalog/categories">← Voltar para categorias</a>
    <section>
      <h2>{{ editing ? "Editar categoria" : "Nova categoria" }}</h2>
      @if (loading()) {
        <p role="status">Carregando categoria…</p>
      }
      @if (success()) {
        <p class="success" role="status">{{ success() }}</p>
      }
      @if (error()) {
        <p role="alert">{{ error() }}</p>
      }
      @if (canWrite) {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label for="name">Nome <span aria-hidden="true">*</span></label>
          <input
            id="name"
            formControlName="name"
            maxlength="120"
            required
            [attr.aria-invalid]="showError('name')"
            aria-describedby="name-hint name-error"
          />
          <small id="name-hint">Até 120 caracteres.</small>
          @if (showError("name")) {
            <small id="name-error" class="field-error">{{ nameError() }}</small>
          }
          <label for="description">Descrição</label>
          <textarea
            id="description"
            formControlName="description"
            maxlength="2000"
            rows="4"
            [attr.aria-invalid]="showError('description')"
            aria-describedby="description-error"
          ></textarea>
          @if (showError("description")) {
            <small id="description-error" class="field-error"
              >A descrição deve ter no máximo 2000 caracteres.</small
            >
          }
          <div class="actions">
            <button
              type="submit"
              [disabled]="form.invalid || saving() || loading()"
            >
              {{
                saving()
                  ? "Salvando…"
                  : editing
                    ? "Salvar alterações"
                    : "Cadastrar categoria"
              }}</button
            ><a routerLink="/catalog/categories">Cancelar</a>
          </div>
        </form>
      } @else if (!loading()) {
        <p role="alert">Sua conta não tem permissão para alterar categorias.</p>
      }
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      section {
        margin-top: 1rem;
        background: #fff;
        border: 1px solid #d9e0ea;
        border-radius: 0.5rem;
        padding: 1.25rem;
      }
      form {
        display: grid;
        gap: 0.45rem;
        max-width: 38rem;
      }
      input,
      textarea {
        font: inherit;
        padding: 0.6rem;
        border: 1px solid #718096;
        border-radius: 0.25rem;
      }
      input[aria-invalid="true"],
      textarea[aria-invalid="true"] {
        border-color: #a32736;
      }
      label {
        margin-top: 0.65rem;
        font-weight: 600;
      }
      small {
        color: #46556b;
      }
      .field-error {
        color: #a32736;
      }
      .actions {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-top: 1rem;
      }
      button {
        background: #174ea6;
        color: white;
        border: 0;
        border-radius: 0.25rem;
        padding: 0.65rem 0.85rem;
        cursor: pointer;
      }
      button:disabled {
        opacity: 0.6;
        cursor: wait;
      }
      .success {
        color: #176b3a;
      }
    `,
  ],
})
export class CategoryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CategoryApiService);
  private readonly identity = inject(IdentityApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly form = this.fb.nonNullable.group({
    name: [
      "",
      [
        Validators.required,
        Validators.maxLength(120),
        Validators.pattern(/\S/),
      ],
    ],
    description: ["", [Validators.maxLength(2000)]],
  });
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal("");
  readonly success = signal("");
  readonly editing = !!this.route.snapshot.paramMap.get("id");
  readonly canWrite =
    this.identity
      .identity()
      ?.authorities.some((role) =>
        ["ROLE_ADMIN", "ROLE_MANAGER"].includes(role),
      ) ?? false;
  private readonly id = this.route.snapshot.paramMap.get("id");

  ngOnInit(): void {
    if (!this.editing || !this.id) return;
    this.loading.set(true);
    this.api.get(this.id).subscribe({
      next: (category) => {
        this.form.setValue({
          name: category.name,
          description: category.description ?? "",
        });
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(
          error instanceof HttpErrorResponse && error.status === 404
            ? "Categoria não encontrada."
            : "Não foi possível carregar a categoria.",
        );
        this.loading.set(false);
      },
    });
  }

  showError(field: "name" | "description"): boolean {
    const control = this.form.controls[field];
    return control.invalid && (control.touched || this.formSubmitted);
  }

  nameError(): string {
    const control = this.form.controls.name;
    if (control.hasError("required") || control.hasError("pattern"))
      return "Informe um nome que não esteja vazio.";
    return "O nome deve ter no máximo 120 caracteres.";
  }

  private formSubmitted = false;

  submit(): void {
    this.formSubmitted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving() || !this.canWrite) return;
    const description = this.form.controls.description.value;
    const body = {
      name: this.form.controls.name.value.trim(),
      description: description === "" ? null : description,
    };
    this.saving.set(true);
    this.error.set("");
    const request =
      this.editing && this.id
        ? this.api.update(this.id, body)
        : this.api.create(body);
    request.subscribe({
      next: () => {
        const message = this.editing
          ? "Categoria atualizada."
          : "Categoria cadastrada.";
        this.success.set(message);
        this.saving.set(false);
        void this.router.navigateByUrl("/catalog/categories", {
          state: { categorySuccess: message },
        });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(this.messageFor(error));
      },
    });
  }

  private messageFor(error: unknown): string {
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    if (status === 400)
      return "Revise os campos informados. O servidor rejeitou os dados enviados.";
    if (status === 409) return "Já existe uma categoria com esse nome.";
    if (status === 403)
      return "Você não tem permissão para alterar categorias.";
    if (status === 404) return "A categoria não foi encontrada.";
    return "Não foi possível salvar a categoria. Tente novamente.";
  }
}
