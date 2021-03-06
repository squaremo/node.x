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

package org.nodex.tests.core.net;

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
import org.nodex.tests.core.TestBase;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NetTest extends TestBase {

  @Test
  public void testConnect() throws Exception {

    final int connectCount = 10;
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicInteger totalConnectCount = new AtomicInteger(0);


    new NodexMain() {
      public void go() throws Exception {

        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                latch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            if (totalConnectCount.incrementAndGet() == 2 * connectCount) {
              Nodex.instance.sendMessage(actorId, "foo");
            }
          }
        }).listen(8181);


        NetClient client = new NetClient();

        for (int i = 0; i < connectCount; i++) {
          client.connect(8181, new NetConnectHandler() {
            public void onConnect(NetSocket sock) {
              sock.close();
              if (totalConnectCount.incrementAndGet() == 2 * connectCount) {
                Nodex.instance.sendMessage(actorId, "foo");
              }
            }
          });
        }
      }
    }.run();

    azzert(latch.await(5, TimeUnit.SECONDS));

    throwAssertions();
  }

  @Test
  /*
  Test setting all the server params
  Actually quite hard to test this meaningfully
   */
  public void testServerParams() throws Exception {
    //TODO
  }

  @Test
  /*
  Test setting all the client params
  Actually quite hard to test this meaningfully
   */
  public void testClientParams() throws Exception {
    //TODO
  }

  @Test
  public void testCloseHandlerCloseFromClient() throws Exception {
    testCloseHandler(true);
  }

  @Test
  public void testCloseHandlerCloseFromServer() throws Exception {
    testCloseHandler(false);
  }

  @Test
  public void testSendDataClientToServerString() throws Exception {
    testSendData(true, true);
  }

  @Test
  public void testSendDataServerToClientString() throws Exception {
    testSendData(false, true);
  }

  @Test
  public void testSendDataClientToServerBytes() throws Exception {
    testSendData(true, false);
  }

  @Test
  public void testSendDataServerToClientBytes() throws Exception {
    testSendData(false, false);
  }

  @Test
  /*
  Test writing with a completion
   */
  public void testWriteWithCompletion() throws Exception {

    final CountDownLatch latch = new CountDownLatch(1);
    final int numSends = 10;
    final int sendSize = 100;

    new NodexMain() {
      public void go() throws Exception {

        final NetServer server = new NetServer();

        final Buffer sentBuff = Buffer.create(0);
        final Buffer receivedBuff = Buffer.create(0);

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            azzert(Utils.buffersEqual(sentBuff, receivedBuff));
            server.close(new Runnable() {
              public void run() {
                latch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            sock.dataHandler(new DataHandler() {
              public void onData(Buffer data) {
                receivedBuff.appendBuffer(data);
                if (receivedBuff.length() == numSends * sendSize) {
                  Nodex.instance.sendMessage(actorId, "foo");
                }
              }
            });
          }
        }).listen(8181);

        NetClient client = new NetClient().connect(8181, new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            final ContextChecker checker = new ContextChecker();
            doWrite(sentBuff, sock, numSends, sendSize, checker);
          }
        });
      }
    }.run();


    azzert(latch.await(5, TimeUnit.SECONDS));

    throwAssertions();
  }

  @Test
  public void testSendFileClientToServer() throws Exception {
    testSendFile(true);
  }

  @Test
  public void testSendFileServerToClient() throws Exception {
    testSendFile(false);
  }

  private void testSendFile(final boolean clientToServer) throws Exception {
    final String path = "foo.txt";

    final String content = Utils.randomAlphaString(10000);
    final File file = setupFile(path, content);
    final CountDownLatch latch = new CountDownLatch(1);

    new NodexMain() {
      public void go() throws Exception {
        NetConnectHandler sender = new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            String fileName = "./" + path;
            sock.sendFile(fileName);
          }
        };

        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                latch.countDown();
              }
            });
          }
        });

        NetConnectHandler receiver = new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            final Buffer buff = Buffer.create(0);
            sock.dataHandler(new DataHandler() {
              public void onData(final Buffer data) {
                buff.appendBuffer(data);
                if (buff.length() == file.length()) {
                  azzert(content.equals(buff.toString()));
                  Nodex.instance.sendMessage(actorId, "foo");
                }
              }
            });
          }
        };

        NetConnectHandler serverHandler = clientToServer ? receiver: sender;
        NetConnectHandler clientHandler = clientToServer ? sender: receiver;

        server.connectHandler(serverHandler).listen(8181);
        new NetClient().connect(8181, clientHandler);
      }
    }.run();

    assert latch.await(5, TimeUnit.SECONDS);
    throwAssertions();
    file.delete();
  }

  //Recursive - we don't write the next packet until we get the completion back from the previous write
  private void doWrite(final Buffer sentBuff, final NetSocket sock, int count, final int sendSize,
                       final ContextChecker checker) {
    Buffer b = Utils.generateRandomBuffer(sendSize);
    sentBuff.appendBuffer(b);
    count--;
    final int c = count;
    if (count == 0) {
      sock.write(b);
    } else {
      sock.write(b, new Runnable() {
        public void run() {
          checker.check();
          doWrite(sentBuff, sock, c, sendSize, checker);
        }
      });
    }
  }

  private void testSendData(final boolean clientToServer, final boolean string) throws Exception {

    final CountDownLatch latch = new CountDownLatch(1);
    final int numSends = 10;
    final int sendSize = 100;

    new NodexMain() {
      public void go() throws Exception {

        final NetServer server = new NetServer();

        final Buffer sentBuff = Buffer.create(0);
        final Buffer receivedBuff = Buffer.create(0);

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            azzert(Utils.buffersEqual(sentBuff, receivedBuff));
            server.close(new Runnable() {
              public void run() {
                latch.countDown();
              }
            });
          }
        });

        NetConnectHandler receiver = new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            final ContextChecker checker = new ContextChecker();
            sock.dataHandler(new DataHandler() {
              public void onData(Buffer data) {
                checker.check();
                receivedBuff.appendBuffer(data);
                if (receivedBuff.length() == numSends * sendSize) {
                  sock.close();
                  Nodex.instance.sendMessage(actorId, "foo");
                }
              }
            });
          }
        };

        NetConnectHandler sender = new NetConnectHandler() {
          public void onConnect(NetSocket sock) {
            for (int i = 0; i < numSends; i++) {
              if (string) {
                byte[] bytes = new byte[sendSize];
                Arrays.fill(bytes, (byte) 'X');
                try {
                  String s = new String(bytes, "UTF-8");
                  sentBuff.appendBytes(bytes);
                  sock.write(s);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              } else {
                Buffer b = Utils.generateRandomBuffer(sendSize);
                sentBuff.appendBuffer(b);
                sock.write(b);
              }
            }
          }
        };
        NetConnectHandler serverHandler = clientToServer ? receiver : sender;
        NetConnectHandler clientHandler = clientToServer ? sender : receiver;

        server.connectHandler(serverHandler).listen(8181);
        new NetClient().connect(8181, clientHandler);
      }
    }.run();

    azzert(latch.await(5, TimeUnit.SECONDS));
    throwAssertions();
  }


  private void testCloseHandler(final boolean closeClient) throws Exception {
    final int connectCount = 10;

    final AtomicInteger clientCloseCount = new AtomicInteger(0);
    final AtomicInteger serverCloseCount = new AtomicInteger(0);
    final CountDownLatch clientCloseLatch = new CountDownLatch(1);
    final CountDownLatch serverCloseLatch = new CountDownLatch(1);

    new NodexMain() {
      public void go() throws Exception {

        final NetServer server = new NetServer();

        final long actorId = Nodex.instance.registerActor(new Actor<String>() {
          public void onMessage(String msg) {
            server.close(new Runnable() {
              public void run() {
                serverCloseLatch.countDown();
              }
            });
          }
        });

        server.connectHandler(new NetConnectHandler() {
          public void onConnect(final NetSocket sock) {
            final ContextChecker checker = new ContextChecker();
            sock.closedHandler(new Runnable() {
              public void run() {
                checker.check();
                if (serverCloseCount.incrementAndGet() == connectCount) {
                  Nodex.instance.sendMessage(actorId, "foo");
                }
              }
            });
            if (!closeClient) sock.close();
          }
        }).listen(8181);

        NetClient client = new NetClient();

        for (int i = 0; i < connectCount; i++) {
          client.connect(8181, new NetConnectHandler() {
            public void onConnect(NetSocket sock) {
              final ContextChecker checker = new ContextChecker();
              sock.closedHandler(new Runnable() {
                public void run() {
                  checker.check();
                  if (clientCloseCount.incrementAndGet() == connectCount) {
                    clientCloseLatch.countDown();
                  }
                }
              });
              if (closeClient) sock.close();
            }
          });
        }
      }
    }.run();

    azzert(serverCloseLatch.await(5, TimeUnit.SECONDS));
    azzert(clientCloseLatch.await(5, TimeUnit.SECONDS));
    throwAssertions();
  }
}
