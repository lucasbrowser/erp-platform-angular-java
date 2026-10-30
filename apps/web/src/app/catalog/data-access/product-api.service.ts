import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable } from "rxjs";
import {
  PageResponse,
  Product,
  ProductListQuery,
  ProductWriteRequest,
} from "../models/catalog.models";
import {
  appendNonEmptyParam,
  catalogResourceUrl,
  pageableHttpParams,
} from "./catalog-api.utils";

@Injectable({ providedIn: "root" })
export class ProductApiService {
  private readonly http = inject(HttpClient);
  private readonly url = catalogResourceUrl("products");

  list(query: ProductListQuery = {}): Observable<PageResponse<Product>> {
    let params: HttpParams = pageableHttpParams(query);
    params = appendNonEmptyParam(params, "name", query.name);
    params = appendNonEmptyParam(params, "sku", query.sku);
    params = appendNonEmptyParam(params, "categoryId", query.categoryId);
    if (query.active !== undefined) {
      params = params.set("active", query.active);
    }

    return this.http.get<PageResponse<Product>>(this.url, { params });
  }

  get(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${encodeURIComponent(id)}`);
  }

  create(request: ProductWriteRequest): Observable<Product> {
    return this.http.post<Product>(this.url, request);
  }

  update(id: string, request: ProductWriteRequest): Observable<Product> {
    return this.http.put<Product>(
      `${this.url}/${encodeURIComponent(id)}`,
      request,
    );
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${encodeURIComponent(id)}`);
  }
}
