# 数据库设计

后端采用两张表实现联系人与联系方式的一对多关系。

```text
contact 1 ───── N contact_method
```

## contact

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 姓名，必填 |
| company | VARCHAR(150) | 公司 |
| job_title | VARCHAR(100) | 职位 |
| note | VARCHAR(500) | 备注 |
| is_favorite | BOOLEAN | 是否收藏 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## contact_method

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 主键 |
| contact_id | BIGINT | 所属联系人 |
| method_type | VARCHAR(20) | 联系方式类型 |
| custom_label | VARCHAR(32) | 自定义平台名称 |
| method_value | VARCHAR(500) | 联系方式内容 |
| sort_order | INT | 展示顺序 |
| created_at | DATETIME | 创建时间 |

`contact_method.contact_id` 设置外键并级联删除。编辑联系人时，旧联系方式通过 `orphanRemoval` 删除，新联系方式在同一事务中保存。
