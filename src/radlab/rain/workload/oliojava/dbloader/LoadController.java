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

import java.util.logging.Logger;
import radlab.rain.workload.oliojava.ScaleFactors;

import static radlab.rain.workload.oliojava.dbloader.framework.Loader.*;

/**
 * Load controller.
 * <br/>
 * NOTE: Code based on {@code org.apache.olio.workload.loader.LoadController}
 * class and adapted for the Java version of Olio.
 */
public class LoadController {

    static Logger logger = Logger.getLogger(LoadController.class.getName());

    public static void main(String[] args) throws Exception {
        setJDBCDriverClassName(args[0]);
        setConnectionURL(args[1]);
        ScaleFactors.setActiveUsers(Integer.parseInt(args[2]));

        // Clear the database
        clear(Person.class);
        clear(Friends.class);
        clear(Address.class);
        clear(Tag.class);
        clear(SocialEvent.class);
        clear(EventTag.class);
        clear(Attendees.class);
        clear(Comments.class);
        clear(Invitation.class);
        clear(IDGen.class);
        logger.info("Done clearing database tables.");

        // load person, friends, and addresses
        load(Person.class, ScaleFactors.getLoadedUsers());
        load(Friends.class, ScaleFactors.getLoadedUsers());
        load(Address.class, ScaleFactors.getLoadedUsers());
        load(Invitation.class, ScaleFactors.getLoadedUsers());

        // load tags
        load(Tag.class, ScaleFactors.getTags());

        // load events and all relationships to events
        load(SocialEvent.class, ScaleFactors.events);
        load(EventTag.class, ScaleFactors.events);
        load(Attendees.class, ScaleFactors.events);
        load(Comments.class, ScaleFactors.events);

        waitProcessing();
        logger.info("Done data creation.");

        // Now we need to check that all loading is done.
        shutdown();
        logger.info("Done data loading.");

        // We use a new set of connections and thread pools for postLoad.
        // This is to ensure all load tasks are done before this one starts.
        postLoad(Tag.class);
        postLoad(Address.class);
        postLoad(Person.class);
        postLoad(SocialEvent.class);
        postLoad(Comments.class);
        postLoad(Invitation.class);

        shutdown();
        logger.info("Done post-load.");
        System.exit(0); // Signal successful loading.
    }
}
