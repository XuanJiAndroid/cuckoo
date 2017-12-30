

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
- public abstract Set<? super Scope> getScopes(); Transform作用域  
                                                                           
作用域一共有6种：
                                                                       
1. PROJECT	只处理当前项目
2. SUB_PROJECTS	只处理子项目
3. PROJECT_LOCAL_DEPS	只处理当前项目的本地依赖,例如jar, aar
4. EXTERNAL_LIBRARIES	只处理外部的依赖库
5. PROVIDED_ONLY	只处理本地或远程以provided形式引入的依赖库
6. TESTED_CODE	测试代码                                                                    

- public abstract boolean isIncremental(); 是否支持增量编译。
- public boolean isCacheable(); 输出的文件是否缓存

当然主要的函数就是transform()
    
- public void transform(TransformInvocation transformInvocation); 执行Transform。

更多关于函数细节的介绍可以参考[Transform API 文档](http://google.github.io/android-gradle-dsl/javadoc/current/)。


```java
public abstract class Transform {
    public Transform() {
    }

 

    

    public  {
        return this.getInputTypes();
    }

  

    public Set<? super Scope> getReferencedScopes() {
        return ImmutableSet.of();
    }

    /** @deprecated */
    @Deprecated
    public Collection<File> getSecondaryFileInputs() {
        return ImmutableList.of();
    }

    public Collection<SecondaryFile> getSecondaryFiles() {
        return ImmutableList.of();
    }

    public Collection<File> getSecondaryFileOutputs() {
        return ImmutableList.of();
    }

    public Collection<File> getSecondaryDirectoryOutputs() {
        return ImmutableList.of();
    }

    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.of();
    }

    


}

```