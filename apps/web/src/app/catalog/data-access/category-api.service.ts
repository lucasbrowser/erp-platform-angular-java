import { HttpClient } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import {
  Category,
  CategoryListQuery,
  CategoryWriteRequest,
  PageResponse,
} from "../models/catalog.models";
import { catalogResourceUrl, pageableHttpParams } from "./catalog-api.utils";

@Injectable({ providedIn: "root" })
export class CategoryApiService {
  private readonly http = inject(HttpClient);
  private readonly url = catalogResourceUrl("categories");

  list(query: CategoryListQuery = {}): Observable<PageResponse<Category>> {
    return this.http.get<PageResponse<Category>>(this.url, {
      params: pageableHttpParams(query),
    });
  }

  get(id: string): Observable<Category> {
    return this.http.get<Category>(`${this.url}/${encodeURIComponent(id)}`);
  }

  create(request: CategoryWriteRequest): Observable<Category> {
    return this.http.post<Category>(this.url, request);
  }

  update(id: string, request: CategoryWriteRequest): Observable<Category> {
    return this.http.put<Category>(
      `${this.url}/${encodeURIComponent(id)}`,
      request,
    );
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${encodeURIComponent(id)}`);
  }
}
