import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { ActivatedRoute, Router, provideRouter } from "@angular/router";
import { CategoryFormComponent } from "./category-form.component";
import { IdentityApiService } from "../../auth/identity-api.service";

const URL = "http://localhost:8082/api/v1/categories";
const ID = "b8d2f9ee-2c57-4b3c-9ab8-804c97a9a908";

describe("CategoryFormComponent", () => {
  let http: HttpTestingController;

  function setup(id: string | null = null) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CategoryFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => (key === "id" ? id : null) },
            },
          },
        },
        {
          provide: IdentityApiService,
          useValue: {
            identity: signal({ userId: "u1", authorities: ["ROLE_MANAGER"] }),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(CategoryFormComponent);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), "navigateByUrl").mockResolvedValue(true);
    return fixture;
  }

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it("blocks invalid input and posts a valid create request", () => {
    const fixture = setup();
    fixture.detectChanges();
    fixture.componentInstance.submit();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    http.expectNone(URL);

    fixture.componentInstance.form.setValue({
      name: "  Escritório  ",
      description: "  ",
    });
    fixture.componentInstance.submit();
    const request = http.expectOne(URL);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      name: "Escritório",
      description: "  ",
    });
    request.flush(
      {
        id: ID,
        name: "Escritório",
        description: null,
        active: true,
        createdAt: "",
        updatedAt: "",
      },
      { status: 201, statusText: "Created" },
    );
  });

  it("loads existing values and sends a complete PUT body", () => {
    const fixture = setup(ID);
    fixture.detectChanges();
    http.expectOne(`${URL}/${ID}`).flush({
      id: ID,
      name: "Papel",
      description: "Folhas",
      active: false,
      createdAt: "",
      updatedAt: "",
    });
    expect(fixture.componentInstance.form.getRawValue()).toEqual({
      name: "Papel",
      description: "Folhas",
    });
    fixture.componentInstance.form.controls.name.setValue("Papelaria");
    fixture.componentInstance.submit();
    const request = http.expectOne(`${URL}/${ID}`);
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      name: "Papelaria",
      description: "Folhas",
    });
    expect(request.request.body.active).toBeUndefined();
    request.flush({
      id: ID,
      name: "Papelaria",
      description: "Folhas",
      active: false,
      createdAt: "",
      updatedAt: "",
    });
  });

  it("preserves the form after a validation or conflict response", () => {
    const fixture = setup();
    fixture.detectChanges();
    fixture.componentInstance.form.controls.name.setValue("Duplicada");
    fixture.componentInstance.submit();
    http
      .expectOne(URL)
      .flush({ title: "Conflict" }, { status: 409, statusText: "Conflict" });
    expect(fixture.componentInstance.form.controls.name.value).toBe(
      "Duplicada",
    );
    expect(fixture.componentInstance.error()).toContain("Já existe");
  });

  it("shows a validation message for HTTP 400 and keeps the entered data", () => {
    const fixture = setup();
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      name: "Categoria revisada",
      description: "Observação mantida",
    });
    fixture.componentInstance.submit();
    http
      .expectOne(URL)
      .flush(
        { title: "Bad Request" },
        { status: 400, statusText: "Bad Request" },
      );
    expect(fixture.componentInstance.form.getRawValue()).toEqual({
      name: "Categoria revisada",
      description: "Observação mantida",
    });
    expect(fixture.componentInstance.error()).toContain("Revise os campos");
  });

  it("does not submit writes for a reader without a write authority", () => {
    TestBed.configureTestingModule({
      imports: [CategoryFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } },
        },
        {
          provide: IdentityApiService,
          useValue: {
            identity: signal({ userId: "u2", authorities: ["ROLE_SALES"] }),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(CategoryFormComponent);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), "navigateByUrl").mockResolvedValue(true);
    fixture.componentInstance.form.controls.name.setValue("Leitura");
    fixture.componentInstance.submit();
    http.expectNone(URL);
  });
});
