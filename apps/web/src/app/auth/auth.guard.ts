import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "./auth.service";

export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.status() === "authenticated") return true;
  if (auth.status() === "error") return router.parseUrl("/access-denied?reason=identity-provider");
  await auth.login(state.url);
  return false;
};
