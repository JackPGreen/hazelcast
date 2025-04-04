/*
 * Copyright 2025 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.sql.impl.opt.logical;

import com.hazelcast.jet.sql.impl.opt.OptimizerTestSupport;
import com.hazelcast.jet.sql.impl.schema.HazelcastTable;
import com.hazelcast.sql.impl.type.QueryDataType;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.hazelcast.sql.impl.extract.QueryPath.KEY;
import static com.hazelcast.sql.impl.extract.QueryPath.VALUE;
import static com.hazelcast.sql.impl.type.QueryDataType.INT;
import static com.hazelcast.sql.impl.type.QueryDataType.VARCHAR;
import static java.util.Arrays.asList;

@RunWith(JUnitParamsRunner.class)
public class LogicalSelectTest extends OptimizerTestSupport {

    @BeforeClass
    public static void setUpClass() {
        initialize(1, null);
    }

    @Test
    public void test_requiresJob() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 0);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = 1", true, table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
        assertPlan(
                optimizeLogical("SELECT this || '-s' FROM m WHERE __key = 1", true, table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectWithoutWhere() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByValue() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE this = '1'", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyAndValue() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = 1 AND this = '1'", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyAndKey() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = 1 AND __key = 2", table),
                plan(
                        planRow(0, ValuesLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyOrKey() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = 1 OR __key = 2", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectWithConstantCondition() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 10);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE 1 = 1", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @SuppressWarnings("unused")
    private Object[] literals() {
        return new Object[]{
                new Object[]{QueryDataType.BOOLEAN, "true"},
                new Object[]{QueryDataType.BOOLEAN, "false"},
                new Object[]{QueryDataType.TINYINT, '1'},
                new Object[]{QueryDataType.SMALLINT, '1'},
                new Object[]{QueryDataType.INT, '1'},
                new Object[]{QueryDataType.BIGINT, '1'},
                new Object[]{QueryDataType.DECIMAL, '1'},
                new Object[]{QueryDataType.REAL, '1'},
                new Object[]{QueryDataType.DOUBLE, '1'},
                new Object[]{QueryDataType.VARCHAR, "'string'"},
                new Object[]{QueryDataType.TIME, "'12:23:34'"},
                new Object[]{QueryDataType.DATE, "'2021-07-01'"},
                new Object[]{QueryDataType.TIMESTAMP, "'2021-07-01T12:23:34'"},
                new Object[]{QueryDataType.TIMESTAMP_WITH_TZ_OFFSET_DATE_TIME, "'2021-07-01T12:23:34Z'"},
                new Object[]{QueryDataType.OBJECT, "CAST(1 AS OBJECT)"},
        };
    }

    @Test
    @Parameters(method = "literals")
    public void test_selectByKeyWithLiteral(QueryDataType type, String literalValue) {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, type), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = " + literalValue, table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE " + literalValue + " = __key", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyWithLiteralExpression() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = 1 + 1", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }

    @SuppressWarnings("unused")
    private Object[] types() {
        return new Object[]{
                new Object[]{QueryDataType.BOOLEAN},
                new Object[]{QueryDataType.TINYINT},
                new Object[]{QueryDataType.SMALLINT},
                new Object[]{QueryDataType.INT},
                new Object[]{QueryDataType.BIGINT},
                new Object[]{QueryDataType.DECIMAL},
                new Object[]{QueryDataType.REAL},
                new Object[]{QueryDataType.DOUBLE},
                new Object[]{QueryDataType.VARCHAR},
                new Object[]{QueryDataType.TIME},
                new Object[]{QueryDataType.DATE},
                new Object[]{QueryDataType.TIMESTAMP},
                new Object[]{QueryDataType.TIMESTAMP_WITH_TZ_OFFSET_DATE_TIME},
                new Object[]{QueryDataType.OBJECT},
        };
    }

    @Test
    @Parameters(method = "types")
    public void test_selectByKeyWithDynamicParam(QueryDataType type) {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, type), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = ?", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyWithDynamicParamAndImplicitCastOnKey() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = ? + 1", table),
                plan(
                        planRow(0, FullScanLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyWithDynamicParamExpression() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT * FROM m WHERE __key = CAST(? + 1 AS INT)", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyWithProject() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT this FROM m WHERE __key = 1", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }

    @Test
    public void test_selectByKeyWithProjectExpression() {
        HazelcastTable table = partitionedTable("m", asList(field(KEY, INT), field(VALUE, VARCHAR)), 1);
        assertPlan(
                optimizeLogical("SELECT this || '-s' FROM m WHERE __key = 1", table),
                plan(
                        planRow(0, SelectByKeyMapLogicalRel.class)
                )
        );
    }
}
