/** A category returned by the catalog API. */
export interface Category {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Complete body accepted by both POST and PUT category endpoints. */
export interface CategoryWriteRequest {
  name: string;
  description?: string | null;
}

export type CreateCategoryRequest = CategoryWriteRequest;
export type UpdateCategoryRequest = CategoryWriteRequest;

/** A product returned by the catalog API. Price is the JSON number emitted by BigDecimal. */
export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  categoryId: string;
  /** JavaScript number cannot exactly represent every decimal in the NUMERIC(14,2) range. */
  price: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Complete body accepted by both POST and PUT product endpoints. */
export interface ProductWriteRequest {
  sku: string;
  name: string;
  description?: string | null;
  categoryId: string;
  /** Pass through as supplied; this HTTP layer performs no conversion or rounding. */
  price: number;
}

export type CreateProductRequest = ProductWriteRequest;
export type UpdateProductRequest = ProductWriteRequest;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PageableQuery {
  page?: number;
  size?: number;
  /** Spring Pageable accepts repeated sort parameters in property,direction form. */
  sort?: string | readonly string[];
}

export type CategoryListQuery = PageableQuery;

export interface ProductListQuery extends PageableQuery {
  name?: string;
  sku?: string;
  categoryId?: string;
  active?: boolean;
}
