import request from './request'

// 上传Excel并预览
export const uploadExcel = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/import/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 执行导入
export const executeImport = (data) => {
  return request({
    url: '/admin/import/execute',
    method: 'post',
    data
  })
}

// 获取模板列表
export const getTemplateList = () => {
  return request({
    url: '/admin/import/templates',
    method: 'get'
  })
}

// 删除模板
export const deleteTemplate = (id) => {
  return request({
    url: `/admin/import/template/${id}`,
    method: 'delete'
  })
}
