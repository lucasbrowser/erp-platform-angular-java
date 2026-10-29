import { TestBed } from "@angular/core/testing";
import { AuthService } from "./auth/auth.service";
import { AppComponent } from "./app.component";

describe("AppComponent", () => {
  it("renders the technical foundation landing page", async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: AuthService, useValue: { status: () => "anonymous" } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector("h1").textContent).toContain(
      "ERP Core",
    );
  });
});
