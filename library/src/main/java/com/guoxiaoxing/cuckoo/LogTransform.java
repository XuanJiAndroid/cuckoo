package com.guoxiaoxing.cuckoo;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;

import java.util.Set;

/**
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/12/30 下午3:54
 */
public class LogTransform extends Transform {

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return null;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return null;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }
}
