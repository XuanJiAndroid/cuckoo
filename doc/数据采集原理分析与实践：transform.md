

>Transform API是Android Gradle Plugin 1.5.0以后推出的一套API框架，它可以让第三方插件在dex打包之前修改编译后的class文件，这是个
很有用的特性，jarMerge、proguard、multi-dex、Instant-Run都是基于这套API实现的。

- [Transform API Doc](http://google.github.io/android-gradle-dsl/javadoc/current/)

添加依赖

```java
provided 'com.android.tools.build:gradle:3.0.1'
```

我们先看看Transform API里有哪些类，如下所示：


可以看到最主要的类就是Transform，它是一个抽象类，用来完成class文件的修改。

- public abstract String getName(); Transform的名字
- public abstract Set<ContentType> getInputTypes(); Transform要处理的输入数据类型，一共有两种：CLASSES编译后的字节码（可能是JAR包也可能是目录），RESOURCES表示标准的Java资源。
- public Set<ContentType> getOutputTypes(); 输出类型，和输入类型一样。

这里可以发现输入输出的类型是Set<ContentType>，它一共有五种：

- public static final Set<ContentType> CONTENT_CLASS = ImmutableSet.<ContentType>of(CLASSES); 类文件
- public static final Set<ContentType> CONTENT_JARS = ImmutableSet.<ContentType>of(CLASSES, RESOURCES); JAR文件
- public static final Set<ContentType> CONTENT_RESOURCES = ImmutableSet.<ContentType>of(RESOURCES); 资源文件
- public static final Set<ContentType> CONTENT_NATIVE_LIBS = ImmutableSet.<ContentType>of(ExtendedContentType.NATIVE_LIBS); native库文件
- public static final Set<ContentType> CONTENT_DEX = ImmutableSet.<ContentType>of(ExtendedContentType.DEX); dex文件

- public abstract Set<? super Scope> getScopes(); Transform作用域  

作用域的返回类型是Set<? super Scope>，它一共有两种：

- public static final Set<Scope> SCOPE_FULL_PROJECT = Sets.immutableEnumSet( Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS, Scope.SUB_PROJECTS, Scope.SUB_PROJECTS_LOCAL_DEPS, Scope.EXTERNAL_LIBRARIES); 工程作用域
- public static final Set<Scope> SCOPE_FULL_LIBRARY = Sets.immutableEnumSet(Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS); 库作用域

                                                                           
作用域一共有6种：
                                                                       
1. PROJECT	只处理当前项目
2. SUB_PROJECTS	只处理子项目
3. PROJECT_LOCAL_DEPS	只处理当前项目的本地依赖,例如jar, aar
4. EXTERNAL_LIBRARIES	只处理外部的依赖库
5. PROVIDED_ONLY	只处理本地或远程以provided形式引入的依赖库
6. TESTED_CODE	测试代码     

ContentType和Scope组合在一起就组成了我们常见的各种gradle task的名字。/proguard/qihoo/debug/jars/3/1f/main.jar

例如：

```
transformClassesAndResourcesWithProguardForDebug
```

transform + 输入输出类型：ClassesAndResources + 功能：Proguard + 编译类型：Debug

Transform生成的文件存放在build/intermediates/transforms文件夹下，子文件夹也根据ContentType和Scope生成。

例如：

```
/proguard/debug/jars/3/1f/main.jar
```

功能:proguard + 编译类型：debug + 文件目录：jars + 输出类型：3 + 作用域：1f + 文件名：main.jar
                                                                                                                            
- public abstract boolean isIncremental(); 是否支持增量编译。
- public boolean isCacheable(); 输出的文件是否缓存

当然主要的函数就是transform()
    
- public void transform(TransformInvocation transformInvocation); 执行Transform。

更多关于函数细节的介绍可以参考[Transform API 文档](http://google.github.io/android-gradle-dsl/javadoc/current/)。

前面我们也提到jarMerge、proguard、multi-dex、Instant-Run都是基于Transform API来实现的，在编译项目的时候我们就可以看到这些task，它们都以transform打头，如下所示：

Transform生成的文件存放在build/intermediates/transforms文件夹下，如下所示：

而管理这些Transform就是TransformManager。

[TransformManager](https://android.googlesource.com/platform/tools/base/+/gradle_2.0.0/build-system/gradle-core/src/main/groovy/com/android/build/gradle/internal/pipeline/TransformManager.java)用来管理
transform task。

Transform在TransformManager的管理下，以TransformStream流的形式通过Transform进行串行的处理，上一个Transform的输出是下一个Transform的输入，如下所示：

