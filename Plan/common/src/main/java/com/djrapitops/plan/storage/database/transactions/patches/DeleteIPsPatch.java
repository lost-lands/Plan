/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.storage.database.transactions.patches;

import com.djrapitops.plan.storage.database.queries.QueryAllStatement;
import com.djrapitops.plan.storage.database.sql.tables.GeoInfoTable;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.djrapitops.plan.storage.database.sql.parsing.Sql.*;

/**
 * Patch that replaces plan_ips with plan_geolocations table.
 *
 * @author Rsl1122
 */
public class DeleteIPsPatch extends Patch {

    private final String tempTableName;

    public DeleteIPsPatch() {
        tempTableName = "temp_ips";
    }

    @Override
    public boolean hasBeenApplied() {
        return !hasTable("plan_ips");
    }

    @Override
    protected void applyPatch() {
        if (hasTable(GeoInfoTable.TABLE_NAME) && hasLessDataInPlanIPs()) {
            dropTable("plan_ips");
            return;
        }
        tempOldTable();

        dropTable(GeoInfoTable.TABLE_NAME);
        execute(GeoInfoTable.createTableSQL(dbType));

        execute("INSERT INTO " + GeoInfoTable.TABLE_NAME + " (" +
                GeoInfoTable.USER_UUID + ',' +
                GeoInfoTable.LAST_USED + ',' +
                GeoInfoTable.GEOLOCATION +
                ") SELECT " + DISTINCT +
                GeoInfoTable.USER_UUID + ',' +
                GeoInfoTable.LAST_USED + ',' +
                GeoInfoTable.GEOLOCATION +
                FROM + tempTableName
        );

        dropTable(tempTableName);
    }

    private boolean hasLessDataInPlanIPs() {
        Integer inIPs = query(new QueryAllStatement<Integer>(SELECT + "COUNT(1) as c" + FROM + "plan_ips") {
            @Override
            public Integer processResults(ResultSet set) throws SQLException {
                return set.next() ? set.getInt("c") : 0;
            }
        });
        Integer inGeoInfo = query(new QueryAllStatement<Integer>(SELECT + "COUNT(1) as c" + FROM + GeoInfoTable.TABLE_NAME) {
            @Override
            public Integer processResults(ResultSet set) throws SQLException {
                return set.next() ? set.getInt("c") : 0;
            }
        });
        return inIPs <= inGeoInfo;
    }

    private void tempOldTable() {
        if (!hasTable(tempTableName)) {
            renameTable("plan_ips", tempTableName);
        }
    }
}