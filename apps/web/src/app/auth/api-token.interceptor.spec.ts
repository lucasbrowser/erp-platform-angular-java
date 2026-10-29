import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { firstValueFrom } from "rxjs";
import Keycloak from "keycloak-js";
import { AuthService } from "./auth.service";
import { apiTokenInterceptor } from "./api-token.interceptor";

describe("apiTokenInterceptor", () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let updateToken: ReturnType<typeof vi.fn>;
  let client: Keycloak;

  beforeEach(() => {
    updateToken = vi.fn().mockResolvedValue(false);
    client = {
      authenticated: true,
      token: "test-access-token",
      updateToken,
    } as unknown as Keycloak;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiTokenInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { client } as unknown as AuthService,
        },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it("adds a refreshed bearer only to the configured API origin and path", async () => {
    const request = firstValueFrom(
      http.get("http://localhost:8080/api/v1/platform/me"),
    );
    await vi.waitFor(() => expect(updateToken).toHaveBeenCalledOnce());
    const pending = backend.expectOne(
      "http://localhost:8080/api/v1/platform/me",
    );
    expect(pending.request.headers.get("Authorization")).toBe(
      "Bearer test-access-token",
    );
    pending.flush({ userId: "subject", authorities: [] });
    await request;
  });

  it("passes Keycloak and unrelated host requests without a bearer", async () => {
    const request = firstValueFrom(
      http.get("http://localhost:8081/realms/erp-core"),
    );
    const pending = backend.expectOne("http://localhost:8081/realms/erp-core");
    expect(pending.request.headers.has("Authorization")).toBe(false);
    pending.flush({});
    await request;
    expect(updateToken).not.toHaveBeenCalled();
  });

  it("does not attach a token to the API when the session is anonymous", async () => {
    (client as unknown as { authenticated: boolean }).authenticated = false;
    const request = firstValueFrom(
      http.get("http://localhost:8080/api/v1/platform/me"),
    );
    const pending = backend.expectOne(
      "http://localhost:8080/api/v1/platform/me",
    );
    expect(pending.request.headers.has("Authorization")).toBe(false);
    pending.flush({});
    await request;
    expect(updateToken).not.toHaveBeenCalled();
  });
});
