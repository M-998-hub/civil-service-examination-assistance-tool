import request from './request'

// 获取支持的导入类型列表
export const getImportTypes = () => {
  return request({
    url: '/admin/import/types',
    method: 'get',
  })
}

// 获取指定导入类型的字段元数据
export const getFieldMetas = (type) => {
  return request({
    url: '/admin/import/fields',
    method: 'get',
    params: { type },
  })
}

// 上传Excel并预览
export const uploadExcel = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/import/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 执行导入
export const executeImport = (data) => {
  return request({
    url: '/admin/import/execute',
    method: 'post',
    data,
  })
}

// 获取模板列表（按类型过滤）
export const getTemplateList = (importType) => {
  return request({
    url: '/admin/import/templates',
    method: 'get',
    params: importType ? { importType } : {},
  })
}

// 删除模板
export const deleteTemplate = (id) => {
  return request({
    url: `/admin/import/template/${id}`,
    method: 'delete',
  })
}

// 保存模板
export const saveTemplate = (data) => {
  return request({
    url: '/admin/import/template',
    method: 'post',
    data,
  })
}
