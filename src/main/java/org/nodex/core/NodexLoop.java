package org.nodex.core;

public class NodexLoop {
    public void run(final Completion callback) throws Exception {
        new NodexMain() {
            public void go() throws Exception {
                callback.onCompletion();
            }
        }.run();
    }
}
