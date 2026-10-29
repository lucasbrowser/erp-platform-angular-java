import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { from, switchMap } from "rxjs";
import { environment } from "../../environments/environment";
import { AuthService } from "./auth.service";

function isApiRequest(requestUrl: string): boolean {
  if (!environment.apiBaseUrl) return false;
  try {
    const api = new URL(environment.apiBaseUrl);
    const request = new URL(requestUrl, document.baseURI);
    const apiPath = api.pathname.replace(/\/$/, "");
    return (
      request.origin === api.origin &&
      (request.pathname === apiPath ||
        request.pathname.startsWith(`${apiPath}/`))
    );
  } catch {
    return false;
  }
}

export const apiTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url)) return next(request);
  const auth = inject(AuthService);
  if (!auth.client.authenticated) return next(request);

  return from(auth.client.updateToken(30)).pipe(
    switchMap(() => {
      const token = auth.client.token;
      return next(
        token
          ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
          : request,
      );
    }),
  );
};
