import { Component, inject } from "@angular/core";
import { Router } from "@angular/router";
import { AuthService } from "../auth/auth.service";

@Component({
  standalone: true,
  template: `<p role="status">Concluindo autenticação…</p>`,
})
export class AuthCallbackComponent {
  constructor() {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.status() === "authenticated") {
      void router.navigateByUrl(auth.takeReturnPath(), { replaceUrl: true });
    } else {
      void router.navigateByUrl("/access-denied?reason=authentication", { replaceUrl: true });
    }
  }
}
