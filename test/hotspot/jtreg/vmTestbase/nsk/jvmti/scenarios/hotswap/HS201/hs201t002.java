/*
 * Copyright (c) 2004, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jvmti.scenarios.hotswap.HS201;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

import nsk.share.*;
import nsk.share.jvmti.*;

public class hs201t002 extends DebugeeClass {


    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.hotswap.HS201";
    static final String TESTED_EXCEPTION_NAME = PACKAGE_NAME + ".hs201t002a";
    static final String PATH_TO_NEW_BYTECODE = "pathToNewByteCode";

    static final String METHOD_NAME = "doInit";
    static final int MAX_TRIES_TO_SUSPEND_THREAD = 10;

    public static volatile int currentStep = 0;

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new hs201t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    public static Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static native void setThread(Thread thread);
    static native boolean suspendThread(Thread thread);
    static native boolean resumeThread(Thread thread);
    static native boolean popFrame(Thread thread);

    // run debuggee
    public int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        log.display(">>> starting tested thread");
        hs201t002Thread thread = new hs201t002Thread();

        // testing sync
        status = checkStatus(status);

        thread.start();

        // setThread(thread) enables JVMTI events, and that can only be done on a live thread,
        // so wait until the thread has started.
        try {
            thread.ready.await();
        } catch (InterruptedException e) {
        }
        setThread(thread);

        thread.go.countDown();

        while (currentStep != 4) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        int suspendTry = 1;
        while ( true ) {
            suspendThread(thread);

            log.display("Checking that the thread is inside " + METHOD_NAME);

            StackTraceElement[] stackTrace = thread.getStackTrace();
            if ( stackTrace.length == 0 ) {
                log.complain("Thread has empty stack!");
                status = Consts.TEST_FAILED;
                break;
            }

            if ( stackTrace[0].getMethodName().equals(METHOD_NAME) ) {
                break;
            }

            if ( suspendTry > MAX_TRIES_TO_SUSPEND_THREAD ) {
                log.complain("Unable to suspend thread in method " + METHOD_NAME);
                for (int i = 0; i < stackTrace.length; i++) {
                    log.display("\t" + i + ". " + stackTrace[i]);
                }
                status = Consts.TEST_FAILED;
                break;
            }

            log.display("Thread suspended in a wrong moment. Retrying...");
            for (int i = 0; i < stackTrace.length; i++) {
                log.display("\t" + i + ". " + stackTrace[i]);
            }
            log.display("Retrying...");
            resumeThread(thread);
            suspendTry++;
            // Test thread will be suspended at the top of the loop. Let it run for a while.
            safeSleep(50);
        }

        popFrame(thread);

        try {
            thread.join();
        } catch (InterruptedException e) {
        }

        // testing sync
        log.display("Testing sync: thread finished");
        status = checkStatus(status);

        return status;
    }

class hs201t002Thread extends Thread {

    CountDownLatch ready = new CountDownLatch(1);
    CountDownLatch go = new CountDownLatch(1);

    hs201t002Thread() {
        setName("hs201t002Thread");
    }

    public void run() {
        // run method
        ready.countDown();
        try {
            go.await();
        } catch (InterruptedException e) {
        }
        try {
            throwException();
        } catch (Exception e) {
        }

    }

    void throwException() throws Exception {

        ClassUnloader unloader = new ClassUnloader();
        Class cls = null;
        String path = argHandler.findOptionValue(PATH_TO_NEW_BYTECODE)
                            + "/newclass";

        try {
            log.display("[debuggee] loading exception...");
            unloader.loadClass(TESTED_EXCEPTION_NAME, path);
            cls = unloader.getLoadedClass();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            log.display("[debuggee] throwing exception...");
            throw (Exception )cls.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
    }

}

}
