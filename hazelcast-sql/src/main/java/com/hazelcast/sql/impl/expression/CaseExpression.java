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

package com.hazelcast.sql.impl.expression;

import com.hazelcast.jet.sql.impl.JetSqlSerializerHook;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.sql.impl.expression.predicate.ComparisonMode;
import com.hazelcast.sql.impl.expression.predicate.ComparisonPredicate;
import com.hazelcast.sql.impl.expression.predicate.IsNotNullPredicate;
import com.hazelcast.sql.impl.expression.predicate.TernaryLogic;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.type.QueryDataType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class CaseExpression<T> implements Expression<T> {
    private Expression<Boolean>[] whenExpressions;
    private Expression<?>[] thenExpressions;
    private Expression<?> elseExpression;

    public CaseExpression() {
    }

    private CaseExpression(Expression<Boolean>[] whenExpressions,
                           Expression<?>[] thenExpressions,
                           Expression<?> elseExpression) {
        this.whenExpressions = whenExpressions;
        this.thenExpressions = thenExpressions;
        this.elseExpression = elseExpression;
    }

    @SuppressWarnings("unchecked")
    public static <T> CaseExpression<T> nullif(Expression<?> left, Expression<?> right) {
        return new CaseExpression<>(
                new Expression[]{ComparisonPredicate.create(left, right, ComparisonMode.EQUALS)},
                new Expression[]{ConstantExpression.create(null, left.getType())},
                left
        );
    }

    public static <T> CaseExpression<T> coalesce(Expression<?>... operands) {
        int branchesSize = operands.length - 1;
        @SuppressWarnings("unchecked")
        Expression<Boolean>[] whenExpressions = new Expression[branchesSize];
        Expression<?>[] thenExpressions = new Expression[branchesSize];
        for (int i = 0; i < branchesSize; i++) {
            whenExpressions[i] = IsNotNullPredicate.create(operands[i]);
            thenExpressions[i] = operands[i];
        }
        return new CaseExpression<>(whenExpressions, thenExpressions, operands[operands.length - 1]);
    }

    @SuppressWarnings("unchecked")
    public static <T> CaseExpression<T> create(Expression<?>[] operands) {
        assert operands.length % 2 == 1 : "CASE expression must have odd number of operands";

        int branchesSize = operands.length / 2;
        Expression<Boolean>[] whenExpressions = new Expression[branchesSize];
        Expression<?>[] thenExpressions = new Expression[branchesSize];
        for (int i = 0; i < branchesSize; i++) {
            whenExpressions[i] = (Expression<Boolean>) operands[2 * i];
            thenExpressions[i] = operands[2 * i + 1];
        }
        return new CaseExpression<>(whenExpressions, thenExpressions, operands[operands.length - 1]);
    }

    @Override
    public int getClassId() {
        return JetSqlSerializerHook.EXPRESSION_CASE;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(whenExpressions.length);

        for (int i = 0; i < whenExpressions.length; i++) {
            out.writeObject(whenExpressions[i]);
            out.writeObject(thenExpressions[i]);
        }

        out.writeObject(elseExpression);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readData(ObjectDataInput in) throws IOException {
        int branchesSize = in.readInt();

        whenExpressions = new Expression[branchesSize];
        thenExpressions = new Expression[branchesSize];

        for (int i = 0; i < branchesSize; i++) {
            whenExpressions[i] = in.readObject();
            thenExpressions[i] = in.readObject();
        }

        elseExpression = in.readObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T eval(Row row, ExpressionEvalContext context) {
        for (int i = 0; i < whenExpressions.length; i++) {
            Expression<Boolean> condition = whenExpressions[i];

            Boolean conditionHolds = condition.eval(row, context);
            if (TernaryLogic.isTrue(conditionHolds)) {
                return (T) thenExpressions[i].eval(row, context);
            }
        }

        return (T) elseExpression.eval(row, context);
    }

    @Override
    public QueryDataType getType() {
        return elseExpression.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseExpression<?> that = (CaseExpression<?>) o;
        return Arrays.equals(whenExpressions, that.whenExpressions)
                && Arrays.equals(thenExpressions, that.thenExpressions)
                && Objects.equals(elseExpression, that.elseExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(whenExpressions), Arrays.hashCode(thenExpressions), elseExpression);
    }

    @Override
    public String toString() {
        return "CaseExpression{"
                + "whenExpressions=" + Arrays.toString(whenExpressions)
                + ", thenExpressions=" + Arrays.toString(thenExpressions)
                + ", elseExpression=" + elseExpression
                + '}';
    }

    @Override
    public boolean isCooperative() {
        for (Expression<Boolean> e : whenExpressions) {
            if (!e.isCooperative()) {
                return false;
            }
        }
        for (Expression<?> e : thenExpressions) {
            if (!e.isCooperative()) {
                return false;
            }
        }

        return elseExpression.isCooperative();
    }
}
