import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../environments/environment";

export interface CurrentIdentity {
  userId: string;
  authorities: string[];
}

export type IdentityLoadState = "loading" | "loaded" | "unauthenticated" | "forbidden" | "error";

@Injectable({ providedIn: "root" })
export class IdentityApiService {
  private readonly http = inject(HttpClient);
  readonly state = signal<IdentityLoadState>("loading");
  readonly identity = signal<CurrentIdentity | null>(null);

  load(): void {
    this.state.set("loading");
    this.http
      .get<CurrentIdentity>(`${environment.apiBaseUrl}/v1/platform/me`)
      .subscribe({
        next: (identity) => {
          this.identity.set(identity);
          this.state.set("loaded");
        },
        error: (error: unknown) => {
          this.identity.set(null);
          const status = error instanceof HttpErrorResponse ? error.status : 0;
          this.state.set(status === 401 ? "unauthenticated" : status === 403 ? "forbidden" : "error");
        },
      });
  }
}
