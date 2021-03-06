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

package org.nodex.mods.stomp;

import org.nodex.core.net.NetClient;
import org.nodex.core.net.NetConnectHandler;
import org.nodex.core.net.NetSocket;

public class StompClient {
  public static void connect(int port, final StompConnectHandler connectHandler) {
    connect(port, "localhost", null, null, connectHandler);
  }

  public static void connect(int port, String host, final StompConnectHandler connectHandler) {
    connect(port, host, null, null, connectHandler);
  }

  public static void connect(int port, String host, final String username, final String password,
                             final StompConnectHandler connectHandler) {
    new NetClient().connect(port, host, new NetConnectHandler() {
      public void onConnect(NetSocket sock) {
        final StompConnection conn = new StompConnection(sock);
        conn.connect(username, password, new Runnable() {
          public void run() {
            connectHandler.onConnect(conn);
          }
        });
      }
    });
  }
}


