import { ApplicationConfig, inject, provideAppInitializer } from "@angular/core";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter } from "@angular/router";
import Keycloak from "keycloak-js";
import { environment } from "../environments/environment";
import { apiTokenInterceptor } from "./auth/api-token.interceptor";
import { AuthService } from "./auth/auth.service";
import { routes } from "./app.routes";

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiTokenInterceptor])),
    {
      provide: Keycloak,
      useFactory: () =>
        new Keycloak({
          url: environment.keycloak.url,
          realm: environment.keycloak.realm,
          clientId: environment.keycloak.clientId,
        }),
    },
    provideAppInitializer(() => inject(AuthService).initialize()),
  ],
};
