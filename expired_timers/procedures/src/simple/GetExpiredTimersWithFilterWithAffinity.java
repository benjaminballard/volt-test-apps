/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package simple;

import org.voltdb.*;

public class GetExpiredTimersWithFilterWithAffinity extends VoltProcedure {

  public final SQLStmt GET_EXPIRED_TIMERS_WITH_FILTER_WITH_AFFINITY_SQL = new SQLStmt(
      "SELECT entry_id, timer_name, timer_value, affinity, context, document_id "+
      "FROM timer "+
      "WHERE "+
      "  TIMER.timer_value < NOW AND "+
      "  TIMER.timer_code >= ? AND "+
      "  TIMER.timer_code <= ?  AND "+
      "  TIMER.last_active_cluster_id IN ? AND "+
      "  TIMER.affinity = ? " +
      "LIMIT ?;");

    public VoltTable[] run(String partitionKey, int rangeStart, int rangeStop, String[] failoverFilter, String[] affinities, int limit) throws VoltAbortException {

        for (String affinity : affinities) {
            voltQueueSQL(GET_EXPIRED_TIMERS_WITH_FILTER_WITH_AFFINITY_SQL,
                         rangeStart,
                         rangeStop,
                         failoverFilter,
                         affinity,
                         limit
                         );
        }
        return voltExecuteSQL();
    }
}
