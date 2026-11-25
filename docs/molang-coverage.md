# Molang 解释执行合同

`3.0.3-ec.4` 提供通用解析器、解释器和数学函数。`eval` 解释执行；`prepareEval` 只预解析并保存 AST，每次执行读取当前绑定并建立独立 temp。原有显式 `compile` 接口保持上游实现，不属于游戏默认后端，也不承诺本页扩充语义的编译覆盖。当前分支不提供实验性全局字节码编译器和程序缓存。

## 官方依据

- [Molang Syntax Guide](https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/syntax-guide?view=minecraft-bedrock-stable)：类型、运算符、变量、结构体、数组和控制流。
- [Math Functions](https://github.com/MicrosoftDocs/minecraft-creator/blob/main/creator/Reference/Content/MolangReference/Examples/MolangConcepts/MathFunctions.md)：核对目录保存在 `src/test/resources/official-math-functions.txt`，包含60个函数与math.pi。
- 官方依据核对日期为2026-11-04。30个easing函数及inverse_lerp声明最低资源格式1.21.120；宿主负责版本许可，库中存在函数不等于旧BE引擎支持。

## 解释语义与边界

| 能力 | 规则 |
| --- | --- |
| 标识符和数字 | 标识符大小写无关、字符串大小写敏感；支持f后缀、前导小数与科学计数法 |
| 运算与分支 | 短路保持副作用次序；嵌套三元与合并右结合；缺失值与0、false、空字符串分开 |
| 变量与结构 | v/variable、q/query、t/temp、c/context别名；按需创建缺失结构；context赋值不可写 |
| 数组与实体 | 索引整数截断、负数夹零、超界取模、空数组返回缺失；ActorReference由宿主提供许可作用域和有效性 |
| 控制流 | 块、return、loop、for_each、break、continue；嵌套return向外传播；高层多语句无return时结果为0 |
| 宿主回调 | 参数按现有Function约定惰性求值；异常不重放；准备表达式不执行回调 |
| 数学 | 三角函数使用度；min_angle为[-180,180)，sign对非正数返回-1；随机整数包含两端；dice求和 |

数值沿用Java double，不承诺与原版float逐位等价。easing采用标准曲线，官方条目未给全部精确常数，真实BE中间采样仍需独立验证。宿主提供query、资源名称和实体字段；缺失值数值转换为0不代表该查询已实现。Scope只读不自动递归保护嵌套对象。

单loop最多1024次；一次求值另限累计1048576次迭代，dice最多1024次。这些预算不限制任意Java回调成本。缓存属于消费者：BedrockMotion为256项／16MiB估计权重，BEParticle为512项／16MiB估计权重，均只保留AST，不保留实体和实时值。

## 验证入口

从NeoForgeWorkspace根目录运行：

```sh
./gradlew -PbedrockRuntimeOnly=true -PneteaseBridgeTarget=async-logging \
  :Mocha:test :BedrockMotion:test :BEParticleNeoForge:check \
  :ViaBedrockUtilityNeoForge:compileJava :GeyserUtilsBridgeNeoForge:compileJava
```

OfficialMathTest检查官方目录、固定数值、easing端点和随机边界。InterpreterSemanticsTest检查解释与预解析入口、控制流、变量作用域及准备期间不产生字节码。原有编译接口测试继续运行，不扩大其支持声明。BEParticle的正式JAR检查验证实际内嵌库能解释表达式且不含实验编译器。

构建及基础库测试不替代真实游戏。枪械运行、性能、已验产物和剩余范围统一维护在框架 `docs/testing-status.md`。
