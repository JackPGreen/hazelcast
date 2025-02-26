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

package com.hazelcast.jet.sql.impl.aggregate.function;

import com.hazelcast.jet.sql.impl.validate.HazelcastCallBinding;
import com.hazelcast.jet.sql.impl.validate.operators.common.HazelcastAggFunction;
import com.hazelcast.jet.sql.impl.validate.operators.typeinference.ReplaceUnknownOperandTypeInference;
import com.hazelcast.jet.sql.impl.validate.types.HazelcastJsonType;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlJsonConstructorNullClause;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperandCountRange;
import org.apache.calcite.sql.type.SqlOperandCountRanges;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Optionality;

public class HazelcastJsonObjectAggFunction extends HazelcastAggFunction {
    public static final HazelcastJsonObjectAggFunction ABSENT_ON_NULL_INSTANCE
            = new HazelcastJsonObjectAggFunction(SqlJsonConstructorNullClause.ABSENT_ON_NULL);
    public static final HazelcastJsonObjectAggFunction NULL_ON_NULL_INSTANCE
            = new HazelcastJsonObjectAggFunction(SqlJsonConstructorNullClause.NULL_ON_NULL);

    private final SqlJsonConstructorNullClause nullClause;

    protected HazelcastJsonObjectAggFunction(SqlJsonConstructorNullClause nullClause) {
        super(
                "JSON_OBJECTAGG" + "_" + nullClause.name(),
                SqlKind.JSON_OBJECTAGG,
                opBinding -> HazelcastJsonType.create(true),
                new ReplaceUnknownOperandTypeInference(SqlTypeName.ANY),
                null,
                SqlFunctionCategory.SYSTEM,
                false,
                false,
                Optionality.OPTIONAL
        );
        this.nullClause = nullClause;
    }

    @Override
    protected boolean checkOperandTypes(HazelcastCallBinding callBinding, boolean throwOnFailure) {
        return true;
    }

    @Override
    public SqlOperandCountRange getOperandCountRange() {
        return SqlOperandCountRanges.from(1);
    }

    public boolean isAbsentOnNull() {
        return nullClause == SqlJsonConstructorNullClause.ABSENT_ON_NULL;
    }
}
