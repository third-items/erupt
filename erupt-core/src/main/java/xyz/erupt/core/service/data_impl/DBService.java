package xyz.erupt.core.service.data_impl;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.constant.AnnotationConst;
import xyz.erupt.annotation.sub_erupt.Tree;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTreeType;
import xyz.erupt.core.bean.EruptFieldModel;
import xyz.erupt.core.bean.EruptModel;
import xyz.erupt.core.bean.Page;
import xyz.erupt.core.bean.TreeModel;
import xyz.erupt.core.dao.EruptJpaDao;
import xyz.erupt.core.dao.EruptJpaUtils;
import xyz.erupt.core.service.CoreService;
import xyz.erupt.core.service.DataService;
import xyz.erupt.core.util.AnnotationUtil;
import xyz.erupt.core.util.DataHandlerUtil;
import xyz.erupt.core.util.ReflectUtil;
import xyz.erupt.tool.EruptDao;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author liyuepeng
 * @date 2019-03-06.
 */
@Service
public class DBService implements DataService {

    @Autowired
    private EruptJpaDao eruptJpaDao;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EruptDao eruptDao;

    @Override
    public Object findDataById(EruptModel eruptModel, Object id) {
        return entityManager.find(eruptModel.getClazz(), id);
    }

    @Override
    public Page queryList(EruptModel eruptModel, Page page, JsonObject searchCondition, String customCondition) {
        return eruptJpaDao.queryEruptList(eruptModel, page, searchCondition, customCondition);
    }

    @Override
    public List<TreeModel> queryTree(EruptModel eruptModel) {
        return treeDataUtil(eruptModel, null, null);
    }

    private List<TreeModel> treeDataUtil(EruptModel eruptModel, String condition, String sort) {
        Tree tree = eruptModel.getErupt().tree();
        List<String> cols = new ArrayList<>();
        cols.add(EruptJpaUtils.completeHqlPath(eruptModel.getEruptName(), tree.id()) + " as " + AnnotationConst.ID);
        cols.add(EruptJpaUtils.completeHqlPath(eruptModel.getEruptName(), tree.label()) + " as " + AnnotationConst.LABEL);
        if (StringUtils.isNotBlank(tree.pid())) {
            cols.add(EruptJpaUtils.completeHqlPath(eruptModel.getEruptName(), tree.pid()) + " as " + AnnotationConst.PID);
        }
        List<Map<String, Object>> list = eruptJpaDao.getDataMap(eruptModel, condition, sort, cols, null);
        List<TreeModel> treeModels = new ArrayList<>();
        for (Map map : list) {
            TreeModel treeModel = new TreeModel(map.get(AnnotationConst.ID), map.get(AnnotationConst.LABEL), map.get(AnnotationConst.PID), null);
            treeModels.add(treeModel);
        }
        if (StringUtils.isBlank(tree.pid()) && StringUtils.isBlank(tree.pid())) {
            return treeModels;
        } else {
            return DataHandlerUtil.treeModelToTree(treeModels, null);
        }
    }

    @Transactional
    @Override
    public void addData(EruptModel eruptModel, Object object) throws Exception {
        try {
            jpaManyToOneConvert(eruptModel, object);
            eruptJpaDao.addEntity(object);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException(gcRepeatHint(eruptModel));
        }
    }

