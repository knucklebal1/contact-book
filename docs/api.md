# 联系人后端接口文档

基础地址：`http://localhost:8080`

Swagger UI：`http://localhost:8080/swagger-ui.html`

所有 JSON 接口统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

## 接口列表

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| GET | `/api/contacts` | 分页查询联系人 |
| GET | `/api/contacts/{id}` | 联系人详情 |
| POST | `/api/contacts` | 新增联系人 |
| PUT | `/api/contacts/{id}` | 编辑联系人和联系方式 |
| DELETE | `/api/contacts/{id}` | 删除联系人 |
| PATCH | `/api/contacts/{id}/favorite` | 设置或取消收藏 |
| POST | `/api/contacts/import` | 导入 `.xlsx` 文件 |
| GET | `/api/contacts/export` | 导出联系人 |
| GET | `/api/contacts/import-template` | 下载导入模板 |

查询参数：

- `keyword`：姓名、公司、职位、备注或联系方式模糊搜索。
- `isFavorite`：`true` 查询收藏，`false` 查询未收藏，不传查询全部。
- `page`：从 1 开始，默认 1。
- `pageSize`：默认 20，最大 100。

## 新增或编辑联系人

```json
{
  "name": "张三",
  "company": "示例公司",
  "jobTitle": "工程师",
  "note": "重要客户",
  "isFavorite": true,
  "methods": [
    {
      "methodType": "PHONE",
      "methodValue": "13800000000",
      "sortOrder": 0
    },
    {
      "methodType": "EMAIL",
      "methodValue": "zhangsan@example.com",
      "sortOrder": 1
    },
    {
      "methodType": "SOCIAL",
      "customLabel": "微博",
      "methodValue": "zhangsan",
      "sortOrder": 2
    }
  ]
}
```

联系方式类型：`PHONE`、`EMAIL`、`WECHAT`、`QQ`、`SOCIAL`、`ADDRESS`、`OTHER`。

## 收藏

```http
PATCH /api/contacts/1/favorite
Content-Type: application/json

{"isFavorite": true}
```

接口使用明确的 `true/false` 状态，前端重复点击不会导致状态反向。

## Excel 格式

固定表头：

```text
姓名 | 公司 | 职位 | 备注 | 收藏 | 电话 | 邮箱 | 微信 | QQ | 社交账号 | 地址 | 其他
```

- 收藏填写“是”或“否”。
- 同一类型的多个值在单元格内换行，或使用中英文分号分隔。
- 社交账号和其他可写成“平台:账号”。
- 只支持不超过 5 MB 的 `.xlsx` 文件。
- 有错误行时，合法行正常导入，错误行号和原因会返回给前端。

导入返回示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 3,
    "success": 2,
    "failed": 1,
    "errors": [
      {
        "row": 4,
        "message": "邮箱格式不正确：invalid-email"
      }
    ]
  }
}
```
