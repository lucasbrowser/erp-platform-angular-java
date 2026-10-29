import { Injectable, signal } from "@angular/core";
import Keycloak from "keycloak-js";
import { environment } from "../../environments/environment";

export type AuthStatus = "loading" | "authenticated" | "anonymous" | "error";

const RETURN_PATH_KEY = "erp-core.auth.return-path";

@Injectable({ providedIn: "root" })
export class AuthService {
  readonly status = signal<AuthStatus>("loading");

  constructor(readonly client: Keycloak) {}

  get roles(): string[] {
    const claims = this.client.tokenParsed as Record<string, unknown> | undefined;
    const roles = claims?.["realm_access"] as
      | { roles?: unknown }
      | undefined;
    return Array.isArray(roles?.roles)
      ? roles.roles.filter((role): role is string => typeof role === "string")
      : [];
  }

  async initialize(): Promise<void> {
    if (!environment.keycloak.url || !environment.keycloak.realm || !environment.keycloak.clientId) {
      this.status.set("error");
      return;
    }
    try {
      const authenticated = await this.client.init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        responseMode: "query",
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        checkLoginIframe: false,
      });
      this.status.set(authenticated ? "authenticated" : "anonymous");
    } catch {
      this.status.set("error");
    }
  }

  async login(returnPath: string): Promise<void> {
    const safePath = this.safeReturnPath(returnPath);
    if (safePath) {
      sessionStorage.setItem(RETURN_PATH_KEY, safePath);
    }
    await this.client.login({
      redirectUri: `${window.location.origin}/auth/callback`,
    });
  }

  takeReturnPath(): string {
    const path = sessionStorage.getItem(RETURN_PATH_KEY) ?? "/home";
    sessionStorage.removeItem(RETURN_PATH_KEY);
    return this.safeReturnPath(path) ?? "/home";
  }

  async logout(): Promise<void> {
    sessionStorage.removeItem(RETURN_PATH_KEY);
    await this.client.logout({ redirectUri: window.location.origin });
  }

  private safeReturnPath(value: string): string | null {
    if (!value.startsWith("/") || value.startsWith("//")) return null;
    try {
      const parsed = new URL(value, window.location.origin);
      if (parsed.origin !== window.location.origin) return null;
      return `${parsed.pathname}${parsed.search}${parsed.hash}`;
    } catch {
      return null;
    }
  }
}
