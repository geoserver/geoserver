package org.geoserver.geofence.rest;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.geofence.GeofenceBaseTest;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.enums.AdminGrantType;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.rest.xml.Batch;
import org.geoserver.geofence.rest.xml.BatchOperation;
import org.geoserver.geofence.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.server.rest.BatchRestController;
import org.geoserver.geofence.services.AdminRuleAdminService;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.ShortAdminRule;
import org.geoserver.geofence.services.dto.ShortRule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class BatchRestControllerTest extends GeofenceBaseTest {

    private BatchRestController controller;

    private RuleAdminService ruleService;

    private AdminRuleAdminService ruleAdminService;

    @Before
    public void initGeoFenceControllers() {
        controller = (BatchRestController) applicationContext.getBean("batchRestController");
        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
        ruleAdminService =
                (AdminRuleAdminService) applicationContext.getBean("adminRuleAdminService");
    }

    @Test
    public void testRuleBatch() {
        List<Long> idsToDel = new ArrayList<>();
        try {
            JaxbRule rule = new JaxbRule();
            rule.setPriority(2l);
            rule.setRoleName("ROLE_USER");
            rule.setWorkspace("ws");
            rule.setLayer("layer");
            rule.setAccess("ALLOW");

            JaxbRule rule2 = new JaxbRule();
            rule2.setPriority(2l);
            rule2.setRoleName("ROLE_USER");
            rule2.setWorkspace("ws");
            rule2.setLayer("layer");
            rule2.setAccess("LIMIT");
            JaxbRule.Limits limits = new JaxbRule.Limits();
            limits.setCatalogMode("MIXED");
            rule2.setLimits(limits);

            long id = ruleService.insert(rule.toRule());
            long id2 = ruleService.insert(rule2.toRule());
            ruleService.setLimits(id2, rule2.getLimits().toRuleLimits(null));

            JaxbRule rule3 = new JaxbRule();
            rule3.setPriority(99l);
            rule3.setRoleName("ROLE_ANONYMOUS");
            rule3.setWorkspace("ws");
            rule3.setLayer("layer");
            rule3.setAccess("DENY");

            Batch batch = new Batch();
            BatchOperation op = new BatchOperation();
            op.setPayload(rule3);
            op.setService(BatchOperation.ServiceName.rules);
            op.setType(BatchOperation.TypeName.insert);
            batch.add(op);

            BatchOperation op2 = new BatchOperation();
            op2.setService(BatchOperation.ServiceName.rules);
            op2.setType(BatchOperation.TypeName.delete);
            op2.setId(id2);
            batch.add(op2);

            BatchOperation op3 = new BatchOperation();
            op3.setService(BatchOperation.ServiceName.rules);
            op3.setType(BatchOperation.TypeName.update);
            op3.setId(id);
            rule.setAccess("DENY");
            op3.setPayload(rule);
            batch.add(op3);

            HttpStatus status = controller.exec(batch);
            assertEquals(200, status.value());
            Rule deleted = null;
            try {
                deleted = ruleService.get(id2);
            } catch (Exception e) {
            }
            ;
            assertNull(deleted);
            Rule updated = ruleService.get(id);
            assertEquals(GrantType.DENY, updated.getAccess());
            assertEquals("ROLE_USER", updated.getRolename());
            ShortRule inserted = ruleService.getRuleByPriority(99L);
            assertEquals(GrantType.DENY, inserted.getAccess());
            assertEquals("ROLE_ANONYMOUS", inserted.getRoleName());
            idsToDel.add(id);
            idsToDel.add(inserted.getId());
        } finally {
            deleteRules(idsToDel);
        }
    }

    @Test
    public void testAdminRuleBatch() {
        List<Long> idsToDel = new ArrayList<>();
        try {
            JaxbAdminRule adminRule = new JaxbAdminRule();
            adminRule.setRoleName("ROLE_USER");
            adminRule.setWorkspace("ws00");
            adminRule.setAccess(AdminGrantType.USER.name());
            Long id = ruleAdminService.insert(adminRule.toRule());

            AdminRule adminRule2 = new AdminRule();
            adminRule2.setRolename("ROLE_USER2");
            adminRule2.setWorkspace("ws22");
            adminRule2.setAccess(AdminGrantType.ADMIN);
            Long id2 = ruleAdminService.insert(adminRule2);

            JaxbAdminRule adminRule3 = new JaxbAdminRule();
            adminRule3.setRoleName("ROLE_USER3");
            adminRule3.setWorkspace("ws33");
            adminRule3.setPriority(999L);
            adminRule3.setAccess(AdminGrantType.ADMIN.name());

            Batch batch = new Batch();
            BatchOperation operation = new BatchOperation();
            operation.setService(BatchOperation.ServiceName.adminrules);
            operation.setType(BatchOperation.TypeName.insert);
            operation.setPayload(adminRule3);
            batch.add(operation);

            BatchOperation operation2 = new BatchOperation();
            operation2.setService(BatchOperation.ServiceName.adminrules);
            operation2.setType(BatchOperation.TypeName.delete);
            operation2.setId(id2);
            batch.add(operation2);

            BatchOperation operation3 = new BatchOperation();
            operation3.setService(BatchOperation.ServiceName.adminrules);
            operation3.setType(BatchOperation.TypeName.update);
            operation3.setId(id);
            adminRule.setWorkspace("ws012");
            operation3.setPayload(adminRule);
            batch.add(operation3);

            controller.exec(batch);

            AdminRule rule = ruleAdminService.get(id);
            assertEquals("ws012", rule.getWorkspace());
            assertEquals(AdminGrantType.USER, rule.getAccess());
            assertEquals("ROLE_USER", rule.getRolename());
            AdminRule deleted = null;
            try {
                deleted = ruleAdminService.get(id2);
            } catch (Exception e) {
            }
            assertNull(deleted);

            ShortAdminRule inserted = ruleAdminService.getRuleByPriority(999L);
            assertEquals("ws33", inserted.getWorkspace());
            assertEquals("ROLE_USER3", inserted.getRoleName());
            assertEquals(AdminGrantType.ADMIN, inserted.getAccess());
            idsToDel.add(id);
            idsToDel.add(inserted.getId());
        } finally {
            deleteAdminRules(idsToDel);
        }
    }

    @Test
    public void testMixedBatch() {
        Long adminId = null;
        Long ruleId = null;
        try {
            JaxbRule rule = new JaxbRule();
            rule.setPriority(2l);
            rule.setRoleName("ROLE_USER4");
            rule.setWorkspace("ws");
            rule.setLayer("layer");
            rule.setAccess("ALLOW");
            long id = ruleService.insert(rule.toRule());
            Batch batch = new Batch();
            BatchOperation op = new BatchOperation();
            op.setService(BatchOperation.ServiceName.rules);
            op.setType(BatchOperation.TypeName.update);
            op.setId(id);
            rule.setAccess("DENY");
            rule.setRoleName("ROLE_USER5");
            op.setPayload(rule);
            batch.add(op);

            JaxbAdminRule adminRule = new JaxbAdminRule();
            adminRule.setPriority(9999L);
            adminRule.setRoleName("ROLE_USER6");
            adminRule.setAccess("ADMIN");
            adminRule.setWorkspace("ws99");

            BatchOperation op2 = new BatchOperation();
            op2.setService(BatchOperation.ServiceName.adminrules);
            op2.setType(BatchOperation.TypeName.insert);
            op2.setPayload(adminRule);
            batch.add(op2);

            HttpStatus status = controller.exec(batch);
            assertEquals(200, status.value());
            Rule updated = ruleService.get(id);
            assertEquals(GrantType.DENY, updated.getAccess());
            assertEquals("ROLE_USER5", updated.getRolename());
            ShortAdminRule inserted = ruleAdminService.getRuleByPriority(9999L);
            assertEquals(AdminGrantType.ADMIN, inserted.getAccess());
            assertEquals("ws99", inserted.getWorkspace());
            assertEquals("ROLE_USER6", inserted.getRoleName());
            ruleId = id;
            adminId = inserted.getId();
        } finally {
            if (adminId != null) ruleAdminService.delete(adminId);
            if (ruleId != null) ruleService.delete(ruleId);
        }
    }

    @Test
    public void testGlobalRollback() {
        Long ruleId = null;
        try {
            JaxbRule rule = new JaxbRule();
            rule.setPriority(2l);
            rule.setRoleName("ROLE_USER4");
            rule.setWorkspace("ws44");
            rule.setLayer("layer");
            rule.setAccess("ALLOW");
            long id = ruleService.insert(rule.toRule());
            ruleId = id;
            Batch batch = new Batch();
            BatchOperation op = new BatchOperation();
            op.setService(BatchOperation.ServiceName.rules);
            op.setType(BatchOperation.TypeName.update);
            op.setId(id);
            rule.setAccess("DENY");
            rule.setRoleName("ROLE_USER5");
            op.setPayload(rule);
            batch.add(op);

            JaxbRule dup = new JaxbRule();
            dup.setPriority(1l);
            dup.setRoleName("ROLE_USER9");
            dup.setWorkspace("ws99");
            dup.setLayer("layer99");
            dup.setAccess("ALLOW");

            ruleService.insert(dup.toRule());

            BatchOperation op2 = new BatchOperation();
            op2.setService(BatchOperation.ServiceName.rules);
            op2.setType(BatchOperation.TypeName.insert);
            dup.setPriority(99999L);
            op2.setPayload(dup);
            batch.add(op2);

            try {
                controller.exec(batch);
            } catch (Exception e) {
            }
            ;

            Rule updated = ruleService.get(id);
            assertEquals(GrantType.ALLOW, updated.getAccess());
            assertEquals("ROLE_USER4", updated.getRolename());
            ShortRule inserted = null;
            try {
                inserted = ruleService.getRuleByPriority(99999L);
            } catch (Exception e) {
            }
            ;
            assertNull(inserted);
        } finally {
            if (ruleId != null) ruleService.delete(ruleId);
        }
    }

    private void deleteRules(List<Long> ids) {
        for (Long id : ids) {
            ruleService.delete(id);
        }
    }

    private void deleteAdminRules(List<Long> ids) {
        for (Long id : ids) {
            ruleAdminService.delete(id);
        }
    }
}
