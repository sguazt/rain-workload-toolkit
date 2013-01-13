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
import radlab.rain.workload.oliojava.dbloader.framework.ThreadConnection;
import radlab.rain.workload.oliojava.dbloader.framework.ThreadResource;
import radlab.rain.workload.oliojava.Random;
import radlab.rain.workload.oliojava.RandomUtil;
import radlab.rain.workload.oliojava.ScaleFactors;
import radlab.rain.workload.oliojava.UserName;


/**
 * Person loader.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.Person}
 * class and adapted for the Java version of Olio.
 */
public class Person extends Loadable {

    private static final String STATEMENT = "insert into PERSON (username, " +
            "password, firstname, lastname, email, telephone, imageurl, " +
            "imagethumburl, summary, timezone, ADDRESS_addressid)" +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    static Logger logger = Logger.getLogger(Person.class.getName());

    String[] fields = new String[10];
    int addressId;
  
    public String getClearStatement() {
        return "truncate table PERSON";
    }

    public synchronized void prepare() {
        int id = getSequence();
        ++id;
        ThreadResource tr = ThreadResource.getInstance();
        Random r = tr.getRandom();
        StringBuilder b = tr.getBuffer();
        fields[0] = UserName.getUserName(id);
        fields[1] = String.valueOf(id);
        fields[2] = RandomUtil.randomName(r, b, 2, 12).toString();
        b.setLength(0);
        fields[3] = RandomUtil.randomName(r, b, 5, 15).toString();
        fields[4] = r.makeCString(3, 10);
        fields[4] = fields[2] + '_' + fields[3] + '@' + fields[4] + ".com";
        b.setLength(0);
        fields[5] = RandomUtil.randomPhone(r, b);
        fields[6] = "p" + id + ".jpg";
        fields[7] = "p" + id + "t.jpg";
        fields[8] = RandomUtil.randomText(r, 250, 2500);
        fields[9] = "PST";
        addressId = r.random(1, ScaleFactors.getLoadedUsers());
    }

    public void load() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            PreparedStatement s = c.prepareStatement(STATEMENT);
            for (int i = 0; i < fields.length; i++)
                s.setString(i + 1, fields[i]);
            s.setInt(11, addressId);
            c.addBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
     /**
     * Update ID after all the data is loaded.
     * So we update the ID_GEN table at postload and add 1 to count.
     */
    public void postLoad() {
        ThreadConnection c = ThreadConnection.getInstance();
        try {
            //update id
             logger.fine("Updating Person ID");
              
             c.prepareStatement("INSERT INTO ID_GEN " +
                    "(GEN_KEY, GEN_VALUE) " +
                    "VALUES ('PERSON_ID', "+ ScaleFactors.getLoadedUsers()+1  + ")");
             c.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            //LoadController.increaseErrorCount();
        }


    }
}
