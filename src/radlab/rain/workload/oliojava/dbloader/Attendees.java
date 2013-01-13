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
import radlab.rain.workload.oliojava.ScaleFactors;
import radlab.rain.workload.oliojava.UserName;

/**
 * Attendees Loader.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.Attendees} class
 * and adapted for the Java version of Olio.
 */
public class Attendees extends Loadable {
    // We use on average of 55 attendees per event. Random 10..100 Attendees.

    private static final String STATEMENT = "insert into " +
            "PERSON_SOCIALEVENT " +
            "(username, socialeventid) values (?, ?)";

    static Logger logger = Logger.getLogger(Attendees.class.getName());

    int eventId;
    String[] userNames;

    public String getClearStatement() {
        return "truncate table PERSON_SOCIALEVENT";
    }


    public void prepare() {
        eventId = getSequence();
        ++eventId;
        ThreadResource tr = ThreadResource.getInstance();
        Random r = tr.getRandom();
        int attendees = r.random(10, 100);
        LinkedHashSet<Integer> userIdSet = 
                                    new LinkedHashSet<Integer>(attendees);
        for (int i = 0; i <attendees; i++)
            while(!userIdSet.add(r.random(1, ScaleFactors.getLoadedUsers())));

        userNames = new String[userIdSet.size()];
        int idx = 0;
        for (int userId : userIdSet)
            userNames[idx++] = UserName.getUserName(userId);
    }


    public void load() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            for (String userName : userNames) {
                PreparedStatement s = c.prepareStatement(STATEMENT);
                s.setString(1, userName);
                s.setInt(2, eventId);
                c.addBatch();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
