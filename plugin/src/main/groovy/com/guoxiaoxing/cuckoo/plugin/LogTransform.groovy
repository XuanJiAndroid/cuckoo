package com.guoxiaoxing.cuckoo.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.gradle.api.Project

class LogTransform extends Transform {

    private static final String TRANSFORM_NAME = "LogTransform";

    Project mProject;

    LogTransform(Project project) {
        mProject = project
    }

    @Override
    String getName() {
        return TRANSFORM_NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        //遍历输入文件
        transformInvocation.inputs.each { TransformInput input ->

            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->

                //获得产物目录
                File dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes)
                String buildType = directoryInput.file.name
                String productFlavors = directoryInput.file.parentFile.name

                //TODO 执行我们的处理

                //处理完拷贝到目标文件
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }
}