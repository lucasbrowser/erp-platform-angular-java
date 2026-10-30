import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { provideRouter } from "@angular/router";
import { IdentityApiService } from "../../auth/identity-api.service";
import { CategoryListComponent } from "./category-list.component";

const URL = "http://localhost:8082/api/v1/categories";
const page = (content: unknown[], current = 0, last = true) => ({
  content,
  page: current,
  size: 20,
  totalElements: content.length,
  totalPages: last ? current + 1 : current + 2,
  first: current === 0,
  last,
});

describe("CategoryListComponent", () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<CategoryListComponent>;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [CategoryListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: IdentityApiService,
          useValue: {
            identity: signal({ userId: "u1", authorities: ["ROLE_ADMIN"] }),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(CategoryListComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it("loads a zero-based page with the backend default sort and shows inactive state", () => {
    fixture.detectChanges();
    const request = http.expectOne((req) => req.url === URL);
    expect(request.request.params.get("page")).toBe("0");
    expect(request.request.params.get("size")).toBe("20");
    expect(request.request.params.get("sort")).toBe("name,asc");
    request.flush(
      page([
        {
          id: "1",
          name: "Antiga",
          description: null,
          active: false,
          createdAt: "",
          updatedAt: "",
        },
      ]),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Inativa");
    expect(fixture.nativeElement.textContent).toContain("Editar");
  });

  it("loads the next page and renders the empty state", () => {
    fixture.detectChanges();
    http
      .expectOne((req) => req.url === URL)
      .flush(
        page(
          [
            {
              id: "1",
              name: "Papel",
              description: null,
              active: true,
              createdAt: "",
              updatedAt: "",
            },
          ],
          0,
          false,
        ),
      );
    fixture.detectChanges();
    const next = fixture.nativeElement.querySelector(
      ".pagination button:last-child",
    ) as HTMLButtonElement;
    next.click();
    const request = http.expectOne((req) => req.url === URL);
    expect(request.request.params.get("page")).toBe("1");
    request.flush(page([], 1));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      "Não há categorias cadastradas.",
    );
  });

  it("shows an HTTP error and retries the page", () => {
    fixture.detectChanges();
    http
      .expectOne((req) => req.url === URL)
      .flush({}, { status: 500, statusText: "Server Error" });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      "Não foi possível carregar",
    );
    fixture.nativeElement.querySelector("button").click();
    http.expectOne((req) => req.url === URL).flush(page([]));
  });

  it("requires confirmation and refreshes after the 204 deactivation", () => {
    fixture.detectChanges();
    http
      .expectOne((req) => req.url === URL)
      .flush(
        page([
          {
            id: "1",
            name: "Papel",
            description: null,
            active: true,
            createdAt: "",
            updatedAt: "",
          },
        ]),
      );
    fixture.detectChanges();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    fixture.nativeElement.querySelector(".actions button").click();
    expect(confirm).toHaveBeenCalledOnce();
    http.expectNone(`${URL}/1`);
    confirm.mockReturnValue(true);
    fixture.nativeElement.querySelector(".actions button").click();
    http
      .expectOne(`${URL}/1`)
      .flush(null, { status: 204, statusText: "No Content" });
    http
      .expectOne((req) => req.url === URL)
      .flush(
        page([
          {
            id: "1",
            name: "Papel",
            description: null,
            active: false,
            createdAt: "",
            updatedAt: "",
          },
        ]),
      );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      "Categoria desativada.",
    );
    expect(fixture.nativeElement.textContent).toContain("Inativa");
    confirm.mockRestore();
  });
});
