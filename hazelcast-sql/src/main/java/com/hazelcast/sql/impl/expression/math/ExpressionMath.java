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

package com.hazelcast.sql.impl.expression.math;

import com.hazelcast.sql.impl.QueryException;
import com.hazelcast.sql.impl.SqlErrorCode;
import com.hazelcast.sql.impl.expression.Expression;

import static com.hazelcast.sql.impl.type.QueryDataType.INTERVAL_YEAR_MONTH;
import static com.hazelcast.sql.impl.type.QueryDataType.TIME;

/**
 * Utility methods for math functions.
 */
public final class ExpressionMath {

    private ExpressionMath() {
        // No-op.
    }

    /**
     * Divides the left-hand side operand by the right-hand side operand.
     * <p>
     * Unlike the regular Java division operator, throws an exception if division
     * resulted in overflow.
     *
     * @param left  the left-hand side operand.
     * @param right the right-hand side operand.
     * @return a division result.
     * @throws QueryException if overflow or division by zero is detected.
     */
    public static long divideExact(long left, long right) {
        if (left == Long.MIN_VALUE && right == -1) {
            throw QueryException.error(SqlErrorCode.DATA_EXCEPTION,
                    "BIGINT overflow in '/' operator (consider adding explicit CAST to DECIMAL)");
        }

        return left / right;
    }

    /**
     * Divides the left-hand side operand by the right-hand side operand.
     * <p>
     * Unlike the regular Java division operator, throws an exception if division
     * by zero is detected.
     *
     * @param left  the left-hand side operand.
     * @param right the right-hand side operand.
     * @return a division result.
     * @throws QueryException if division by zero is detected.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static double divideExact(double left, double right) {
        if (right == +0.0 || right == -0.0) {
            throw new ArithmeticException("Division by zero");
        }

        return left / right;
    }

    /**
     * Divides the left-hand side operand by the right-hand side operand.
     * <p>
     * Unlike the regular Java division operator, throws an exception if division
     * by zero is detected.
     *
     * @param left  the left-hand side operand.
     * @param right the right-hand side operand.
     * @return a division result.
     * @throws QueryException if division by zero is detected.
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static float divideExact(float left, float right) {
        if (right == +0.0f || right == -0.0f) {
            throw new ArithmeticException("Division by zero");
        }

        return left / right;
    }

    /**
     * Check if the plus or minus operation could be simplified to no-op when a temporal type is involved.
     * Specifically, when a YEAR-MONTH interval is added to a TIME datatype, the operation is always no-op.
     *
     * @param operand1 first operand
     * @param operand2 second operand
     * @return {@code true} if operation is no-op
     */
    public static boolean canSimplifyTemporalPlusMinus(Expression<?> operand1, Expression<?> operand2) {
        return operand1.getType() == TIME && operand2.getType() == INTERVAL_YEAR_MONTH;
    }
}
