/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.nodex.examples.pubsub;

import org.nodex.core.Nodex;
import org.nodex.core.NodexMain;
import org.nodex.core.buffer.Buffer;
import org.nodex.core.buffer.DataHandler;
import org.nodex.core.net.NetConnectHandler;
import org.nodex.core.net.NetServer;
import org.nodex.core.net.NetSocket;
import org.nodex.core.parsetools.RecordParser;
import org.nodex.core.shared.SharedData;

import java.util.Set;

public class PubSubServer extends NodexMain {

  public static void main(String[] args) throws Exception {
    new PubSubServer().run();

    System.out.println("Hit enter to exit");
    System.in.read();
  }

  public void go() throws Exception {
    new NetServer(new NetConnectHandler() {
      public void onConnect(final NetSocket socket) {
        socket.dataHandler(RecordParser.newDelimited("\n", new DataHandler () {
          public void onData(Buffer frame) {
            String line = frame.toString().trim();
            String[] parts = line.split("\\,");
            if (line.startsWith("subscribe")) {
              Set<Long> set = SharedData.<Long>getSet(parts[1]);
              set.add(socket.writeActorID);
            } else if (line.startsWith("unsubscribe")) {
              SharedData.<Long>getSet(parts[1]).remove(socket.writeActorID);
            } else if (line.startsWith("publish")) {
              Set<Long> actorIDs = SharedData.getSet(parts[1]);
              for (Long actorID: actorIDs) {
                Nodex.instance.sendMessage(actorID, Buffer.create(parts[2]));
              }
            }
          }
        }));
      }
    }).listen(8080);
  }
}
