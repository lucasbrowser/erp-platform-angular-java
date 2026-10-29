import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideHttpClientTesting, HttpTestingController } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import Keycloak from "keycloak-js";
import { AuthService } from "./auth.service";
import { apiTokenInterceptor } from "./api-token.interceptor";
import { IdentityApiService } from "./identity-api.service";

describe("IdentityApiService", () => {
  let api: IdentityApiService;
  let backend: HttpTestingController;
  let updateToken: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    updateToken = vi.fn().mockResolvedValue(false);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiTokenInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            client: {
              authenticated: true,
              token: "mock-token",
              updateToken,
            } as unknown as Keycloak,
          } as unknown as AuthService,
        },
      ],
    });
    api = TestBed.inject(IdentityApiService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it("loads the current identity through the protected API contract", async () => {
    api.load();
    await vi.waitFor(() => expect(updateToken).toHaveBeenCalledOnce());
    const request = backend.expectOne("http://localhost:8080/api/v1/platform/me");
    expect(request.request.headers.get("Authorization")).toBe("Bearer mock-token");
    request.flush({ userId: "subject-id", authorities: ["ROLE_SALES"] });
    await vi.waitFor(() => expect(api.state()).toBe("loaded"));
    expect(api.identity()).toEqual({ userId: "subject-id", authorities: ["ROLE_SALES"] });
  });

  it("distinguishes forbidden and expired or missing authentication responses", async () => {
    api.load();
    await vi.waitFor(() => expect(updateToken).toHaveBeenCalledOnce());
    backend.expectOne("http://localhost:8080/api/v1/platform/me").flush({}, { status: 403, statusText: "Forbidden" });
    await vi.waitFor(() => expect(api.state()).toBe("forbidden"));
    updateToken.mockClear();
    api.load();
    await vi.waitFor(() => expect(updateToken).toHaveBeenCalledOnce());
    backend.expectOne("http://localhost:8080/api/v1/platform/me").flush({}, { status: 401, statusText: "Unauthorized" });
    await vi.waitFor(() => expect(api.state()).toBe("unauthenticated"));
  });
});
