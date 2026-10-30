import { HttpParams } from "@angular/common/http";
import { environment } from "../../../environments/environment";
import { PageableQuery } from "../models/catalog.models";

const API_BASE_URL = environment.apiBaseUrl.replace(/\/+$/, "");

export function catalogResourceUrl(
  resource: "categories" | "products",
): string {
  return `${API_BASE_URL}/v1/${resource}`;
}

export function pageableHttpParams(query: PageableQuery): HttpParams {
  let params = new HttpParams();

  if (query.page !== undefined) params = params.set("page", query.page);
  if (query.size !== undefined) params = params.set("size", query.size);

  if (typeof query.sort === "string") {
    params = params.append("sort", query.sort);
  } else {
    for (const sort of query.sort ?? []) params = params.append("sort", sort);
  }

  return params;
}

export function appendNonEmptyParam(
  params: HttpParams,
  name: string,
  value: string | undefined,
): HttpParams {
  return value === undefined || value === "" ? params : params.set(name, value);
}
