import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { Observable, catchError, finalize, of, shareReplay, tap } from "rxjs";
import { environment } from "../../environments/environment";

export interface CurrentIdentity {
  userId: string;
  authorities: string[];
}

export type IdentityLoadState =
  | "loading"
  | "loaded"
  | "unauthenticated"
  | "forbidden"
  | "error";

@Injectable({ providedIn: "root" })
export class IdentityApiService {
  private readonly http = inject(HttpClient);
  readonly state = signal<IdentityLoadState>("loading");
  readonly identity = signal<CurrentIdentity | null>(null);
  private request: Observable<CurrentIdentity | null> | null = null;

  load(): void {
    this.ensureLoaded().subscribe();
  }

  ensureLoaded(): Observable<CurrentIdentity | null> {
    if (this.state() === "loaded") return of(this.identity());
    if (this.request) return this.request;

    this.state.set("loading");
    this.request = this.http
      .get<CurrentIdentity>(`${environment.apiBaseUrl}/v1/platform/me`)
      .pipe(
        tap((identity) => {
          this.identity.set(identity);
          this.state.set("loaded");
        }),
        catchError((error: unknown) => {
          this.identity.set(null);
          const status = error instanceof HttpErrorResponse ? error.status : 0;
          this.state.set(
            status === 401
              ? "unauthenticated"
              : status === 403
                ? "forbidden"
                : "error",
          );
          return of(null);
        }),
        finalize(() => (this.request = null)),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
    return this.request;
  }
}
