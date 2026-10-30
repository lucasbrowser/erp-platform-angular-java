import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { IdentityApiService } from "../../auth/identity-api.service";

const READ_AUTHORITIES = [
  "ROLE_ADMIN",
  "ROLE_MANAGER",
  "ROLE_SALES",
  "ROLE_STOCK_OPERATOR",
];
const WRITE_AUTHORITIES = ["ROLE_ADMIN", "ROLE_MANAGER"];

export const categoryReadGuard: CanActivateFn = async () => {
  const identity = inject(IdentityApiService);
  const router = inject(Router);
  const current = await firstValueFrom(identity.ensureLoaded());
  return current?.authorities.some((role) => READ_AUTHORITIES.includes(role))
    ? true
    : router.parseUrl("/access-denied?reason=catalog");
};

export const categoryWriteGuard: CanActivateFn = async () => {
  const identity = inject(IdentityApiService);
  const router = inject(Router);
  const current = await firstValueFrom(identity.ensureLoaded());
  return current?.authorities.some((role) => WRITE_AUTHORITIES.includes(role))
    ? true
    : router.parseUrl("/access-denied?reason=catalog");
};
