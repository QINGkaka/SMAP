# API 测试请求体

这些 JSON 文件用于直接调用后端接口，验证手工输入型模块。

建议用法：

```bash
curl -X POST http://localhost:8080/api/projects/<projectId>/function-point/analyze \
  -H "Content-Type: application/json" \
  -d @/Users/xiazixuan/大三下/软件质量保证/exp/code/api-payloads/function-point-detailed.json
```

```bash
curl -X POST http://localhost:8080/api/projects/<projectId>/use-case-point/analyze \
  -H "Content-Type: application/json" \
  -d @/Users/xiazixuan/大三下/软件质量保证/exp/code/api-payloads/use-case-manual.json
```

```bash
curl -X POST http://localhost:8080/api/projects/<projectId>/estimation/analyze \
  -H "Content-Type: application/json" \
  -d @/Users/xiazixuan/大三下/软件质量保证/exp/code/api-payloads/estimation-organic.json
```
