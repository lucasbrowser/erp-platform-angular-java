import { Routes } from "@angular/router";
import { authGuard } from "./auth/auth.guard";
import { AccessDeniedComponent } from "./pages/access-denied.component";
import { AuthCallbackComponent } from "./pages/auth-callback.component";
import { HomeComponent } from "./pages/home.component";
import { LandingComponent } from "./pages/landing.component";

export const routes: Routes = [
  { path: "", component: LandingComponent, pathMatch: "full" },
  { path: "auth/callback", component: AuthCallbackComponent },
  { path: "access-denied", component: AccessDeniedComponent },
  { path: "home", component: HomeComponent, canActivate: [authGuard] },
  { path: "**", redirectTo: "" },
];
