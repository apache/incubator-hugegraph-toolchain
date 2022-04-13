export interface AsyncTask {
  id: number;
  task_type: string;
  task_name: string;
  task_status: string;
  task_create: string;
  task_update: string;
  task_result: string;
}

export interface AsyncTaskListResponse {
  records: AsyncTask[];
  total: number;
}
