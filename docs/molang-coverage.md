# Molang 执行合同与官方覆盖表

**实机状态：2026-11-04 E1 性能验收失败，默认消费者切换未获接受，E1 已恢复原已验 JAR。下文单测与小基准只证明各自范围，不能作为发布批准。**

本页维护 `3.0.3-ec.3` 的语言执行边界。核对日期：2026-11-04。目标是当前文档中的 Molang 语法；不模拟不同旧 `min_engine_version` 的历史错误或旧优先级。

## 官方依据

- [Molang Syntax Guide](https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/syntax-guide?view=minecraft-bedrock-stable)：运算符、类型、变量生命周期、结构体、实体引用、数组、控制流。
- [MicrosoftDocs 语法源文件](https://github.com/MicrosoftDocs/minecraft-creator/blob/main/creator/Documents/molang/syntax-guide.md)。
- [Math Functions 官方目录](https://github.com/MicrosoftDocs/minecraft-creator/blob/main/creator/Reference/Content/MolangReference/Examples/MolangConcepts/MathFunctions.md)及其 61 个条目页面：60 个函数和 `math.pi`。核对时全部成功取得；测试中的 `official-math-functions.txt` 固定本次目录，新增官方函数时须同时扩充覆盖表和断言。
- 30 个 easing 函数以及 `inverse_lerp` 的条目声明最低资源格式 1.21.120。Mocha 提供这些函数；是否允许某资源使用它们由宿主版本策略决定，不意味着 BE 旧引擎已支持。

官方文档核对、语义单测、真实 Bedrock 对照是三种证据。本轮具备前两种；不能写成“与原版所有表达式逐位等价”。

## 语言覆盖

| 能力 | 编译执行与解释执行 | 验证 |
| --- | --- | --- |
| 数字、字符串、标识符、分组 | 支持；大小写无关标识符、大小写敏感字符串；词法额外支持 `f` 后缀、前导小数点、科学计数法 | 词法／解析器测试、ProgramSafetyTest |
| `+ - * /`、一元负号、`!` | 支持；除零返回零，数值非有限结果归零 | 原有算术测试、ProgramCompilerTest |
| 比较、`== !=`、`&& ||` | 支持，保留短路顺序；现代运算优先级 | 官方示例、独立预期值、惰性副作用测试 |
| `? :`、条件执行 `?`、`??` | 支持；嵌套三元与合并右结合；缺失不等于零 | ProgramCompilerTest、ProgramSafetyTest |
| `variable/v`、`temp/t`、`query/q`、`context/c`、`this` | 支持别名；context 禁止通过赋值／for_each 写入；this 与查询由宿主提供 | 作用域、上下文、并发、嵌套求值测试 |
| 对象、隐式结构体、引用共享 | 赋值按需建立缺失中间结构，不覆盖已有非结构值；尊重宿主只读绑定 | 官方结构示例、共享引用测试 |
| 数组索引 | 整数截断、负值夹到 0、超界取模；空数组返回缺失 | 空数组、负数、小数索引及 for_each |
| `->` 实体上下文 | `ActorReference` 提供实体、许可作用域及有效性；失效引用返回缺失；现有 JavaValue 实体查询仍可用 | 跨实体作用域／公开只读变量／失效引用 |
| `{}`、`return` | 支持嵌套；裸块执行，未显式返回的块值为 0；多语句表达式无 return 时高层结果为 0 | 块、嵌套 return、高层与低层接口测试 |
| `loop`、`for_each`、`break`、`continue` | 支持；循环体可以是单表达式或块；break/continue 归最近循环，return 逃逸 | 官方 Fibonacci、嵌套循环、提前返回、预算测试 |
| 动态函数／惰性参数／表达式回调 | 支持，不在编译时调用；参数 AST 只给副本；异常不重试／不回放 | 自定义回调、修改绑定、异常与调用次序测试 |
| 数学标准库 | 61 条目全部登记且有覆盖；三角函数单位为度；随机值保持动态 | OfficialMathTest 名称全集、40 个固定案例及 30 个 easing 案例 |

## 明确的精度与宿主边界

- 原版文档称数值为 float；本库沿用 Java double。数学函数和算术有数值误差／精度差异，不承诺位级等价。
- 官方 easing 页面只描述曲线行为，未给全部精确常数。本库采用标准 Penner 风格曲线，测试端点、对称中点、有限性及两条执行路径一致；原版中间采样点的精确一致性仍待真实引擎验证。不是从另一解释器生成期望值来冒充官方证明。
- 官方 `math.sign` 是正数 1，否则 -1；`math.min_angle` 区间为 `[-180,180)`；`random_integer` 包含两端；dice 求和。旧 MolangJS 的逆向 lerprotate 预期曾错误，不再用那条作为规范。
- 库不内建 Minecraft 世界。`query.*`、资源名称／资源数组、实体公开变量、私有变量许可和实体失效由宿主绑定。缺失值的数值结果为 0 只是语言转换，不能把未实现的查询称为已支持。
- `ActorReference.bindings()` 必须只暴露允许访问的只读／可写字段。`Scope.readOnly` 只限制顶层替换，不自动递归保护宿主对象。
- Java 自定义 Function 保留惰性语义。显式 `NumericFunction` 和无上下文、0～3 个 double 参数的 Java 数值方法才走快速入口；不通过名字猜函数是否可提前求值，也不把绑定对象放进程序缓存。

## 执行与缓存边界

```
资源加载／prepare
  → 解析 AST → 不可变快照与结构键 → 有界 JVM 字节码程序
当前实体／作用域 → Program.evaluate[Fresh] → JVM 执行／JIT
```

这是**进程内 JVM 字节码缓存**。HotSpot 自行把热点方法 JIT 为本机机器码；本库不保存跨 JVM、跨系统的原生机器码，不从资源包加载 Java class。缓存按 Mocha 的类加载器隔离；1.21.8 唯一运行时提供者使正常消费者共享同一份缓存。

- 同一结构的空白变体可复用程序。程序不捕获实体、世界、scope 或可变 query 值；每次执行有独立控制流与预算。调用方负责自己的可变变量绑定的线程安全。
- 默认 LRU 最多 256 项、16 MiB 估计权重（字节码＋键字符＋每节点估算），不是 JVM 堆／JIT 内存的精确上限。资源对象可以保留已淘汰程序；只有其也释放后，专用子类加载器才可被 GC 卸载。`clear()` 不让活跃程序失效，不强制 GC。
- 单程序源码 256K 字符、12000 节点、深度 256、2 MiB 字节码；字符串累计键预算 1 MiB。超过 JVM 方法／常量池限制会在准备时明确报编译错误，不在执行半途回退解释器。
- 官方单 loop 上限 1024；本库另有一次求值累计 1048576 次循环预算、dice 1024 次保护。它们是库的额外限制，非官方原生无限能力。宿主仍须约束回调本身的开销；该库不是任意 Java 回调的安全沙箱。
- `evaluateFresh` 为每次表达式建立 temp/t，遵循多语句无 return 为 0。低层 `evaluate` 使用调用方 temp 和最后值，供现有动画采样接口使用；不是不同的运算／控制流算法。
- BedrockMotion 在资源编译时保留普通／actor-aware 程序，字面量直接返回；重入使用独立 scope/temp 槽，结束时释放 parent 引用。其源码查找缓存也有 256 项／16 MiB 估计预算。
- BEParticle 的 JSON 表达式入口在加载时 prepare，执行走同一 Program；动态／程序化传入的表达式首次使用时编译。其查找缓存限制为 512 项／16 MiB 估计预算，资源清理释放本地引用。缓存淘汰后的动态字符串可能再次编译，不能称作永久驻留。

## 验证与复现

在 NeoForgeWorkspace：

```sh
./gradlew -PbedrockRuntimeOnly=true -PneteaseBridgeTarget=async-logging \
  :Mocha:test :BedrockMotion:test :BEParticleNeoForge:check \
  :ViaBedrockUtilityNeoForge:compileJava :GeyserUtilsBridgeNeoForge:compileJava
./gradlew -PbedrockRuntimeOnly=true -PneteaseBridgeTarget=async-logging :Mocha:programBenchmark
```

`verifyBundledMocha` 从正式 BEParticle JAR 提取唯一 Mocha 和 Javassist，以隔离类加载器执行含循环／math 的编译程序，不能只通过坐标文本判断成功。生成程序的 class 版本为 Java 8；本轮 JVM 测试运行于工具链 Java 17，消费者为 Java 21，未声称在 Java 8 虚拟机实测。

2026-11-04：Mocha 122、BedrockMotion 66、BEParticle 22 个测试全部通过。VBU／GeyserUtilsBridge 编译通过。BedrockMotion 三处原有测试预期按现行材质解析及 prepareFrame 生命周期校正，没有为编译器修改这两项产品逻辑。

内部 CPU 对照为 aarch64、JVM 17.0.14，4 轮预热、7 轮交替顺序、每轮 100000 次；双方同样更新 this 并隔离 temp。中位结果：简单算术 190.7→145.8 ns（1.31x），三角与 clamp 483.3→252.9 ns（1.91x），8 次循环 1509.3→788.2 ns（1.91x）。首次编译含 JVM 冷启动，三个程序为 61.0／10.6／9.4 ms；不放进帧执行。该小型对照不是 JMH 置信区间，也不是游戏 FPS、整包冷启动或 GPU 测量。

E1 后续实测：32人40枪换弹，原版本渲染CPU中位18.659ms、帧间隔23.286ms；固定原VBU外壳仅替换共享动画库后，编译路径仍为39.605ms／47.004ms，不能启用。原版本同一JVM内32人空手为1.698ms／5.792ms，静止持枪为14.835ms／18.616ms。各有效对照为单个30秒窗口，非重复统计实验。源码实验保留、未发布；E1恢复ec.2运行库及原VBU，测试进程清理。正式功能／BE／Blockbench和26.1不据此标通过。详细证据集中在枪械框架现行attack-implementation-plan的本轮记录。

实际资源编译核查：v120候选BP/RP中的动画、控制器、挂件、渲染控制器与粒子，共108份JSON中提取7087条去重动态表达式，最长48773字符。最终代码全部编译成功；独立JVM21进程约51.01秒，累计生成91650753字节class。累计值不是同时驻留内存（核查使用64项／8MiB估计LRU），也不含实际宿主额外actor转换、纹理／模型上传和JIT成本；不能据此宣称游戏冷加载已达标。该核查只编译，没有假定query值执行武器动画，不能代替渲染一致性验收。过程中捕获的负常数减法生成问题已以通用回归用例固化。最新本机回执位于build/reports/molang-acceptance.json。

实机加载暴露的通用修正：ProgramCompiler在串行编译锁下复用只包含固定运行时类型的ClassPool，生成代码使用已导入的短类型名，生成CtClass仍在finally中detach。避免每个程序重建类型解析与反复解析完整包名前缀；不缓存实体或开放资源输入作为Java类型。210项库／消费者测试与嵌套JAR执行检查重新通过。E1资源准备观察约174秒降至85～86秒（单次启动观察，非严格冷启动基准），仍不合格；此修正没有消除稳态性能回归。
