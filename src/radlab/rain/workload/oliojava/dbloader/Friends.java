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
import java.util.logging.Logger;
import java.util.logging.Level;
import radlab.rain.workload.oliojava.dbloader.framework.Loadable;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadConnection;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadResource;
import radlab.rain.workload.oliojava.Random;
import radlab.rain.workload.oliojava.ScaleFactors;
import radlab.rain.workload.oliojava.UserName;

/**
 * Friends loader.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.Friends} class
 * and adapted for the Java version of Olio.
 */
public class Friends extends Loadable {
    // We use on average of 15 friends. Random 2..28 Friends.

    private static final String STATEMENT = "insert into PERSON_PERSON " +
            "(Person_userid, friends_userid) values (?, ?)";

    static Logger logger = Logger.getLogger(Friends.class.getName());

    int id;
    //String userName;
    //String[] friends;
    LinkedHashSet<Integer> friendSet;
    public String getClearStatement() {
        return "truncate table PERSON_PERSON";
    }

    public void prepare() {
        id = getSequence();
        ++id;
        ThreadResource tr = ThreadResource.getInstance();
        Random r = tr.getRandom();
        //userName = UserName.getUserName(id);
        int count = r.random(2, 28);
	    friendSet = new LinkedHashSet<Integer>(count);

        for (int i = 0; i < count; i++) {
            int friendId;
            do { // Prevent friend to be the same user.
                friendId = r.random(1, ScaleFactors.getLoadedUsers());
            } while (friendId == id || !friendSet.add(friendId));
        }

        //friends = new String[friendSet.size()];
        //int idx = 0;
        //for (int friendId : friendSet)
         //   friends[idx++] = UserName.getUserName(friendId);
    }

    public void load() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
        //    for (String friend : friends) {
			  for (Integer friend : friendSet) {
                PreparedStatement s = c.prepareStatement(STATEMENT);
                //s.setString(1, userName);
                //s.setString(2, friend);
				s.setInt(1, id);
				s.setInt(2, friend);
                c.addBatch();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            //LoadController.increaseErrorCount();
        }
    }
}
