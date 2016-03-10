/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.shield.triggers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.Date;
import java.net.InetAddress;
import java.util.UUID;

import org.apache.cassandra.config.CFMetaData;

import org.apache.cassandra.db.*;
import org.apache.cassandra.db.composites.CellName;
import org.apache.cassandra.triggers.ITrigger;
import org.apache.cassandra.utils.ByteBufferUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateHubWriteTime implements ITrigger
{
    private static final Logger logger = LoggerFactory.getLogger(UpdateHubWriteTime.class);

    public Collection<Mutation> augment(ByteBuffer key, ColumnFamily update)
    {
//        logger.info("SHIELD: Made it into the Trigger");
//        logger.info("SHIELD: update is: " + update.toString());

        String fieldName = "hub_ts";
        Date ts = new Date();

        modifyUpdate(key, update, fieldName, ts);

        return null;
    }

    protected void modifyUpdate(ByteBuffer key, ColumnFamily update, String column, Object value)
    {
        final CFMetaData cfm = update.metadata();
        final List<Cell> cellsOut = new ArrayList<Cell>();

        Iterator<Cell> cells = update.iterator();
        while (cells.hasNext())
        {
            Cell cell = cells.next();
            CellName cellName = cell.name();
            // the actual (single) update for the cell
            String cqlCellName = cellName.cql3ColumnName(cfm).toString();

            Long ts = System.currentTimeMillis();

            if (column.equals(cqlCellName))
            {
//                if (value != null)
                    cellsOut.add(AbstractCell.create(cellName, getBytes(ts), ts, 0, cfm));
            }
            else
                cellsOut.add(cell);
        }
        update.clear();
        for (Cell cell : cellsOut)
            update.addColumn(cell);
    }

    protected ByteBuffer getBytes(Object obj)
    {
        if (obj instanceof Double)
            return ByteBufferUtil.bytes((double) obj);
        else if (obj instanceof Float)
            return ByteBufferUtil.bytes((float) obj);
        else if (obj instanceof InetAddress)
            return ByteBufferUtil.bytes((InetAddress) obj);
        else if (obj instanceof Integer)
            return ByteBufferUtil.bytes((int) obj);
        else if (obj instanceof Long)
            return ByteBufferUtil.bytes((long) obj);
        else if (obj instanceof Date)
            return ByteBufferUtil.bytes(((Date) obj).getTime());
        else if (obj instanceof String)
            return ByteBufferUtil.bytes((String) obj);
        else if (obj instanceof UUID)
            return ByteBufferUtil.bytes((UUID) obj);

        return ByteBuffer.allocate(0); //null
    }

}