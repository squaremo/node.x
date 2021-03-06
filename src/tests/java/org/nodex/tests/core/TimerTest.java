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

package org.nodex.tests.core;

import org.nodex.core.Actor;
import org.nodex.core.Nodex;
import org.nodex.core.NodexMain;
import org.nodex.core.buffer.Buffer;
import org.nodex.core.buffer.DataHandler;
import org.nodex.core.net.NetClient;
import org.nodex.core.net.NetConnectHandler;
import org.nodex.core.net.NetServer;
import org.nodex.core.net.NetSocket;
import org.testng.annotations.Test;
import org.nodex.tests.Utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimerTest extends TestBase {

  @Test
  /*
  Test that can't set a timer without a context
   */
  public void testNoContext() throws Exception {
    final AtomicBoolean fired = new AtomicBoolean(false);
    try {
      Nodex.instance.setTimeout(1, new Runnable() {
        public void run() {
          fired.set(true);
        }
      });
      assert false : "Should throw Exception";
    } catch (IllegalStateException e) {
      //OK
    }
    Thread.sleep(100);
    assert !fired.get();
  }

  @Test
  /*
  Test that timers can be set from connect handler
   */
  public void testOneOffInConnect() throws Exception {
    final CountDownLatch endLatch = new CountDownLatch(1);

    new NodexMain() {
      public void go() throws Exception {
        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                endLatch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            final Thread th = Thread.currentThread();
            final long contextID = Nodex.instance.getContextID();
            Nodex.instance.setTimeout(1, new Runnable() {
              public void run() {
                azzert(th == Thread.currentThread());
                azzert(contextID == Nodex.instance.getContextID());
                Nodex.instance.sendMessage(actorId, "foo");
              }
            });
          }
        }).listen(8181);

        NetClient client = new NetClient();
        client.connect(8181, new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
          }
        });
      }
    }.run();

    azzert(endLatch.await(5, TimeUnit.SECONDS));
    throwAssertions();
  }

  @Test
  /*
  Test that timers can be set from dataHandler handler
   */
  public void testOneOffInData() throws Exception {
    final CountDownLatch endLatch = new CountDownLatch(1);

    new NodexMain() {
      public void go() throws Exception {
        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                endLatch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            final Thread th = Thread.currentThread();
            final long contextID = Nodex.instance.getContextID();
            sock.dataHandler(new DataHandler() {
              public void onData(Buffer data) {
                Nodex.instance.setTimeout(1, new Runnable() {
                  public void run() {
                    assert th == Thread.currentThread();
                    assert contextID == Nodex.instance.getContextID();
                    Nodex.instance.sendMessage(actorId, "foo");
                  }
                });
              }
            });

          }
        }).listen(8181);

        NetClient client = new NetClient();
        client.connect(8181, new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            sock.write(Utils.generateRandomBuffer(100));
          }
        });
      }
    }.run();

    azzert(endLatch.await(5, TimeUnit.SECONDS));
    throwAssertions();
  }

  @Test
  /*
  Test the timers fire with approximately the correct delay
   */
  public void testTimings() throws Exception {
    final CountDownLatch endLatch = new CountDownLatch(1);

    new NodexMain() {
      public void go() throws Exception {
        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                endLatch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            final Thread th = Thread.currentThread();
            final long contextID = Nodex.instance.getContextID();
            final long start = System.nanoTime();
            final long delay = 100;
            Nodex.instance.setTimeout(delay, new Runnable() {
              public void run() {
                long dur = (System.nanoTime() - start) / 1000000;
                azzert(dur >= delay);
                azzert(dur < delay * 1.25); // 25% margin of error
                azzert(th == Thread.currentThread());
                azzert(contextID == Nodex.instance.getContextID());
                Nodex.instance.sendMessage(actorId, "foo");
              }
            });
          }
        }).listen(8181);

        NetClient client = new NetClient();
        client.connect(8181, new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
          }
        });
      }
    }.run();

    azzert(endLatch.await(5, TimeUnit.SECONDS));
    throwAssertions();
  }

}
