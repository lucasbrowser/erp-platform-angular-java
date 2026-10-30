import { TestBed } from "@angular/core/testing";
import { provideRouter, UrlTree } from "@angular/router";
import { of } from "rxjs";
import { IdentityApiService } from "../../auth/identity-api.service";
import { routes } from "../../app.routes";
import { categoryReadGuard, categoryWriteGuard } from "./category-access.guard";

describe("category authorization", () => {
  beforeEach(() => TestBed.resetTestingModule());
  afterEach(() => TestBed.resetTestingModule());

  it("allows only catalog readers and ADMIN/MANAGER writers", async () => {
    let authorities = ["ROLE_SALES"];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: IdentityApiService,
          useValue: {
            ensureLoaded: () =>
              of({
                userId: "u",
                get authorities() {
                  return authorities;
                },
              }),
          },
        },
      ],
    });
    const route = {} as never;
    const state = {} as never;
    await expect(
      TestBed.runInInjectionContext(() => categoryReadGuard(route, state)),
    ).resolves.toBe(true);
    const deniedWriter = await TestBed.runInInjectionContext(() =>
      categoryWriteGuard(route, state),
    );
    expect(deniedWriter).toBeInstanceOf(UrlTree);
    authorities = ["ROLE_FINANCE"];
    const deniedReader = await TestBed.runInInjectionContext(() =>
      categoryReadGuard(route, state),
    );
    expect(deniedReader).toBeInstanceOf(UrlTree);
    authorities = ["ROLE_ADMIN"];
    await expect(
      TestBed.runInInjectionContext(() => categoryWriteGuard(route, state)),
    ).resolves.toBe(true);
  });

  it("protects category read and write routes with authentication and role guards", () => {
    expect(
      routes.find((route) => route.path === "catalog/categories")?.canActivate,
    ).toHaveLength(2);
    expect(
      routes.find((route) => route.path === "catalog/categories/new")
        ?.canActivate,
    ).toHaveLength(3);
    expect(
      routes.find((route) => route.path === "catalog/categories/:id/edit")
        ?.canActivate,
    ).toHaveLength(3);
  });
});
