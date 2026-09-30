import request from './request'

export const listIngestionRuns = () => request.get('/admin/ingestion/runs')
export const listIngestionCandidates = (id, pageNum, pageSize) =>
  request.get(`/admin/ingestion/runs/${id}/candidates`, { params: { pageNum, pageSize } })
export const startIngestion = (payload) => request.post('/admin/ingestion/runs', payload)
export const publishIngestion = (id) => request.post(`/admin/ingestion/runs/${id}/publish`)
export const rejectIngestion = (id) => request.post(`/admin/ingestion/runs/${id}/reject`)
