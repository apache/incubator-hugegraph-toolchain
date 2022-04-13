export const baseUrl = '/api/v1.2/graph-connections';

export type dict<T> = Record<string, T>;

export interface responseData<T> {
  status: number;
  data: T;
  message: string;
}