    @Transactional
    @Override
    public void editData(EruptModel eruptModel, Object data) throws Exception {
        try {
            //没有用eruptField修饰但是使用了hibernate管理的字段 设置为数据库原有存储值
            Object entity = entityManager.find(eruptModel.getClazz(),
                    ReflectUtil.findClassField(data.getClass(), eruptModel.getErupt().primaryKeyCol()).get(data));
            ReflectUtil.findClassAllFields(entity.getClass(), field -> {
                try {
                    EruptField eruptField = field.getAnnotation(EruptField.class);
                    if ((null != eruptField && AnnotationConst.EMPTY_STR.equals(eruptField.edit().title()))
                            || (null == eruptField && null == field.getAnnotation(Transient.class))) {
                        field.setAccessible(true);
                        Field dataField = ReflectUtil.findClassField(eruptModel.getClazz(), field.getName());
                        dataField.setAccessible(true);
                        dataField.set(data, field.get(entity));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            eruptJpaDao.editEntity(data);
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            throw new RuntimeException(gcRepeatHint(eruptModel));
        }
    }

    //@ManyToOne数据处理
    private void jpaManyToOneConvert(EruptModel eruptModel, Object object) throws NoSuchFieldException, IllegalAccessException {
        for (EruptFieldModel fieldModel : eruptModel.getEruptFieldModels()) {
            if (fieldModel.getEruptField().edit().type() == EditType.TAB_TABLE_ADD) {
                Field field = object.getClass().getDeclaredField(fieldModel.getFieldName());
                field.setAccessible(true);
                Collection collection = (Collection) field.get(object);
                if (null != collection) {
                    for (Object o : collection) {
                        //删除主键ID
                        //TODO 强制删除id的处理方式并不好
                        Field pk = ReflectUtil.findClassField(o.getClass(), CoreService
                                .getErupt(fieldModel.getFieldReturnName()).getErupt().primaryKeyCol());
                        pk.set(o, null);
                    }
                }
            }
        }
    }

    //生成数据重复的提示字符串
    private String gcRepeatHint(EruptModel eruptModel) {
        StringBuilder str = new StringBuilder();
        for (UniqueConstraint uniqueConstraint : eruptModel.getClazz().getAnnotation(Table.class).uniqueConstraints()) {
            for (String columnName : uniqueConstraint.columnNames()) {
                EruptField eruptField = eruptModel.getEruptFieldMap().get(columnName).getEruptField();
                str.append(eruptField.views()[0].title()).append("|");
            }
        }
        return str.substring(0, str.length() - 1) + "数据重复";
    }

    @Transactional
    @Override
    public void deleteData(EruptModel eruptModel, Object id) throws Exception {
        entityManager.remove(this.findDataById(eruptModel, id));
    }

    @Override
    public List<TreeModel> findTabTree(EruptModel eruptModel, String fieldName) {
        EruptFieldModel eruptTabFieldModel = eruptModel.getEruptFieldMap().get(fieldName);
        EruptModel subEruptModel = CoreService.getErupt(eruptModel.getEruptFieldMap().get(fieldName).getFieldReturnName());
        String condition = AnnotationUtil.switchFilterConditionToStr(eruptTabFieldModel.getEruptField().edit().filter());
        return treeDataUtil(subEruptModel, condition, eruptTabFieldModel.getEruptField().edit().orderBy());
    }


    @Override
    public Collection<TreeModel> getReferenceTree(EruptModel eruptModel, String fieldName, Serializable dependValue) {
        EruptFieldModel eruptFieldModel = eruptModel.getEruptFieldMap().get(fieldName);
        Edit edit = eruptFieldModel.getEruptField().edit();
        ReferenceTreeType refTree = edit.referenceTreeType();
        List<String> cols = new ArrayList<>();
        cols.add(EruptJpaUtils.completeHqlPath(eruptFieldModel.getFieldReturnName(), refTree.id()) + " as " + AnnotationConst.ID);
        cols.add(EruptJpaUtils.completeHqlPath(eruptFieldModel.getFieldReturnName(), refTree.label()) + " as " + AnnotationConst.LABEL);
        if (StringUtils.isNotBlank(refTree.pid())) {
            cols.add(EruptJpaUtils.completeHqlPath(eruptFieldModel.getFieldName(), refTree.pid()) + " as " + AnnotationConst.PID);
        }
        StringBuilder condition = new StringBuilder(AnnotationUtil.switchFilterConditionToStr(edit.filter()));
        //处理depend参数代码
        Map<String, Object> conditionParameter = null;
        if (StringUtils.isNotBlank(refTree.dependField()) && null != dependValue) {
            String dependVal = "dependVal";
            conditionParameter = new HashMap<>(1);
            if (StringUtils.isNotBlank(edit.filter().condition())) {
                condition.append(EruptJpaUtils.AND);
            }
            condition.append(eruptFieldModel.getEruptField().edit().referenceTreeType().dependColumn()).append("=:").append(dependVal);
            conditionParameter.put(dependVal, dependValue);
        }
        List<Map<String, Object>> list = eruptJpaDao.getDataMap(CoreService.getErupt(eruptFieldModel.getFieldReturnName()), condition.toString(), null, cols, conditionParameter);
        List<TreeModel> treeModels = new ArrayList<>();
        for (Map<String, Object> map : list) {
            treeModels.add(new TreeModel(map.get(AnnotationConst.ID), map.get(AnnotationConst.LABEL), map.get(AnnotationConst.PID), null));
        }
        if (StringUtils.isBlank(refTree.pid())) {
            return treeModels;
        } else {
            return DataHandlerUtil.treeModelToTree(treeModels, refTree.rootLabel());
        }
    }
}
