import { Routes } from "@angular/router";
import { authGuard } from "./auth/auth.guard";
import {
  categoryReadGuard,
  categoryWriteGuard,
} from "./catalog/categories/category-access.guard";
import { CategoryFormComponent } from "./catalog/categories/category-form.component";
import { CategoryListComponent } from "./catalog/categories/category-list.component";
import { ProductFormComponent } from "./catalog/products/product-form.component";
import { ProductListComponent } from "./catalog/products/product-list.component";
import { AccessDeniedComponent } from "./pages/access-denied.component";
import { AuthCallbackComponent } from "./pages/auth-callback.component";
import { HomeComponent } from "./pages/home.component";
import { LandingComponent } from "./pages/landing.component";

export const routes: Routes = [
  { path: "", component: LandingComponent, pathMatch: "full" },
  { path: "auth/callback", component: AuthCallbackComponent },
  { path: "access-denied", component: AccessDeniedComponent },
  { path: "home", component: HomeComponent, canActivate: [authGuard] },
  {
    path: "catalog/products",
    component: ProductListComponent,
    canActivate: [authGuard, categoryReadGuard],
  },
  {
    path: "catalog/products/new",
    component: ProductFormComponent,
    canActivate: [authGuard, categoryReadGuard, categoryWriteGuard],
  },
  {
    path: "catalog/products/:id/edit",
    component: ProductFormComponent,
    canActivate: [authGuard, categoryReadGuard, categoryWriteGuard],
  },
  {
    path: "catalog/categories",
    component: CategoryListComponent,
    canActivate: [authGuard, categoryReadGuard],
  },
  {
    path: "catalog/categories/new",
    component: CategoryFormComponent,
    canActivate: [authGuard, categoryReadGuard, categoryWriteGuard],
  },
  {
    path: "catalog/categories/:id/edit",
    component: CategoryFormComponent,
    canActivate: [authGuard, categoryReadGuard, categoryWriteGuard],
  },
  { path: "**", redirectTo: "" },
];
