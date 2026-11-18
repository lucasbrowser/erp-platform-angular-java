import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { provideRouter } from "@angular/router";
import { IdentityApiService } from "../../auth/identity-api.service";
import { ProductListComponent } from "./product-list.component";

const PRODUCTS = "http://localhost:8082/api/v2/products";
const CATEGORIES = "http://localhost:8082/api/v1/categories";
const response = (content: unknown[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true });

describe("ProductListComponent", () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ProductListComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: IdentityApiService, useValue: { identity: signal({ userId: "u1", authorities: ["ROLE_ADMIN"] }) } }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });

  it("loads paged products and displays a maximum price without precision loss", () => {
    const fixture = TestBed.createComponent(ProductListComponent);
    fixture.detectChanges();
    http.expectOne((req) => req.url === CATEGORIES).flush(response([]));
    const list = http.expectOne((req) => req.url === PRODUCTS);
    expect(list.request.params.get("page")).toBe("0");
    expect(list.request.params.get("size")).toBe("20");
    expect(list.request.params.get("sort")).toBe("name,asc");
    list.flush(response([{ id: "p1", sku: "AB-1", name: "Parafuso", description: null, categoryId: "c1", price: "999999999999.99", active: true, createdAt: "", updatedAt: "" }]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("R$ 999.999.999.999,99");
  });

  it("sends product filters and retains false for inactive status", () => {
    const fixture = TestBed.createComponent(ProductListComponent);
    fixture.detectChanges();
    http.expectOne((req) => req.url === CATEGORIES).flush(response([]));
    http.expectOne((req) => req.url === PRODUCTS).flush(response([]));
    fixture.componentInstance.filters.setValue({ name: "Porca", sku: "AB", categoryId: "c1", active: "false" });
    fixture.componentInstance.search();
    const request = http.expectOne((req) => req.url === PRODUCTS);
    expect(request.request.params.get("name")).toBe("Porca");
    expect(request.request.params.get("sku")).toBe("AB");
    expect(request.request.params.get("categoryId")).toBe("c1");
    expect(request.request.params.get("active")).toBe("false");
    request.flush(response([]));
  });

  it("deactivates a product after confirmation and accepts HTTP 204", () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const fixture = TestBed.createComponent(ProductListComponent);
    fixture.detectChanges();
    http.expectOne((req) => req.url === CATEGORIES).flush(response([]));
    http.expectOne((req) => req.url === PRODUCTS).flush(response([{ id: "p1", sku: "AB-1", name: "Parafuso", description: null, categoryId: "c1", price: "10.99", active: true, createdAt: "", updatedAt: "" }]));
    fixture.componentInstance.deactivate({ id: "p1", sku: "AB-1", name: "Parafuso", description: null, categoryId: "c1", price: "10.99", active: true, createdAt: "", updatedAt: "" });
    http.expectOne(`${PRODUCTS}/p1`).flush(null, { status: 204, statusText: "No Content" });
    http.expectOne((req) => req.url === PRODUCTS).flush(response([]));
    expect(fixture.componentInstance.success()).toBe("Produto desativado.");
    confirm.mockRestore();
  });
});
