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
import java.util.logging.Level;
import java.util.logging.Logger;
import radlab.rain.workload.oliojava.dbloader.framework.Loadable;
import radlab.rain.workload.oliojava.dbloader.framework.Loader;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadConnection;
import radlab.rain.workload.oliojava.ScaleFactors;
import radlab.rain.workload.oliojava.UserName;

/**
 * The tag loader.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.Tag}
 * class and adapted for the Java version of Olio.
 */
public class Tag extends Loadable {

    // Note that the tag id in the database is autoincrement and may
    // not coincide with this tag id/name when using multi-thread loading.
    private static final String STATEMENT = "insert into SOCIALEVENTTAG " +
            //"(socialeventtagid, tag, refcount) values (?, ?, ?)";
            "(tag, refcount) values (?, ?)";

    static Logger logger = Logger.getLogger(Tag.class.getName());

    String tag;

   

    public String getClearStatement() {
        return "truncate table SOCIALEVENTTAG ";
    }

    public void prepare() {
        int id = getSequence();
        ++id;
        tag = UserName.getUserName(id);
    }


    public void load() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            PreparedStatement s = c.prepareStatement(STATEMENT);
            s.setString(1, tag);
            s.setInt(2, 0); // Initialize it to 0 first, count and add later.
            c.addBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            Loader.increaseErrorCount();
        }
    }

    /**
     * For tags, we won't know the refcount till all the data is loaded.
     * So we update the table at postload.
     */
    public void postLoad() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            c.prepareStatement("update SOCIALEVENTTAG set refcount = " +
                    "(select count(*) from SOCIALEVENTTAG_SOCIALEVENT " +
                    "where socialeventtagid = " +
                    "SOCIALEVENTTAG.socialeventtagid)");
            c.executeUpdate();
            //update id
            
            logger.fine("updating Tag ID_GEN ID");
             /* 
             c.prepareStatement("update ID_GEN set GEN_VALUE = " + 
                    "(select count(*) +1 from SOCIALEVENTTAG) " +
                    "where GEN_KEY='SOCIAL_EVENT_TAG_ID'"); 
             c.executeUpdate();
             */                        
             c.prepareStatement("INSERT INTO ID_GEN " +
                    "(GEN_KEY, GEN_VALUE) " +
                    "VALUES ('SOCIAL_EVENT_TAG_ID', "+ ScaleFactors.getTags() + ")");
             c.executeUpdate();
             
            logger.fine("After updating Tag ID_GEN ID");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }


    }
}
