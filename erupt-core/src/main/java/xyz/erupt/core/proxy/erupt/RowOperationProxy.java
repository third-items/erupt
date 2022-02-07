package xyz.erupt.core.proxy.erupt;

import org.aopalliance.intercept.MethodInvocation;
import xyz.erupt.annotation.constant.AnnotationConst;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.core.proxy.AnnotationProxy;

/**
 * @author YuePeng
 * date 2022/2/7 18:22
 */
public class RowOperationProxy extends AnnotationProxy<RowOperation> {

    @Override
    protected Object invocation(MethodInvocation invocation) {
        Object rtn = this.invoke(invocation);
        if ("code".equals(invocation.getMethod().getName()) && AnnotationConst.EMPTY_STR.equals(this.rawAnnotation.code())) {
            return Integer.toString(this.rawAnnotation.title().hashCode());
        }
        return rtn;
    }
}