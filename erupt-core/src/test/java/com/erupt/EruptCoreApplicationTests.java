package com.erupt;

import com.erupt.annotation.EruptField;
import com.erupt.dao.EruptJpaDao;
import com.erupt.model.EruptFieldModel;
import com.erupt.model.EruptModel;
import com.erupt.model.Page;
import com.erupt.service.CoreService;
import com.erupt.util.EruptUtil;
import com.google.gson.Gson;
import org.hibernate.SQLQuery;
import org.hibernate.jpa.HibernateQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EruptCoreApplicationTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EruptJpaDao eruptJpaDao;

    @Test
    public void contextLoads() {
//        System.err.println(new Gson().toJson(entityManager.createQuery("select new map(id as id,name as name,number as number,choice as choice,mmo.age as mmo_age,mmo.choice as mmo_choice)" +
//                " from SubMmo").getResultList()));
        EruptModel eruptModel = CoreService.ERUPTS.get("subMmo");
//        System.out.println(eruptJpaDao.queryEruptList(eruptModel, new Page(1, 3)));

    }


}