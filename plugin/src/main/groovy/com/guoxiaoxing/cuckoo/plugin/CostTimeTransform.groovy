package com.guoxiaoxing.cuckoo.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2018/1/4 下午5:19
 */
class CostTimeTransform extends Transform {

    private static final String TRANSFORM_NAME = "CostTimeTransform";

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
        super.transform(transformInvocation);

        //ASM start
        def startTime = System.currentTimeMillis()
        transformInvocation.inputs.each { TransformInput input ->

            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->

                //TODO 对Class执行ASM操作
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->

                        def name = file.name
                        if (name.endsWith(".class") && !name.startsWith("R\$") && !"R.class".equals(name)
                                && !"BuildConfig.class".equals("name")) {
                            ClassReader cr = new ClassReader(file.bytes)
                            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                            ClassVisitor cv = new CostClassVisitor(cw)
                            cr.accept(cv, ClassReader.EXPAND_FRAMES)
                            byte[] code = cw.toByteArray()
                            FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + name)
                            fos.write(code)
                            fos.close()
                        }
                    }

                    def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name,
                            directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                    FileUtils.copyDirectory(directoryInput.file, dest)
                }

                //遍历JAR
                input.jarInputs.each { JarInput jarInput ->
                    def jarName = jarInput.name
                    def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4)
                    }

                    def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes,
                            jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                }

                def cost = (System.currentTimeMillis() - startTime) / 1000

            }

            //ASM end
        }
    }
}