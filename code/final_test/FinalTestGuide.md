# final_test 使用说明

这套目录用于最终演示，文件全部放在同一层级，便于一次性上传。

当前组成：

- `22` 个 Java 源文件
- `25` 个模型文件（`xml / xmi / oom`）
- `1` 个说明文件
- `1` 个压缩包 `final_test_bundle.zip`
- 合计 `49` 个可直接展示文件

建议展示方式：

1. 先新建一个项目，例如 `final-test-demo`
2. 直接上传本目录全部文件，观察：
   - 项目管理：文件数量、类型分布
   - 代码行度量：Java 文件数量、LoC、SLoC
   - 控制流图度量：`EnrollmentService / FinanceService / ReviewWorkflowService / AccessControlService`
   - 面向对象度量：继承、接口、耦合、RFC、WMC、LCOM
   - 模型文件度量：`final-class-model.*`
   - 功能点度量：`final_fp_*` 和 `final_ucp_*`
   - 用例图度量：`final_ucp_*`
   - 估算分析：依赖功能点 / 用例点 / LoC 结果
   - 智能分析：综合输出

重点观察：

- Java 文件不少于 20 个
- 模型文件同时覆盖 `.xml / .xmi / .oom`
- 还额外提供一个 `final_test_bundle.zip`

重点复杂方法：

- `EnrollmentService.enroll`
- `EnrollmentService.determinePriority`
- `FinanceService.settle`
- `ReviewWorkflowService.approve`
- `AccessControlService.classifyZone`

功能点 / 用例点建议关注的模型文件：

- `dfd-course-enrollment.xml`
- `dfd-finance-approval.oom`
- `dfd-device-maintenance.xmi`
- `usecase-campus-payroll.xml`
- `usecase-internship-management.xmi`
- `usecase-dormitory-access.oom`

模型文件度量建议关注的类模型文件：

- `final-class-model.xml`
- `final-class-model.xmi`
- `final-class-model.oom`
- `final-class-model-analytics.xml`
- `final-class-model-finance.xmi`
- `final-class-model-library.oom`
- `final-class-model-dormitory.xml`

重点面向对象关系：

- `GraduateStudent -> Student -> Person`
- `Teacher -> Person`
- `WeightedScorePolicy -> ScorePolicy`
- `ThresholdRiskPolicy -> RiskPolicy`
- `EmailNotificationGateway -> NotificationGateway`
