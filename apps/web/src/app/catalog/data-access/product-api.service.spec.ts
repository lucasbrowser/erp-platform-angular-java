import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { ProductApiService } from "./product-api.service";

const PRODUCT_URL = "http://localhost:8082/api/v1/products";
const PRODUCT_ID = "2b4adbd6-53ca-47c7-90f9-e17c80f03a9a";
const CATEGORY_ID = "8fb0b316-2f89-4cf2-a80b-0ed939615607";

describe("ProductApiService", () => {
  let service: ProductApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("serializes supported filters and repeated pageable parameters", async () => {
    const response = firstValueFrom(
      service.list({
        page: 2,
        size: 50,
        sort: ["name,desc", "sku,asc"],
        name: "Porca & arruela",
        sku: "AB 12",
        categoryId: CATEGORY_ID,
        active: false,
      }),
    );
    const request = http.expectOne((req) => req.url === PRODUCT_URL);

    expect(request.request.method).toBe("GET");
    expect(request.request.params.keys().sort()).toEqual([
      "active",
      "categoryId",
      "name",
      "page",
      "size",
      "sku",
      "sort",
    ]);
    expect(request.request.params.get("page")).toBe("2");
    expect(request.request.params.get("size")).toBe("50");
    expect(request.request.params.getAll("sort")).toEqual([
      "name,desc",
      "sku,asc",
    ]);
    expect(request.request.params.get("name")).toBe("Porca & arruela");
    expect(request.request.params.get("sku")).toBe("AB 12");
    expect(request.request.params.get("categoryId")).toBe(CATEGORY_ID);
    expect(request.request.params.get("active")).toBe("false");

    const page = {
      content: [],
      page: 2,
      size: 50,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
    };
    request.flush(page);
    await expect(response).resolves.toEqual(page);
  });

  it("omits absent and empty filters without dropping false values", async () => {
    const response = firstValueFrom(
      service.list({ name: "", sku: "", active: false }),
    );
    const request = http.expectOne((req) => req.url === PRODUCT_URL);

    expect(request.request.params.keys()).toEqual(["active"]);
    expect(request.request.params.get("active")).toBe("false");
    request.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });
    await response;
  });

  it("gets, creates, updates, and deactivates products using the complete DTO", async () => {
    const getResponse = firstValueFrom(service.get(PRODUCT_ID));
    const getRequest = http.expectOne(`${PRODUCT_URL}/${PRODUCT_ID}`);
    expect(getRequest.request.method).toBe("GET");
    getRequest.flush({
      id: PRODUCT_ID,
      sku: "AB-120",
      name: "Parafuso",
      description: null,
      categoryId: CATEGORY_ID,
      price: 12.5,
      active: true,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });
    await getResponse;

    const body = {
      sku: "AB-120",
      name: "Parafuso",
      description: null,
      categoryId: CATEGORY_ID,
      price: 12.5,
    };
    const createResponse = firstValueFrom(service.create(body));
    const createRequest = http.expectOne(PRODUCT_URL);
    expect(createRequest.request.method).toBe("POST");
    expect(createRequest.request.body).toEqual(body);
    createRequest.flush(
      {
        id: PRODUCT_ID,
        ...body,
        active: true,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
      { status: 201, statusText: "Created" },
    );
    await createResponse;

    const updateResponse = firstValueFrom(service.update(PRODUCT_ID, body));
    const updateRequest = http.expectOne(`${PRODUCT_URL}/${PRODUCT_ID}`);
    expect(updateRequest.request.method).toBe("PUT");
    expect(updateRequest.request.body).toEqual(body);
    updateRequest.flush({
      id: PRODUCT_ID,
      ...body,
      active: true,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-02T00:00:00Z",
    });
    await updateResponse;

    let completed = false;
    service.deactivate(PRODUCT_ID).subscribe(() => (completed = true));
    const deleteRequest = http.expectOne(`${PRODUCT_URL}/${PRODUCT_ID}`);
    expect(deleteRequest.request.method).toBe("DELETE");
    deleteRequest.flush(null, { status: 204, statusText: "No Content" });
    expect(completed).toBe(true);
  });

  it("propagates HTTP errors without replacing Problem Details", async () => {
    const problem = {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      detail: "Product SKU already exists",
    };
    const response = firstValueFrom(service.get(PRODUCT_ID));
    http.expectOne(`${PRODUCT_URL}/${PRODUCT_ID}`).flush(problem, {
      status: 409,
      statusText: "Conflict",
    });

    await expect(response).rejects.toMatchObject({
      status: 409,
      error: problem,
    });
  });
});
