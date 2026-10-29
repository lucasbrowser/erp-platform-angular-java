import Keycloak from "keycloak-js";
import { TestBed } from "@angular/core/testing";
import { AuthService } from "./auth.service";

describe("AuthService", () => {
  let auth: AuthService;
  let client: {
    init: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    tokenParsed: Record<string, unknown>;
  };

  beforeEach(() => {
    sessionStorage.clear();
    client = {
      init: vi.fn().mockResolvedValue(true),
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      tokenParsed: { realm_access: { roles: ["SALES", 3, "MANAGER"] } },
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: client as unknown as Keycloak },
      ],
    });
    auth = TestBed.inject(AuthService);
  });

  it("initializes the OIDC adapter with silent SSO and PKCE S256", async () => {
    await auth.initialize();
    expect(client.init).toHaveBeenCalledWith(
      expect.objectContaining({ onLoad: "check-sso", pkceMethod: "S256" }),
    );
    expect(auth.status()).toBe("authenticated");
  });

  it("exposes only string realm roles from the parsed access token", () => {
    expect(auth.roles).toEqual(["SALES", "MANAGER"]);
  });

  it("keeps only same-origin internal return paths and delegates login", async () => {
    await auth.login("/home?tab=overview");
    expect(auth.takeReturnPath()).toBe("/home?tab=overview");
    await auth.login("//attacker.example/path");
    expect(auth.takeReturnPath()).toBe("/home");
    expect(client.login).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/auth/callback`,
    });
  });
});
