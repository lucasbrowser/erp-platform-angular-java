import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import { CategoryApiService } from "./category-api.service";

const CATEGORY_URL = "http://localhost:8082/api/v1/categories";
const CATEGORY_ID = "b8d2f9ee-2c57-4b3c-9ab8-804c97a9a908";

describe("CategoryApiService", () => {
  let service: CategoryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoryApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("lists categories with only pageable and sort parameters", async () => {
    const response = firstValueFrom(
      service.list({ page: 0, size: 100, sort: ["name,asc", "id,desc"] }),
    );
    const request = http.expectOne((req) => req.url === CATEGORY_URL);

    expect(request.request.method).toBe("GET");
    expect(request.request.params.keys().sort()).toEqual([
      "page",
      "size",
      "sort",
    ]);
    expect(request.request.params.get("page")).toBe("0");
    expect(request.request.params.get("size")).toBe("100");
    expect(request.request.params.getAll("sort")).toEqual([
      "name,asc",
      "id,desc",
    ]);

    const page = {
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    request.flush(page);
    await expect(response).resolves.toEqual(page);
  });

  it("omits pageable parameters when backend defaults are requested", async () => {
    const response = firstValueFrom(service.list());
    const request = http.expectOne(CATEGORY_URL);

    expect(request.request.method).toBe("GET");
    expect(request.request.params.keys()).toEqual([]);
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

  it("gets, creates, updates, and deactivates categories with the API contract", async () => {
    const getResponse = firstValueFrom(service.get(CATEGORY_ID));
    const getRequest = http.expectOne(`${CATEGORY_URL}/${CATEGORY_ID}`);
    expect(getRequest.request.method).toBe("GET");
    getRequest.flush({
      id: CATEGORY_ID,
      name: "Tools",
      description: null,
      active: true,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });
    await getResponse;

    const body = { name: "Tools", description: null };
    const createResponse = firstValueFrom(service.create(body));
    const createRequest = http.expectOne(CATEGORY_URL);
    expect(createRequest.request.method).toBe("POST");
    expect(createRequest.request.body).toEqual(body);
    createRequest.flush(
      {
        id: CATEGORY_ID,
        ...body,
        active: true,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
      { status: 201, statusText: "Created" },
    );
    await createResponse;

    const updateResponse = firstValueFrom(service.update(CATEGORY_ID, body));
    const updateRequest = http.expectOne(`${CATEGORY_URL}/${CATEGORY_ID}`);
    expect(updateRequest.request.method).toBe("PUT");
    expect(updateRequest.request.body).toEqual(body);
    updateRequest.flush({
      id: CATEGORY_ID,
      ...body,
      active: true,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-02T00:00:00Z",
    });
    await updateResponse;

    let completed = false;
    service.deactivate(CATEGORY_ID).subscribe(() => (completed = true));
    const deleteRequest = http.expectOne(`${CATEGORY_URL}/${CATEGORY_ID}`);
    expect(deleteRequest.request.method).toBe("DELETE");
    deleteRequest.flush(null, { status: 204, statusText: "No Content" });
    expect(completed).toBe(true);
  });

  it("propagates HTTP errors without replacing Problem Details", async () => {
    const problem = {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      detail: "Category name already exists",
    };
    const response = firstValueFrom(service.get(CATEGORY_ID));
    http.expectOne(`${CATEGORY_URL}/${CATEGORY_ID}`).flush(problem, {
      status: 409,
      statusText: "Conflict",
    });

    await expect(response).rejects.toMatchObject({
      status: 409,
      error: problem,
    });
  });
});
