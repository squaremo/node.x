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

package org.nodex.examples.echo;

import org.nodex.core.NodexMain;
import org.nodex.core.buffer.Buffer;
import org.nodex.core.buffer.DataHandler;
import org.nodex.core.net.NetClient;
import org.nodex.core.net.NetConnectHandler;
import org.nodex.core.net.NetSocket;

public class EchoClient extends NodexMain {

  public static void main(String[] args) throws Exception {
    new EchoClient().run();

    System.out.println("Hit enter to exit");
    System.in.read();
  }

  public void go() throws Exception {
    new NetClient().connect(8080, "localhost", new NetConnectHandler() {
      public void onConnect(NetSocket socket) {

        socket.dataHandler(new DataHandler() {
          public void onData(Buffer buffer) {
            System.out.println("Net client receiving: " + buffer.toString("UTF-8"));
          }
        });

        //Now send some dataHandler
        for (int i = 0; i < 10; i++) {
          String str = "hello" + i + "\n";
          System.out.print("Net client sending: " + str);
          socket.write(Buffer.create(str));
        }
      }
    });
  }
}
