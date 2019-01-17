package xyz.erupt.core.dao;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Filter;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceType;
import xyz.erupt.annotation.util.ConfigUtil;
import xyz.erupt.core.model.EruptFieldModel;
import xyz.erupt.core.model.EruptModel;
import xyz.erupt.core.model.HqlModel;
import xyz.erupt.core.util.TypeUtil;

import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by liyuepeng on 11/5/18.
 */
public class EruptJapUtils {

    public static final String AND = " and ";

    public static final String CONDITION_KEY = "condition";

    public static final String NULL = "$null$";

    public static final String NOT_NULL = "$notNull$";

    public static Set<String> getEruptColJapKeys(EruptModel eruptModel) {
        Set<String> fieldKeys = new HashSet<>();
        String eruptNameSymbol = eruptModel.getEruptName() + ".";
        eruptModel.getEruptFieldModels().forEach(field -> {
            if (field.getEruptField().edit().type() == EditType.TAB) {
                return;
            }
            if (null != field.getField().getAnnotation(Transient.class)) {
                return;
            }
            String fieldKey;
            for (View view : field.getEruptField().views()) {
                if ("".equals(view.column())) {
                    fieldKey = eruptNameSymbol + field.getFieldName() + " as " + field.getFieldName();
                } else {
                    fieldKey = eruptNameSymbol + field.getFieldName() + "." + view.column() + " as " + field.getFieldName() + "_"
                            + view.column().replace(".", "_");
                }
                fieldKeys.add(fieldKey);
            }
            if (field.getEruptField().edit().type() == EditType.REFERENCE) {
                ReferenceType referenceType = field.getEruptField().edit().referenceType()[0];
                fieldKey = eruptNameSymbol + field.getFieldName() + "." + referenceType.id() + " as " + field.getFieldName() + "_" + referenceType.id();
                fieldKeys.add(fieldKey);
                fieldKey = eruptNameSymbol + field.getFieldName() + "." + referenceType.label() + " as " + field.getFieldName() + "_" + referenceType.label();
                fieldKeys.add(fieldKey);
            }
            if (field.getEruptField().views().length == 0) {
                if (field.getEruptField().edit().type() != EditType.REFERENCE) {
                    if (TypeUtil.isFieldSimpleType(field.getField().getType().getSimpleName())) {
                        fieldKey = eruptNameSymbol + field.getFieldName() + " as " + field.getFieldName();
                        fieldKeys.add(fieldKey);
                    }
                }
            }
        });
        return fieldKeys;
    }


    public static String generateEruptJpaHql(EruptModel eruptModel, HqlModel hqlModel) {
        StringBuilder hql = new StringBuilder("select ");
        hql.append(hqlModel.getCols());
        hql.append(" from ").
                append(eruptModel.getEruptName()).
                append(" ").
                append(eruptModel.getEruptName());
        eruptModel.getEruptFieldModels().forEach(field -> {
            if (null != field.getField().getAnnotation(ManyToOne.class) || null != field.getField().getAnnotation(OneToOne.class)) {
                hql.append(" left join ").append(eruptModel.getEruptName()).append(".").append(field.getFieldName()).append(" ").append(field.getFieldName());
            }
        });
        hql.append(" where 1=1");
        Filter filter = eruptModel.getErupt().filter();
        if (!"".equals(filter.condition())) {
            hql.append(AND).append(ConfigUtil.switchFilterConditionToStr(filter));
        }
        //condition
        JsonObject condition = hqlModel.getCondition();
        if (null != condition && condition.size() > 0) {
            for (String key : hqlModel.getCondition().keySet()) {
                EruptFieldModel eruptFieldModel = eruptModel.getEruptFieldMap().get(key);
                if (null != eruptFieldModel && eruptFieldModel.getEruptField().edit().search().search()) {
                    String _key = EruptJapUtils.compleHqlPath(eruptModel.getEruptName(), key);
                    if (condition.get(key).toString().contains(EruptJapUtils.NULL)) {
                        hql.append(EruptJapUtils.AND).append(_key).append(" is null");
                    } else if (condition.get(key).toString().contains(EruptJapUtils.NOT_NULL)) {
                        hql.append(EruptJapUtils.AND).append(_key).append(" is not null");
                    } else {
                        hql.append(EruptJapUtils.AND).append(_key).append("=").append("'" + condition.get(key) + "'");
                    }
                }
            }
        }
        //sort
        if (StringUtils.isNotBlank(hqlModel.getOrderBy())) {
            hql.append(" order by " + compleHqlPath(eruptModel.getEruptName(), hqlModel.getOrderBy()));
        } else if (StringUtils.isNotBlank(eruptModel.getErupt().sort())) {
            hql.append(" order by " + compleHqlPath(eruptModel.getEruptName(), eruptModel.getErupt().sort()));
        }
        return hql.toString();
    }

    //在left join的情况下要求必须指定表信息，通过此方法生成；
    public static String compleHqlPath(String eruptName, String hqlPath) {
        if (hqlPath.contains(".")) {
            return hqlPath;
        } else {
            return eruptName + "." + hqlPath;
        }

    }

    public static String generateEruptJapCondition(EruptModel eruptModel) {

        return null;
    }


}
