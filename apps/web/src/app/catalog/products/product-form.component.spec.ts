import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { ActivatedRoute, Router, provideRouter } from "@angular/router";
import { IdentityApiService } from "../../auth/identity-api.service";
import { ProductFormComponent } from "./product-form.component";

const PRODUCTS = "http://localhost:8082/api/v2/products";
const CATEGORIES = "http://localhost:8082/api/v1/categories";
const ID = "2b4adbd6-53ca-47c7-90f9-e17c80f03a9a";
const CATEGORY = "8fb0b316-2f89-4cf2-a80b-0ed939615607";
const page = { content: [{ id: CATEGORY, name: "Ferragens", description: null, active: true, createdAt: "", updatedAt: "" }], page: 0, size: 100, totalElements: 1, totalPages: 1, first: true, last: true };

describe("ProductFormComponent", () => {
  let http: HttpTestingController;

  function setup(id: string | null = null) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [ProductFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (key: string) => key === "id" ? id : null } } } },
        { provide: IdentityApiService, useValue: { identity: signal({ userId: "u1", authorities: ["ROLE_MANAGER"] }) } }],
    });
    const fixture = TestBed.createComponent(ProductFormComponent);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), "navigateByUrl").mockResolvedValue(true);
    fixture.detectChanges();
    http.expectOne((req) => req.url === CATEGORIES).flush(page);
    return fixture;
  }

  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });

  it("creates a product using a comma decimal converted to canonical string", () => {
    const fixture = setup();
    fixture.componentInstance.form.setValue({ sku: "AB-1", name: "Parafuso", description: "", categoryId: CATEGORY, price: "150,5" });
    fixture.componentInstance.submit();
    const request = http.expectOne(PRODUCTS);
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({ sku: "AB-1", name: "Parafuso", description: null, categoryId: CATEGORY, price: "150.50" });
    request.flush({ id: ID }, { status: 201, statusText: "Created" });
  });

  it.each([["-1.00"], ["1.234"], ["1000000000000.00"], [""]])("blocks invalid price %s", (price) => {
    const fixture = setup();
    fixture.componentInstance.form.setValue({ sku: "AB-1", name: "Parafuso", description: "", categoryId: CATEGORY, price });
    expect(fixture.componentInstance.form.controls.price.invalid).toBe(true);
    fixture.componentInstance.submit();
    http.expectNone(PRODUCTS);
  });

  it("loads and preserves the exact API price in edit, then sends the complete PUT body", () => {
    const fixture = setup(ID);
    http.expectOne(`${PRODUCTS}/${ID}`).flush({ id: ID, sku: "AB-1", name: "Parafuso", description: null, categoryId: CATEGORY, price: "999999999999.99", active: true, createdAt: "", updatedAt: "" });
    expect(fixture.componentInstance.form.controls.price.value).toBe("999999999999.99");
    fixture.componentInstance.submit();
    const request = http.expectOne(`${PRODUCTS}/${ID}`);
    expect(request.request.method).toBe("PUT");
    expect(request.request.body.price).toBe("999999999999.99");
    request.flush({ id: ID });
  });
});
