 /*
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
 *
 * Modifications by: Marco Guazzone
 * 1) Adapted to the Java version of Olio.
 * 2) Added note to class doc about the origin of the code.
 */
//package org.apache.olio.workload.loader;
 package radlab.rain.workload.oliojava.dbloader;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import radlab.rain.workload.oliojava.dbloader.framework.Loadable;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadConnection;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadResource;
import radlab.rain.workload.oliojava.Random;
import radlab.rain.workload.oliojava.RandomUtil;

/**
 * The Tag-Event relationship loader.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.EventTag} class
 * and adapted for the Java version of Olio.
 */
public class EventTag extends Loadable{

    // We use on average of 3.5 tags per event. Random 1..6 tags.
    // Once we know the tag count, we have to select tags.

    private static final String STATEMENT = "insert into " +
            "SOCIALEVENTTAG_SOCIALEVENT " +
            "(socialeventtagid, socialeventid) values (?, ?)";

    static Logger logger = Logger.getLogger(EventTag.class.getName());

    int eventId;
    int [] tagIds;

    public String getClearStatement() {
        return "truncate table SOCIALEVENTTAG_SOCIALEVENT";
    }

    public void prepare() {
        eventId = getSequence();
        ++eventId;
        ThreadResource tr = ThreadResource.getInstance();
        Random r = tr.getRandom();
        int numTags = r.random(1, 7); // Avg is 4 tags per event
        LinkedHashSet<Integer> tagSet = new LinkedHashSet<Integer>(numTags);
        for (int i = 0; i < numTags; i++)
            while (!tagSet.add(RandomUtil.randomTagId(r, 0.1d)));

        tagIds = new int[tagSet.size()];
        int idx = 0;
        for (int tagId : tagSet)
            tagIds[idx++] = tagId;
    }


    public void load() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            for (int tagId : tagIds) {
                PreparedStatement s = c.prepareStatement(STATEMENT);
                s.setInt(1, tagId);
                s.setInt(2, eventId);
                c.addBatch();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
