
>ASM是一个Java字节码操控框架，被用来动态生成类或者增强既有类的功能，它将Java Class文件描述成一棵树，使用Visitor模式遍历整个二进制结构，利用Java Class文件里的元数据来
解析类里的元素（例如：类名称，方法，属性以及Java字节码）然后根据需求生成新类。

- [ASM官方网站](http://forge.ow2.org/projects/asm/)

- [ASM Bytecode Outline](https://plugins.jetbrains.com/plugin/5918-asm-bytecode-outline)

添加依赖

```
implementation 'org.ow2.asm:asm:6.0'
```